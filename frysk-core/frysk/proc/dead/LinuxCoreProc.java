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

import lib.dwfl.Elf;
import lib.dwfl.ElfData;
import lib.dwfl.ElfException;
import lib.dwfl.ElfPrpsinfo;
import lib.dwfl.ElfPrAuxv;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPrstatus;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfPrFPRegSet;
import lib.dwfl.ElfPrXFPRegSet;
import frysk.sys.proc.AuxvBuilder;
import java.util.Iterator;
import java.io.File;
import java.util.ArrayList;
import frysk.proc.Task;
import frysk.proc.Auxv;
import frysk.proc.MemoryMap;
import frysk.isa.ISA;
import frysk.isa.ElfMap;
import frysk.rsl.Log;

public class LinuxCoreProc extends DeadProc {
  
    private ElfData elfData = null;
    private ElfPrpsinfo elfProc = null;
    private CorefileByteBuffer memory = null;
    private File corefileBackEnd = null;
    private File exefileBackEnd = null;

    // Segment and solib metadata
    private MapAddressHeader metaData[];
    private boolean metaDataBuilt = false;
    
    private static final Log fine = Log.fine(LinuxCoreProc.class);


    public LinuxCoreProc(ElfData data, LinuxCoreHost host, int pid) {
	super(host, null, pid);
	this.elfData = data;
	this.elfProc = ElfPrpsinfo.decode(elfData);
	this.corefileBackEnd = host.coreFile;
	fine.log(this, "LinuxCoreProc elfData", data, "host", host, "pid", pid);

	// Executable is null (non-specified), find the executable
	// as it is written in the corefile. 
	if ((host.exeFile == null) && (host.exeSetToNull == false)) {
	    File exeFileName = new File(getExe());
	    if ((exeFileName.exists()) && (exeFileName.canRead()))
		host.exeFile = exeFileName;
	
	}
	this.exefileBackEnd = host.exeFile;
    }	

    public String getCommand() {
	fine.log(this,"getCommand()", elfProc.getPrFname());
	return elfProc.getPrFname();
    }

    public String getExe() {
	if (this.exefileBackEnd != null) {
	    fine.log(this,"getExe()", this.exefileBackEnd.getPath());
	    return this.exefileBackEnd.getPath();
	}
	// Only place to find full path + exe is
	// in the args list. Remove ./ if present.
	String[] args = getCmdLine();
	if (args.length > 0) {
	    if (args[0].startsWith("./"))
		args[0]=args[0].substring(2);
	    fine.log(this, "getExe()", args[0]);
	    return args[0];
	} else {
	    fine.log(this, "getExe()", elfProc.getPrFname());
	    return elfProc.getPrFname();
	}
    }

    public int getUID() {
	fine.log(this,"getUID()", elfProc.getPrUid());
	return (int) elfProc.getPrUid();
    }

    public int getGID() {
	fine.log(this,"getGID()", elfProc.getPrGid());
	return (int) elfProc.getPrGid();
    }

    public String[] getCmdLine() {

	// Split arguments by space
	String rawArgs = elfProc.getPrPsargs();
	String args[] = rawArgs.split(" ");
 
	fine.log(this,"getCmdLine()", args);
	return args;
    }

    protected CorefileByteBuffer getMemory() {

	// Build meta data once, and only on demand.
	if (!metaDataBuilt)
	    constructMetaData ();

	// Only instantiate the memory access when asked, on demand
	// This save on fd's as every CorefileByteBuffer will use 
	// 2.

	if (memory == null)
	    try {
		memory = new CorefileByteBuffer(this.corefileBackEnd, this.metaData);
	    } catch (ElfException e) 		{
		throw new RuntimeException(e);
	    }

	fine.log(this, "getMemory() caller: ", Log.CALLER);
	return memory;
    }

