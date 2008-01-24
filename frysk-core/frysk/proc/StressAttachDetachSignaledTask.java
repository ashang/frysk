// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.testbed.SlaveOffspring;
import frysk.testbed.TestLib;
import frysk.testbed.TaskObserverBase;
import frysk.Config;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.sys.Signal;

/**
 * Test attaching to a process with many many tasks.
 * 
 * When a the kernel is sent both a request to attach to a task and,
 * moments later, a signal, the attach may either find the task
 * stopped, or stopped-with-signal.  Since this isn't really
 * deterministic, exercise the edge case via a stress test.
 */

public class StressAttachDetachSignaledTask
    extends TestLib
{
    /**
     * An agressive observer that spends its life adding and removing
     * itself.
     */
    class AttachDetach
	extends TaskObserverBase
	implements TaskObserver.Attached
    {
	public Action updateAttached (Task task)
	{
	    return Action.CONTINUE;
	}
    }

    /**
     * Stress test attaching and detaching a process that is
     * constantly receiving signals.
     */
    abstract class Spawn
    {
	/**
	 * Perform arbitrary operation OP.
	 */
	abstract void op (SlaveOffspring child, int iteration);

	Spawn ()
	{
	    SlaveOffspring child = SlaveOffspring.createDaemon();
	    AttachDetach attachDetach = new AttachDetach ();
	    Task task = child.findTaskUsingRefresh (true);

	    for (int i = 0; i < 20; i++) {
		// Ask for the observer to be attached, then run the
		// event loop sufficiently for the attach request to
		// be sent to the kernel.  Finally issue an operation
		// which make a signal pending on that same task.
		task.requestAddAttachedObserver (attachDetach);
		runPending ();
		op (child, i * 2 + 0);

		// Ask for the observer to be attached, then run the event
		// loop sufficiently for the attach request to be
		// initiated but not completed.
		task.requestDeleteAttachedObserver (attachDetach);
		runPending ();
		op (child, i * 2 + 1);
	    }
	}
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously creating and deleting
     * child processes.
     */
    public void testForking ()
    {
	if (unresolved(2952))
	    return;
	new Spawn ()
	{
	    void op (SlaveOffspring child, int iteration)
	    {
		switch (iteration % 2) {
		case 0:
		    child.assertSendAddForkWaitForAcks ();
		    break;
		case 1:
		    child.assertSendDelForkWaitForAcks ();
		    break;
		}
	    }
	};
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously creating and deleting new
     * tasks.
     */
    public void testCloning ()
    {
	if (unresolved(2953))
	    return;
	new Spawn ()
	{
	    void op (SlaveOffspring child, int iteration)
	    {
		switch (iteration % 2) {
		case 0:
		    child.assertSendAddCloneWaitForAcks ();
		    break;
		case 1:
		    child.assertSendDelCloneWaitForAcks ();
		    break;
		}
	    }
	};
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * receiving signals, and simultaneously doing execs.
     */
    public void testExecing ()
    {
	new Spawn ()
	{
	    void op (SlaveOffspring child, int iteration)
	    {
		child.assertSendExecWaitForAcks ();
	    }
	};
    }

    /**
     * A signal class; that just adds then delets itself.  If an ABORT
     * signal is seen (the child panics) fail the test.
     */
    private static class SignalStorm
	extends TaskObserverBase implements TaskObserver.Signaled
    {
	private int count = 1000;
	private final Action action;
	SignalStorm(Action action) {
	    this.action = action;
	}
	public Action updateSignaled(Task task,
				     frysk.isa.signals.Signal signal) {
	    assertTrue("child did not abort",
		       signal.intValue() != Signal.TERM.intValue());
	    assertEquals("signal HUP", Signal.HUP.intValue(),
			 signal.intValue());
	    task.requestDeleteSignaledObserver(this);
	    return action;
	}
	public void deletedFrom(Object o) {
	    if (--count == 0)
		Manager.eventLoop.requestStop();
	    Task task = (Task)o;
	    task.requestAddSignaledObserver(this);
	}
    }
    private void stressSignalStorm(Action action) {
	    DaemonBlockedAtEntry daemon
		= new DaemonBlockedAtEntry(new String[] {
			Config.getPkgLibFile("funit-hups").getAbsolutePath(),
			"-t",
			"" + getTimeoutSeconds()
		    });
	    daemon.requestRemoveBlock();
	    SignalStorm storm = new SignalStorm(action);
	    daemon.getMainTask().requestAddSignaledObserver(storm);
	    assertRunUntilStop("storming");
    }

    /**
     * Stress attaching and detaching a task that is constantly
     * signalling itself.  If a signal is lost, the child process will
     * abort.
     */
    public void testBlockedSignalStorm() {
	stressSignalStorm(Action.BLOCK);
    }
    /**
     * Stress attaching and detaching a task that is constantly
     * signalling itself.  If a signal is lost, the child process will
     * abort.
     */
    public void testUnblockedSignalStorm() {
	stressSignalStorm(Action.CONTINUE);
    }
}
