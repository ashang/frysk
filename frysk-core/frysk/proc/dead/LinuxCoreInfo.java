// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.proc.dead;

import java.util.List;
import java.util.LinkedList;
import java.io.File;
import lib.dwfl.Elf;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfPrAuxv;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfPrstatus;
import lib.dwfl.ElfPrFPRegSet;
import frysk.isa.ISA;
import frysk.isa.ElfMap;
import lib.dwfl.ElfPrXFPRegSet;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfPrpsinfo;
import frysk.rsl.Log;
import frysk.proc.Auxv;
import frysk.sys.proc.AuxvBuilder;
import frysk.proc.MemoryMap;
import java.util.Iterator;

/**
 * Extract from a core file all the information needed to construct
 * and maintain a corefile Host, Proc, and Task.  The Elf objects used
 * to extract the information are all closed.
 */

class LinuxCoreInfo {
    private static final Log fine = Log.fine(LinuxCoreInfo.class);

    // The following variables are all carefully disconnected from the
    // Elf object.
    final ElfEHeader eHeader;
    final String[] args;
    final ElfPrpsinfo prpsInfo;
    final File coreFile;
    final File exeFile;
    final Auxv[] auxv;
    final MemoryMap[] memoryMaps;
    final CorefileByteBuffer memory;
    final ElfPrstatus[] elfTasks;
    final ElfPrFPRegSet[] elfFPRegs;
    final ElfPrXFPRegSet[] elfXFPRegs;
    final ISA isa;

    /**
     * Unpack the core file extracting everything needed to create a
     * host, proc, and tasks.
     */
    LinuxCoreInfo(File coreParam, File exeParam, boolean extendedMetaData) {
	Elf coreElf = null;
	Elf exeElf = null;
	try {
	    this.coreFile = coreParam;
	    // Open the core file; validate it.
	    coreElf = new Elf(coreParam, ElfCommand.ELF_C_READ);
	    this.eHeader = coreElf.getEHeader();
	    if (eHeader.type != ElfEHeader.PHEADER_ET_CORE) {
		throw new RuntimeException("'" + coreParam
					   + "' is not a corefile.");
	    }
	
	    // Find the note section that contains all the notes;
	    // there is only ever one note section and it must be
	    // present.
	    ElfData noteSection = getNoteSection(eHeader, coreElf);
	    if (noteSection == null)
		throw new RuntimeException("'" + coreParam
					   + "' is corrupt; no note section");
	    
	    // Extract the pr/ps information from the note.
	    this.prpsInfo = ElfPrpsinfo.decode(noteSection);
	    this.args = prpsInfo.getPrPsargs().split(" ");
	    fine.log("args", args);

	    // Define the real exe file (dependant on parameters might
	    // have to extract this from the process information).
	    this.exeFile = getExeFile(exeParam, args, prpsInfo);
	    if (extendedMetaData)
		exeElf = new Elf(this.exeFile, ElfCommand.ELF_C_READ);

	    this.auxv = constructAuxv(noteSection);

	    // We'll bake basic meta data for all corefiles
	    MapAddressHeader[] metaData
		= constructBasicMapData(coreElf, eHeader, this.auxv);
	    if (exeElf != null) {
		fine.log("constructMetaData() - exe backend is not "
			 + "null, bake enhanced data");
		// If an executable is available that is paired with
		// the corefile, much richer metadata can be
		// constructed.
		addEnhancedMapData(coreFile, exeFile, metaData, exeElf, auxv);
		fine.log("constructMetaData - Enhanced metadata complete");
	    }

	    // Use the meta data to construct the memory maps (what
	    // the proc expects).
	    this.memoryMaps = constructMemoryMaps(metaData);

	    // Create the core file byte buffer; underneath this opens
	    // the files on-demand.
	    fine.log("construct byte-buffer");
	    this.memory = new CorefileByteBuffer(coreFile, metaData);

	    this.elfTasks = ElfPrstatus.decode(noteSection);
	    this.elfFPRegs = ElfPrFPRegSet.decode(noteSection);
	    this.elfXFPRegs = ElfPrXFPRegSet.decode(noteSection);
	    this.isa = ElfMap.getISA(eHeader);
	} finally { 
	    if (coreElf != null)
		coreElf.close();
	    if (exeElf != null)
		exeElf.close();
	}
    }