    public void sendRefresh() {
	// Find tasks. Refresh is a misnomer here as 
	// Corefiles will never spawn new tasks beyond the
	// original refresh, or will lose them. 

	ElfPrstatus elfTasks[] = null;
	ElfPrFPRegSet elfFPRegs[] = null;
	ElfPrXFPRegSet elfXFPRegs[] = null;
	int fpCount = 0;

	// Decode both task and floating point registers
	elfTasks = ElfPrstatus.decode(elfData);
	elfFPRegs = ElfPrFPRegSet.decode(elfData);
	elfXFPRegs = ElfPrXFPRegSet.decode(elfData);
    
	ISA isa = ElfMap.getISA(elfData.getParent().getEHeader());

	// Two methods of whether Floating Point note data exists.
	// In userland generated core-dumps there is no way to test
	// if floating point data operations have actually occurred, so
	// programs like fcore/gcore will always write NT_FPREGSET note data
	// per thread regardless. On kernel generated corefiles, the
	// kernel micro-optimizes whether NT_FPREGSET note data is written
	// per thread by analyzing to see if that thread has performed
	// Floating Point operations. If it has, it will write
	// NT_FPREGSET, and if it hasn't it won't.

	// Account for both these scenarios, here.
    
	Task newTask = null;
    
	// If the number of NT_FPREGSET note objects is equal to the
	// the number of NT_PRSTATUS note objects, then no do not account
	// for mismatch. 
	if (elfFPRegs.length == elfTasks.length)
	    for (int i=0; i<elfTasks.length; i++) {
		// xfpregsets accompany fp registers on a 1:1  basis
		// but only on some architectures.
		ElfPrXFPRegSet xregSet = null;
		if (elfXFPRegs.length > 0)
		    xregSet = elfXFPRegs[i];
		newTask = new LinuxCoreTask(LinuxCoreProc.this, elfTasks[i], elfFPRegs[i], xregSet, isa);
	    } else {
	    // Otherwise add only NT_FPREGSET data if pr_fpvalid is > 0. This
	    // value is not reliable on userland kernels (gdb always sets it
	    // to 0) so if we are here, this is a micro-optimized kernel where
	    // that flag is set correctly.
	    for (int i=0; i<elfTasks.length; i++) {
	    
		if (elfTasks[i].getPrFPValid() > 0) {

		    // xfpregsets accompany fp registers on a 1:1  basis
		    // but only on some architectures.
		    ElfPrXFPRegSet xregSet = null;
		    if (elfXFPRegs.length > 0)
			xregSet = elfXFPRegs[fpCount];
		    newTask = new LinuxCoreTask(LinuxCoreProc.this, elfTasks[i], elfFPRegs[fpCount], xregSet, isa);
		    fpCount++;
		} else
		    newTask = new LinuxCoreTask(LinuxCoreProc.this, elfTasks[i],  null, null, isa);
	    
	    }
	}
	newTask.getClass();
    }


    private MemoryMap[] memoryMaps;
    public MemoryMap[] getMaps () {
	fine.log(this,"getMaps()");
	if (memoryMaps == null) {
	    fine.log(this,"getMaps() - Maps need to be built");
	    ArrayList maps = new ArrayList ();
	    // Build meta data if not already built.
	    if (!metaDataBuilt)
		constructMetaData ();

	    // Refactor metadata into format expected by clients of 
	    // sendrecMaps.
	    for (int i=0; i<metaData.length; i++) {
		fine.log(this,"getMaps() maps ",
			 
			 "0x"+Long.toHexString(metaData[i].vaddr)+ 
			 "-0x" + Long.toHexString(metaData[i].vaddr_end)+
			 " read: " + metaData[i].permRead + 
			 " write: " + metaData[i].permWrite +
			 " execute: " + metaData[i].permExecute + 
			 " false" +
			 " 0x" +  Long.toHexString(metaData[i].solibOffset) + 
			 " -1,-1,-1,-1,-1, " +
			 " " + metaData[i].name+"\n");

		maps.add(new MemoryMap(metaData[i].vaddr, metaData[i].vaddr_end,
				       metaData[i].permRead, metaData[i].permWrite,
				       metaData[i].permExecute,false,
				       metaData[i].solibOffset,-1,-1,-1,-1,-1,
				       metaData[i].name));
		}
	    memoryMaps = (MemoryMap[]) maps.toArray(new MemoryMap[maps.size()]);
	} else { fine.log("getMaps() - maps already built, returning cache");}


	fine.log(this,"getMaps() - Completed metadata.");

	return memoryMaps;
    }

