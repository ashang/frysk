// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
import java.io.IOException;
import java.io.InputStreamReader;
import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.testbed.ForkTestLib;

/**
 * XXX: This code should be simplified, eliminating local parallelism
 * by performing everything in a single test-thread.  Multi-threaded
 * interactions are tested elsewhere.
 */

public class TestSyscallSignal
  extends TestLib
{
  // Process id and Proc representation of our test program.
  int pid;
  Proc proc;

  // How we communicate with the test program.
  BufferedReader in;
  DataOutputStream out;

  // The thread that handles the event loop.
  EventLoopRunner eventloop;

  // Monitor to notify and wait on for state of event changes..
  static Object monitor = new Object();

  /**
   * Launch our test program and setup clean environment with a runner
   * eventloop.
   */
  public void setUp()
  {
    // Make sure everything is setup so spawned processes are recognized
    // and destroyed in tearDown().
    super.setUp();

    // Create a process that we will communicate with through stdin/out.
    String command = TestLib.getExecPrefix() + "funit-syscall-signal";
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
        Manager.eventLoop.requestStop();
      }

      public void procNotFound (ProcId procId, Exception e)
      {
      }});
    Manager.eventLoop.run();


    // Start an EventLoop so we don't have to poll for events all the time.
    eventloop = new EventLoopRunner();
    eventloop.start();
  }

  /**
   * Make sure the test program is really gone and the event loop is
   * stopped.  Individual tests are responsible for nice termination
   * if the want to.
   */
  public void tearDown()
  {
    // Make sure event loop is gone.
    eventloop.requestStop();
    synchronized (monitor)
      {
	while (!eventloop.isStopped())
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

    // And kill off any remaining processes we spawned
    super.tearDown();
  }

  public void testIt() throws IOException
  {
    String line;

    // Make sure the process is "ready"
    line = in.readLine();

    final Task task = proc.getMainTask();

    final SignalObserver sigo = new SignalObserver(Sig.HUP_);
    task.requestAddSignaledObserver(sigo);
    final SyscallObserver syso = new SyscallObserver(42, task);
    task.requestAddSyscallObserver(syso);

    // Make sure the observers are properly installed.
    synchronized (monitor)
      {
	while (! sigo.isAdded() || ! syso.isAdded())
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

    // Kill 1...
    Signal.tkill(pid, Sig.HUP);

    // Tell the process to go some rounds!
    out.writeByte(42);
    out.flush();

    // Wait till our syscall observer triggers and blocks
    // (which is "half way" through the run, there are 42 * 2 syscalls).
    synchronized (monitor)
      {
        while (syso.getEntered() != 42)
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
    
    // Now send a signal to the process while blocked. Then unblock.
    // Do all this on the eventloop so properly serialize calls.
    Manager.eventLoop.add(new TaskEvent()
      {
	public void execute ()
	{
	  Signal.tkill(task.getTid(), Sig.HUP);
	  
	  // And continue running.
	  task.requestUnblock(syso);
	}
      });

    // Sanity check that the functions have actually been run.
    line = in.readLine();
    int hup_cnt = Integer.decode(line).intValue();

    assertEquals(2, hup_cnt);
    assertEquals(2, sigo.getTriggered());

    assertEquals(2 * 42, syso.getEntered());
    assertEquals(2 * 42, syso.getExited());

    // Kill 3...
    Signal.tkill(pid, Sig.HUP);

    // Run some more
    out.writeByte(100);
    out.flush();

    // Sanity check that the functions have actually been run.
    line = in.readLine();
    hup_cnt = Integer.decode(line).intValue();

    assertEquals(3, hup_cnt);
    assertEquals(3, sigo.getTriggered());

    assertEquals(2 * 142, syso.getEntered());
    assertEquals(2 * 142, syso.getExited());

    // We are all done, you can go now.
    out.writeByte(0);
    out.flush();
  }

  class SignalObserver implements TaskObserver.Signaled
  {
    private final int sig;

    private int triggered;
    private boolean added;
    private boolean removed;

    SignalObserver(int sig)
    {
      this.sig = sig;
    }

    public Action updateSignaled(Task task, int signal)
    {
      if (signal == sig)
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

  /**
   * Observer that looks for open and close syscalls.
   * After a given number of calls it will BLOCK from the syscall enter.
   */
  class SyscallObserver implements TaskObserver.Syscall
  {
    private final int stophits;

    private int entered;
    private int exited;
    private boolean added;
    private boolean removed;

    private final frysk.proc.Syscall opensys;
    private final frysk.proc.Syscall closesys;

    SyscallObserver(int stophits, Task task)
    {
      this.stophits = stophits;
      this.opensys = frysk.proc.Syscall.syscallByName("open", task);
      this.closesys = frysk.proc.Syscall.syscallByName("close", task);
    }

    public Action updateSyscallEnter(Task task)
    {
      SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
      frysk.proc.Syscall syscall = syscallEventInfo.getSyscall(task);
      if (opensys.equals(syscall) || closesys.equals(syscall))
	{
	  entered++;
	  if (entered == stophits)
	    {
	      synchronized(monitor)
		{
		  monitor.notifyAll();
		  return Action.BLOCK;
		}
	    }
	}
      return Action.CONTINUE;
    }

    public Action updateSyscallExit(Task task)
    {
      SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
      // XXX - workaround for broken syscall detection on exit
      if (syscallEventInfo.number(task) == -1)
	return Action.CONTINUE;
      frysk.proc.Syscall syscall = syscallEventInfo.getSyscall(task);
      if (opensys.equals(syscall) || closesys.equals(syscall))
	{
	  exited++;
	}
      return Action.CONTINUE;
    }

    int getEntered()
    {
      return entered;
    }

    int getExited()
    {
      return exited;
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

    private SyscallEventInfo getSyscallEventInfo(Task task)
    {
      try
	{
	  return task.getSyscallEventInfo();
	}
      catch (TaskException e)
	{
	  fail("task exception " + e);
	  return null; // not reached
	}
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
