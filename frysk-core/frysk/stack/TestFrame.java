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

package frysk.stack;

import java.util.Iterator;
import java.util.List;

import frysk.config.Config;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rsl.Log;
import frysk.symtab.DwflSymbol;
import frysk.symtab.SymbolFactory;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.SlaveOffspring;
import frysk.testbed.StopEventLoopWhenProcTerminated;
import frysk.testbed.TestLib;

import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.ElfException;

public class TestFrame extends TestLib {
    private static final Log fine = Log.fine(TestFrame.class);
  
  public void testAttached()
  {
    Task task = SlaveOffspring.createAttachedChild().findTaskUsingRefresh(true);
    
    backtrace (task, new BlockingObserver());
    
  }
  
  class BlockingObserver implements TaskObserver.Instruction
  {
    public Action updateExecuted (Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed (Object observable, Throwable w)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
      Manager.eventLoop.requestStop();
    }
  }

  public Frame backtrace(Task task, BlockingObserver blocker)
  {
    task.requestAddInstructionObserver(blocker);
    
    assertRunUntilStop("Attach to process");
    
    Frame baseFrame = StackFactory.createFrame(task);
 
    Frame frame = baseFrame;
    while (frame != null)
      {
     // System.err.println(frame.cursor.getProcName(100).name);
	  fine.log("testAttached, frame name", frame.getSymbol().getName());
      frame = frame.getOuter();
    } 
    
    return baseFrame;
  }
  
  public void testFrameSame ()
  {
    Task task = SlaveOffspring.createAttachedChild().findTaskUsingRefresh(true);
    
    Frame frame = backtrace(task, new BlockingObserver());
    
    Frame otherFrame = StackFactory.createFrame(task);
    
    assertSame("Frames should be the same", frame, otherFrame);
    
  }
  
  public void testContinueNotSame()
  {
    Task task = SlaveOffspring.createAttachedChild()
	.findTaskUsingRefresh(true);
    BlockingObserver blocker = new BlockingObserver();
    
    Frame frame = backtrace(task, blocker);
    
    task.requestDeleteInstructionObserver(blocker);
    assertRunUntilStop("Removing observer, running process again");
    
    Frame otherFrame = backtrace(task, blocker);
    
    assertNotSame("Frames should be different", frame, otherFrame);
    
  }


    class Info {
	private Task task;
	public Info(Task task) {
	    this.task = task;
	}

	private DwflModule getModuleForFile(String path) {
	    Dwfl dwfl = frysk.dwfl.DwflCache.getDwfl(task);
	    DwflModule[] modules = dwfl.getModulesForce();
	    for (int i = 0; i < modules.length; ++i) {
		String name = modules[i].getName();
		if (name.equals(path))
		    return modules[i];
	    }
	    return null;
	}

	public long getFunctionEntryAddress(String func) throws ElfException
	{
	    String path = task.getProc().getExeFile().getSysRootedPath();
	    DwflModule module = getModuleForFile(path);
	    List symbols = SymbolFactory.getSymbols(module);
	    for (Iterator it = symbols.iterator(); it.hasNext();) {
		DwflSymbol symbol = (DwflSymbol)it.next();
		if (symbol.getName().equals(func))
		    return symbol.getAddress();
	    }
	    return 0;
	}
    }

    public void testBogusAddressPrevFrame() throws ElfException {
    	class CodeObserver1 implements TaskObserver.Code
	{
	    public boolean hit = false;

	    public Action updateHit (Task task, long address) {
		hit = true;
		long addr = StackFactory.createFrame(task).getOuter().getAddress();
		assertTrue("Return adress makes sense",
			    addr < -1 || addr > 4096);
		Manager.eventLoop.requestStop();
		return Action.BLOCK;
	    }

	    public void addFailed (Object observable, Throwable w) { }
	    public void addedTo (Object observable) {
		Manager.eventLoop.requestStop();
	    }
	    public void deletedFrom (Object observable) {
		Manager.eventLoop.requestStop();
	    }
	}

	String[] cmd = {Config.getPkgLibFile("funit-empty-functions-nodebug").getPath()};
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
	Task task = child.getMainTask();
	Info info = new Info(task);

	long address = info.getFunctionEntryAddress("__libc_csu_init");
	CodeObserver1 code = new CodeObserver1();
	task.requestAddCodeObserver(code, address);
	assertRunUntilStop("add breakpoint observer");

	new StopEventLoopWhenProcTerminated(child);
	child.requestRemoveBlock();
	assertFalse(code.hit);
	assertRunUntilStop("wait for hit");
	assertTrue(code.hit);
    }

    /**
     * Explicit test for bug #6029. Make sure that when generating
     * backtrace in the updateHit() method the address is correct.
     */
    public void testInnerFrameAddress() throws ElfException
    {
      String[] cmd =
	{ Config.getPkgLibFile("funit-hello").getPath(), "world" };
      DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(cmd);
      Task task = child.getMainTask();
      Info info = new Info(task);
      
      long address = info.getFunctionEntryAddress("print");
      CodeObserver code = new CodeObserver();
      task.requestAddCodeObserver(code, address);
      assertRunUntilStop("add breakpoint observer");
      
      child.requestRemoveBlock();
      assertFalse(code.hit);
      assertRunUntilStop("wait for hit");
      assertTrue(code.hit);
    }

  static class CodeObserver implements TaskObserver.Code
  {
    public boolean hit = false;
    
    public Action updateHit (Task task, long hitAddress)
    {
      hit = true;
      long frameAddress = StackFactory.createFrame(task).getAddress();
      assertEquals("Hit and Frame address", hitAddress, frameAddress);

      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
    
    public void addFailed (Object observable, Throwable w)
    {
      // Whoa
      w.printStackTrace();
    }
    
    public void addedTo (Object observable)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom (Object observable)
    {
      // We never delete
    }
  }
}