    private Auxv[] auxv;
    public Auxv[] getAuxv() {

	fine.log(this,"getAuxv()", Log.CALLER);

	if (auxv == null) {
	    fine.log(this,"Auxv is null, building");
	    final ElfPrAuxv prAuxv =  ElfPrAuxv.decode(elfData);
	    class BuildAuxv extends AuxvBuilder {
		Auxv[] vec;
		public void buildBuffer (byte[] auxv) {
		}
		public void buildDimensions (int wordSize, boolean bigEndian,
					     int length) {
		    vec = new Auxv[length];
		}
		public void buildAuxiliary (int index, int type, long val) {
		    vec[index] = new Auxv (type, val);
		}
	    }
	    BuildAuxv auxv = new BuildAuxv ();
	    auxv.construct (prAuxv.getAuxvBuffer());
	    this.auxv = auxv.vec;
	} else {fine.log(this,"Returning cached Auxv");}
	return auxv;
    }

    /**
     * XXX: Meta Data construction functions.
     * Not really part of the implementation of proc, but has a very
     * close affinity to the data contained in proc. Not figured out a
     * suitable patterns yet
     **/
  

    /**
     *
     * Wrapper function to construct Corefile meta data
     *
     */
    private void constructMetaData () {

	fine.log(this,"constructMetaData()");

	// We'll bake basic meta data for all corefiles
	metaData = constructBasicMapMetadata ();

	if (exefileBackEnd != null) {
	    fine.log(this,"constructMetaData() - exe backend is not " +
		     "null, bake enhanced data");
	    // If an executable is available that is paired with
	    // the corefile, much richer metadata can be constructed.
	    metaData = constructEnhancedMapMetadata (metaData);
	    fine.log(this,"constructMetaData() - Enhanced metadata complete.");
	}
    }


    /**
     *
     * Build basic metadata fore a corefile.
     * A backing executable is not necessary for basic
     * metadata as it only contains the core file program
     * segment headers and the corefile offsets
     *
     */
    private MapAddressHeader[] constructBasicMapMetadata () {

	fine.log(this,"constructBasicMapMetaData()");	
	String name = "";
	ArrayList tempMetaData = new ArrayList ();
	// Read in contents of the corefile 
	Elf coreElf = openElf(this.corefileBackEnd);

	// Abort if cannot read core file.
	if (coreElf == null)
	    return null;

	long coreVDSO = this.getCorefileVDSOAddress();
	ElfEHeader eHeader = coreElf.getEHeader();
	for (int i=0; i<eHeader.phnum; i++) {
	    // Test if pheader is of types LOAD. If so add to list
	    ElfPHeader pHeader = coreElf.getPHeader(i);

	    // Load each PT_LOAD segment
	    if (pHeader.type == ElfPHeader.PTYPE_LOAD) {
		// Calculate flags
		boolean read = (pHeader.flags &  ElfPHeader.PHFLAG_READABLE) > 0 ? true:false;
		boolean write =  (pHeader.flags & ElfPHeader.PHFLAG_WRITABLE) > 0 ? true:false;
		boolean execute = (pHeader.flags & ElfPHeader.PHFLAG_EXECUTABLE) > 0 ? true:false;	    

		if (coreVDSO == pHeader.vaddr)
		    name="[vdso]";
		// Add basic meta data to list
		tempMetaData.add(new MapAddressHeader(pHeader.vaddr,pHeader.vaddr+pHeader.memsz,read,
						      write, execute,
						      pHeader.offset, 
						      0,
						      pHeader.filesz, 
						      pHeader.memsz, 
						      name,0));
		name = "";
	    }
	}  

	coreElf.close();
	metaDataBuilt = true;
	return (MapAddressHeader[]) tempMetaData.toArray(new MapAddressHeader[tempMetaData.size()]);
    
    }
  
