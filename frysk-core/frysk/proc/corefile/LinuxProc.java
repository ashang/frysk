// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.proc.corefile;

import lib.elf.Elf;
import lib.elf.ElfData;
import lib.elf.ElfException;
import lib.elf.ElfPrpsinfo;
import lib.elf.ElfPrAuxv;
import lib.elf.ElfEHeader;
import lib.elf.ElfPrstatus;
import lib.elf.ElfPHeader;
import lib.elf.ElfCommand;
import lib.elf.ElfSection;
import frysk.sys.proc.AuxvBuilder;
import java.util.logging.Level;
import java.util.Iterator;
import java.io.File;
import java.util.ArrayList;
import frysk.proc.Proc;
import frysk.proc.ProcState;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.Auxv;
import frysk.proc.Isa;
import frysk.proc.IsaFactory;
import frysk.proc.MemoryMap;

public class LinuxProc
    extends Proc
{
  
  private ElfData elfData = null;
  private ElfPrpsinfo elfProc = null;
  private CorefileByteBuffer memory = null;
  private File corefileBackEnd = null;
  private File exefileBackEnd = null;

  // Segment and solib metadata
  private MapAddressHeader metaData[];
  private boolean metaDataBuilt = false;

  public LinuxProc(ElfData data, LinuxHost host, ProcId procId )
  {
    super(host, null, procId);
    this.elfData = data;
    this.elfProc = ElfPrpsinfo.decode(elfData);
    this.corefileBackEnd = host.coreFile;

    // Executable is null (non-specified), find the exectuable
    // as it is written in the corefile. 
    if (host.exeFile == null)
      {
	File exeFileName = new File(sendrecExe());
	if ((exeFileName.exists()) && (exeFileName.canRead()))
	  host.exeFile = new File(sendrecExe());
      }
    this.exefileBackEnd = host.exeFile;
  }	

  protected String sendrecCommand() 
  {
    return elfProc.getPrFname();
  }

  protected String sendrecExe() 
  {
    // Only place to find full path + exe is
    // in the args list. Remove ./ if present.
    String[] args = sendrecCmdLine();
    if (args.length > 0)
      {
	if (args[0].startsWith("./"))
	  args[0]=args[0].substring(2);
	return args[0];
      }
    else
      return elfProc.getPrFname();
  }

  protected int sendrecUID() 
  {
    return (int) elfProc.getPrUid();
  }

  protected int sendrecGID() 
  {
    return (int) elfProc.getPrGid();
  }

  protected String[] sendrecCmdLine() 
  {

    // Split arguments by space
    String rawArgs = elfProc.getPrPsargs();
    String args[] = rawArgs.split(" ");
 
    return args;
  }

  protected CorefileByteBuffer getMemory()
  {

    // Build meta data once, and only on demand.
    if (!metaDataBuilt)
      constructMetaData ();

    // Only instantiate the memory access when asked, on demand
    // This save on fd's as every CorefileByteBuffer will use 
    // 2.

    if (memory == null)
      try 
	{
	  memory = new CorefileByteBuffer(this.corefileBackEnd, this.metaData);
	} 
      catch (ElfException e) 
	{
	  throw new RuntimeException(e);
	}

    return memory;
  }

  public void sendRefresh() 
  {
    // Find tasks. Refresh is a misnomer here as 
    // Corefiles will never spawn new tasks beyond the
    // original refresh, and will lose them. 

    ElfPrstatus elfTasks[] = null;
    elfTasks = ElfPrstatus.decode(elfData);
    for (int i=0; i<elfTasks.length; i++)
      {
    	Task newTask = new LinuxTask(LinuxProc.this, elfTasks[i]);
    	newTask.getClass();
      }
  }


  public MemoryMap[] sendrecMaps ()
  {

    ArrayList maps = new ArrayList ();

    // Build meta data if not already built.
    if (!metaDataBuilt)
      constructMetaData ();


    // Refactor metadata into format expected by clients of 
    // sendrecMaps.
    for (int i=0; i<metaData.length; i++)
	maps.add(new MemoryMap(metaData[i].vaddr, metaData[i].vaddr_end,
			       metaData[i].permRead, metaData[i].permWrite,
			       metaData[i].permExecute,false,
			       metaData[i].solibOffset,-1,-1,-1,-1,-1,
			       metaData[i].name));

    
    return (MemoryMap[]) maps.toArray(new MemoryMap[maps.size()]);
  }

  protected Auxv[] sendrecAuxv ()
  {
    final ElfPrAuxv prAuxv =  ElfPrAuxv.decode(elfData);

    class BuildAuxv
      extends AuxvBuilder
    {
      Auxv[] vec;
      public void buildBuffer (byte[] auxv)
      {
      }
      public void buildDimensions (int wordSize, boolean bigEndian,
                                   int length)
      {
        vec = new Auxv[length];
      }
      public void buildAuxiliary (int index, int type, long val)
      {
        vec[index] = new Auxv (type, val);
      }
    }

    BuildAuxv auxv = new BuildAuxv ();
    auxv.construct (prAuxv.getAuxvBuffer());
    return auxv.vec;
  }

  protected Isa sendrecIsa() 
  {
    logger.log(Level.FINE, "{0} sendrecIsa\n", this);

    ElfEHeader header = elfData.getParent().getEHeader();
    
    IsaFactory factory = IsaFactory.getSingleton();
    return factory.getIsaForCoreFile(header.machine);
  }

  protected ProcState getInitialState (boolean procStarting) 
  {
    return LinuxProcState.initial(this);
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
  private void constructMetaData ()
  {

    // We'll bake basic meta data for all corefiles
    metaData = constructBasicMapMetadata ();
    if (exefileBackEnd != null)
      {
	// If an executable is available that is paired with
	// the corefile, much richer metadata can be constructed.
	metaData = constructEnhancedMapMetadata (metaData);
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
  private MapAddressHeader[] constructBasicMapMetadata ()
  {

    ArrayList tempMetaData = new ArrayList ();
    // Read in contents of the corefile 
    Elf coreElf = openElf(this.corefileBackEnd);

    // Abort if cannot read core file.
    if (coreElf == null)
      return null;

    ElfEHeader eHeader = coreElf.getEHeader();
    for (int i=0; i<eHeader.phnum; i++)
      {
	// Test if pheader is of types LOAD. If so add to list
	ElfPHeader pHeader = coreElf.getPHeader(i);

	// Load each PT_LOAD segment
	if (pHeader.type == ElfPHeader.PTYPE_LOAD)
	  {
	    // Calculate flags
	    boolean read = (pHeader.flags &  ElfPHeader.PHFLAG_READABLE) > 0 ? true:false;
	    boolean write =  (pHeader.flags & ElfPHeader.PHFLAG_WRITABLE) > 0 ? true:false;
	    boolean execute = (pHeader.flags & ElfPHeader.PHFLAG_EXECUTABLE) > 0 ? true:false;	    

	    // Add basic meta data to list
	    tempMetaData.add(new MapAddressHeader(pHeader.vaddr,pHeader.vaddr+pHeader.memsz,read,
						  write, execute,
						  pHeader.offset, 
						  0,
						  pHeader.filesz, 
						  pHeader.memsz, 
						  "",0));
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
  private MapAddressHeader[] constructEnhancedMapMetadata(MapAddressHeader[] basicMetaData)
  {

    // Clone data. Don't use data that the caller passed.
    MapAddressHeader[] tempMaps = new MapAddressHeader[basicMetaData.length];
    System.arraycopy(basicMetaData,0,tempMaps,0,basicMetaData.length);

    // Find Dynamic Segment
    DynamicSegmentTuple dynamicTuple = getDynamicSegmentAddress();

    // From that segment address, find linkmap table.
    long linkmapAddress = getLinkmapAddress(dynamicTuple);


    // Edge case: Save interp name as it is not included the linkmap
    // as it is loaded by the kernel.
    String interpName = getExeInterpreterName ();

    // Edge case: Get interp address so when we traverse the linkmap
    // it can be paired with its name
    long interpAddr = getExeInterpreterAddress (); 

    // Edge case: the vdso name is not stored but the address is. Save
    // so we can pair with with [vdso] later.
    long coreVDSO = getCorefileVDSOAddress();

    // Build the linkmap table from the linkmap tabl address
    class BuildLinkMap
      extends LinkmapBuilder
    {

      public ArrayList list = new ArrayList();
      public void buildMap (long l_addr, long l_ld, long saddr, String name)
      {

	list.add(new Linkmap(l_addr, l_ld, saddr, name));
      }
    }

    BuildLinkMap linkMap = new BuildLinkMap();
    linkMap.construct(linkmapAddress, getMemory());
    Iterator linkMapIterator = linkMap.list.iterator();
    while (linkMapIterator.hasNext())
      {
	Linkmap tempMap = (Linkmap) linkMapIterator.next();
	if (tempMap.l_addr == coreVDSO)
	  tempMap.name = "[vdso]";
	if (tempMap.s_addr == interpAddr)
	  tempMap.name = interpName;
      }

    // From the list of solibs in the linkamp,  build
    // maps for each one.
    class BuildSOMaps
      extends SOLibMapBuilder

    {
      public ArrayList list = new ArrayList();

      public void buildMap (long addrLow, long addrHigh, 
			    boolean permRead, boolean permWrite,
			    boolean permExecute, long offset, 
			    String name, long align)
      {
	list.add(new MapAddressHeader(addrLow,addrHigh,permRead,
				      permWrite,permExecute,
				      0,offset, 
				      0,0, name, align));
      }

    }


    BuildSOMaps SOMaps = new BuildSOMaps();
    Iterator mapsIterator = linkMap.list.iterator();
    while (mapsIterator.hasNext())
      {
	Linkmap singleLinkMap = (Linkmap) mapsIterator.next();
	if ((!singleLinkMap.name.equals("")) && (!singleLinkMap.name.equals("[vdso]")))
	  SOMaps.construct(new File(singleLinkMap.name));
	if (singleLinkMap.name.equals("[vdso]"))
	  SOMaps.buildMap(singleLinkMap.l_addr,0,true,true,true,0,singleLinkMap.name,0x1000);
      }


    // Add in case for executables maps.
    SOMaps.construct(this.exefileBackEnd);

    // Reconcile maps
    Iterator i = SOMaps.list.iterator();
    while (i.hasNext())
      {
	MapAddressHeader localMap = (MapAddressHeader) i.next();
	for (int l=0; l<tempMaps.length; l++)
	  {
	    if ((tempMaps[l].vaddr == localMap.vaddr) || 
		((tempMaps[l].vaddr > localMap.vaddr) && (tempMaps[l].vaddr<=localMap.vaddr_end)))
	      {
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
  private boolean isExeProvided () 
  {
    if (this.exefileBackEnd != null) 
      if ((this.exefileBackEnd.isFile()) && (this.exefileBackEnd.canRead()))
	return true;
    return false;
  }

  /**
   * Helper function to return the backing core file's dynamic
   * segment address and size
   */
  private DynamicSegmentTuple getDynamicSegmentAddress()
  {

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
    
    return exeDynamicTuple;
    
  }

  /**
   * Helper function to find the backing executable's dynamic
   * address and size
   **/
  private DynamicSegmentTuple getExeDynamicSegmentAddress ()
  {
    
    DynamicSegmentTuple exeDynamicAddr = null;
    Elf exeElf = openElf(this.exefileBackEnd);
    if (exeElf != null)
      {
	ElfEHeader eHeader = exeElf.getEHeader();
	

	// Find dynamic segment by iterating through program segment
	// headers
	for(int headerCount=0; headerCount<eHeader.phnum; headerCount++)
	  {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);

	    // Found the dynamic section
	    if (pHeader.type == ElfPHeader.PTYPE_DYNAMIC)
	      {
		exeDynamicAddr = new DynamicSegmentTuple(pHeader.vaddr,
							 pHeader.filesz);
		break;
	      }
	  }
	
	exeElf.close();
      }

    return exeDynamicAddr;
  }

  /**
   * Helper function to locate the link map table in the 
   * core file. This is located in the dynamic segment table
   * at the address specified by the DT_DEBUG field.
   */
  private long getLinkmapAddress(DynamicSegmentTuple tuple)
  {
    final int DT_DEBUG = 21;
    if (tuple == null)
      return 0;

    long dynSegmentEndAddress = tuple.addr + tuple.size;
    long dtDebugAddress = 0;
    long actualAddress = 0;
    long dtTest;

    // Get an instance of the corefile's memory 
    // and position it at the corefile's dynamic
    // segment.
    CorefileByteBuffer internalMem = getMemory();
    internalMem.position(tuple.addr);

    // find DT_DEBUG field in table. The tabke is two
    // words. One is the DT_ tag and the other
    // is the address of that DT_ tag table type.
    while (internalMem.position() < dynSegmentEndAddress)
      {
	// Get tag and test if it is DT_DEBUG
	dtTest = internalMem.getUWord();
	if (dtTest == DT_DEBUG)
	  {
	    // If it is record the address in the 
	    // next word.
	    dtDebugAddress = internalMem.getUWord();
	    break;
	  }
	
	// Otherwise, move on.
	internalMem.getUWord();
      }

    if (dtDebugAddress != 0)
      {
	// Go to address that DT_DEBUG tag 
	// specified.
	internalMem.position(dtDebugAddress);

	// discard first word at that address;
	internalMem.getUWord();
	actualAddress = internalMem.getUWord();
      }

    return actualAddress;
  }

  /**
   * Helper function to locate and report the backing Executables
   * entry point
   */
  private long getExeEntryPoint ()
  {
    
    long entryPoint = 0;
    Elf exeElf = openElf(this.exefileBackEnd);

    if (exeElf != null)
      {
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
  private long getExeInterpreterAddress ()
  {
    Elf exeElf = openElf(this.exefileBackEnd);
    long interpreterAddress = 0;
    if (exeElf != null)
      {
	ElfEHeader eHeader = exeElf.getEHeader();
	// Find .interp segment by passing through progream segment
	// header
	for(int headerCount=0; headerCount<eHeader.phnum; headerCount++)
	  {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP)
	      {
		interpreterAddress = pHeader.vaddr;
		break;
	      }
	  }
	
	exeElf.close();
      }

    return interpreterAddress;
  }

  /**
   * Helper function to locate and report the backing Executables
   * interpeters name
   */
  private String getExeInterpreterName ()
  {
    Elf exeElf = openElf(this.exefileBackEnd);
    String interpName = "";
    if (exeElf != null)
      {
	ElfEHeader eHeader = exeElf.getEHeader();
    
	// Find .interp segment by passing through progream segment
	// header
	for(int headerCount=0; headerCount<eHeader.phnum; headerCount++)
	  {
	    ElfPHeader pHeader = exeElf.getPHeader(headerCount);
	    if (pHeader.type == ElfPHeader.PTYPE_INTERP)
	      {
		ElfSection interpSection = exeElf.getSection((long)headerCount);
		ElfData data = interpSection.getData();
		interpName = new String(data.getBytes());
		interpName = interpName.trim();
		break;
	      }
	  }
	
	exeElf.close();
      }

    return interpName;
  }

  /**
   * Helper function to locate and report the backing corefile's
   * entry point
   */
  private long getCorefileEntryPoint ()
  {
    // Need auxv data
    Auxv[] auxv = sendrecAuxv ();
    long entryPoint = 0;

    if (auxv == null)
      return 0;

    // Find the Auxv ENTRY data
    for (int i = 0; i < auxv.length; i++)
      if (auxv[i].type == inua.elf.AT.ENTRY)
	{
	  entryPoint = auxv[i].val;
	  break;
	}

    return entryPoint;
  }

  /**
   * Helper function to locate and report the backing corefile's
   * VDSO address
   */
  private long getCorefileVDSOAddress ()
  {
    Auxv[] auxv = sendrecAuxv ();
    long vdsoEntryPoint = 0;

    if (auxv == null)
      return 0;

    // Find the SYSINFO_EHDR data
    for (int i = 0; i < auxv.length; i++)
      if (auxv[i].type == inua.elf.AT.SYSINFO_EHDR)
	{
	  vdsoEntryPoint = auxv[i].val;
	  break;
	}

    return vdsoEntryPoint;
  }




  /**
   *
   * Helper function to open an elf file
   *
   */
  private Elf openElf(File name)
  {

    Elf exeElf = null;

    if (name == null)
      return null;

    if ((name.exists()) && (name.isFile()) && (name.canRead()))
      {
	// Open up corefile corresponding directory.
	try 
	  {
	    exeElf = new Elf(name.getPath(), ElfCommand.ELF_C_READ);
	  }
	catch (Exception e)
	  {
	    throw new RuntimeException(e);
	  }
	
	return exeElf;
      }
    else
      return null;
  }


  // Private class to hold Dynamic Segment address tuple
  private class DynamicSegmentTuple
  {
    long addr = 0;
    long size = 0;
    public DynamicSegmentTuple(long addr, long size)
    {
      this.addr = addr;
      this.size = size;
    }
  }

  private class Linkmap
  {

    long l_addr = 0;
    long l_dyn = 0;
    long s_addr = 0;
    String name = "";

    public Linkmap(long l_addr, long l_dyn, long s_addr, String name)
    {

      this.l_addr = l_addr;
      this.l_dyn = l_dyn;
      this.s_addr = s_addr;
      this.name = name;
    }
  }




}

