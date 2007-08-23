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
  private String filename;
  private String soname = null;
  private long baseAddress = 0;
  private static HashMap cachedFiles = new HashMap();

  /**
   * Implement this interface to create an iterator over symbols
   * defined in this file.
   */
  public interface SymbolIterator {
    void symbol(Symbol symbol);
  }

  protected ObjectFile (String name)
  {
    this.filename = name;
  }

  protected void addSymbol(Symbol symbol)
  {
    symbolMap.put(symbol.name, symbol);
    symbol.addedTo(this);
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
      return new File(this.filename).getName();
  }

  /**
   * Answer filename.
   */
  public String getFilename()
  {
    return this.filename;
  }

  protected void setBaseAddress(long baseAddress)
  {
    this.baseAddress = baseAddress;
  }

  public long getBaseAddress()
  {
    return this.baseAddress;
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

  public static ObjectFile buildFromFile(String filename)
  {
    {
      ObjectFile objFile = (ObjectFile)cachedFiles.get(filename);
      if (objFile != null)
	return objFile;
    }

    try
      {
	final Elf elfFile = new Elf(filename, ElfCommand.ELF_C_READ);
	if (elfFile == null)
	  return null;

	final ElfEHeader eh = elfFile.getEHeader();
	if (eh == null)
	  return null;

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
	      }
	    else if (ph.type == ElfPHeader.PTYPE_LOAD
		     && ph.offset == 0)
	      {
		haveLoadable = true;
		baseAddress = ph.vaddr;
	      }
	  }

	if (!haveDynamic)
	  {
	    System.err.println("This file doesn't participate in dynamic linking.");
	    return null;
	  }
	if (!haveLoadable)
	  {
	    System.err.println("This file doesn't contain any loadable segments.");
	    return null;
	  }

	final ObjectFile objFile = new ObjectFile(filename);
	objFile.setBaseAddress(baseAddress);

	if (eh.type == ElfEHeader.PHEADER_ET_EXEC)
	  System.err.println("Executable");
	else if (eh.type == ElfEHeader.PHEADER_ET_DYN)
	  System.err.println("DSO/PIE");
	else
	  System.err.println("Unknown ELF type " + eh.type + "!");

	class Locals {
	  public ElfSection dynamicStrtab = null;
	  public ElfSection dynamicSymtab = null;

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
	haveDynamic = false;

	// Find & interpret DYNAMIC section.
	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section))
	  {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (sheader.offset == offDynamic)
	      {
		haveDynamic = true;
		ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
		    public void entry (int tag, long value)
		    {
		      if (tag == ElfDynamic.ELF_DT_STRTAB)
			locals.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_SONAME)
			locals.dynamicSonameIdx = (int)value;
		      else if (tag == ElfDynamic.ELF_DT_SYMTAB)
			locals.dynamicSymtab = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_VERSYM)
			locals.dynamicVersym = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_VERDEF)
			locals.dynamicVerdef = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_VERDEFNUM)
			locals.dynamicVerdefCount = (int)value;
		      else if (tag == ElfDynamic.ELF_DT_VERNEED)
			locals.dynamicVerneed = getElfSectionWithAddr(elfFile, value);
		      else if (tag == ElfDynamic.ELF_DT_VERNEEDNUM)
			locals.dynamicVerneedCount = (int)value;
		    }
		});
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_PROGBITS
		      || sheader.type == ElfSectionHeader.ELF_SHT_NOBITS)
		     && sheader.name.equals(".plt"))
	      {
		havePlt = true;
		locals.pltAddr = sheader.addr;
		locals.pltSize = sheader.size;
	      }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_REL
		      && sheader.name.equals(".rel.plt"))
		     || (sheader.type == ElfSectionHeader.ELF_SHT_RELA
			 && sheader.name.equals(".rela.plt")))
	      {
		haveRelPlt = true;
		locals.pltRelocs = ElfRel.loadFrom(section);
	      }
	  }

	if (!haveDynamic)
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

	// Load DT_SYMTAB.
	{
	  final ArrayList symbolList = new ArrayList();
	  ElfSymbol.loadFrom(locals.dynamicSymtab, locals.dynamicVersym,
			     locals.dynamicVerdef, locals.dynamicVerdefCount,
			     locals.dynamicVerneed, locals.dynamicVerneedCount,
			     new ElfSymbol.Builder() {
	      private int counter = 0;
	      public void symbol (String name, long value, long size,
				  ElfSymbolType type, ElfSymbolBinding bind,
				  ElfSymbolVisibility visibility, long shndx,
				  List versions)
	      {
		String dName = Demangler.demangle(name);
		Symbol sym = new Symbol(dName, type, value, size, shndx, versions);
		symbolList.add(sym);
		counter++;
		if (type == ElfSymbolType.ELF_STT_FUNC)
		  {
		    sym.setEntryAddress(value);
		    objFile.addSymbol(sym);
		  }
	      }
	    });

	  long pltEntrySize = locals.pltSize / (locals.pltRelocs.length + 1);
	  for (int i = 0; i < locals.pltRelocs.length; ++i)
	    /* XXX HACK: 386 specific.  In general we want
	     * platform-independent way of asking whether it's
	     * JMP_SLOT relocation. */
	    if (locals.pltRelocs[i].type == 7)
	      {
		long pltAddress = locals.pltAddr + pltEntrySize * (i + 1);
		long symbolIndex = locals.pltRelocs[i].symbolIndex;
		Symbol symbol = (Symbol)symbolList.get((int)symbolIndex - 1);
		symbol.setPltAddress(pltAddress);
	      }
	}

	// Read SONAME if there was one.
	if (locals.dynamicSonameIdx != -1)
	  {
	    ElfData data = locals.dynamicStrtab.getData();
	    byte[] bytes = data.getBytes();
	    int startIndex = locals.dynamicSonameIdx;
	    int endIndex = startIndex;
	    while (bytes[endIndex] != 0)
	      ++endIndex;
	    String name = new String(bytes, startIndex, endIndex - startIndex);
	    objFile.setSoname(name);
	  }

	cachedFiles.put(filename, objFile);
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
