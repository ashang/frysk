// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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
import lib.dwfl.ElfFileException;
import lib.dwfl.ElfException;
import lib.stdcpp.Demangler;

/**
 * What ltrace needs to know about each loaded executable or shared
 * library.
 */
public class ObjectFile
{
    private File filename;
    private String soname = null;
    private String interp = null;
    private File resolvedInterp = null;
    private long baseAddress = 0;
    private long entryPoint = 0;
    private static HashMap cachedFiles = new HashMap();

    public ElfSection dynamicStrtab = null;
    public ElfSection dynamicSymtab = null;
    public ElfSection staticSymtab = null;

    public ElfSection dynamicVersym = null;
    public ElfSection dynamicVerdef = null;
    public ElfSection dynamicVerneed = null;
    public int dynamicVerdefCount = 0;
    public int dynamicVerneedCount = 0;

    public long pltAddr = 0;
    public long pltSize = 0;
    public ElfRel[] pltRelocs = null;

    /**
     * Implement this interface to create an iterator over tracepoints
     * defined in this file.
     */
    public interface TracePointIterator {
	void tracePoint(TracePoint tracePoint);
    }

    private static void assertFitsToInt(long num, String context) {
	int numi = (int)num;
	if ((long)numi != num)
	    throw new ArithmeticException(context + ": " + num + " doesn't fit into int.");
    }

