// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.ftrace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfDynamic;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfRel;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfSectionHeader;
import lib.dwfl.ElfSymbol;
import lib.dwfl.ElfSymbolBinding;
import lib.dwfl.ElfSymbolType;
import lib.dwfl.ElfSymbolVisibility;

import lib.stdcpp.Demangler;

/**
 * What ltrace needs to know about each loaded executable or shared
 * library.
 */
public class ObjectFile
{
  private Map symbolMap = new HashMap();
  private List tracePoints = new ArrayList();
  private File filename;
  private String soname = null;
  private long baseAddress = 0;
  private long entryPoint = 0;
  private static HashMap cachedFiles = new HashMap();
  protected static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

  /**
   * Implement this interface to create an iterator over symbols
   * defined in this file.
   */
  public interface SymbolIterator {
    void symbol(Symbol symbol);
  }

  /**
   * Implement this interface to create an iterator over tracepoints
   * defined in this file.
   */
  public interface TracePointIterator {
    void tracePoint(TracePoint tracePoint);
  }

  protected ObjectFile(File file)
  {
    this.filename = file;
  }

  protected void addSymbol(Symbol symbol)
  {
    symbolMap.put(symbol.name, symbol);
    symbol.addedTo(this);
  }

  protected void addTracePoint(TracePoint tracePoint)
  {
    tracePoints.add(tracePoint);
  }

  /*
  public Symbol symbolAt(final long address)
  {
    // XXX: Huh, this is ugly.  Eventually either invent some observer
    // mechanism where ObjectFile can cache observed symbol addresses,
    // or, if it turns out that address almost never changes, make it
    // final or something.
    final LinkedList list = new LinkedList();
    this.eachSymbol(new SymbolIterator(){
	public void symbol(Symbol symbol) {
	  if (symbol.entryAddress == address)
	    list.add(symbol);
	}
      });

    if (list.isEmpty())
      return null;
    if (list.size() > 1)
      System.err.println("Strange: symbolAt(0x" + Long.toHexString(address) + ") has more than one symbol...");
    return (Symbol)list.getFirst();
  }
  */

  public void eachSymbol(SymbolIterator client)
  {
    int mapsize = symbolMap.size();
    Iterator it = symbolMap.entrySet().iterator();
    for (int i = 0; i < mapsize; i++)
      {
	Map.Entry entry = (Map.Entry)it.next();
	Symbol sym = (Symbol)entry.getValue();
	client.symbol(sym);
      }
  }

  public void eachTracePoint(TracePointIterator client)
  {
    for (Iterator it = tracePoints.iterator(); it.hasNext();)
      {
	TracePoint tp = (TracePoint)it.next();
	client.tracePoint(tp);
      }
  }

  protected void setSoname(String soname)
  {
    this.soname = soname;
  }

  /**
   * Either answer pre-set soname, or construct soname from
   * filename.
   */
  public String getSoname()
  {
    if (this.soname != null)
      return this.soname;
    else
      return this.filename.getName();
  }

  /**
   * Answer filename.
   */
  public File getFilename()
  {
    return this.filename;
  }

  /** Address of the first loadable segment of ELF file. */
  public long getBaseAddress()
  {
    return this.baseAddress;
  }

  /** Entry point address. */
  public long getEntryPoint()
  {
    return this.entryPoint;
  }

  private static ElfSection getElfSectionWithAddr(Elf elfFile, long addr)
  {
    for (ElfSection section = elfFile.getSection(0);
	 section != null;
	 section = elfFile.getNextSection(section))
      {
	ElfSectionHeader sheader = section.getSectionHeader();
	if (sheader.addr == addr)
	  return section;
      }
    return null;
  }

