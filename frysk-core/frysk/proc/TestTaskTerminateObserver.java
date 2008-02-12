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

import frysk.sys.Pid;
import frysk.isa.signals.Signal;
import frysk.testbed.TestLib;
import frysk.testbed.StopEventLoopWhenProcRemoved;
import frysk.testbed.TaskObserverBase;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.SynchronizedOffspring;

/**
 * Check the Task terminating and terminated observers.
 */

public class TestTaskTerminateObserver
    extends TestLib
{
    private static int INVALID = 128;

    /**
     * Save the Terminating, and Terminated values as they pass by.
     */
    class Terminate extends TaskObserverBase
	implements TaskObserver.Terminating, TaskObserver.Terminated
    {
	int terminating = INVALID;
	int terminated = INVALID;
	public Action updateTerminating(Task task, Signal signal, int value) {
	    if (signal != null) {
		terminating = -signal.intValue();
	    } else {
		terminating = value;
	    }
	    return Action.CONTINUE;
	}
	public Action updateTerminated(Task task, Signal signal, int value) {
	    if (signal != null) {
		terminated = -signal.intValue();
	    } else {
		terminated = value;
	    }
	    return Action.CONTINUE;
	}
    }

    /**
     * When either Terminating, and Terminated is not INVALID, install
     * and verify corresponding observers.
     */
    public void check (int expected, int terminating, int terminated) {
	// Start the program.
	DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(new String[] {
		getExecPath ("funit-exit"),
		Integer.toString(expected)
	    });
	
	// Bail once it has exited.
	new StopEventLoopWhenProcRemoved(child);
	
	// Set up an observer that watches for both Terminating and
	// Terminated events.
	Terminate terminate = new Terminate();
	if (terminated != INVALID) {
	    child.getMainTask().requestAddTerminatedObserver(terminate);
	}
	if (terminating != INVALID) {
	    child.getMainTask().requestAddTerminatingObserver(terminate);
	}
	child.requestRemoveBlock();
	assertRunUntilStop("run \"exit\" to exit");

	assertEquals("terminating value", terminating, terminate.terminating);
	assertEquals("terminated value", terminated, terminate.terminated);
    }

    /**
     * Check that both the Terminating, and Terminated values match
     * EXPECTED.
     */
    public void terminate (int expected) {
	check(expected, expected, expected);
    }
    /**
     * Check that the Terminating value matches EXPECTED.
     */
    public void terminating (int expected) {
	check(expected, expected, INVALID);
    }
    /**
     * Check that the Terminated value matches EXPECTED.
     */
    public void terminated (int expected) {
	check(expected, INVALID, expected);
    }

    public void testTerminateExit0 () {
	terminate(0);
    }

    public void testTerminateExit47 () {
	terminate(47);
    }

    public void testTerminateKillINT () {
	terminate(- frysk.sys.Signal.INT.intValue());
    }

    public void testTerminateKillHUP () {
	terminate(- frysk.sys.Signal.HUP.intValue());
    }

    public void testTerminatingExit0 () {
	terminating(0);
    }

    public void testTerminatingExit47 () {
	terminating(47);
    }

    public void testTerminatingKillINT () {
	terminating(- frysk.sys.Signal.INT.intValue());
    }

    public void testTerminatingKillHUP () {
	terminating(- frysk.sys.Signal.HUP.intValue());
    }

    public void testTerminatedExit0 () {
	terminated(0);
    }

    public void testTerminatedExit47 () {
	terminated(47);
    }

    public void testTerminatedKillINT () {
	terminated(- frysk.sys.Signal.INT.intValue());
    }

    public void testTerminatedKillKILL () {
	// XXX: When a process is sent a SIGKILL, the only guarentee
	// is that a TERMINATED event will be received.  Dependant on
	// various races, there may or may not also be a terminating
	// event.  See bug 3639.
	terminated(- frysk.sys.Signal.KILL.intValue());
    }

    public void testTerminatedKillHUP () {
	terminated(- frysk.sys.Signal.HUP.intValue());
    }

    class TerminatingCounter extends TaskObserverBase
	implements TaskObserver.Terminating
    {
	int count;
	public void addedTo (Object o) {
	    Manager.eventLoop.requestStop();
	}
	public Action updateTerminating(Task task, Signal signal, int value) {
	    count++;
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}
    }

    /**
     * Check that a process with a task, that has exited, but not yet
     * been joined (i.e., in the 'X' state) can be attached and than
     * followed through to its termination.
     */
    public void testAttachToUnJoinedTask () {
	final int timeout = 5; // XXX: Should be constant in TestLib.

	SynchronizedOffspring daemon = new SynchronizedOffspring
	    (SynchronizedOffspring.START_ACK,
	     new String[]{
		getExecPath ("funit-threadexit"),
		Integer.toString(Pid.get().intValue()),
		Integer.toString(SynchronizedOffspring.START_ACK.intValue()),
		Integer.toString(timeout), // Seconds
	    });

	// Find the main task, and get a terminate observer bound to
	// it; as a side effect it will pick up the terminated but not
	// yet joined, thread also part of the process.
	Task task = daemon.findTaskUsingRefresh(true);
	TerminatingCounter terminatingCounter = new TerminatingCounter();
	task.requestAddTerminatingObserver(terminatingCounter);
	assertRunUntilStop("add terminatingCounter");

	// Now terminate the main thread. Trace the processes exit all
	// the way through to being removed so that both terminating
	// and terminated events are seen by this test.
	daemon.signal(frysk.sys.Signal.TERM);
	new StopEventLoopWhenProcRemoved(daemon);
	assertRunUntilStop("terminate process");

	// Check that there was a terminate event.
	assertEquals("Number of terminating processes", 1,
		     terminatingCounter.count);
    }
}