    /**
     *
     * Build enhanced metadata for a corefile.
     * A backing executable is necessary for enhanced
     * metadata and is required. If not exe is available
     * enhanced meta daa is not built, and elided segment access
     * and named maps disallowed
     *
     */
    private MapAddressHeader[] constructEnhancedMapMetadata(MapAddressHeader[] basicMetaData) {
	
	fine.log(this,"constructEnhancedMapMetadata()");
	// Clone data. Don't use data that the caller passed.
	MapAddressHeader[] tempMaps = new MapAddressHeader[basicMetaData.length];
	System.arraycopy(basicMetaData,0,tempMaps,0,basicMetaData.length);

	// Find Dynamic Segment
	DynamicSegmentTuple dynamicTuple = getDynamicSegmentAddress();
	
	// From that segment address, find linkmap table.
	long linkmapAddress = getLinkmapAddress(dynamicTuple);

	// No link map = no libraries loaded (core at entry point)
	if (linkmapAddress == 0)
	    return basicMetaData;

	// Edge case: Save interp name as it is not included the linkmap
	// as it is loaded by the kernel.
	String interpName = getExeInterpreterName ();

	// Edge case: Get interp address so when we traverse the linkmap
	// it can be paired with its name
	long interpAddr = getExeInterpreterAddress (); 
	
	// Build the linkmap table from the linkmap tabl address
	class BuildLinkMap extends LinkmapBuilder {

	    public ArrayList list = new ArrayList();
      
	    public void buildMap (long l_addr, long l_ld, long saddr, String name) {
		fine.log(this, "New linkmap item: l_addr = 0x"+Long.toHexString(l_addr) +
			 " l_ld = 0x"+Long.toHexString(l_ld) +
			 " s_addr = 0x"+Long.toHexString(l_addr) + 
			 " name = " + name);
		list.add(new Linkmap(l_addr, l_ld, saddr, name));
	    }
	}
    
	// Create a temporary, noncache resetting view into the corefile
	// memory. We can pass metadata as basic metadata has already
	// been constructed.	
	CorefileByteBuffer tempMemory = null;
	try {
	    tempMemory = new CorefileByteBuffer(this.corefileBackEnd, metaData);
	} catch (ElfException e) {
	    throw new RuntimeException(e);
	}
    
	fine.log(this, "Building linkmap");
	BuildLinkMap linkMap = new BuildLinkMap();
	linkMap.construct(linkmapAddress,tempMemory);
	Iterator linkMapIterator = linkMap.list.iterator();
	while (linkMapIterator.hasNext()) {
	    Linkmap tempMap = (Linkmap) linkMapIterator.next();
	    if (tempMap.s_addr == interpAddr) {
		fine.log("Found interpretator at linkmap address 0x"+Long.toHexString(tempMap.s_addr));
		tempMap.name = interpName;
	    }
	}

	// From the list of solibs in the linkamp,  build
	// maps for each one.
	class BuildSOMaps extends SOLibMapBuilder {
	    public ArrayList list = new ArrayList();

	    public void buildMap (long addrLow, long addrHigh, 
				  boolean permRead, boolean permWrite,
				  boolean permExecute, long offset, 
				  String name, long align) {
		list.add(new MapAddressHeader(addrLow,addrHigh,permRead,
					      permWrite,permExecute,
					      0,offset, 
					      0,0, name, align));
	    }

	}


	BuildSOMaps SOMaps = new BuildSOMaps();
	Iterator mapsIterator = linkMap.list.iterator();
	while (mapsIterator.hasNext()) {
	    Linkmap singleLinkMap = (Linkmap) mapsIterator.next();
	    if ((!singleLinkMap.name.equals("")) && (!singleLinkMap.name.equals("[vdso]")))
		SOMaps.construct(new File(singleLinkMap.name),
				 singleLinkMap.l_addr);
	    if (singleLinkMap.name.equals("[vdso]"))
		SOMaps.buildMap(singleLinkMap.l_addr,0,true,true,true,0,singleLinkMap.name,0x1000);
	}


	// Add in case for executables maps.
	SOMaps.construct(this.exefileBackEnd, 0);

	// Reconcile maps
	Iterator i = SOMaps.list.iterator();
	while (i.hasNext()) {
	    MapAddressHeader localMap = (MapAddressHeader) i.next();
	    for (int l=0; l<tempMaps.length; l++) {
		if ((tempMaps[l].vaddr == localMap.vaddr) || 
		    ((tempMaps[l].vaddr > localMap.vaddr) && (tempMaps[l].vaddr<localMap.vaddr_end))) {
		    if (tempMaps[l].vaddr_end == 0)
			tempMaps[l].vaddr_end = ((tempMaps[l].vaddr + tempMaps[l].memSize) + 0x1000 -1) &~ (0x1000-1);

		    tempMaps[l].solibOffset = localMap.solibOffset;
		    tempMaps[l].name = localMap.name;
		}
	    }
	}

    
 
	return tempMaps;
    }


