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

package frysk.proc.dead;

import frysk.Config;
import inua.eio.ByteBuffer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import frysk.isa.IA32Registers;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Host;
import frysk.proc.Auxv;
import frysk.testbed.DaemonBlockedAtSignal;
import frysk.testbed.TestLib;
import frysk.proc.ProcId;
import frysk.proc.Manager;
import frysk.proc.MemoryMap;
import frysk.proc.TaskObserver;
import frysk.util.CoredumpAction;
import frysk.util.StacktraceAction;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.ProcBlockAction;
import frysk.proc.ProcCoreAction;
import frysk.testbed.LegacyOffspring;

import lib.dwfl.*;
import frysk.dwfl.*;

public class TestLinuxCore
    extends TestLib
	    
{

  Host coreHost = new LinuxHost(Manager.eventLoop, 
				Config.getPkgDataFile("test-core-x86"));
  

  public void testLinuxCoreFileMaps ()
  {

    // Remove the hasIsa test as on -r test runs the 
    // singleton maintains reference in between runs.

    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File xtestCore = new File(coreFileName);

    Host lcoreHost = new LinuxHost(Manager.eventLoop, 
				   xtestCore);

    
    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid()));

    MemoryMap[] list = ackProc.getMaps();
  

    int redZoneDiscount = 0;
    for (int j=0; j<list.length; j++)
      if (list[j].permRead == false)
        redZoneDiscount++;

    MemoryMap[] clist = coreProc.getMaps();
    assertEquals("Number of maps match in corefile/live process",
		 clist.length,list.length - redZoneDiscount);

    for (int i=0; i<list.length; i++)
      {

	if (list[i].permRead == false)
	    continue;

	int cloc = findCoreMap(list[i].addressLow, clist);
	assertTrue("coreMaps returned -1",cloc >= 0);
	assertEquals("vaddr",list[i].addressLow, clist[cloc].addressLow);
	assertEquals("vaddr_end",list[i].addressHigh,clist[cloc].addressHigh);
	assertEquals("permRead",list[i].permRead,clist[cloc].permRead);
	assertEquals("permWrite",list[i].permWrite,clist[cloc].permWrite);
	assertEquals("permExecute",list[i].permExecute,clist[cloc].permExecute);
      }

    xtestCore.delete();
  }


  private int findCoreMap(long address, MemoryMap[] maps)
  {

    for (int i=0; i<maps.length; i++)
      if (maps[i].addressLow == address)
	return i;

    return -1;
  }

  public void testLinuxCoreFileStackTrace () {
   
    final Proc ackProc; // = giveMeAProc();
    
    File exeFile = Config.getPkgLibFile("funit-stacks");
    ackProc = new DaemonBlockedAtSignal(exeFile).getMainTask().getProc();


    StacktraceAction stacker;
    StacktraceAction coreStack;

    StringWriter stringWriter1 = new StringWriter();
    stacker = new StacktraceAction(new PrintWriter(stringWriter1),ackProc, new RequestStopEvent(Manager.eventLoop), true, false,false, false, false, false)
    {

      public void addFailed (Object observable, Throwable w)
      {
        fail("Proc add failed: " + w.getMessage());
      }
    };

    new ProcBlockAction (ackProc, stacker);
    assertRunUntilStop("perform backtrace");

    String coreFileName = constructCore(ackProc);
    File xtestCore = new File(coreFileName);

    Host lcoreHost = new LinuxHost(Manager.eventLoop, 
				   xtestCore, exeFile);
    
    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid()));

    StringWriter stringWriter2 = new StringWriter();
    coreStack = new StacktraceAction(new PrintWriter(stringWriter2),coreProc, new PrintEvent(),true,false,false,false,false, false)
    {

      public void addFailed (Object observable, Throwable w)
      {
        fail("Proc add failed: " + w.getMessage());
      }
    };
    new ProcCoreAction(coreProc, coreStack);
    assertRunUntilStop("perform corebacktrace");

    assertEquals("Compare stack traces",stringWriter1.getBuffer().toString(),stringWriter2.getBuffer().toString());
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
    
    Proc proc = coreHost.getProc(new ProcId(26799));
    
    assertNotNull("PID 26799 populates from core file?", proc);
    assertEquals("PID  26799 should have one task", 3 ,proc.getTasks().toArray().length);
    assertEquals("LinuxCoreFileProc PID",26799,proc.getPid());
  }
  
  
  public void testLinuxProcPopulation () 
  {
    
    
    assertNotNull("Core file Host is Null?",coreHost);
    
    Proc proc = coreHost.getProc(new ProcId(26799));
    assertNotNull("Proc exists in corefile", proc);
    assertEquals("PID",26799,proc.getPid());
    assertEquals("ProcID",26799,proc.getId().id);
    assertSame("getHost",coreHost,proc.getHost());
    assertEquals("getParent",null,proc.getParent());
    assertEquals("getCommand","segfault",proc.getCommand());
    assertEquals("getExe","/home/pmuldoon/segfault",proc.getExe());
    assertEquals("getUID",500,proc.getUID());
    assertEquals("getGID",500,proc.getGID());
    assertEquals("getMainTask",26799,proc.getMainTask().getTid());
  }
  
  
  public void testLinuxProcAuxV () 
  {
    
    
    assertNotNull("Core file Host is Null?",coreHost);
    
    Proc proc = coreHost.getProc(new ProcId(26799));
    assertNotNull("Proc exists in corefile", proc);
    
    Auxv[] auxv = proc.getAuxv();
    final int[] expectedType = {32,33,16,6,17,3,4,5,7,8,9,11,12,13,14,23,15,0};
    final long[] expectedVal = {0x62a400L,
				0x62a000L,
				0xafe9f1bfL,
				0x1000L,
				0x64L,
				0x8048034L,
				0x20L,
				0x8L,
				0x0L,
				0x0L,
				0x80483e0L,
				0x1f4L,
				0x1f4L,
				0x1f4L,
				0x1f4L,
				0x0L,
				0xbfcfee4bL,
				0x0};
    
    for(int i=0; i<auxv.length; i++)
      {
	assertEquals("Auxv Type", auxv[i].type, 
		     expectedType[i]);
	assertEquals("Auxv Value", auxv[i].val,
		     expectedVal[i]);
      }
    
  }
  
  public void testLinuxTaskMemory ()
  {
	assertNotNull("Core file Host is Null?",coreHost);
    
	Proc proc = coreHost.getProc(new ProcId(26799));
	assertNotNull("Proc exists in corefile", proc);
	Task task = proc.getMainTask();
	assertNotNull("Task exists in proc",task);
	
	ByteBuffer buffer = task.getMemory();
	
	buffer.position(0x411bc150L);
	
	assertEquals("Peek a byte at 0x411bc150",0x28L,buffer.getUByte());
	assertEquals("Peek a byte at 0x411bc151",0x55L,buffer.getUByte());

	buffer.position(0x411bc153L);
	assertEquals("Peek a byte at 0x411bc153",0x08L,buffer.getUByte());
	assertEquals("Peek a byte at 0x411bc154",0x00L,buffer.getUByte());
  }
  
  public void testLinuxTaskPopulation ()
  {


    // Preload 3 threads worth of data into the static data structure

    int[] threadPid = {26801, 26800, 26799};
    String[] threadName = {"Task 26801", "Task 26800", "Task 26799"};
    long[] eax =    {0xfffffffcL, 0xfffffffcL,0x80486a8L};
    long[] ebx =    {0x080498ecL, 0x080498ecL,0x411baff4L};
    long[] ecx =    {0x0L,        0x0L,       0x2L};
    long[] edx =    {0x2L,        0x2L,       0x1L};
    long[] esi =    {0x0L,        0x0L,       0x41067ca0L};
    long[] edi =    {0x080498ecL, 0x080498ecL,0x0L};
    long[] ebp =    {0xb75603a8L, 0xb7f613a8L,0xbfcfec68L};
    long[] esp =    {0xb7560350L, 0xb7f61350L,0xbfcfec20L};
    long[] eip =    {0x0062a402L, 0x0062a402L,0x0804854aL};
    long[] eflags = {0x00200246L, 0x00200202L,0x00210286L};
    long[] oeax =   {0xf0L,       0xf0L,      0xffffffffL};
    long[] cs =     {0x73L,       0x73L,      0x73L};
    long[] ds =     {0x7bL,       0x7bL,      0x7bL};
    long[] es =     {0x7b,        0x7b,       0x7b};
    long[] fs =     {0x0L,        0x0L,       0x0L};
    long[] gs =     {0x33L,       0x33L,      0x33L};

    assertNotNull("Core file Host is Null?",coreHost);

    Proc proc = coreHost.getProc(new ProcId(26799));
    assertNotNull("Proc exists in corefile", proc);
    
    Task[] tasks = (Task[]) proc.getTasks().toArray(new Task[proc.getTasks().size()]);
    assertEquals("PID  26799 should have three tasks", 3 ,tasks.length);
    
    // Loop through the expected three threads
    for (int i=0; i<tasks.length; i++)
      {

	assertNotNull("Task exists in proc",tasks[i]);
	assertEquals("Task ID",threadPid[i],tasks[i].getTaskId().id);
	assertEquals("Task TID",threadPid[i], tasks[i].getTid());
	assertEquals("Task TID",threadName[i],tasks[i].getName());
	assertNotNull("Task ISA",tasks[i].getIsa());
	assertSame("Task getParent",proc,tasks[i].getProc());

	assertEquals("note: ebx", ebx[i], tasks[i].getRegister(IA32Registers.EBX));
	assertEquals("note: ecx", ecx[i], tasks[i].getRegister(IA32Registers.ECX));
	assertEquals("note: edx", edx[i], tasks[i].getRegister(IA32Registers.EDX));
	assertEquals("note: esi", esi[i], tasks[i].getRegister(IA32Registers.ESI));
	assertEquals("note: edi", edi[i], tasks[i].getRegister(IA32Registers.EDI));
	assertEquals("note: ebp", ebp[i], tasks[i].getRegister(IA32Registers.EBP));
	assertEquals("note: eax", eax[i], tasks[i].getRegister(IA32Registers.EAX));
	assertEquals("note: ds", ds[i],   tasks[i].getRegister(IA32Registers.DS));
	assertEquals("note: es", es[i], tasks[i].getRegister(IA32Registers.ES));
	assertEquals("note: fs", fs[i], tasks[i].getRegister(IA32Registers.FS));
	assertEquals("note: gs", gs[i],	tasks[i].getRegister(IA32Registers.GS));
	assertEquals("note: oeax", oeax[i], tasks[i].getRegister(IA32Registers.ORIG_EAX));
	assertEquals("note: eip", eip[i], tasks[i].getRegister(IA32Registers.EIP));
	assertEquals("note: cs", cs[i], tasks[i].getRegister(IA32Registers.CS));
	assertEquals("note: eflags", eflags[i], tasks[i].getRegister(IA32Registers.EFLAGS));
	assertEquals("note: esp", esp[i], tasks[i].getRegister(IA32Registers.ESP));

      }
  }

  /**
   * Tests that inserted breakpoints aren't visible in the fcore file.
   */
  public void testInsertedBreakpoint() throws Exception
  {
    Proc ackProc = giveMeAProc();
    Task procTask = ackProc.getMainTask();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    procTask.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    // Get the memory at a breakpoint before insertion.
    ByteBuffer memory = procTask.getMemory();
    long proc_address = getFunctionEntryAddress(ackProc, "bp1_func");
    memory.position(proc_address);
    byte origb = memory.getByte();

    // Insert the breakpoint (which will change the raw text/code segment).
    CodeObserver code = new CodeObserver(procTask, proc_address);
    procTask.requestAddCodeObserver(code, proc_address);
    assertRunUntilStop("add breakpoint observer");

    // Create a core file from the process and load it back in.
    String coreFileName = constructCore(ackProc);
    File xtestCore = new File(coreFileName);
    Host lcoreHost = new LinuxHost(Manager.eventLoop,
				   xtestCore);
    Proc coreProc = lcoreHost.getProc(new ProcId(ackProc.getPid()));
    Task coreTask = coreProc.getMainTask();

    // Check that the breakpoint isn't visible.
    long core_address = getFunctionEntryAddress(coreProc, "bp1_func");
    memory = coreTask.getMemory();
    memory.position(core_address);
    byte coreb = memory.getByte();
    assertEquals(origb, coreb);
  }


  // Helper class since there there isn't a get symbol method in Dwfl,
  // so we need to wrap it all in a builder pattern.
  static class Symbol implements SymbolBuilder
  {
    private String name;
    private long address;

    private boolean found;

    private Symbol()
    {
      // properties get set in public static get() method.
    }

    static Symbol get(Dwfl dwfl, String name)
    {
      Symbol sym = new Symbol();
      sym.name = name;
      DwflModule[] modules = dwfl.getModules();
      for (int i = 0; i < modules.length && ! sym.found; i++)
        modules[i].getSymbolByName(name, sym);

      if (sym.found)
        return sym;
      else
        return null;
    }

    String getName()
    {
      return name;
    }

    long getAddress()
    {
      return address;
    }

    public void symbol(String name, long value, long size,
                       int type, int bind, int visibility)
    {
      if (name.equals(this.name))
        {
          this.address = value;
          this.found = true;
        }
    }
  }

  /**
   * Returns the address of the requested function through query Dwfl
   * of the main Task of the given Proc.
   */
  private static long getFunctionEntryAddress(Proc proc, String func)
  {
    Task task = proc.getMainTask();
    Dwfl dwfl = DwflCache.getDwfl(task);
    Symbol sym = Symbol.get(dwfl, func);
    return sym.getAddress();
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
      // This code is relying on magic symbols found within
      // funit-child.  Why not write a simple program that does
      // nothing but provide the symbols.
    LegacyOffspring ackProc = LegacyOffspring.createChild();
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

  // Helper class for inserting a Code breakpoint observer.
  static class CodeObserver
    implements TaskObserver.Code
  {
    private final Task task;
    private final long address;

    boolean hit;

    public CodeObserver(Task task, long address)
    {
      this.task = task;
      this.address = address;
    }

    public Action updateHit (Task task, long address)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);
      if (address != this.address)
        throw new IllegalStateException("Wrong address, given " + address
                                        + " not equals expected "
                                        + this.address);

      hit = true;

      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addedTo(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }

  // Helper class for attaching to a task.
  static class AttachedObserver
    implements TaskObserver.Attached
  {
    public Action updateAttached(Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed(Object observable, Throwable w)
    {
      fail(w.toString());
    }

    public void addedTo(Object observable)
    {
      // Ignored
    }

    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

}
