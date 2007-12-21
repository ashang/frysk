// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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


package frysk.util;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import frysk.isa.ISA;
import java.io.File;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfException;
import lib.dwfl.ElfFileException;
import lib.dwfl.ElfKind;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Auxv;
import frysk.proc.Manager;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.dead.LinuxHost;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.SlaveOffspring;
import frysk.testbed.TestLib;
import frysk.Config;

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


    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Checking core file " + coreFileName + " exists.",
               testCore.exists());

    ISA isa = getISA(ackProc);
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
                   header.ident[5], ElfEHeader.PHEADER_ELFDATA2MSB);
    else
      assertEquals("Checking endian is appropriate to platform",
                   header.ident[5], ElfEHeader.PHEADER_ELFDATA2LSB);

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
                     ElfEHeader.PHEADER_ELFCLASS32);
    } else if (isa == ISA.PPC64BE) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_PPC64);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.PHEADER_ELFCLASS64);
    } else if (isa == ISA.PPC32BE) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_PPC);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.PHEADER_ELFCLASS32);
    } else if (isa == ISA.X8664) {
        assertEquals("Checking header machine type", header.machine,
                     ElfEMachine.EM_X86_64);
        assertEquals("Checking elf class", header.ident[4],
                     ElfEHeader.PHEADER_ELFCLASS64);
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
    LinuxHost lcoreHost = new LinuxHost(Manager.eventLoop, 
		   testCore,new File(ackProc.getExe()));      

    assertNotNull("Checking core file Host", lcoreHost);
    
    // Get corefile process
    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid())); 
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

  
  public void testGeneralPurposeRegisters ()
  {
   
      File exec32 = Config.getPkgLib32File(null);
      File nativeFile = Config.getPkgLibFile(null);
      if (nativeFile == exec32)
        return;

      // Construct a process
      Proc ackProc = giveMeABlockedProc();
      assertNotNull("Found Process",ackProc);
      
      // Get main task
      Task mainLiveTask = ackProc.getMainTask();
      assertNotNull("Found main live task",mainLiveTask);
      
      // Get the live process register banks and store them
      ByteBuffer liveRegisterMaps[] = mainLiveTask.getRegisterBuffersFIXME();
      long bankSize = liveRegisterMaps[0].capacity();
      byte[] liveRegBuffer = new byte[(int)bankSize];
      liveRegisterMaps[0].get(0,liveRegBuffer,0,(int) liveRegisterMaps[0].capacity());
      
      // Create a corefile from process
      String coreFileName = constructCore(ackProc);
      File testCore = new File(coreFileName);
      assertTrue("Checking core file " + coreFileName + " exists.",
                 testCore.exists());

      // Model the corefile, and get the Process.
      LinuxHost lcoreHost = new LinuxHost(Manager.eventLoop, 
		   testCore);      
      assertNotNull("Checking core file Host", lcoreHost);
      
      // Get corefile process
      Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid())); 
      assertNotNull("Checking core file process", coreProc);
      
      // Get Main tasks of corefile
      Task mainCoreTask = coreProc.getMainTask();
      assertNotNull("Checking core main task", mainCoreTask);
 
      // Get corefile registers
      ByteBuffer coreRegisterMaps[] = mainCoreTask.getRegisterBuffersFIXME(); 
      byte[] coreRegBuffer = new byte[(int) coreRegisterMaps[0].capacity()];
      coreRegisterMaps[0].get(coreRegBuffer);
      
      // Compare
      for (int i=0; i<liveRegBuffer.length; i++)
	assertEquals("General Purpose Register buffer postion "+i,coreRegBuffer[i],liveRegBuffer[i]);


      testCore.delete();
 }

  public void testFloatingPointRegisters ()
  {
      
      // PowerPc doesnt have a bank for Floating Point registers
      // this registers are with all others in the main bank
      if (unresolvedOnPPC(4890))
         return;	

      // Construct a process
      Proc ackProc = giveMeABlockedProc();
      assertNotNull("Found Process",ackProc);
      
      // Get main task
      Task mainLiveTask = ackProc.getMainTask();
      assertNotNull("Found main live task",mainLiveTask);
      
      // Get the live process register banks and store them
      ByteBuffer liveRegisterMaps[] = mainLiveTask.getRegisterBuffersFIXME();
      byte[] liveRegBuffer = new byte[(int) liveRegisterMaps[1].capacity()];
      liveRegisterMaps[1].get(0,liveRegBuffer,0,(int) liveRegisterMaps[1].capacity());
      
      // Create a corefile from process
      String coreFileName = constructCore(ackProc);
      File testCore = new File(coreFileName);
      assertTrue("Checking core file " + coreFileName + " exists.",
                 testCore.exists());

      // Model the corefile, and get the Process.
      LinuxHost lcoreHost = new LinuxHost(Manager.eventLoop, 
		   testCore);      
      assertNotNull("Checking core file Host", lcoreHost);
      
      // Get corefile process
      Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid())); 
      assertNotNull("Checking core file process", coreProc);
      
      // Get Main tasks of corefile
      Task mainCoreTask = coreProc.getMainTask();
      assertNotNull("Checking core main task", mainCoreTask);
 
      // Get corefile registers
      ByteBuffer coreRegisterMaps[] = mainCoreTask.getRegisterBuffersFIXME(); 
      byte[] coreRegBuffer = new byte[(int) coreRegisterMaps[1].capacity()];
      coreRegisterMaps[1].get(coreRegBuffer);
      
      // Compare
      for (int i=0; i<coreRegBuffer.length; i++)
	  assertEquals("Floating Point Register buffer postion "+i,coreRegBuffer[i],liveRegBuffer[i]);

      testCore.delete();
 }

  
  public void testXFloatingPointRegisters ()
  {
      
      if (unresolvedOnPPC(4890) || unresolvedOnx8664(4890))
	  return;
      // Construct a process
      Proc ackProc = giveMeABlockedProc();
      assertNotNull("Found Process",ackProc);
      
      // Get main task
      Task mainLiveTask = ackProc.getMainTask();
      assertNotNull("Found main live task",mainLiveTask);
      
      // Get the live process register banks and store them
      ByteBuffer liveRegisterMaps[] = mainLiveTask.getRegisterBuffersFIXME();
      byte[] liveRegBuffer = new byte[(int) liveRegisterMaps[2].capacity()];
      liveRegisterMaps[2].get(0,liveRegBuffer,0,(int) liveRegisterMaps[2].capacity());
      
      // Create a corefile from process
      String coreFileName = constructCore(ackProc);
      File testCore = new File(coreFileName);
      assertTrue("Checking core file " + coreFileName + " exists.",
                 testCore.exists());

      // Model the corefile, and get the Process.
      LinuxHost lcoreHost = new LinuxHost(Manager.eventLoop, 
		   testCore);      
      assertNotNull("Checking core file Host", lcoreHost);
      
      // Get corefile process
      Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid())); 
      assertNotNull("Checking core file process", coreProc);
      
      // Get Main tasks of corefile
      Task mainCoreTask = coreProc.getMainTask();
      assertNotNull("Checking core main task", mainCoreTask);
 
      // Get corefile registers
      ByteBuffer coreRegisterMaps[] = mainCoreTask.getRegisterBuffersFIXME(); 
      byte[] coreRegBuffer = new byte[(int) coreRegisterMaps[2].capacity()];
      coreRegisterMaps[2].get(coreRegBuffer);
      
      // Compare
      for (int i=0; i<coreRegBuffer.length; i++)
	  assertEquals("X Floating Point Register buffer postion "+i,coreRegBuffer[i],liveRegBuffer[i]);
  
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
      LinuxHost lcoreHost = new LinuxHost(Manager.eventLoop, 
		   testCore);      
      assertNotNull("Checking core file Host", lcoreHost);
      
      // Get corefile process
      Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid())); 
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
  
  private Elf getElf(String coreFileName)
  {
      Elf local_elf = null;
      // Start new elf file
      try
        {
          local_elf = new Elf(coreFileName, ElfCommand.ELF_C_READ);
        }
      catch (ElfFileException e)
        {
          fail(e.getMessage());
        }
      catch (ElfException e)
        {
          fail(e.getMessage());
        }
      return local_elf;
  }
  
  private int findLowAddress(long address, MemoryMap[] maps)
  {
      for (int i=0; i<maps.length; i++)
	  if (address == maps[i].addressLow)
	      return i;

      return -1;
  }
  

}