    /**
     * All-purpose builder for ftrace Symbols and TracePoints.
     */
    private class ObjFBuilder
	implements ElfSymbol.Builder
    {
	/** Used for tracking of what is currently being loaded. */
	private TracePointOrigin origin = null;

	/** For alias detection. */
	private Map symbolWithValue = new HashMap();

	/** This is the array where newly created tracepoints go to. */
	private ArrayList tracePoints = null;

	/**
	 * This is the array where symbols from DYNAMIC are stored, should
	 * they be needed for PLT tracepoints.
	 */
	private Symbol[] dynamicSymbolList = null;
	private ElfSymbol.Loader dynamicLoader = null;

	/**
	 * Map with tracepoints of various origin.
	 * HashMap&lt;origin, ArrayList&lt;TracePoint&gt;&gt;
	 */
	private Map tracePointMap = new HashMap();

	/** Whether this file takes part in dynamic linking. */
	private boolean haveDynamic = false;

	/** Keep track of loaded dynamic symbols.  We will need this
	 *  when building PLT entries. */
	private void recordDynamicSymbol (long index, Symbol sym)
	{
	    if (this.origin == TracePointOrigin.DYNAMIC
		|| this.origin == TracePointOrigin.PLT) {
		assertFitsToInt(index, "Symbol index");
		this.dynamicSymbolList[(int)index] = sym;
	    }
	}

	public void symbol (long index, String name,
			    long value, long size,
			    ElfSymbolType type, ElfSymbolBinding bind, ElfSymbolVisibility visibility,
			    long shndx, List versions)
	{
	    /// XXX FIXME: We probably want to share symbols to some extent,
	    /// so that entries from SYMTAB that are also present in DYNSYM
	    /// end up being the same symbol actually.  This seems to
	    /// indicate we want a mapping of (name x verdefs) -> symbol.

	    String dName = Demangler.demangle(name);
	    FtraceLogger.finest.log("Got new symbol `" + dName + "' with origin " + this.origin + ".");

	    Long valueL = null;
	    Symbol sym = null;

	    if (value != 0) {
		valueL = new Long(value);
		sym = (Symbol)symbolWithValue.get(valueL);
	    }

	    if (sym != null) {
		FtraceLogger.finest.log("... aliasing `" + sym.name + "'.");
		sym.addAlias(dName);
		recordDynamicSymbol(index, sym);
	    }
	    else {
		// Offset isn't stored in ELF file, value is; but value can be
		// prelinked, so we have to compute the offset from symbol
		// value and the address of first loadable segment.
		long offset = 0;

		// Don't deduce offset of undefined and special symbols.
		if (shndx != ElfSectionHeader.ELF_SHN_UNDEF
		    && shndx < ElfSectionHeader.ELF_SHN_LORESERVE)
		    offset = value - ObjectFile.this.baseAddress;

		sym = new Symbol(dName, type, value, offset, size, shndx, versions);
		sym.addedTo(ObjectFile.this);

		recordDynamicSymbol(index, sym);

		if (type == ElfSymbolType.ELF_STT_FUNC
		    && value != 0)
		    this.addNewTracepoint(value, sym.offset, sym);

		if (valueL != null)
		    symbolWithValue.put(valueL, sym);
	    }
	}

	public void addNewTracepoint(long address, long offset, Symbol symbol)
	{
	    FtraceLogger.fine.log(
		"New tracepoint for `" + symbol + "', origin " + this.origin
		+ ", address=0x" + Long.toHexString(address)
		+ ", offset=0x" + Long.toHexString(offset) + ".");
	    TracePoint tp = new TracePoint(address, offset, symbol, this.origin);
	    tracePoints.add(tp);
	}

	public synchronized ArrayList getTracePoints(TracePointOrigin origin) {
	    ArrayList tracePoints = (ArrayList)this.tracePointMap.get(origin);
	    if (tracePoints != null) {
		FtraceLogger.fine.log(
		    "" + tracePoints.size() + " tracepoints for origin "
		    + origin + " retrieved from cache.");
		return tracePoints;
	    }

	    FtraceLogger.fine.log("Loading tracepoints for origin " + origin + ".");
	    if ((origin == TracePointOrigin.PLT
		 || origin == TracePointOrigin.DYNAMIC)
		&& this.haveDynamic) {
		// Initialize dynamic symbol list for PLT if necessary...
		if (this.dynamicSymbolList == null) {
		    long count = ElfSymbol.symbolsCount(ObjectFile.this.dynamicSymtab);
		    assertFitsToInt(count, "Symbol count");
		    this.dynamicSymbolList = new Symbol[(int)count];
		    this.dynamicLoader
			= new ElfSymbol.Loader(ObjectFile.this.dynamicSymtab, ObjectFile.this.dynamicVersym,
					       ObjectFile.this.dynamicVerdef, ObjectFile.this.dynamicVerdefCount,
					       ObjectFile.this.dynamicVerneed, ObjectFile.this.dynamicVerneedCount);
		}

		if (origin == TracePointOrigin.DYNAMIC) {
		    // Load dynamic symtab and PLT entries.
		    FtraceLogger.finest.log("Loading dynamic symtab.");
		    this.origin = TracePointOrigin.DYNAMIC;
		    this.tracePoints = new ArrayList();
		    this.tracePointMap.put(this.origin, this.tracePoints);

		    this.dynamicLoader.loadAll(this);
		}

		if (origin == TracePointOrigin.PLT) {
		    int pltCount = ObjectFile.this.pltRelocs.length;
		    FtraceLogger.finest.log("Loading " + pltCount + " PLT entries.");

		    ArrayList tracePointsPlt = new ArrayList();
		    this.tracePointMap.put(TracePointOrigin.PLT, tracePointsPlt);

		    ArrayList tracePointsDynamic
			= (ArrayList)this.tracePointMap.get(TracePointOrigin.DYNAMIC);
		    if (tracePointsDynamic == null)
			this.tracePointMap.put(TracePointOrigin.DYNAMIC,
					       tracePointsDynamic = new ArrayList());

		    long pltEntrySize = ObjectFile.this.pltSize / (ObjectFile.this.pltRelocs.length + 1);
		    for (int i = 0; i < pltCount; ++i)
			/* XXX HACK: 386/x64 specific.  In general we want
			 * platform-independent way of asking whether it's
			 * JMP_SLOT relocation. */
			if (ObjectFile.this.pltRelocs[i].type == 7) {
			    long pltEntryAddr = ObjectFile.this.pltAddr + pltEntrySize * (i + 1);
			    long symbolIndex = ObjectFile.this.pltRelocs[i].symbolIndex;

			    assertFitsToInt(symbolIndex, "Symbol associated with PLT entry");
			    Symbol symbol = this.dynamicSymbolList[(int)symbolIndex];
			    if (symbol == null) {
				FtraceLogger.finest.log("Lazy loading symbol #" + symbolIndex);
				this.origin = TracePointOrigin.DYNAMIC;
				this.tracePoints = tracePointsDynamic;
				this.dynamicLoader.load(symbolIndex, this);
				symbol = this.dynamicSymbolList[(int)symbolIndex];
			    }
			    if (symbol == null)
				throw new AssertionError("Dynamic symbol still not initialized.");

			    FtraceLogger.finest.log(
				"Got plt entry for `" + symbol.name + "' at 0x"
				+ Long.toHexString(pltEntryAddr) + ".");
			    this.origin = TracePointOrigin.PLT;
			    this.tracePoints = tracePointsPlt;
			    long pltEntryOffset = pltEntryAddr - ObjectFile.this.baseAddress;
			    this.addNewTracepoint(pltEntryAddr, pltEntryOffset, symbol);
			}
		}
	    }
	    else if (origin == TracePointOrigin.SYMTAB
		     && ObjectFile.this.staticSymtab != null)	{
		// Load static symtab.
		FtraceLogger.finest.log("Loading static symtab.");
		this.origin = TracePointOrigin.SYMTAB;
		this.tracePoints = new ArrayList();
		this.tracePointMap.put(this.origin, this.tracePoints);

		ElfSymbol.Loader loader;
		loader = new ElfSymbol.Loader(ObjectFile.this.staticSymtab);
		loader.loadAll(this);
	    }
	    else
		this.tracePoints = new ArrayList(); //java.util.Collections.EMPTY_LIST;

	    return this.tracePoints;
	}
    }
    private ObjFBuilder builder;

