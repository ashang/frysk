// This file is part of the program FRYSK.
//
// Copyright 2007 Oracle Corporation.
// Copyright 2006, 2007 Red Hat Inc.
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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import frysk.testbed.TestLib;
import frysk.testbed.TearDownProcess;
import frysk.Config;
import frysk.sys.DaemonPipePair;

public class TestBreakpoints
  extends TestLib
{
  // Process id, Proc, and Task representation of our test program.
  private Proc proc;
  private Task task;

  // How we communicate with the test program.
  private BufferedReader in;
  private DataOutputStream out;

  // Whether or not to install an Instruction observer
  private boolean installInstructionObserver;

  // Addresses to put breakpoints on.
  private long breakpoint1;
  private long breakpoint2;

  private AttachedObserver attachedObserver;
  private TerminatingObserver terminatingObserver;

  /**
   * Launch our test program and setup clean environment with a runner
   * eventLoop.
   */
  public void setUp()
  {
    // Make sure everything is setup so spawned processes are recognized
    // and destroyed in tearDown().
    super.setUp();

    // Some tests run with an InstructionObserver. Default is not.
    installInstructionObserver = false;

    // Create a process that we will communicate with through stdin/out.
    DaemonPipePair process
	= new DaemonPipePair(new String[] {
				 Config.getPkgLibFile("funit-breakpoints").getPath()
			     });
    TearDownProcess.add(process.pid);
    in = new BufferedReader(new InputStreamReader(process.in.getInputStream()));
    out = new DataOutputStream(process.out.getOutputStream());
    
    // Make sure the core knows about it.
    Manager.host.requestFindProc(new ProcId(process.pid.hashCode()),
				 new Host.FindProc()
	{
	    public void procFound (ProcId procId)
	    {
		proc = Manager.host.getProc(procId);
		Manager.eventLoop.requestStop();
	    }
	    public void procNotFound (ProcId procId, Exception e)
	    {
	    }
	});
    assertRunUntilStop("finding proc");
    task = proc.getMainTask();

    // Read the addresses to put breakpoints on.
    try {
	breakpoint1 = Long.decode(in.readLine()).longValue();
	breakpoint2 = Long.decode(in.readLine()).longValue();
    }
    catch (IOException e) {
	fail("reading breakpoint addresses");
    }
    catch (NullPointerException e) {
	fail("parsing breakpoint addresses");
    }

    // Make sure we are attached - XXX - do we need this?  Can't the
    // requestAddCodeObserver() do that for us?  The observer will
    // block the task so we can put in the breakpoints.
    attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    terminatingObserver = new TerminatingObserver();
    task.requestAddTerminatingObserver(terminatingObserver);
    assertRunUntilStop("adding TerminatingObserver");
  }

  /**
   * Make sure the test program is really gone and the event loop is
   * stopped.  Individual tests are responsible for nice termination
   * if the want to.
   */
  public void tearDown()
  {
      Manager.eventLoop.requestStop();
      // And kill off any remaining processes we spawned
      super.tearDown();
  }

  public void testHitAndRun() throws IOException
  {
    // Test can run with or without stepping.
    InstructionObserver io1 = new InstructionObserver(task, breakpoint1);
    InstructionObserver io2 = new InstructionObserver(task, breakpoint2);
    if (installInstructionObserver)
      {
        task.requestAddInstructionObserver(io1);
        task.requestAddInstructionObserver(io2);
      }

    // Put in breakpoint observers
    CodeObserver code1 = new CodeObserver(breakpoint1);
    task.requestAddCodeObserver(code1, breakpoint1);
    CodeObserver code2 = new CodeObserver(breakpoint2);
    task.requestAddCodeObserver(code2, breakpoint2);

    // Make sure the observers are properly installed.
    while (! code1.isAdded() || ! code2.isAdded())
	assertRunUntilStop ("code1 and code2 added");

    // Unblock and tell the process to go some rounds!
    task.requestUnblock(attachedObserver);

    // XXX: Better name welcome
    new GoAround(3).goneAround(3);

    assertEquals("code1 triggered", 3, code1.getTriggered());
    assertEquals("code2 triggered", 3, code2.getTriggered());

    // Remove the stepper
    if (installInstructionObserver)
      {
	assertEquals(3, io1.getAddrHit());
	assertEquals(3, io2.getAddrHit());

        task.requestDeleteInstructionObserver(io1);
        task.requestDeleteInstructionObserver(io2);
      }
    else
      {
	// Shouldn't be triggered.
	assertEquals(0, io1.getAddrHit());
	assertEquals(0, io2.getAddrHit());
      }

    // We are all done, you can go now.
    out.writeByte(0);
    out.flush();

    // The process will now exec itself again.
    // Make sure Code Observers have been notified.
    while (! (code1.isRemoved() && code2.isRemoved()))
	assertRunUntilStop ("code1 and code2 removed");

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // So how did it all go, did it exit properly?
    while (! terminatingObserver.terminating)
	assertRunUntilStop ("terminating");

    assertEquals("exitValue", 0, terminatingObserver.exitValue);
    assertFalse("exitSignal", terminatingObserver.exitSignal);
  }

  public void testSteppingtestHitAndRun() throws IOException
  {
    if (unresolved(4847))
      return;

    installInstructionObserver = true;
    testHitAndRun();
  }

  public void testInsertRemove() throws IOException
  {
    // Test can run with or without stepping.
    InstructionObserver io1 = new InstructionObserver(task, breakpoint1);
    InstructionObserver io2 = new InstructionObserver(task, breakpoint2);
    if (installInstructionObserver)
      {
        task.requestAddInstructionObserver(io1);
        task.requestAddInstructionObserver(io2);
      }

    // Put in breakpoint observers
    CodeObserver code1 = new CodeObserver(breakpoint1);
    CodeObserver code2 = new CodeObserver(breakpoint2);
    task.requestAddCodeObserver(code1, breakpoint1);
    task.requestAddCodeObserver(code2, breakpoint2);

    // Make sure the observers are properly installed.
    while (! code1.isAdded() || ! code2.isAdded())
	assertRunUntilStop ("code1 an code2 added");

    // Unblock and tell the process to go!
    task.requestUnblock(attachedObserver);

    new GoAround(3).goneAround(3);
    assertEquals(3, code1.getTriggered());
    assertEquals(3, code2.getTriggered());

    // Remove one breakpoint.
    task.requestDeleteCodeObserver(code2, breakpoint2);

    // Wait for removal
    while (! code2.isRemoved())
	assertRunUntilStop ("code 2 removed");

    new GoAround(5).goneAround(8);
    assertEquals(8, code1.getTriggered());
    assertEquals(3, code2.getTriggered());

    // Remove the other breakpoint.
    task.requestDeleteCodeObserver(code1, breakpoint1);

    // Wait for removal
    while (! code1.isRemoved())
	assertRunUntilStop("code1 removed");

    // And go again for 5 times.
    new GoAround(5).goneAround(13);

    // For fun (and to test exec) insert another one.
    CodeObserver code3 = new CodeObserver(breakpoint1);
    task.requestAddCodeObserver(code3, breakpoint1);
    
    // Make sure the observer is properly installed.
    while (! code3.isAdded())
	assertRunUntilStop ("code3 added");

    // Remove the stepper
    if (installInstructionObserver)
      {
	assertEquals(13, io1.getAddrHit());
	assertEquals(13, io2.getAddrHit());

        task.requestDeleteInstructionObserver(io1);
        task.requestDeleteInstructionObserver(io2);
      }
    else
      {
	// Shouldn't be triggered.
	assertEquals(0, io1.getAddrHit());
	assertEquals(0, io2.getAddrHit());
      }

    // And we are done.
    out.writeByte(0);
    out.flush();

    // The process will now exec itself again.
    // Make sure Code Observers have been notified.
    while (! code3.isRemoved())
	assertRunUntilStop ("code3 removed");

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // Wait for termination. How did it all go, did it exit properly?
    while (! terminatingObserver.terminating)
	assertRunUntilStop ("terminating");

    assertEquals("exitValue", 0, terminatingObserver.exitValue);
    assertFalse("exitSignal", terminatingObserver.exitSignal);

    assertEquals(8, code1.getTriggered());
    assertEquals(3, code2.getTriggered());
  }

  public void testSteppingtestInsertRemove() throws IOException
  {
    if (unresolved(4847))
      return;

    installInstructionObserver = true;
    testInsertRemove();
  }

  public void testAddLots() throws IOException
  {
    // Test can run with or without stepping.
    InstructionObserver io1 = new InstructionObserver(task, breakpoint1);
    InstructionObserver io2 = new InstructionObserver(task, breakpoint2);
    if (installInstructionObserver)
      {
        task.requestAddInstructionObserver(io1);
        task.requestAddInstructionObserver(io2);
      }

    // Put in breakpoint observers
    CodeObserver[] codes1 = new CodeObserver[1512];
    for (int i = 0; i < 1512; i++)
      {
	CodeObserver code = new CodeObserver(breakpoint1);
	task.requestAddCodeObserver(code, breakpoint1);
	codes1[i] = code;
      }
    CodeObserver[] codes2 = new CodeObserver[1512];
    for (int i = 0; i < 1512; i++)
      {
	CodeObserver code = new CodeObserver(breakpoint2);
	task.requestAddCodeObserver(code, breakpoint2);
	codes2[i] = code;
      }

    // Make sure the observers are properly installed.
    boolean allAdded = true;
    for (int i = 0; i < 1512 && allAdded; i++)
	allAdded = codes1[i].isAdded() && codes2[i].isAdded();
    while (! allAdded) {
	assertRunUntilStop("allAdded");
	allAdded = true;
	for (int i = 0; i < 1512 && allAdded; i++)
	    allAdded = codes1[i].isAdded() && codes2[i].isAdded();
    }

    // Unblock and tell the process to go!
    task.requestUnblock(attachedObserver);

    // Run a couple of times.
    new GoAround(3).goneAround(3);

    // Remove the stepper
    if (installInstructionObserver)
      {
	assertEquals(3, io1.getAddrHit());
	assertEquals(3, io2.getAddrHit());

        task.requestDeleteInstructionObserver(io1);
        task.requestDeleteInstructionObserver(io2);
      }
    else
      {
	// Shouldn't be triggered.
	assertEquals(0, io1.getAddrHit());
	assertEquals(0, io2.getAddrHit());
      }

    // And we are done.
    out.writeByte(0);
    out.flush();

    // The process will now exec itself again.
    // Make sure all the observers are notified that they have been removed.
    boolean allRemoved = true;
    for (int i = 0; i < 1512 && allRemoved; i++)
	allRemoved = codes1[i].isRemoved() && codes2[i].isRemoved();
    while (! allRemoved) {
	assertRunUntilStop("allRemoved");
	allRemoved = true;
	for (int i = 0; i < 1512 && allRemoved; i++)
	    allRemoved = codes1[i].isRemoved() && codes2[i].isRemoved();
    }

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // Wait for termination. How did it all go, did it exit properly?
    while (! terminatingObserver.terminating)
	assertRunUntilStop("terminating");

    assertEquals("exitValue", 0, terminatingObserver.exitValue);
    assertFalse("exitSignal", terminatingObserver.exitSignal);

    for (int i = 0; i < 1512; i++)
      {
	assertEquals(3, codes1[i].getTriggered());
	assertEquals(3, codes2[i].getTriggered());
      }
  }
  
  public void testSteppingAddLots() throws IOException
  {
    if (unresolved(4847))
      return;

    installInstructionObserver = true;
    testAddLots();
  }

  private class AttachedObserver
      implements TaskObserver.Attached
  {
    public Action updateAttached(Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
    public void addFailed(Object observable, Throwable w)
    {
      fail(w.getMessage());
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

  private class TerminatingObserver
      implements TaskObserver.Terminating
  {
    // How the process exits.
    boolean terminating;
    boolean exitSignal;
    int exitValue;
    public Action updateTerminating (Task task, boolean signal, int value)
    {
	terminating = true;
	exitValue = value;
	exitSignal = signal;
	Manager.eventLoop.requestStop();
	return Action.CONTINUE;
    }

    public void addFailed(Object observable, Throwable w)
    {
      fail(w.getMessage());
    }
    
    public void addedTo(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

  private class CodeObserver
      implements TaskObserver.Code
  {
    private long address;
    private int triggered;

    private boolean added;
    private boolean removed;

    CodeObserver(long address)
    {
      this.address = address;
    }

    public Action updateHit (Task task, long address)
    {
      if (address != this.address)
	fail("updateHit on unknown address");

      triggered++;
      return Action.CONTINUE;
    }

    int getTriggered()
    {
      return triggered;
    }

    public void addFailed(Object observable, Throwable w)
    {
      w.printStackTrace();
      fail(w.getMessage());
    }
    
    public void addedTo(Object observable)
    {
	added = true;
	removed = false;
	Manager.eventLoop.requestStop();
    }

    public boolean isAdded()
    {
      return added;
    }
    
    public void deletedFrom(Object observable)
    {
	removed = true;
	added = true;
	Manager.eventLoop.requestStop();
    }

    public boolean isRemoved()
    {
      return removed;
    }
  }

  private static class InstructionObserver
      implements TaskObserver.Instruction
  { 
    boolean added;
    boolean deleted;

    private final Task task; 
    private final long addr;
    private int addr_hit;

    public void addedTo(Object o)
    {
      added = true;
    }

    public void deletedFrom(Object o)
    {
      deleted = true;
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }

    InstructionObserver(Task task, long addr)
    {
      this.task = task;
      this.addr = addr;
    }

    // Always continue, just counts steps on specified address.
    public Action updateExecuted(Task task)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);

      if (task.getIsa().pc(task) == addr)
	addr_hit++;
      return Action.CONTINUE;
    }

    public int getAddrHit()
    {
      return addr_hit;
    }
  }

    /**
     * Tells the inferior process to loop for COUNT time.
     */
    private class GoAround
	extends Thread
    {
	private int bp1;
	private int bp2;
	private int count;
	private boolean ran;
	private RuntimeException r;
	GoAround(int count)
	{
	    this.count = count;
	}
	/**
	 * Interact with the child process using a separate thread -
	 * so that the operation is independent of the EventLoop.
	 */
	public void run()
	{
	    try {
		out.writeByte(count);
		out.flush();
		// Sanity check that the functions have actually been run.
		bp1 = Integer.decode(in.readLine()).intValue();
		bp2 = Integer.decode(in.readLine()).intValue();
	    }
	    catch (RuntimeException r) {
		// E.g., NULL pointer when in.readLine() returns NULL.
		this.r = r;
	    }
	    catch (Exception e) {
		// E.g., when out.writeByte() fails.
		this.r = new RuntimeException(e);
	    }
	    ran = true;
	    Manager.eventLoop.requestStop();
	}
	/**
	 * In a separate thread (so that the EventLoop remains in the
	 * main thread).  Tell the inferior to iterate for a few
	 * times; and then check that it completed ok.
	 */
	void goneAround(int result)
	{
	    start();
	    while(!ran)
		assertRunUntilStop ("go around for " + count);
	    if (r != null)
		throw r;
	    assertEquals("bp1", result, bp1);
	    assertEquals("bp2", result, bp2);
	}
    }

}