    /**
     * Private helper functions for class. Perhaps these should be
     * refactored out to a util class, or into a factory of some kind
     *
     **/ 

    /**
     * Single instance test to determine whether a backing store
     * executable has been provided
     */
    private boolean isExeProvided () {
	boolean provided = false;

	if (this.exefileBackEnd != null) 
	    if ((this.exefileBackEnd.isFile()) && (this.exefileBackEnd.canRead()))
		provided = true;
 	fine.log(this, "isExeProvided()" + provided);
	
	return provided;
    }

    /**
     * Helper function to return the backing core file's dynamic
     * segment address and size
     */
    private DynamicSegmentTuple getDynamicSegmentAddress() {

	fine.log(this,"getDynamicSegmentAddress()");
	// If we do not have an executable, we cannot find
	// the dynamic segment in the corefile.
	if (!isExeProvided ())
	    return null;

	long coreEntryPoint = getCorefileEntryPoint ();
	long exeEntryPoint = getExeEntryPoint ();
	DynamicSegmentTuple exeDynamicTuple = getExeDynamicSegmentAddress ();

	// Calculate relocated segment address, if necessary.
	if (exeDynamicTuple != null)
	    exeDynamicTuple.addr = exeDynamicTuple.addr + 
		coreEntryPoint - exeEntryPoint;
    
	fine.log(this,"getDynamicSegmentAddress() tuple: 0x" + 
		 Long.toHexString(exeDynamicTuple.addr) + 
		 " Size " + exeDynamicTuple.size);
	return exeDynamicTuple;
    
    }

    /**
     * Helper function to find the backing executable's dynamic
     * address and size
     **/
    private DynamicSegmentTuple getExeDynamicSegmentAddress () {
    
	fine.log(this,"getExeDynamicSegmentAddress()");
	DynamicSegmentTuple exeDynamicAddr = null;
	Elf exeElf = openElf(this.exefileBackEnd);
	if (exeElf != null) {
	    ElfEHeader eHeader = exeElf.getEHeader();
	    
	    // Find dynamic segment by iterating through program segment
	    // headers
	    for(int headerCount=0; headerCount<eHeader.phnum; headerCount++) {
		ElfPHeader pHeader = exeElf.getPHeader(headerCount);

		// Found the dynamic section
		if (pHeader.type == ElfPHeader.PTYPE_DYNAMIC) {
		    exeDynamicAddr = new DynamicSegmentTuple(pHeader.vaddr,
							     pHeader.filesz);
		    break;
		}
	    }
	
	    exeElf.close();
	} 

	fine.log(this,"getDynamicExeSegmentAddress() tuple: 0x" + 
		 Long.toHexString(exeDynamicAddr.addr) + 
		 " Size " + exeDynamicAddr.size);

	return exeDynamicAddr;
    }