    protected ObjectFile(File file, final Elf elfFile, ElfEHeader eh) {
	this.filename = file;
	this.entryPoint = eh.entry;
	this.builder = new ObjFBuilder();

	boolean haveLoadable = false;
	boolean havePlt = false;
	boolean haveRelPlt = false;
	long offDynamic = 0;
	for (int i = 0; i < eh.phnum; ++i) {
	    ElfPHeader ph = elfFile.getPHeader(i);
	    if (ph.type == ElfPHeader.PTYPE_DYNAMIC) {
		builder.haveDynamic = true;
		offDynamic = ph.offset;
		FtraceLogger.finest.log("Found DYNAMIC segment.");
	    }
	    else if (ph.type == ElfPHeader.PTYPE_LOAD
		     && ph.offset == 0) {
		haveLoadable = true;
		this.baseAddress = ph.vaddr;
		FtraceLogger.finest.log(
		    "Found LOADABLE segment, base address = 0x"
		    + Long.toHexString(this.baseAddress));
	    }
	    else if (ph.type == ElfPHeader.PTYPE_INTERP) {
		ElfData interpData = elfFile.getRawData(ph.offset, ph.filesz - 1); // -1 for trailing zero
		String interp = new String(interpData.getBytes());
		this.setInterp(interp);
		FtraceLogger.finest.log("Found INTERP `" + interp + "'.");
	    }
	}

	if (!haveLoadable) {
	    FtraceLogger.fine.log("Failed, didn't find any loadable segments.");
	    throw new ElfFileException(file, "Failed, didn't find any loadable segments.");
	}

	if (eh.type == ElfEHeader.PHEADER_ET_EXEC)
	    FtraceLogger.finest.log("This file is EXECUTABLE.");
	else if (eh.type == ElfEHeader.PHEADER_ET_DYN)
	    FtraceLogger.finest.log("This file is DSO or PIE EXECUTABLE.");
	else {
	    FtraceLogger.fine.log("Failed, unsupported ELF file type.");
	    throw new ElfFileException(file, "Failed, unsupported ELF file type.");
	}

	boolean foundDynamic = false;

	class Locals {
	    public int dynamicSonameIdx = -1;
	}
	final Locals locals = new Locals();

	// Find & interpret DYNAMIC section.
	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section)) {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (builder.haveDynamic && sheader.offset == offDynamic) {
		FtraceLogger.finest.log("Processing DYNAMIC section.");
		foundDynamic = true;
		ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
			public void entry (int tag, long value)
			{
			    if (tag == ElfDynamic.ELF_DT_STRTAB)
				{
				    FtraceLogger.finest.log(" * dynamic strtab at 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
				}
			    else if (tag == ElfDynamic.ELF_DT_SONAME)
				{
				    FtraceLogger.finest.log(" * soname index = 0x" + Long.toHexString(value));
				    assertFitsToInt(value, "SONAME index");
				    locals.dynamicSonameIdx = (int)value;
				}
			    else if (tag == ElfDynamic.ELF_DT_SYMTAB)
				{
				    FtraceLogger.finest.log(" * dynamic symtab = 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicSymtab = getElfSectionWithAddr(elfFile, value);
				}
			    else if (tag == ElfDynamic.ELF_DT_VERSYM)
				{
				    FtraceLogger.finest.log(" * versym = 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicVersym = getElfSectionWithAddr(elfFile, value);
				}
			    else if (tag == ElfDynamic.ELF_DT_VERDEF)
				{
				    FtraceLogger.finest.log(" * verdef = 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicVerdef = getElfSectionWithAddr(elfFile, value);
				}
			    else if (tag == ElfDynamic.ELF_DT_VERDEFNUM)
				{
				    FtraceLogger.finest.log(" * verdefnum = " + Long.toString(value));
				    assertFitsToInt(value, "Count of VERDEF entries");
				    ObjectFile.this.dynamicVerdefCount = (int)value;
				}
			    else if (tag == ElfDynamic.ELF_DT_VERNEED)
				{
				    FtraceLogger.finest.log(" * verneed = 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicVerneed = getElfSectionWithAddr(elfFile, value);
				}
			    else if (tag == ElfDynamic.ELF_DT_VERNEEDNUM)
				{
				    FtraceLogger.finest.log(" * verneednum = " + Long.toString(value));
				    assertFitsToInt(value, "Count of VERNEED entries");
				    ObjectFile.this.dynamicVerneedCount = (int)value;
				}
			}
		    });
	    }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_PROGBITS
		      || sheader.type == ElfSectionHeader.ELF_SHT_NOBITS)
		     && sheader.name.equals(".plt")) {
		FtraceLogger.finest.log("Found PLT section.");
		havePlt = true;
		this.pltAddr = sheader.addr;
		this.pltSize = sheader.size;
	    }
	    else if ((sheader.type == ElfSectionHeader.ELF_SHT_REL
		      && sheader.name.equals(".rel.plt"))
		     || (sheader.type == ElfSectionHeader.ELF_SHT_RELA
			 && sheader.name.equals(".rela.plt"))) {
		FtraceLogger.finest.log("Found PLT relocation section.");
		haveRelPlt = true;
		this.pltRelocs = ElfRel.loadFrom(section);
	    }
	    else if (sheader.type == ElfSectionHeader.ELF_SHT_SYMTAB) {
		if (this.staticSymtab != null)
		    throw new ElfFileException(file, "Strange: More than one static symbol tables.");
		FtraceLogger.finest.log("Found static symtab section `" + sheader.name + "'.");
		this.staticSymtab = section;
	    }
	}

	if (builder.haveDynamic) {
	    // Elf consistency sanity checks.
	    if (!foundDynamic)
		throw new ElfFileException(file, "DYNAMIC section not found in ELF file.");
	    if (!havePlt)
		throw new ElfFileException(file, "No (suitable) .plt found in ELF file.");
	    if (!haveRelPlt)
		throw new ElfFileException(file, "No (suitable) .rel.plt found in ELF file.");
	    if (this.dynamicSymtab == null)
		throw new ElfFileException(file, "Couldn't get SYMTAB from DYNAMIC section.");
	    if (this.dynamicStrtab == null)
		throw new ElfFileException(file, "Couldn't get STRTAB from DYNAMIC section.");
	    if ((this.dynamicVerneed != null || this.dynamicVerdef != null) && this.dynamicVersym == null)
		throw new ElfFileException(file, "Versym section missing when verdef or verneed present.");
	    if (this.dynamicVerneed == null && this.dynamicVerdef == null && this.dynamicVersym != null)
		throw new ElfFileException(file, "Versym section present when neither verdef nor verneed present.");
	    if (this.dynamicVerdefCount != 0 && this.dynamicVerdef == null)
		throw new ElfFileException(file, "Strange: VERDEFNUM tag present, but not VERDEF.");
	    if (this.dynamicVerneedCount != 0 && this.dynamicVerneed == null)
		throw new ElfFileException(file, "Strange: VERNEEDNUM tag present, but not VERNEED.");
	}

	// Read SONAME, if there was one.
	if (locals.dynamicSonameIdx != -1) {
	    FtraceLogger.finest.log("Reading SONAME.");
	    ElfData data = this.dynamicStrtab.getData();
	    byte[] bytes = data.getBytes();
	    int startIndex = locals.dynamicSonameIdx;
	    int endIndex = startIndex;
	    while (bytes[endIndex] != 0)
		++endIndex;
	    String name = new String(bytes, startIndex, endIndex - startIndex);
	    this.setSoname(name);
	    FtraceLogger.finest.log("Found SONAME `" + name + "'.");
	}

	FtraceLogger.fine.log("Loading finished successfully.");
    }

    public void eachTracePoint(TracePointIterator client, TracePointOrigin origin) {
	FtraceLogger.fine.log("Loading tracepoints for origin " + origin + ".");
	List tracePoints = builder.getTracePoints(origin);

	FtraceLogger.fine.log("Got them, now processing each loaded.");
	for (Iterator it = tracePoints.iterator(); it.hasNext();) {
	    TracePoint tp = (TracePoint)it.next();
	    FtraceLogger.finest.log("Tracepoint for `" + tp.symbol.name
				    + "', origin " + tp.origin);
	    client.tracePoint(tp);
	}

	FtraceLogger.fine.log("Done processing tracepoints for origin "
			      + origin + ".");
    }

    public TracePoint lookupTracePoint(String name, TracePointOrigin origin) {
	FtraceLogger.fine.log("Looking up tracepoint for `"
			      + name + "' in " + origin + ".");
	List tracePoints = builder.getTracePoints(origin);
	for (Iterator it = tracePoints.iterator(); it.hasNext();) {
	    TracePoint tp = (TracePoint)it.next();
	    if (tp.symbol.name.equals(name))
		return tp;
	}
	return null;
    }

    public void eachTracePoint(TracePointIterator client) {
	FtraceLogger.fine.log("Load ALL tracepoints.");
	eachTracePoint(client, TracePointOrigin.PLT);
	eachTracePoint(client, TracePointOrigin.DYNAMIC);
	eachTracePoint(client, TracePointOrigin.SYMTAB);
	FtraceLogger.fine.log("ALL tracepoints processed.");
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

    protected void setInterp(String interp)
    {
	this.interp = interp;
    }

    public String getInterp()
    {
	return this.interp;
    }

    /** Find a canonical path to interpreter and answer that. */
    public File resolveInterp()
    {
	if (this.resolvedInterp == null) {
	    File interppath = new File(this.interp);
	    try {
		this.resolvedInterp = interppath.getCanonicalFile();
	    }
	    catch (java.io.IOException e) {
		FtraceLogger.warning.log(
		    "Couldn't get canonical path of ELF interpreter `{0}'.",
		    interppath);
	    }
	}
	return this.resolvedInterp;
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
	     section = elfFile.getNextSection(section)) {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (sheader.addr == addr)
		return section;
	}
	return null;
    }

    public static ObjectFile buildFromFile(File filename)
    {
	FtraceLogger.fine.log("Loading object file `" + filename + "'");
	ObjectFile objFile = (ObjectFile)cachedFiles.get(filename);
	if (objFile != null) {
	    FtraceLogger.fine.log("Retrieved from cache.");
	    return objFile;
	}

	Elf elfFile;
	try {
	    elfFile = new Elf(filename, ElfCommand.ELF_C_READ);
	} catch (ElfException eexp) {
	    eexp.printStackTrace();
	    System.err.println("load error: " + eexp);
	    return null;
	}

	ElfEHeader eh;
	try {
	    eh = elfFile.getEHeader();
	} catch (ElfException e) {
	    FtraceLogger.fine.log("Failed, couldn't get an ELF header.");
	    return null;
	}

	objFile = new ObjectFile(filename, elfFile, eh);
	cachedFiles.put(filename, objFile);
	FtraceLogger.fine.log("Done.");
	return objFile;
    }
}