  public static ObjectFile buildFromFile(File filename)
  {
    logger.log(Level.FINE, "Loading object file `" + filename + "'");
    {
      ObjectFile objFile = (ObjectFile)cachedFiles.get(filename);
      if (objFile != null)
	{
	  logger.log(Level.FINE, "Retrieved from cache.");
	  return objFile;
	}
    }

    try
      {
	final Elf elfFile = new Elf(filename.getPath(), ElfCommand.ELF_C_READ);
	if (elfFile == null)
	  {
	    logger.log(Level.FINE, "Failed, probably not an ELF.");
	    return null;
	  }

	final ElfEHeader eh = elfFile.getEHeader();
	if (eh == null)
	  {
	    logger.log(Level.FINE, "Failed, couldn't get an ELF header.");
	    return null;
	  }

	boolean haveDynamic = false;
	boolean haveLoadable = false;
	boolean havePlt = false;
	boolean haveRelPlt = false;
	long offDynamic = 0;
	long baseAddress = 0;

	for (int i = 0; i < eh.phnum; ++i)
	  {
	    ElfPHeader ph = elfFile.getPHeader(i);
	    if (ph.type == ElfPHeader.PTYPE_DYNAMIC)
	      {
		haveDynamic = true;
		offDynamic = ph.offset;
		logger.log(Level.FINER, "Found DYNAMIC segment.");
	      }
	    else if (ph.type == ElfPHeader.PTYPE_LOAD
		     && ph.offset == 0)
	      {
		haveLoadable = true;
		baseAddress = ph.vaddr;
		logger.log(Level.FINER,
			   "Found LOADABLE segment, base address = 0x"
			   + Long.toHexString(baseAddress));
	      }
	  }

	if (!haveLoadable)
	  {
	    logger.log(Level.FINE, "Failed, didn't find any loadable segments.");
	    return null;
	  }

	final ObjectFile objFile = new ObjectFile(filename);
	objFile.baseAddress = baseAddress;
	objFile.entryPoint = eh.entry;

	if (eh.type == ElfEHeader.PHEADER_ET_EXEC)
	  logger.log(Level.FINER, "This file is EXECUTABLE.");
	else if (eh.type == ElfEHeader.PHEADER_ET_DYN)
	  logger.log(Level.FINER, "This file is DSO or PIE EXECUTABLE.");
	else
	  {
	    logger.log(Level.FINE, "Failed, unsupported ELF file type.");
	    return null;
	  }

	class Locals {
	  public ElfSection dynamicStrtab = null;
	  public ElfSection dynamicSymtab = null;
	  public ElfSection staticSymtab = null;

	  public ElfSection dynamicVersym = null;
	  public ElfSection dynamicVerdef = null;
	  public ElfSection dynamicVerneed = null;
	  public int dynamicVerdefCount = 0;
	  public int dynamicVerneedCount = 0;

	  public int dynamicSonameIdx = -1;
	  public long pltAddr = 0;
	  public long pltSize = 0;
	  public ElfRel[] pltRelocs = null;
	}
	final Locals locals = new Locals();
	boolean foundDynamic = false;

	// Find & interpret DYNAMIC section.
	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section))
	  {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (haveDynamic && sheader.offset == offDynamic)
	      {
		logger.log(Level.FINER, "Processing DYNAMIC section.");
		foundDynamic = true;
		ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
		    public void entry (int tag, long value)
		    {
		      if (tag == ElfDynamic.ELF_DT_STRTAB)
			{
			  logger.log(Level.FINEST, " * dynamic strtab at 0x" + Long.toHexString(value));
			  locals.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
			}
		      else if (tag == ElfDynamic.ELF_DT_SONAME)
			{
			  logger.log(Level.FINEST, " * soname index = 0x" + Long.toHexString(value));
			  locals.dynamicSonameIdx = (int)value;
			}
		      else if (tag == ElfDynamic.ELF_DT_SYMTAB)
			{
			  logger.log(Level.FINEST, " * dynamic symtab = 0x" + Long.toHexString(value));
			  locals.dynamicSymtab = getElfSectionWithAddr(elfFile, value);
			}
		      else if (tag == ElfDynamic.ELF_DT_VERSYM)
			{
			  logger.log(Level.FINEST, " * versym = 0x" + Long.toHexString(value));
			  locals.dynamicVersym = getElfSectionWithAddr(elfFile, value);
			}
		      else if (tag == ElfDynamic.ELF_DT_VERDEF)
			{
			  logger.log(Level.FINEST, " * verdef = 0x" + Long.toHexString(value));
			  locals.dynamicVerdef = getElfSectionWithAddr(elfFile, value);
			}
		      else if (tag == ElfDynamic.ELF_DT_VERDEFNUM)
			{
			  logger.log(Level.FINEST, " * verdefnum = " + Long.toString(value));
			  locals.dynamicVerdefCount = (int)value;
			}
		      else if (tag == ElfDynamic.ELF_DT_VERNEED)
			{
			  logger.log(Level.FINEST, " * verneed = 0x" + Long.toHexString(value));
			  locals.dynamicVerneed = getElfSectionWithAddr(elfFile, value);
			}
		      else if (tag == ElfDynamic.ELF_DT_VERNEEDNUM)
			{
			  logger.log(Level.FINEST, " * verneednum = " + Long.toString(value));
			  locals.dynamicVerneedCount = (int)value;
			}
		    }
		});
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_PROGBITS
		      || sheader.type == ElfSectionHeader.ELF_SHT_NOBITS)
		     && sheader.name.equals(".plt"))
	      {
		logger.log(Level.FINER, "Found PLT section.");
		havePlt = true;
		locals.pltAddr = sheader.addr;
		locals.pltSize = sheader.size;
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_REL
		      && sheader.name.equals(".rel.plt"))
		     || (sheader.type == ElfSectionHeader.ELF_SHT_RELA
			 && sheader.name.equals(".rela.plt")))
	      {
		logger.log(Level.FINER, "Found PLT relocation section.");
		haveRelPlt = true;
		locals.pltRelocs = ElfRel.loadFrom(section);
	      }
	    else if (sheader.type == ElfSectionHeader.ELF_SHT_SYMTAB)
	      {
		if (locals.staticSymtab != null)
		  throw new lib.dwfl.ElfFileException("Strange: More than one static symbol tables.");
		logger.log(Level.FINER, "Found static symtab section `" + sheader.name + "'.");
    		locals.staticSymtab = section;
	      }
	  }

	if (haveDynamic)
	  {
	    // Elf consistency sanity checks.
	    if (!foundDynamic)
	      throw new lib.dwfl.ElfFileException("DYNAMIC section not found in ELF file.");
	    if (!havePlt)
	      throw new lib.dwfl.ElfFileException("No (suitable) .plt found in ELF file.");
	    if (!haveRelPlt)
	      throw new lib.dwfl.ElfFileException("No (suitable) .rel.plt found in ELF file.");
	    if (locals.dynamicSymtab == null)
	      throw new lib.dwfl.ElfFileException("Couldn't get SYMTAB from DYNAMIC section.");
	    if (locals.dynamicStrtab == null)
	      throw new lib.dwfl.ElfFileException("Couldn't get STRTAB from DYNAMIC section.");
	    if ((locals.dynamicVerneed != null || locals.dynamicVerdef != null) && locals.dynamicVersym == null)
	      throw new lib.dwfl.ElfFileException("Versym section missing when verdef or verneed present.");
	    if (locals.dynamicVerneed == null && locals.dynamicVerdef == null && locals.dynamicVersym != null)
	      throw new lib.dwfl.ElfFileException("Versym section present when neither verdef nor verneed present.");
	    if (locals.dynamicVerdefCount != 0 && locals.dynamicVerdef == null)
	      throw new lib.dwfl.ElfFileException("Strange: VERDEFNUM tag present, but not VERDEF.");
	    if (locals.dynamicVerneedCount != 0 && locals.dynamicVerneed == null)
	      throw new lib.dwfl.ElfFileException("Strange: VERNEEDNUM tag present, but not VERNEED.");
	  }

	// List of symbols in the order in which it's defined in
    	// symbol table.  All symbols from dynamic symbol table are
    	// stored before any symbol from static symbol table, which is
    	// necessary for correct PLT references.
	final ArrayList symbolList = new ArrayList();

	// All-purpose builder for ftrace Symbols and TracePoints.
	// The member variable ORIGIN is used to track what type of
	// tracepoint is being defined.
	class Builder implements ElfSymbol.Builder {
	    Map symbolMap = new HashMap();
	    public TracePointOrigin origin = null;
	    public void symbol (String name, long value, long size,
				ElfSymbolType type, ElfSymbolBinding bind,
				ElfSymbolVisibility visibility, long shndx,
				List versions)
	    {
	      Symbol sym = (Symbol)symbolMap.get(name);
	      if (sym == null)
		{
		  String dName = Demangler.demangle(name);
		  logger.log(Level.FINEST, "Got new symbol `" + dName + "'.");
		  sym = new Symbol(dName, type, value, size, shndx, versions);
		  symbolMap.put(name, sym);
		  objFile.addSymbol(sym);
		  if (type == ElfSymbolType.ELF_STT_FUNC)
		    this.addNewTracepoint(value, sym);
		}
	      symbolList.add(sym);
	    }

	    public void addNewTracepoint(long address, Symbol symbol)
	    {
	      TracePoint tp = new TracePoint(address, symbol, this.origin);
	      objFile.addTracePoint(tp);
	    }
	  }
	Builder builder = new Builder();

	// Load dynamic symtab and PLT entries.
	if (haveDynamic)
	  {
	    logger.log(Level.FINER, "Loading dynamic symtab.");
	    builder.origin = TracePointOrigin.DYNAMIC;
	    ElfSymbol.loadFrom(locals.dynamicSymtab, locals.dynamicVersym,
			       locals.dynamicVerdef, locals.dynamicVerdefCount,
			       locals.dynamicVerneed, locals.dynamicVerneedCount,
			       builder);

	    logger.log(Level.FINER, "Loading PLT entries.");
	    builder.origin = TracePointOrigin.PLT;
	    long pltEntrySize = locals.pltSize / (locals.pltRelocs.length + 1);
	    for (int i = 0; i < locals.pltRelocs.length; ++i)
	      /* XXX HACK: 386 specific.  In general we want
	       * platform-independent way of asking whether it's
	       * JMP_SLOT relocation. */
	      if (locals.pltRelocs[i].type == 7)
		{
		  long pltEntryAddr = locals.pltAddr + pltEntrySize * (i + 1);
		  long symbolIndex = locals.pltRelocs[i].symbolIndex;
		  Symbol symbol = (Symbol)symbolList.get((int)symbolIndex - 1);
		  logger.log(Level.FINEST,
			     "Got plt entry for `" + symbol.name
			     + "' at 0x" + Long.toHexString(pltEntryAddr) + ".");
		  builder.addNewTracepoint(pltEntryAddr, symbol);
		}
	  }

	// Load static symtab, if there was one.
	if (locals.staticSymtab != null)
	  {
	    logger.log(Level.FINER, "Loading static symtab.");
	    builder.origin = TracePointOrigin.SYMTAB;
	    ElfSymbol.loadFrom(locals.staticSymtab, builder);
	  }

	// Read SONAME, if there was one.
	if (locals.dynamicSonameIdx != -1)
	  {
	    logger.log(Level.FINER, "Reading SONAME.");
	    ElfData data = locals.dynamicStrtab.getData();
	    byte[] bytes = data.getBytes();
	    int startIndex = locals.dynamicSonameIdx;
	    int endIndex = startIndex;
	    while (bytes[endIndex] != 0)
	      ++endIndex;
	    String name = new String(bytes, startIndex, endIndex - startIndex);
	    objFile.setSoname(name);
	    logger.log(Level.FINEST, "Found SONAME `" + name + "'.");
	  }

	cachedFiles.put(filename, objFile);
	logger.log(Level.FINE, "Loading finished successfully.");
	return objFile;
      }
    catch (lib.dwfl.ElfFileException efe)
      {
	efe.printStackTrace();
	System.err.println("load error: " + efe);
      }
    catch (lib.dwfl.ElfException eexp)
      {
	eexp.printStackTrace();
	System.err.println("load error: " + eexp);
      }

    return null;
  }
}