    /**
     * Find and return the the sole note section (which contains
     * .notes for things like the process status, the registers, and
     * so on).
     */
    private static ElfData getNoteSection(ElfEHeader eHeader, Elf coreElf) {
	ElfData noteSection = null;
	for (int i = 0; i < eHeader.phnum; i++) {
	    // Test if pheader is of types notes..
	    ElfPHeader pHeader = coreElf.getPHeader(i);
	    if (pHeader.type == ElfPHeader.PTYPE_NOTE) {
		// if so, copy, break an leave.
		noteSection = coreElf.getRawData(pHeader.offset,
						 pHeader.filesz);
		break;
	    }
	}
	return noteSection;
    }

    /**
     *
     */
    private static File getExeFile(File exeParam, String[] args,
				   ElfPrpsinfo prpsInfo) {
	if (exeParam == null) {
	    // Only place to find full path + exe is in the args
	    // list. Remove ./ if present.
	    if (args.length > 0) {
		if (args[0].startsWith("./"))
		    exeParam = new File(args[0].substring(2));
		else
		    exeParam = new File(args[0]);
	    } else {
		exeParam = new File(prpsInfo.getPrFname());
	    }
	    fine.log("exe from core", exeParam);
	} else {
	    fine.log("exe for core", exeParam);
	}
	return new File(exeParam.getAbsolutePath());
    }


    /**
     * Construct the memory maps from the core file (and executable file).
     */
    private static MemoryMap[] constructMemoryMaps(MapAddressHeader[] metaData) {
	fine.log("constructMemoryMaps");
	List maps = new LinkedList();

	// Refactor metadata into format expected by clients of
	// sendrecMaps.
	for (int i = 0; i < metaData.length; i++) {
	    MapAddressHeader map = metaData[i];
	    if (fine.logging())
		fine.prefix()
		    .print("constructMemoryMaps range =")
		    .print(map.vaddr)
		    .print("-").print(map.vaddr_end)
		    .print("read =").print(map.permRead)
		    .print("write =").print(map.permWrite)
		    .print("execute =").print(map.permExecute)
		    .print("offset =").print(map.solibOffset)
		    .print("name =").print(map.name)
		    .suffix();
	    maps.add(new MemoryMap(map.vaddr, map.vaddr_end,
				   map.permRead, map.permWrite,
				   map.permExecute, false,
				   map.solibOffset,
				   -1, -1, -1, -1, -1,
				   map.name));
	}
	MemoryMap[] memoryMaps = new MemoryMap[maps.size()];
	maps.toArray(memoryMaps);
	return memoryMaps;
    }

    /**
     * Extract the AUXV .note from the notSection.
     */
    private static Auxv[] constructAuxv(ElfData noteSection) {
	fine.log("constructAuxv");
	final ElfPrAuxv prAuxv =  ElfPrAuxv.decode(noteSection);
	class BuildAuxv extends AuxvBuilder {
	    Auxv[] vec;
	    public void buildBuffer(byte[] auxv) {
	    }
	    public void buildDimensions(int wordSize, boolean bigEndian,
					int length) {
		vec = new Auxv[length];
	    }
	    public void buildAuxiliary(int index, int type, long val) {
		vec[index] = new Auxv(type, val);
	    }
	}
	BuildAuxv auxv = new BuildAuxv();
	auxv.construct(prAuxv.getAuxvBuffer());
	return auxv.vec;
    }

    /**
     * Build basic metadata fore a corefile.  A backing executable is
     * not necessary for basic metadata as it only contains the core
     * file program segment headers and the corefile offsets
     */
    private static MapAddressHeader[] constructBasicMapData(Elf coreElf,
							    ElfEHeader eHeader,
							    Auxv[] auxv) {
	fine.log("constructBasicMapMetaData()");	
	List metaData = new LinkedList();
	long coreVDSO = getCorefileVDSOAddress(auxv);
	for (int i=0; i < eHeader.phnum; i++) {
	    // Test if pheader is of types LOAD. If so add to list
	    ElfPHeader pHeader = coreElf.getPHeader(i);
	    // Load each PT_LOAD segment
	    if (pHeader.type == ElfPHeader.PTYPE_LOAD) {
		// Calculate flags
		boolean read = (pHeader.flags & ElfPHeader.PHFLAG_READABLE) > 0 ? true : false;
		boolean write =  (pHeader.flags & ElfPHeader.PHFLAG_WRITABLE) > 0 ? true : false;
		boolean execute = (pHeader.flags & ElfPHeader.PHFLAG_EXECUTABLE) > 0 ? true : false;	    
		String name = "";
		if (coreVDSO == pHeader.vaddr)
		    name="[vdso]";
		// Add basic meta data to list
		metaData.add
		    (new MapAddressHeader
		     (pHeader.vaddr, pHeader.vaddr + pHeader.memsz,
		      read, write, execute,
		      pHeader.offset, 0, pHeader.filesz, 
		      pHeader.memsz, name, 0));
	    }
	}  
	MapAddressHeader[] maps = new MapAddressHeader[metaData.size()];
	metaData.toArray(maps);
	return maps;
    }
  
