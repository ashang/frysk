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

package frysk.proc;

import frysk.testbed.ForkTestLib;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;

public class TestInstructions
  extends TestLib
{
  // Process id, Proc, and Task representation of our test program.
  private int pid;
  private Proc proc;
  private Task task;

  // How we communicate with the test program.
  private BufferedReader in;
  private DataOutputStream out;

  private ArrayList labels;

  /**
   * Launch our test program and setup clean environment with the test
   * program spawned, readers, writers setup and eventLoop and all
   * labels read.
   */
  public void setUp()
  {
    // Make sure everything is setup so spawned processes are recognized
    // and destroyed in tearDown().
    super.setUp();

    // Create a process that we will communicate with through stdin/out.
    String command = getExecPath ("funit-instructions");
    ForkTestLib.ForkedProcess process;
    process = ForkTestLib.fork(new String[] { command });
    pid = process.pid;
    in = new BufferedReader(new InputStreamReader(process.in));
    out = new DataOutputStream(process.out);
    
    // Make sure the core knows about it.
    Manager.host.requestFindProc(new ProcId(pid), new Host.FindProc()
      {
	public void procFound (ProcId procId)
	{
	  proc = Manager.host.getProc(procId);
	  Manager.eventLoop.requestStop();
	}
	public void procNotFound (ProcId procId, Exception e)
	{
	  fail(procId + " not found: " + e);
	}
      });
    assertRunUntilStop("finding proc");
    
    task = proc.getMainTask();

    // Read the addresses of the labels
    labels = new ArrayList();
    try
      {
	String line = in.readLine();
	while (! line.equals("(nil)"))
	  {
	    Long label = Long.decode(line);
	    labels.add(label);
	    line = in.readLine();
	  }
      }
    catch (IOException e)
      {
	fail("reading breakpoint addresses: " + e);
      }
  }

  /**
   * Make sure the test program is really gone and the event loop is
   * stopped.  Individual tests are responsible for nice termination
   * if the want to.
   */
  public void tearDown()
  {
    pid = -1;
    proc = null;
    task = null;
    in = null;
    out = null;
    labels = null;

    Manager.eventLoop.requestStop();

    // And kill off any remaining processes we spawned
    super.tearDown();
  }

  public void testBreakAndStepInstructions() throws IOException
  {
    long first = ((Long) labels.remove(0)).longValue();
    CodeObserver code = new CodeObserver(first);
    task.requestAddCodeObserver(code, first);
    assertRunUntilStop("first code observer added");

    out.writeByte(1);
    assertRunUntilStop("go!");

    assertEquals("stopped at first breakpoint",
		 task.getIsa().pc(task), first);

    InstructionObserver io = new InstructionObserver(task);
    task.requestAddInstructionObserver(io);
    assertRunUntilStop("add instruction observer");

    task.requestUnblock(code);
    Iterator it = labels.iterator();
    while (it.hasNext())
      {
	long nextLabel = ((Long) it.next()).longValue();
	task.requestUnblock(io);
	assertRunUntilStop("unblock for " + nextLabel);
	assertEquals("step observer hit: " + nextLabel,
		     io.getAddr(), nextLabel);
      }

      task.requestUnblock(io);
  }

  public void testAllBreakpoints() throws IOException
  {
    ArrayList codeObservers = new ArrayList();
    Iterator it = labels.iterator();
    while (it.hasNext())
      {
	long label = ((Long) it.next()).longValue();
	CodeObserver code = new CodeObserver(label);
	task.requestAddCodeObserver(code, label);
	codeObservers.add(code);
	assertRunUntilStop("add code observer for " + label);
      }

    out.writeByte(1);
    assertRunUntilStop("go!");

    it = labels.iterator();
    while (it.hasNext())
      {
	long label = ((Long) it.next()).longValue();
	CodeObserver code = (CodeObserver) codeObservers.remove(0);
	assertEquals("code observer hit: " + label,
		     task.getIsa().pc(task), label);
	task.requestUnblock(code);
	if (it.hasNext())
	  assertRunUntilStop("wait for next code observer hit after "
			     + Long.toHexString(label));
      }
  }

  private class CodeObserver
    implements TaskObserver.Code
  {
    long address;
    
    CodeObserver(long address)
    {
      this.address = address;
    }
    
    public Action updateHit (Task task, long address)
    {
      if (address != this.address)
	fail("updateHit on unknown address");
      
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed(Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
    
    public void addedTo(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
    
    public void deletedFrom(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
  }

  private static class InstructionObserver
    implements TaskObserver.Instruction
  { 
    private final Task task; 
    private long addr;

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

    InstructionObserver(Task task)
    {
      this.task = task;
    }
    
    public Action updateExecuted(Task task)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);
      
      addr = task.getIsa().pc(task);
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public long getAddr()
    {
      return addr;
    }
  }
}
