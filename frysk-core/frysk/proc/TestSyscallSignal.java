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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import frysk.sys.Signal;
import frysk.sys.DaemonPipePair;
import frysk.testbed.TestLib;
import frysk.testbed.TearDownProcess;
import frysk.Config;
import frysk.sys.ProcessIdentifier;

/**
 * XXX: This code should be simplified, eliminating local parallelism
 * by performing everything in a single test-thread.  Multi-threaded
 * interactions are tested elsewhere.
 */

public class TestSyscallSignal
  extends TestLib
{
  // Process id and Proc representation of our test program.
  private ProcessIdentifier pid;
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
				 Config.getPkgLibFile("funit-syscall-signal")
				 .getPath()
			     });
    pid = process.pid;
    TearDownProcess.add(pid);
    in = new BufferedReader(new InputStreamReader(process.in.getInputStream()));
    out = new DataOutputStream(process.out.getOutputStream());

    // Make sure the core knows about it.
    Manager.host.requestFindProc(new ProcId(pid.hashCode()),
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

  public void testIt() throws IOException
  {
    // Make sure the process is "ready"
    in.readLine();

    final Task task = proc.getMainTask();

    final SignalObserver sigo = new SignalObserver(Signal.HUP);
    task.requestAddSignaledObserver(sigo);
    final SyscallObserver syso = new SyscallObserver(42, task);
    task.requestAddSyscallObserver(syso);

    // Make sure the observers are properly installed.
    while (! sigo.isAdded() || ! syso.isAdded())
	assertRunUntilStop("sigo and syso added");

    // Kill 1...
    pid.tkill(Signal.HUP);

    // Tell the process to go some rounds!
    out.writeByte(42);
    out.flush();

    // Wait till our syscall observer triggers and blocks
    // (which is "half way" through the run, there are 42 * 2 syscalls).
    while (syso.getEntered() != 42)
	assertRunUntilStop ("syso entered is 42");
    
    // Now send a signal to the process while blocked. Then unblock.
    Signal.HUP.tkill(task.getTid());
    task.requestUnblock(syso);

    // Sanity check that the functions have actually been run.
    class HupCount
	extends Thread
    {
	int hup_cnt;
	boolean ran;
	RuntimeException r;
	public void run()
	{
	    try {
		hup_cnt = Integer.decode(in.readLine()).intValue();
	    }
	    catch (RuntimeException r) {
		this.r = r;
	    }
	    catch (Exception e) {
		this.r = new RuntimeException(e);
	    }
	    ran = true;
	    Manager.eventLoop.requestStop();
	}
	void assertCount(int count)
	{
	    setDaemon(true);
	    start();
	    while (!ran)
		assertRunUntilStop ("reading hup_count " + count);
	    if (r != null)
		throw r;
	    assertEquals("hup_cnt", count, hup_cnt);
	}
    }
    new HupCount().assertCount(2);
    assertEquals(2, sigo.getTriggered());

    assertEquals(2 * 42, syso.getEntered());
    assertEquals(2 * 42, syso.getExited());

    // Kill 3...
    pid.tkill(Signal.HUP);

    // Run some more
    out.writeByte(100);
    out.flush();

    // Sanity check that the functions have actually been run.
    new HupCount().assertCount(3);
    assertEquals(3, sigo.getTriggered());

    assertEquals(2 * 142, syso.getEntered());
    assertEquals(2 * 142, syso.getExited());
  }

    class SignalObserver implements TaskObserver.Signaled {
	private final Signal sig;
	private int triggered;
	private boolean added;
	private boolean removed;

	SignalObserver(Signal sig) {
	    this.sig = sig;
	}

	public Action updateSignaled(Task task, int signal) {
	    if (sig.equals(signal))
		triggered++;
	    return Action.CONTINUE;
	}

	int getTriggered() {
	    return triggered;
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
    }

    /**
     * Observer that looks for open and close syscalls.
     * After a given number of calls it will BLOCK from the syscall enter.
     */
    class SyscallObserver implements TaskObserver.Syscall {
	private final int stophits;

	private int entered;
	private int exited;
	private boolean added;
	private boolean removed;

	private final frysk.proc.Syscall opensys;
	private final frysk.proc.Syscall closesys;

	SyscallObserver(int stophits, Task task) {
	    SyscallTable syscallTable
		= SyscallTableFactory.getSyscallTable(task.getISA());
	    this.stophits = stophits;
	    this.opensys = syscallTable.syscallByName("open");
	    this.closesys = syscallTable.syscallByName("close");
	}

	public Action updateSyscallEnter(Task task) {
	    SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
	    frysk.proc.Syscall syscall = syscallEventInfo.getSyscall(task);
	    if (opensys.equals(syscall) || closesys.equals(syscall)) {
		entered++;
		if (entered == stophits) {
		    Manager.eventLoop.requestStop();
		    return Action.BLOCK;
		}
	    }
	    return Action.CONTINUE;
	}

	public Action updateSyscallExit(Task task) {
	    SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
	    // XXX - workaround for broken syscall detection on exit
	    if (syscallEventInfo.number(task) == -1)
		return Action.CONTINUE;
	    frysk.proc.Syscall syscall = syscallEventInfo.getSyscall(task);
	    if (opensys.equals(syscall) || closesys.equals(syscall)) {
		exited++;
	    }
	    return Action.CONTINUE;
	}

	int getEntered() {
	    return entered;
	}

	int getExited() {
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