    /**
     * Build enhanced metadata for a corefile.  A backing executable
     * is necessary for enhanced metadata and is required. If not exe
     * is available enhanced meta daa is not built, and elided segment
     * access and named maps disallowed
     *
     */
    private static void addEnhancedMapData(File coreFile, File exeFile,
					   MapAddressHeader[] metaData,
					   Elf exeElf, Auxv[] auxv) {
	fine.log("constructEnhancedMapMetadata");

	// Find Dynamic Segment
	DynamicSegmentTuple dynamicTuple
	    = getDynamicSegmentAddress(auxv, exeElf);

	// Create a temporary, noncache resetting view into the
	// corefile memory so that the link-map table can be
	// extracted. The metadata from the basic map is is used.
	CorefileByteBuffer tempMemory = new CorefileByteBuffer(coreFile,
							       metaData);
    
	// From that segment address, find linkmap table.
	long linkmapAddress = getLinkmapAddress(tempMemory, dynamicTuple);

	// No link map, hence no libraries have so far been loaded
	// (core was taken while the program was at the entry point or
	// shortly after).  This is ok, the executable is still being
	// used for things like the exe path.
	if (linkmapAddress == 0)
	    return;

	// Edge case: Save interp name as it is not included the
	// linkmap as it is loaded by the kernel.
	String interpName = getExeInterpreterName(exeElf);

	// Edge case: Get interp address so when we traverse the
	// linkmap it can be paired with its name
	long interpAddr = getExeInterpreterAddress(exeElf); 

	// Build the link-map table from the link-map table address
	fine.log("Building linkmap");
	class BuildLinkMap extends LinkmapBuilder {
	    List list = new LinkedList();
	    public void buildMap(long l_addr, long l_ld, long saddr,
				 String name) {
		fine.log("New linkmap item: l_addr", l_addr,
			 "l_ld", l_ld, "s_addr", l_addr, "name", name);
		list.add(new Linkmap(l_addr, l_ld, saddr, name));
	    }
	}
	BuildLinkMap linkMap = new BuildLinkMap();
	linkMap.construct(linkmapAddress, tempMemory);
	for (Iterator i = linkMap.list.iterator(); i.hasNext(); ) {
	    Linkmap tempMap = (Linkmap) i.next();
	    if (tempMap.s_addr == interpAddr) {
		fine.log("Found interpretator at linkmap address",
			 tempMap.s_addr);
		tempMap.name = interpName;
	    }
	}

	// From the list of solibs in the linkamp,  build
	// maps for each one.
	class BuildSOMaps extends SOLibMapBuilder {
	    List list = new LinkedList();
	    public void buildMap (long addrLow, long addrHigh, 
				  boolean permRead, boolean permWrite,
				  boolean permExecute, long offset, 
				  String name, long align) {
		list.add(new MapAddressHeader(addrLow, addrHigh, permRead,
					      permWrite, permExecute,
					      0, offset, 0,0, name, align));
	    }
	}

	BuildSOMaps SOMaps = new BuildSOMaps();
	for (Iterator i = linkMap.list.iterator(); i.hasNext(); ) {
	    Linkmap singleLinkMap = (Linkmap) i.next();
	    if ((!singleLinkMap.name.equals("")) && (!singleLinkMap.name.equals("[vdso]")))
		SOMaps.construct(new File(singleLinkMap.name),
				 singleLinkMap.l_addr);
	    if (singleLinkMap.name.equals("[vdso]"))
		SOMaps.buildMap(singleLinkMap.l_addr,0,true,true,true,0,singleLinkMap.name,0x1000);
	}


	// Add in case for executables maps.
	SOMaps.construct(exeElf, exeFile, 0);

	// Reconcile maps.
	for (Iterator i = SOMaps.list.iterator(); i.hasNext(); ) {
	    MapAddressHeader localMap = (MapAddressHeader) i.next();
	    for (int j = 0; j < metaData.length; j++) {
		MapAddressHeader map = metaData[j];
		if ((map.vaddr == localMap.vaddr)
		    || ((map.vaddr > localMap.vaddr)
			&& (map.vaddr<localMap.vaddr_end))) {
		    if (map.vaddr_end == 0) {
			map.vaddr_end = ((map.vaddr + map.memSize) + 0x1000 -1) &~ (0x1000-1);
		    }
		    map.solibOffset = localMap.solibOffset;
		    map.name = localMap.name;
		}
	    }
	}
    }

