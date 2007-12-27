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

package frysk.proc;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.io.OutputStream;
import frysk.event.Event;
import frysk.testbed.TestLib;
import frysk.testbed.TearDownProcess;
import frysk.Config;
import frysk.sys.DaemonPipePair;

/**
 * XXX: This code should be simplified, eliminating local parallelism
 * by performing everything in a single test-thread.  Multi-threaded
 * interactions are tested elsewhere.
 */

public class TestSyscallRunning
  extends TestLib
{
  // Process id and Proc representation of our test program.
  Proc proc;

  // How we communicate with the test program.
  BufferedReader in;
  DataOutputStream out;

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
    DaemonPipePair process
	= new DaemonPipePair(new String[] {
				 Config.getPkgLibFile("funit-syscall-running").getPath()
			     });
    TearDownProcess.add(process.pid);

    in = new BufferedReader(new InputStreamReader(process.in.getInputStream()));
    out = new DataOutputStream(process.out.getOutputStream());

    // Make sure the core knows about it.
    Manager.host.requestFindProc(new ProcId(process.pid.hashCode()),
				 new FindProc()
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
  }

  public void testSyscallRunning() throws IOException
  {
    // Get the port that will be listened on.
    int port = Integer.decode(in.readLine()).intValue();

    final Task procTask = proc.getMainTask();

    final SyscallObserver syso = new SyscallObserver("accept", procTask, false);
    procTask.requestAddSyscallObserver(syso);

    // Make sure the observer is properly installed.
    while (! syso.isAdded())
	assertRunUntilStop ("syso added");

    // Tell the process to go some rounds!
    out.writeByte(1);
    out.flush();

    // Wait till our syscall observer triggers and blocks
    while (! syso.getEntered())
	assertRunUntilStop("syso entered");

    // Now unblock and then attach another observer.
    // Do all this on the eventloop so properly serialize calls.
    final SyscallObserver syso2 = new SyscallObserver("accept", procTask, true);
    Manager.eventLoop.add(new Event() {
	    public void execute () {
		// Continue running (inside syscall), while attaching
		// another syscall observer
		procTask.requestUnblock(syso);
		procTask.requestAddSyscallObserver(syso2);
	    }
	});

    // Wait till we are properly added...
    while (! syso2.isAdded())
	assertRunUntilStop("syso2 added");

    // Sanity check
    assertTrue("syso entered", syso.getEntered());
    assertFalse("syso exited", syso.getExited());
    assertTrue("syso2 entered", syso2.getEntered());
    assertFalse("syso2 exited", syso2.getExited());

    // Write something to the socket and close it so the syscall exits.
    Socket s = new Socket("localhost", port);
    OutputStream out = s.getOutputStream();
    out.write(1);
    out.flush();
    s.close();

    // And check that the observers trigger
    while (! syso.getExited() || ! syso2.getExited())
	assertRunUntilStop("syso and syso2 exited");
  }

    /**
     * Observer that looks for open and close syscalls.
     * After a given number of calls it will BLOCK from the syscall enter.
     */
    class SyscallObserver implements TaskObserver.Syscall {
	private boolean entered;
	private boolean exited;
	private boolean added;
	private boolean removed;

	private final frysk.proc.Syscall syscall;

	SyscallObserver(String call, Task task, boolean entered) {
	    SyscallTable syscallTable
		= SyscallTableFactory.getSyscallTable(task.getISA());
	    syscall = syscallTable.syscallByName(call);
	    this.entered = entered;
	}

	public Action updateSyscallEnter(Task task) {
	    SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
	    if (syscallEventInfo.getSyscall(task).equals(syscall)) {
		entered = true;
		Manager.eventLoop.requestStop();
		return Action.BLOCK;
	    }
	    return Action.CONTINUE;
	}

	public Action updateSyscallExit(Task task) {
	    if (entered) {
		exited = true;
		Manager.eventLoop.requestStop();
	    }
	    return Action.CONTINUE;
	}

	boolean getEntered() {
	    return entered;
	}

	boolean getExited() {
	    return exited;
	}

	public void addFailed(Object observable, Throwable w) {
	    w.printStackTrace();
	    fail(w.getMessage());
	}
    
	public void addedTo(Object observable) {
	    added = true;
	    removed = false;
	    Manager.eventLoop.requestStop();
	}

	public boolean isAdded() {
	    return added;
	}
    
	public void deletedFrom(Object observable) {
	    removed = true;
	    added = true;
	    Manager.eventLoop.requestStop();
	}

	public boolean isRemoved() {
	    return removed;
	}

	private SyscallEventInfo getSyscallEventInfo(Task task) {
	    return task.getSyscallEventInfo();
	}
    }
}
