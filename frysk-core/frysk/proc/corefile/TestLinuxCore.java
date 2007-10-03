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

import frysk.Config;
import inua.eio.ByteBuffer;

import java.io.File;
import java.math.BigInteger;

import frysk.proc.Isa;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Host;
import frysk.proc.Auxv;
import frysk.proc.TestLib;
import frysk.proc.ProcId;
import frysk.proc.Manager;
import frysk.proc.MemoryMap;
import frysk.util.CoredumpAction;
import frysk.util.StacktraceAction;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcCoreAction;
public class TestLinuxCore
    extends TestLib
	    
{

  Host coreHost = new LinuxHost(Manager.eventLoop, 
					new File(Config.getPkgDataDir (), 
						 "test-core"));
  

  public void testLinuxCoreFileMaps ()
  {

    if (brokenX8664XXX(4640) || brokenPpcXXX(4640))
      return;

    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File xtestCore = new File(coreFileName);

    Host lcoreHost = new LinuxHost(Manager.eventLoop, 
				   xtestCore);

    
    lcoreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();

    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid()));

    MemoryMap[] list = ackProc.getMaps();
    MemoryMap[] clist = coreProc.getMaps();

    assertEquals("Number of maps match in corefile/live process",
		 clist.length,list.length);

    for (int i=0; i<list.length; i++)
      {
	assertEquals("vaddr",list[i].addressLow, clist[i].addressLow);
	assertEquals("vaddr_end",list[i].addressHigh,clist[i].addressHigh);
	assertEquals("permRead",list[i].permRead,clist[i].permRead);
	assertEquals("permWrite",list[i].permWrite,clist[i].permWrite);
	assertEquals("permExecute",list[i].permExecute,clist[i].permExecute);
      }

    xtestCore.delete();
  }

  public void testLinuxCoreFileStackTrace ()
  {

    if (brokenX8664XXX(4601) || brokenPpcXXX(4601))
      return;
    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File xtestCore = new File(coreFileName);

    Host lcoreHost = new LinuxHost(Manager.eventLoop, 
				   xtestCore);
    
    lcoreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();

    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid()));


    StacktraceAction stacker;
    StacktraceAction coreStack;

    stacker = new StacktraceAction(ackProc, new RequestStopEvent(Manager.eventLoop), true, false, false, false)
    {

      public void addFailed (Object observable, Throwable w)
      {
        fail("Proc add failed: " + w.getMessage());
      }
    };

    new ProcBlockAction (ackProc, stacker);
    assertRunUntilStop("perform backtrace");

    coreStack = new StacktraceAction(coreProc, new PrintEvent(),true,false,false,false)
    {

      public void addFailed (Object observable, Throwable w)
      {
        fail("Proc add failed: " + w.getMessage());
      }
    };
    new ProcCoreAction(coreProc, coreStack);
    assertRunUntilStop("perform corebacktrace");

    assertEquals("Compare stack traces",stacker.toPrint(),coreStack.toPrint());
    xtestCore.delete();
  }

  private static class PrintEvent implements Event
  {
    public void execute()
    {
      Manager.eventLoop.requestStop();
    }
  }

  public void testLinuxHostPopulation ()
  {
    
    assertNotNull("Core File Host Is Null?",coreHost);
    coreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();
    
    Proc proc = coreHost.getProc(new ProcId(31497));
    
    assertNotNull("PID 31497 populates from core file?", proc);
    assertEquals("PID  31497 should have one task", 1 ,proc.getTasks().toArray().length);
    assertEquals("LinuxCoreFileProc PID",31497,proc.getPid());
  }
  
  
  public void testLinuxProcPopulation () 
  {
    
    
    assertNotNull("Core file Host is Null?",coreHost);
    
    
    coreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();
    
    Proc proc = coreHost.getProc(new ProcId(31497));
    assertNotNull("Proc exists in corefile", proc);
    assertEquals("PID",31497,proc.getPid());
    assertEquals("ProcID",31497,proc.getId().id);
    assertSame("getHost",coreHost,proc.getHost());
    assertEquals("getParent",null,proc.getParent());
    assertEquals("getCommand","a.out",proc.getCommand());
    assertEquals("getExe","a.out",proc.getExe());
    assertEquals("getUID",500,proc.getUID());
    assertEquals("getGID",100,proc.getGID());
    assertEquals("getMainTask",31497,proc.getMainTask().getTid());
  }
  
  
  public void testLinuxProcAuxV () 
  {
    
    
    assertNotNull("Core file Host is Null?",coreHost);
    
    coreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();
    
    Proc proc = coreHost.getProc(new ProcId(31497));
    assertNotNull("Proc exists in corefile", proc);
    
    Auxv[] auxv = proc.getAuxv();
    
    final int[] expectedType = {32,33,16,6,17,3,4,5,7,8,9,11,12,13,14,23,15,0};
    final BigInteger[] expectedVal = {new BigInteger("1508352",10),
				      new BigInteger("1507328",10), 				
				      new BigInteger("3219782655",10),						    	
				      new BigInteger("4096",10),
				      new BigInteger("100",10),
				      new BigInteger("134512692",10),
				      new BigInteger("32",10),
				      new BigInteger("7",10),
				      new BigInteger("0",10),
				      new BigInteger("0",10),
				      new BigInteger("134513376",10),
				      new BigInteger("500",10),
				      new BigInteger("500",10),
				      new BigInteger("100",10),
				      new BigInteger("100",10),
				      new BigInteger("0",10),
				      new BigInteger("3220187851",10),
				      new BigInteger("0",10)};
    
    for(int i=0; i<auxv.length; i++)
      {
	assertEquals("Auxv Type", auxv[i].type, 
		     expectedType[i]);
	assertEquals("Auxv Value", auxv[i].val,
		     expectedVal[i].longValue());
      }
    
  }
  
  public void testLinuxTaskMemory ()
  {
	assertNotNull("Core file Host is Null?",coreHost);
    
	coreHost.requestRefreshXXX();  
	Manager.eventLoop.runPending();
	   
	Proc proc = coreHost.getProc(new ProcId(31497));
	assertNotNull("Proc exists in corefile", proc);
	Task task = proc.getMainTask();
	assertNotNull("Task exists in proc",task);
	
	ByteBuffer buffer = task.getMemory();
	
	buffer.position(0x00170000L);
	
	assertEquals("Peek a byte at 0x00170000",0x7f,buffer.getByte());
	assertEquals("Peek a byte at 0x00170001",0x45,buffer.getByte());
	assertEquals("Peek a byte at 0x00170002",0x4c,buffer.getByte());
	assertEquals("Peek a byte at 0x00170003",0x46,buffer.getByte());
	assertEquals("Peek a byte at 0x00170004",0x01,buffer.getByte());
  }
  
  public void testLinuxTaskPopulation ()
  {

    assertNotNull("Core file Host is Null?",coreHost);


    coreHost.requestRefreshXXX();
    Manager.eventLoop.runPending();
    
    Proc proc = coreHost.getProc(new ProcId(31497));
    assertNotNull("Proc exists in corefile", proc);
    Task task = proc.getMainTask();
    assertNotNull("Task exists in proc",task);
    assertEquals("Task ID",31497,task.getTaskId().id);
    assertEquals("Task TID",31497, task.getTid());
    assertEquals("Task TID","Task 31497",task.getName());
    assertEquals("Task has ISA before getISA",task.hasIsa(),false);
    assertNotNull("Task ISA",task.getIsa());
    assertEquals("Task has ISA?",task.hasIsa(), true);
    assertSame("Task getParent",proc,task.getProc());

    Isa isa = task.getIsa();

    assertEquals("ebx register",0x00007b09,
		 isa.getRegisterByName("ebx").get(task));
    assertEquals("ecx register",0x00007b09,
		 isa.getRegisterByName("ecx").get(task));
    assertEquals("edx register",0x00000006,
		 isa.getRegisterByName("edx").get(task));

    BigInteger tmp = new BigInteger("3220187212"); //0xbff0284c
    assertEquals("esi register",
		 tmp.longValue(),isa.getRegisterByName("esi").get(task));
   
    assertEquals("edi register",0x00b2cff4,
		 isa.getRegisterByName("edi").get(task));

    tmp = new BigInteger("3220187052"); //0xbff027ac
    assertEquals("ebp register",
		 tmp.longValue(),isa.getRegisterByName("ebp").get(task));

    assertEquals("eax register",0,
		 isa.getRegisterByName("eax").get(task));

    assertEquals("ds register",0x0000007b,
		 isa.getRegisterByName("ds").get(task));

   tmp = new BigInteger("-1072693125"); //0xc010007b
   BigInteger es = isa.getRegisterByName("es").getBigInteger(task);

   assertEquals("es register",tmp.longValue(), es.longValue());

    assertEquals("fs register",0x00000000,
		 isa.getRegisterByName("fs").get(task));

    assertEquals("gs register",0x00000033,
		 isa.getRegisterByName("gs").get(task));

    assertEquals("cs register",0x00000073,
		 isa.getRegisterByName("cs").get(task));

    assertEquals("oeax register",0x0000010e,
		 isa.getRegisterByName("orig_eax").get(task));

    assertEquals("eip register",0x00170410,
		 isa.getRegisterByName("eip").get(task));

    assertEquals("eflags register",0x00000246,
		 isa.getRegisterByName("efl").get(task));

    tmp = new BigInteger("3220187028");
    assertEquals("note: esp",tmp.longValue(),
		 isa.getRegisterByName("esp").get(task));
  }


  /**
   * Generate a process suitable for attaching to (ie detached when returned).
   * Stop the process, check that is is found in the frysk state machine, then
   * return a proc oject corresponding to that process.
   * 
   * @return - Proc - generated process.
   */
  protected Proc giveMeAProc ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    assertNotNull(ackProc);
    Proc proc = ackProc.assertFindProcAndTasks();
    assertNotNull(proc);
    return proc;
  }


  /**
   * Given a Proc object, generate a core file from that given proc.
   * 
   * @param ackProc - proc object to generate core from.
   * @return - name of constructed core file.
   */
  public String constructCore (final Proc ackProc)
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
}