    /**
     * Helper function to return the backing core file's dynamic
     * segment address and size
     */
    private static DynamicSegmentTuple getDynamicSegmentAddress(Auxv[] auxv,
								Elf exeElf) {
	fine.log("getDynamicSegmentAddress");
	// If we do not have an executable, we cannot find the dynamic
	// segment in the corefile.
	long coreEntryPoint = getCorefileEntryPoint(auxv);
	long exeEntryPoint = getExeEntryPoint(exeElf);
	DynamicSegmentTuple exeDynamicTuple
	    = getExeDynamicSegmentAddress(exeElf);

	// Calculate relocated segment address, if necessary.
	if (exeDynamicTuple != null)
	    exeDynamicTuple.addr = exeDynamicTuple.addr + 
		coreEntryPoint - exeEntryPoint;
    
	fine.log("getDynamicSegmentAddress tuple",
		 exeDynamicTuple.addr, "size", exeDynamicTuple.size);
	return exeDynamicTuple;
    
    }

    /**
     * Helper function to find the backing executable's dynamic
     * address and size
     **/
    private static DynamicSegmentTuple getExeDynamicSegmentAddress(Elf exeElf) {
    	fine.log("getExeDynamicSegmentAddress");
	DynamicSegmentTuple exeDynamicAddr = null;
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find dynamic segment by iterating through program segment
	// headers
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    // Found the dynamic section
	    if (pHeader.type == ElfPHeader.PTYPE_DYNAMIC) {
		exeDynamicAddr = new DynamicSegmentTuple(pHeader.vaddr,
							 pHeader.filesz);
		break;
	    }
	}
	fine.log("getDynamicExeSegmentAddress tuple",
		 exeDynamicAddr.addr, "size", exeDynamicAddr.size);
	return exeDynamicAddr;
    }

    /**
     * Helper function to locate the link map table in the 
     * core file. This is located in the dynamic segment table
     * at the address specified by the DT_DEBUG field.
     */
    private static long getLinkmapAddress(CorefileByteBuffer internalMem,
					  DynamicSegmentTuple tuple) {
	fine.log("getLinkmapAddress");
	final int DT_DEBUG = 21;
	if (tuple == null) {
	    fine.log("Dynamic segment is null, linkmap set to 0");
	    return 0;

	}

	long dynSegmentEndAddress = tuple.addr + tuple.size;
	long dtDebugAddress = 0;
	long actualAddress = 0;
	long dtTest;

	// Position the core-file's memory at the corefile's dynamic
	// segment.
	internalMem.position(tuple.addr);

	// find DT_DEBUG field in table. The tabke is two
	// words. One is the DT_ tag and the other
	// is the address of that DT_ tag table type.
	while (internalMem.position() < dynSegmentEndAddress) {
	    // Get tag and test if it is DT_DEBUG
	    dtTest = internalMem.getUWord();
	    if (dtTest == DT_DEBUG) {
		// If it is record the address in the 
		// next word.
		dtDebugAddress = internalMem.getUWord();
		break;
	    }
	
	    // Otherwise, move on.
	    internalMem.getUWord();
	}

	if (dtDebugAddress != 0) {
	    // Go to address that DT_DEBUG tag 
	    // specified.
	    internalMem.position(dtDebugAddress);

	    // discard first word at that address;
	    internalMem.getInt();
	    long pos = internalMem.position();
	    int wordSize = internalMem.wordSize();
	    if (pos % wordSize > 0)
		pos = (pos - (pos % wordSize))+wordSize;
	
	    internalMem.position(pos);
	    actualAddress = internalMem.getUWord();
	}

	fine.log("Linkmap address is", actualAddress);
	return actualAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * entry point
     */
    private static long getExeEntryPoint(Elf exeElf) {
	ElfEHeader eHeader = exeElf.getEHeader();
	if (eHeader == null)
	    throw new RuntimeException("executable is not elf");
	return eHeader.entry;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters address.
     */
    private static long getExeInterpreterAddress(Elf exeElf) {
	fine.log("getExeInterpreterAddress");
	long interpreterAddress = 0;
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find .interp segment by passing through progream segment
	// header
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		interpreterAddress = pHeader.vaddr;
		break;
	    }
	}
	fine.log("Interpreter Addr", interpreterAddress);
	return interpreterAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters name
     */
    private static String getExeInterpreterName(Elf exeElf) {
	fine.log("getExeInterpreterName");
	String interpName = "";
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find .interp segment by passing through progream segment
	// header
	for (int headerCount = 0; headerCount < eHeader.phnum; headerCount++) {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		ElfSection interpSection = exeElf.getSection((long)headerCount);
		ElfData data = interpSection.getData();
		interpName = new String(data.getBytes());
		interpName = interpName.trim();
		break;
	    }
	}
	fine.log("Interpreter name", interpName);
	return interpName;
    }

    /**
     * Helper function to locate and report the backing corefile's
     * entry point
     */
    private static long getCorefileEntryPoint(Auxv[] auxv) {
	fine.log("getCorefileEntryPoint");
	// Need auxv data
	long entryPoint = 0;
	// Find the Auxv ENTRY data
	for (int i = 0; i < auxv.length; i++) {
	    if (auxv[i].type == inua.elf.AT.ENTRY) {
		entryPoint = auxv[i].val;
		break;
	    }
	}
	fine.log("Corefile Entrypoint", entryPoint);
	return entryPoint;
    }

    /**
     * Helper function to locate and report the backing corefile's
     * VDSO address
     */
    private static long getCorefileVDSOAddress(Auxv[] auxv) {
	fine.log("getCorefileVDSOAddress");
	long vdsoEntryPoint = 0;
	// Find the SYSINFO_EHDR data
	for (int i = 0; i < auxv.length; i++) {
	    if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR) {
		vdsoEntryPoint = auxv[i].val;
		break;
	    }
	}
	fine.log("Corefile VDSO Address", vdsoEntryPoint);
	return vdsoEntryPoint;
    }



    // Private class to hold Dynamic Segment address tuple
    private static class DynamicSegmentTuple {
	long addr = 0;
	long size = 0;
	public DynamicSegmentTuple(long addr, long size) {
	    this.addr = addr;
	    this.size = size;
	}
    }

    private static class Linkmap {
	long l_addr = 0;
	long l_dyn = 0;
	long s_addr = 0;
	String name = "";
	Linkmap(long l_addr, long l_dyn, long s_addr, String name) {
	    this.l_addr = l_addr;
	    this.l_dyn = l_dyn;
	    this.s_addr = s_addr;
	    this.name = name;
	}
    }


    /**
     * Find and create the core tasks.
     */
    void constructTasks(LinuxCoreProc proc) {
	// Two methods of whether Floating Point note data exists.  In
	// userland generated core-dumps there is no way to test if
	// floating point data operations have actually occurred, so
	// programs like fcore/gcore will always write NT_FPREGSET
	// note data per thread regardless. On kernel generated
	// corefiles, the kernel micro-optimizes whether NT_FPREGSET
	// note data is written per thread by analyzing to see if that
	// thread has performed Floating Point operations. If it has,
	// it will write NT_FPREGSET, and if it hasn't it won't.
    
	if (elfFPRegs.length == elfTasks.length) {
	    // The number of NT_FPREGSET note objects is equal to the
	    // the number of NT_PRSTATUS note objects, then no do not
	    // account for mismatch.
	    for (int i = 0; i < elfTasks.length; i++) {
		// xfpregsets accompany fp registers on a 1:1 basis
		// but only on some architectures.
		ElfPrXFPRegSet xregSet = null;
		if (elfXFPRegs.length > 0)
		    xregSet = elfXFPRegs[i];
		new LinuxCoreTask(proc, elfTasks[i], elfFPRegs[i],
				  xregSet, isa);
	    }
	} else {
	    // Otherwise add only NT_FPREGSET data if pr_fpvalid is >
	    // 0. This value is not reliable on userland kernels (gdb
	    // always sets it to 0) so if we are here, this is a
	    // micro-optimized kernel where that flag is set
	    // correctly.
	    int fpCount = 0;
	    for (int i = 0; i < elfTasks.length; i++) {
		if (elfTasks[i].getPrFPValid() > 0) {
		    // xfpregsets accompany fp registers on a 1:1
		    // basis but only on some architectures.
		    ElfPrXFPRegSet xregSet = null;
		    if (elfXFPRegs.length > 0)
			xregSet = elfXFPRegs[fpCount];
		    new LinuxCoreTask(proc, elfTasks[i],
				      elfFPRegs[fpCount],
				      xregSet, isa);
		    fpCount++;
		} else
		    new LinuxCoreTask(proc, elfTasks[i],  null, null, isa);
	    }
	}
    }
}
