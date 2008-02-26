// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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


package frysk.testbed;

import inua.eio.ByteOrder;

import java.io.File;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfKind;
import lib.dwfl.ElfPHeader;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.isa.ISA;
import frysk.proc.Auxv;
import frysk.proc.Manager;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.dead.LinuxCoreFactory;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.SlaveOffspring;
import frysk.testbed.TestLib;

public class TestCoredumpAction
    extends TestLib
{

  public void testCoreFileCreated ()
  {

    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Core file " + coreFileName + " does not exist.",
               testCore.exists());
    testCore.delete();
  }

  public void testElfCoreHeader ()
  {


    Proc ackProc = giveMeABlockedProc();
    ISA isa = getISA(ackProc);
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Checking core file " + coreFileName + " exists.",
               testCore.exists());

    ByteOrder order = isa.order();

    Elf local_elf = getElf(coreFileName);
    assertNotNull("elf variable is null", local_elf);

    assertEquals("Checking ELF Kind", local_elf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals("Checkin base 0", local_elf.getBase(), 0);

    ElfEHeader header = local_elf.getEHeader();

    // Check the elf file MSB/LSB byte according to the endian
    // level returned from Isa.
    if (order == inua.eio.ByteOrder.BIG_ENDIAN)
      assertEquals("Checking endian is appropriate to platform",
                   header.ident[5], ElfEHeader.DATA2MSB);
    else
      assertEquals("Checking endian is appropriate to platform",
                   header.ident[5], ElfEHeader.DATA2LSB);

    // Check version written
    assertEquals("Checking elf version and ident core file version",
                 header.ident[6], (byte) local_elf.getElfVersion());

    // Check version written
    assertEquals("Checking elf version and non-ident core file version",
                 header.version, (byte) local_elf.getElfVersion());

    // Check Header Type
    assertEquals("Checking Header type is ET_CORE", header.type,
                 ElfEHeader.PHEADER_ET_CORE);

    // Check machine and class
    if (isa == ISA.IA32) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_386);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.CLASS32);
    } else if (isa == ISA.PPC64BE) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_PPC64);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.CLASS64);
    } else if (isa == ISA.PPC32BE) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_PPC);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.CLASS32);
    } else if (isa == ISA.X8664) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_X86_64);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.CLASS64);
    } else {
	fail("unknown isa: " + isa);
    }

    testCore.delete();
  }

  public void testProgramSegmentHeader ()
  {


    Proc ackProc = giveMeAProc();

    // Create a corefile from process
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
 
    assertTrue("Checking core file " + coreFileName + " exists.",
            testCore.exists());

    // Model the corefile, and get the Process.
    Proc coreProc = LinuxCoreFactory.createProc(testCore,
						new File(ackProc.getExe()));
    assertNotNull("Checking core file process", coreProc);    
   
    MemoryMap[] coreMaps = coreProc.getMaps();
    MemoryMap[] liveMaps = ackProc.getMaps();
    
    for(int i=0; i<liveMaps.length; i++)
    {
	if (liveMaps[i].permRead == false)
	    continue;
	int index = findLowAddress(liveMaps[i].addressLow,coreMaps);
	assertTrue("Check can locate core map 0x"+Long.toHexString(liveMaps[i].addressLow),
		index >= 0);
	assertEquals("addressLow matches in core and live proc", coreMaps[index].addressLow, 
			liveMaps[i].addressLow);	
	assertEquals("addressHigh matches in core and live proc", coreMaps[index].addressHigh, 
			liveMaps[i].addressHigh);	
	assertEquals("execute flag matches in core and live proc", coreMaps[index].permExecute, 
		liveMaps[i].permExecute);	
	assertEquals("write flag matches in core and live proc", coreMaps[index].permWrite, 
		liveMaps[i].permWrite);	
	assertEquals("read flag matches in core and live proc", coreMaps[index].permRead, 
		liveMaps[i].permRead);	
	
    }
    
    testCore.delete();
  }

  public void testAuxv ()
  {
      // Construct a process
      Proc ackProc = giveMeABlockedProc();
      assertNotNull("Found Process",ackProc);
      
      // Create a corefile from process
      String coreFileName = constructCore(ackProc);
      File testCore = new File(coreFileName);
      assertTrue("Checking core file " + coreFileName + " exists.",
                 testCore.exists());

      // Model the corefile, and get the Process.
      Proc coreProc = LinuxCoreFactory.createProc(testCore);      
      assertNotNull("Checking core file process", coreProc);
      
      Auxv[] coreAux = coreProc.getAuxv();
      Auxv[] liveAux = ackProc.getAuxv();

      assertEquals("AuxV length is same as live", coreAux.length,  liveAux.length);
      for (int i=0; i<coreAux.length; i++)
	{
	  assertEquals("AuxV types matches live", coreAux[i].type, liveAux[i].type);
	  assertEquals("AuxV value matches live", coreAux[i].val, liveAux[i].val);
	}
      
      testCore.delete();
  }


  public void testStackOnlyMap () 
  {
    Proc ackProc = giveMeAProc();
    MemoryMap stackMap = null;
    MemoryMap coreMap = null;
    // Create a corefile from process
    String coreFileName = constructStackOnlyCore(ackProc);
    File testCore = new File(coreFileName);
 
    assertTrue("Checking core file " + coreFileName + " exists.",
            testCore.exists());

    // Model the corefile, and get the Process.
    Proc coreProc = LinuxCoreFactory.createProc(testCore,
						new File(ackProc.getExe()));
    assertNotNull("Checking core file process", coreProc);    
   
    MemoryMap[] coreMaps = coreProc.getMaps();
    MemoryMap[] liveMaps = ackProc.getMaps();
    
    for(int i=0; i<liveMaps.length; i++) {
	if (liveMaps[i].name.equals("[stack]")) {
	    stackMap = liveMaps[i];
	    break;
	}
    }

    assertNotNull("Cannot find stack in live process", stackMap);
    int mapNo = findLowAddress(stackMap.addressLow, coreMaps);
    coreMap = coreMaps[mapNo];
    assertNotNull("Cannot find stack in core process", coreMap);    

    Elf testElf = new Elf(testCore, ElfCommand.ELF_C_READ);
    ElfEHeader header = testElf.getEHeader();
    int count = header.phnum;
    int segCount = 0;

    for (int i = 0; i < count; i++)
      {
        ElfPHeader pheader = testElf.getPHeader(i);
        assertNotNull(pheader);
	if(pheader.filesz > 0)
	    segCount++;
      }
    testElf.close();

    assertEquals("stack only corefile segCount +stack +notes != 2",segCount,2);
  }


 /**
   * Given a Proc object, generate a core file from that given proc.
   * 
   * @param ackProc - proc object to generate core from.
   * @return - name of constructed core file.
   */
  private String constructCore (final Proc ackProc)
  {

    final CoredumpAction coreDump = new CoredumpAction(ackProc, new Event()
    {

      public void execute ()
      {
        ackProc.requestAbandonAndRunEvent(new RequestStopEvent(
                                                               Manager.eventLoop));
      }
    }, false);
        
    new ProcBlockAction(ackProc, coreDump);
    assertRunUntilStop("Running event loop for core file");
    return coreDump.getConstructedFileName();
  }

 /**
   * Given a Proc object, generate a core file from that given proc.
   * 
   * @param ackProc - proc object to generate core from.
   * @return - name of constructed core file.
   */
  private String constructStackOnlyCore (final Proc ackProc)
  {

      CoredumpAction coreDump = null;
      coreDump = new CoredumpAction(ackProc, "core", 
				    new Event() {
					public void execute () {
					    ackProc.
						requestAbandonAndRunEvent(
									  new RequestStopEvent(
											       Manager.eventLoop));
					}
				    }, false, true);
      
      new ProcBlockAction(ackProc, coreDump);
      assertRunUntilStop("Running event loop for core file");
      return coreDump.getConstructedFileName();
  }

 
  /**
   * Generate a process suitable for attaching to (ie detached when returned).
   * Stop the process, check that is is found in the frysk state machine, then
   * return a proc oject corresponding to that process.
   * 
   * @return - Proc - generated process.
   */
  private Proc giveMeAProc ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    assertNotNull(ackProc);
    Proc proc = ackProc.assertFindProcAndTasks();
    assertNotNull(proc);
    return proc;
  }
  
  private Proc giveMeABlockedProc ()
  {
    String[] nocmds = {};
    DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(nocmds);
    //SlaveOffspring ackProc = SlaveOffspring.createDaemon();
    assertNotNull(ackProc);
    ackProc.getMainTask().getProc();
    //Proc proc = ackProc.assertFindProcAndTasks();
    //assertNotNull(proc);
    return ackProc.getMainTask().getProc();
  }
  
    /**
     * Returns the ISA that corresponds to the given Proc
     * 
     * @param Proc - the proc to test
     * @return Isa - the Isa that corresponds to given proc.
     */
    private ISA getISA (Proc proc) {
	return proc.getMainTask().getISA();
    }
  
    private Elf getElf(String coreFileName) {
	// Start new elf file
	return new Elf(new File(coreFileName), ElfCommand.ELF_C_READ);
    }
  
  private int findLowAddress(long address, MemoryMap[] maps)
  {
      for (int i=0; i<maps.length; i++)
	  if (address == maps[i].addressLow)
	      return i;

      return -1;
  }
  

}
