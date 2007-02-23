// This file is part of the program FRYSK.
//
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

import frysk.testbed.ForkTestLib;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;


public class TestBreakpoints
  extends TestLib
{
  private boolean brokenSignals ()
  {
     return brokenIfKernelXXX (4019, new String[]
	 {
	     "2.6.18-1.2257.fc5",
	     "2.6.19-1.2288.fc5"
	 });
  }

  // Process id and Proc representation of our test program.
  int pid;
  Proc proc;

  // How we communicate with the test program.
  BufferedReader in;
  DataOutputStream out;

  // The thread that handles the event loop.
  EventLoopRunner eventLoop;

  // Whether we are attached (the AttachedObserver has triggered).
  boolean attached;

  // How the process exits (set by the TerminatingObserver).
  boolean terminating;
  boolean exitSignal;
  int exitValue;

  // Whether the proc has really terminited (cannot be seen by the host).
  boolean procTerminated;

  // Monitor to notify and wait on for state of event changes..
  static Object monitor = new Object();

  // Whether or not to install an Instruction observer
  boolean installInstructionObserver;

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
    String command = getExecPath ("funit-breakpoints");
    ForkTestLib.ForkedProcess process;
    process = ForkTestLib.fork(new String[] { command });
    pid = process.pid;
    in = new BufferedReader(new InputStreamReader(process.in));
    out = new DataOutputStream(process.out);
    
    // Make sure the core knows about it.
    Manager.host.requestFindProc(new ProcId(pid), new Host.FindProc() {

      public void procFound (ProcId procId)
      {
      proc = Manager.host.getProc(procId);
      attached = false;
      terminating = false;
      procTerminated = false;
      Manager.eventLoop.requestStop();
      }

      public void procNotFound (ProcId procId, Exception e)
      {
      }});
    
    Manager.eventLoop.run();
  }

  /**
   * Make sure the test program is really gone and the event loop is
   * stopped.  Individual tests are responsible for nice termination
   * if the want to.
   */
  public void tearDown()
  {
      if (eventLoop != null) {
	  // Make sure event loop is gone.
	  eventLoop.requestStop();
	  synchronized (monitor)
	      {
		  while (!eventLoop.isStopped())
		      {
			  try
			      {
				  monitor.wait();
			      }
			  catch (InterruptedException ie)
			      {
				  // Ignored
			      }
		      }
	      }
      }

      // And kill off any remaining processes we spawned
      super.tearDown();
  }

  public void testHitAndRun() throws IOException
  {
    // Start an EventLoop so there's no need to poll for events all
    // the time.
    eventLoop = new EventLoopRunner();
    eventLoop.start();

    String line;
    
    // Request addresses to put breakpoints on.
    line = in.readLine();
    long breakpoint1 = Long.decode(line).longValue();

    // Never used.
    line = in.readLine();
    long breakpoint2 = Long.decode(line).longValue();

    // Make sure we are attached - XXX - do we need this?
    // Can't the requestAddCodeObserver() do that for us?
    // The observer will block the task so we can put in the breakpoints.
    Task task = proc.getMainTask();

    AttachedObserver ao = new AttachedObserver();
    task.requestAddAttachedObserver(ao);

    TerminatingObserver to = new TerminatingObserver();
    task.requestAddTerminatingObserver(to);

    // Wait till we are attached.
    synchronized (monitor)
      {
	while (! attached)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

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
    synchronized (monitor)
      {
	while (! code1.isAdded() || ! code2.isAdded())
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// ignored
	      }
	  }
      }

    // Unblock and tell the process to go some rounds!
    task.requestUnblock(ao);

    out.writeByte(3);
    out.flush();
    
    // Sanity check that the functions have actually been run.
    line = in.readLine();
    int bp1 = Integer.decode(line).intValue();
    line = in.readLine();
    int bp2 = Integer.decode(line).intValue();

    assertEquals(3, bp1);
    assertEquals(3, bp2);

    assertEquals(3, code1.getTriggered());
    assertEquals(3, code2.getTriggered());

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
    synchronized (monitor)
      {
	while (! (code1.isRemoved() && code2.isRemoved()))
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // So how did it all go, did it exit properly?
    synchronized (monitor)
      {
	while (! terminating)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

    assertEquals(0, exitValue);
    assertFalse(exitSignal);
  }

  public void testSteppingtestHitAndRun() throws IOException
  {
    if (brokenSignals ())
      return;
    installInstructionObserver = true;
    testHitAndRun();
  }

  public void testInsertRemove() throws IOException
  {
    if (brokenSignals ())
      return;
    // Start an EventLoop so there's no need to poll for events all
    // the time.
    eventLoop = new EventLoopRunner();
    eventLoop.start();

    String line;

    // Request addresses to put breakpoints on.
    line = in.readLine();
    long breakpoint1 = Long.decode(line).longValue();
    line = in.readLine();
    long breakpoint2 = Long.decode(line).longValue();

    // Make sure we are attached and look for termination.
    Task task = proc.getMainTask();

    AttachedObserver ao = new AttachedObserver();
    task.requestAddAttachedObserver(ao);
    TerminatingObserver to = new TerminatingObserver();
    task.requestAddTerminatingObserver(to);

    // Wait till we are attached.
    synchronized (monitor)
      {
	while (! attached)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

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
    synchronized (monitor)
      {
	while (! code1.isAdded() || ! code2.isAdded())
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// ignored
	      }
	  }
      }

    // Unblock and tell the process to go!
    task.requestUnblock(ao);

    // Run a couple of times.
    out.writeByte(3);
    out.flush();

    // Sanity check that the functions have actually been run.
    line = in.readLine();
    int bp1 = Integer.decode(line).intValue();
    line = in.readLine();
    int bp2 = Integer.decode(line).intValue();

    assertEquals(3, bp1);
    assertEquals(3, bp2);
    assertEquals(3, code1.getTriggered());
    assertEquals(3, code2.getTriggered());

    // Remove one breakpoint.
    task.requestDeleteCodeObserver(code2, breakpoint2);

    // Wait for removal
    synchronized (monitor)
      {
        while (! code2.isRemoved())
          {
            try
              {
                monitor.wait();
              }
            catch (InterruptedException ie)
              {
                // Ignored
              }
          }
      }

    // And go again for 5 times.
    out.writeByte(5);
    out.flush();

    line = in.readLine();
    bp1 = Integer.decode(line).intValue();
    line = in.readLine();
    bp2 = Integer.decode(line).intValue();

    assertEquals(8, bp1);
    assertEquals(8, bp2);
    assertEquals(8, code1.getTriggered());
    assertEquals(3, code2.getTriggered());

    // Remove the other breakpoint.
    task.requestDeleteCodeObserver(code1, breakpoint1);

    // Wait for removal
    synchronized (monitor)
      {
        while (! code1.isRemoved())
          {
            try
              {
                monitor.wait();
              }
            catch (InterruptedException ie)
              {
                // Ignored
              }
          }
      }

    // And go again for 5 times.
    out.writeByte(5);
    out.flush();

    line = in.readLine();
    bp1 = Integer.decode(line).intValue();
    line = in.readLine();
    bp2 = Integer.decode(line).intValue();

    // For fun (and to test exec) insert another one.
    CodeObserver code3 = new CodeObserver(breakpoint1);
    task.requestAddCodeObserver(code3, breakpoint1);
    
    // Make sure the observer is properly installed.
    synchronized (monitor)
      {
	while (! code3.isAdded())
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// ignored
	      }
	  }
      }

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
    synchronized (monitor)
      {
	while (! code3.isRemoved())
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // Wait for termination. How did it all go, did it exit properly?
    synchronized (monitor)
      {
	while (! terminating)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

    assertEquals(0, exitValue);
    assertFalse(exitSignal);

    assertEquals(13, bp1);
    assertEquals(13, bp2);
    assertEquals(8, code1.getTriggered());
    assertEquals(3, code2.getTriggered());
  }

  public void testSteppingtestInsertRemove() throws IOException
  {
    installInstructionObserver = true;
    testInsertRemove();
  }

  public void testAddLots() throws IOException
  {
    if (brokenSignals ())
      return;

    // Start an EventLoop so there's no need to poll for events all
    // the time.
    eventLoop = new EventLoopRunner();
    eventLoop.start();

    String line;

    // Request addresses to put breakpoints on.
    line = in.readLine();
    long breakpoint1 = Long.decode(line).longValue();
    line = in.readLine();
    long breakpoint2 = Long.decode(line).longValue();

    // Make sure we are attached and look for termination.
    Task task = proc.getMainTask();

    AttachedObserver ao = new AttachedObserver();
    task.requestAddAttachedObserver(ao);
    TerminatingObserver to = new TerminatingObserver();
    task.requestAddTerminatingObserver(to);

    // Wait till we are attached.
    synchronized (monitor)
      {
	while (! attached)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

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
    synchronized (monitor)
      {
	boolean allAdded = true;
	for (int i = 0; i < 1512 && allAdded; i++)
	  allAdded = codes1[i].isAdded() && codes2[i].isAdded();
	while (! allAdded)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// ignored
	      }
	    
	    allAdded = true;
	    for (int i = 0; i < 1512 && allAdded; i++)
	      allAdded = codes1[i].isAdded() && codes2[i].isAdded();
	  }
      }

    // Unblock and tell the process to go!
    task.requestUnblock(ao);

    // Run a couple of times.
    out.writeByte(3);
    out.flush();

    // Sanity check that the functions have actually been run.
    line = in.readLine();
    int bp1 = Integer.decode(line).intValue();
    line = in.readLine();
    int bp2 = Integer.decode(line).intValue();

    assertEquals(3, bp1);
    assertEquals(3, bp2);

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
    synchronized (monitor)
      {
	boolean allRemoved = true;
	for (int i = 0; i < 1512 && allRemoved; i++)
	  allRemoved = codes1[i].isRemoved() && codes2[i].isRemoved();
	while (! allRemoved)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// ignored
	      }
	    
	    allRemoved = true;
	    for (int i = 0; i < 1512 && allRemoved; i++)
	      allRemoved = codes1[i].isRemoved() && codes2[i].isRemoved();
	  }
      }

    // And let it go. We are really done.
    out.writeByte(0);
    out.flush();

    // Wait for termination. How did it all go, did it exit properly?
    synchronized (monitor)
      {
	while (! terminating)
	  {
	    try
	      {
		monitor.wait();
	      }
	    catch (InterruptedException ie)
	      {
		// Ignored
	      }
	  }
      }

    assertEquals(0, exitValue);
    assertFalse(exitSignal);

    for (int i = 0; i < 1512; i++)
      {
	assertEquals(3, codes1[i].getTriggered());
	assertEquals(3, codes2[i].getTriggered());
      }
  }
  
  public void testSteppingAddLots() throws IOException
  {
    installInstructionObserver = true;
    testAddLots();
  }

  class AttachedObserver implements TaskObserver.Attached
  {
    private boolean added;

    public Action updateAttached(Task task)
    {
      synchronized (monitor)
	{
	  attached = true;
	  monitor.notifyAll();
	}
      return Action.BLOCK;
    }

    public void addFailed(Object observable, Throwable w)
    {
      w.printStackTrace();
      fail(w.getMessage());
    }
    
    public void addedTo(Object observable)
    {
      // Hurray! Lets notify everybody.
      synchronized (monitor)
	{
	  added = true;
	  monitor.notifyAll();
	}
    }

    public boolean isAdded()
    {
      return added;
    }
    
    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

  class TerminatingObserver implements TaskObserver.Terminating
  {
    private boolean added;
    
    public Action updateTerminating (Task task, boolean signal, int value)
    {
      synchronized (monitor)
	{
	  terminating = true;
	  exitValue = value;
	  exitSignal = signal;
	  monitor.notifyAll();
	}
      return Action.CONTINUE;
    }

    public void addFailed(Object observable, Throwable w)
    {
      w.printStackTrace();
      fail(w.getMessage());
    }
    
    public void addedTo(Object observable)
    {
      // Hurray! Lets notify everybody.
      synchronized (monitor)
	{
	  added = true;
	  monitor.notifyAll();
	}
    }

    public boolean isAdded()
    {
      return added;
    }
    
    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

  class CodeObserver implements TaskObserver.Code
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
      // Hurray! Lets notify everybody.
      synchronized (monitor)
	{
	  added = true;
	  removed = false;
	  monitor.notifyAll();
	}
    }

    public boolean isAdded()
    {
      return added;
    }
    
    public void deletedFrom(Object observable)
    {
      synchronized (monitor)
	{
	  removed = true;
	  added = true;
	  monitor.notifyAll();
	}
    }

    public boolean isRemoved()
    {
      return removed;
    }
  }

  static class InstructionObserver implements TaskObserver.Instruction
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


  static class EventLoopRunner extends Thread
  {
    private boolean stopped;

    public void run()
    {
      stopped = false;
      try
	{
	  Manager.eventLoop.run();
	}
      finally
	{
	  synchronized (monitor)
	    {
	      stopped = true;
	      monitor.notifyAll();
	    }
	}
    }

    public void requestStop()
    {
      Manager.eventLoop.requestStop();
    }

    public boolean isStopped()
    {
      return stopped;
    }

    public String toString()
    {
      return "EventLoop-" + super.toString();
    }
  }
}