    /**
     * Helper function to locate the link map table in the 
     * core file. This is located in the dynamic segment table
     * at the address specified by the DT_DEBUG field.
     */
    private long getLinkmapAddress(DynamicSegmentTuple tuple) {

	fine.log(this,"getLinkmapAddress()");
	final int DT_DEBUG = 21;
	if (tuple == null) {
	    fine.log(this,"Dynamic segment is null, linkmap set to 0");
	    return 0;

	}

	long dynSegmentEndAddress = tuple.addr + tuple.size;
	long dtDebugAddress = 0;
	long actualAddress = 0;
	long dtTest;

	// Create an instance of the corefile's memory 
	// and position it at the corefile's dynamic
	// segment.
	CorefileByteBuffer internalMem = null;
	try {
	    internalMem = new CorefileByteBuffer(this.corefileBackEnd, metaData);
	} catch (ElfException e) {
	    throw new RuntimeException(e);
	}
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

	fine.log(this,"Linkmap address is: 0x"+Long.toHexString(actualAddress));
	return actualAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * entry point
     */
    private long getExeEntryPoint () {
    
	long entryPoint = 0;
	Elf exeElf = openElf(this.exefileBackEnd);

	if (exeElf != null) {
	    ElfEHeader eHeader = exeElf.getEHeader();
	    exeElf.close();
	    entryPoint = eHeader.entry;
	}
	
	return entryPoint;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters address
     */
    private long getExeInterpreterAddress () {

	fine.log(this,"getExeInterpreterAddress()");
	Elf exeElf = openElf(this.exefileBackEnd);
	long interpreterAddress = 0;
	if (exeElf != null) {
	    ElfEHeader eHeader = exeElf.getEHeader();
	    // Find .interp segment by passing through progream segment
	    // header
	    for(int headerCount=0; headerCount<eHeader.phnum; headerCount++) {
		ElfPHeader pHeader = exeElf.getPHeader(headerCount);
		if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		    interpreterAddress = pHeader.vaddr;
		    break;
		}
	    }
	
	    exeElf.close();
	}

	fine.log(this,"Interpreter Addr: 0x"+Long.toHexString(interpreterAddress));
	return interpreterAddress;
    }

    /**
     * Helper function to locate and report the backing Executables
     * interpeters name
     */
    private String getExeInterpreterName () {
	fine.log(this,"getExeInterpreterName()");
	Elf exeElf = openElf(this.exefileBackEnd);
	String interpName = "";
	if (exeElf != null) {
	    ElfEHeader eHeader = exeElf.getEHeader();
    
	    // Find .interp segment by passing through progream segment
	    // header
	    for(int headerCount=0; headerCount<eHeader.phnum; headerCount++) {
		ElfPHeader pHeader = exeElf.getPHeader(headerCount);
		if (pHeader.type == ElfPHeader.PTYPE_INTERP) {
		    ElfSection interpSection = exeElf.getSection((long)headerCount);
		    ElfData data = interpSection.getData();
		    interpName = new String(data.getBytes());
		    interpName = interpName.trim();
		    break;
		}
	    }
	
	    exeElf.close();
	}
	fine.log(this,"Interpreter name:", interpName);
	return interpName;
    }

    /**
     * Helper function to locate and report the backing corefile's
     * entry point
     */
    private long getCorefileEntryPoint () {
	fine.log(this,"getCorefileEntryPoint()");
	// Need auxv data
	Auxv[] auxv = getAuxv ();
	long entryPoint = 0;

	if (auxv == null)
	    return 0;

	// Find the Auxv ENTRY data
	for (int i = 0; i < auxv.length; i++)
	    if (auxv[i].type == inua.elf.AT.ENTRY) {
		entryPoint = auxv[i].val;
		break;
	    }

	fine.log(this,"Corefile Entrypoint: 0x"+Long.toHexString(entryPoint));
	return entryPoint;
    }

    /**
     * Helper function to locate and report the backing corefile's
     * VDSO address
     */
    private long getCorefileVDSOAddress () {
	fine.log(this,"getCorefileVDSOAddress()");
	Auxv[] auxv = getAuxv();
	long vdsoEntryPoint = 0;

	if (auxv == null)
	    return 0;

	// Find the SYSINFO_EHDR data
	for (int i = 0; i < auxv.length; i++)
	    if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR) {
		vdsoEntryPoint = auxv[i].val;
		break;
	    }
	fine.log(this,"Corefile VDSO Address 0x"+Long.toHexString(vdsoEntryPoint));
	return vdsoEntryPoint;
    }




    /**
     *
     * Helper function to open an elf file
     *
     */
    private Elf openElf(File name) {

	Elf exeElf = null;

	if (name == null)
	    return null;

	if ((name.exists()) && (name.isFile()) && (name.canRead())) {
	    // Open up corefile corresponding directory.
	    exeElf = new Elf(name, ElfCommand.ELF_C_READ);
	    return exeElf;
	} else
	    return null;
    }


    // Private class to hold Dynamic Segment address tuple
    private class DynamicSegmentTuple {
	long addr = 0;
	long size = 0;
	public DynamicSegmentTuple(long addr, long size) {
	    this.addr = addr;
	    this.size = size;
	}
    }

    private class Linkmap {

	long l_addr = 0;
	long l_dyn = 0;
	long s_addr = 0;
	String name = "";

	public Linkmap(long l_addr, long l_dyn, long s_addr, String name) {

	    this.l_addr = l_addr;
	    this.l_dyn = l_dyn;
	    this.s_addr = s_addr;
	    this.name = name;
	}
    }
}
