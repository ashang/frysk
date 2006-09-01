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

import java.io.*;
import java.net.*;

import frysk.sys.SyscallNum;

public class TestSyscallRunning
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
    String command = TestLib.getExecPrefix() + "funit-syscall-running";
    ForkTestLib.ForkedProcess process;
    process = ForkTestLib.fork(new String[] { command });
    pid = process.pid;

    in = new BufferedReader(new InputStreamReader(process.in));
    out = new DataOutputStream(process.out);

    // Make sure the core knows about it.
    Manager.host.requestRefreshXXX(true);
    Manager.eventLoop.runPending();
    proc = Manager.host.getProc(new ProcId(pid));

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

  public void testSyscallRunning() throws IOException
  {
    // Get the port that will be listened on.
    String line = in.readLine();
    int port = Integer.decode(line).intValue();

    final Task task = proc.getMainTask();

    final SyscallObserver syso = new SyscallObserver();
    task.requestAddSyscallObserver(syso);

    // Make sure the observer is properly installed.
    synchronized (monitor)
      {
	while (! syso.isAdded())
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

    // Tell the process to go some rounds!
    out.writeByte(1);
    out.flush();

    // Wait till our syscall observer triggers and blocks
    synchronized (monitor)
      {
        while (! syso.getEntered())
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

    // Now unblock and then attach another observer.
    // Do all this on the eventloop so properly serialize calls.
    final SyscallObserver syso2 = new SyscallObserver();
    Manager.eventLoop.add(new TaskEvent()
      {
	public void execute ()
	{
	  // Continue running (inside syscall),
	  // while attaching another syscall observer
	  task.requestUnblock(syso);
	  task.requestAddSyscallObserver(syso2);
	}
      });

    // Wait till we are properly added...
    synchronized (monitor)
      {
	while (! syso2.isAdded())
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

    // Sanity check
    assertTrue(syso.getEntered());
    assertFalse(syso.getExited());
    assertFalse(syso2.getEntered());
    assertFalse(syso2.getExited());

    Socket s = new Socket("localhost", port);
    s.close();

    // And check that the observers trigger
    synchronized (monitor)
      {
	while (! syso.getExited() || ! syso2.getExited())
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
  }

  /**
   * Observer that looks for open and close syscalls.
   * After a given number of calls it will BLOCK from the syscall enter.
   */
  class SyscallObserver implements TaskObserver.Syscall
  {
    private boolean entered;
    private boolean exited;
    private boolean added;
    private boolean removed;

    SyscallObserver()
    {
    }

    public Action updateSyscallEnter(Task task)
    {
      SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
      int syscallNum = syscallEventInfo.number (task);
      if (syscallNum == SyscallNum.SYSaccept)
	{
	  entered = true;
	  synchronized(monitor)
	    {
	      monitor.notifyAll();
	      return Action.BLOCK;
	    }
	}
      return Action.CONTINUE;
    }

    public Action updateSyscallExit(Task task)
    {
      SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
      int syscallNum = syscallEventInfo.number (task);
      if (syscallNum == SyscallNum.SYSaccept)
	{
	  exited = true;
	  synchronized(monitor)
            {
              monitor.notifyAll();
            }
	}
      return Action.CONTINUE;
    }

    boolean getEntered()
    {
      return entered;
    }

    boolean getExited()
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
