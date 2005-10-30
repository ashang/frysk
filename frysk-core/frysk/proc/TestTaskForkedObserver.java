// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

/**
 * Check that the observer TaskObserver.Forked works.
 */

public class TestTaskForkedObserver
    extends TestLib
{
    static int n = 10;

    /**
     * Test that the fork count from a sub-program that, in turn,
     * creates lots and lots of sub-processes matches the expected.
     */
    public void testTaskForkedObserver ()
    {
	ProcCounter procCounter = new ProcCounter ();
	new StopEventLoopWhenChildProcRemoved ();

	// Watch for any Task fork events, accumulating them as they
	// arrive.
	class ForkObserver
	    extends AutoAddTaskObserverBase
	    implements TaskObserver.Forked
	{
	    int count;
	    public Action updateForked (Task task, Proc proc)
	    {
		count++;
		return Action.CONTINUE;
	    }
	    void updateTaskAdded (Task task)
	    {
		task.requestAddForkedObserver (this);
	    }
	}
	ForkObserver forkObserver = new ForkObserver ();

	// Run a program that forks wildly.
	Manager.host.requestCreateAttachedContinuedProc
	    (null, "/dev/null", null, new String[] {
		"./prog/fib/fork",
		Integer.toString (n)
	    });

	assertRunUntilStop ("run \"fork\" until exit");

	Fibonacci fib = new Fibonacci (n);

	assertEquals ("number of child processes created",
		      fib.callCount, procCounter.getAdjustedNumberAdded ());
	assertEquals ("number of child processes destroyed",
		      fib.callCount, procCounter.getAdjustedNumberRemoved ());
	assertEquals ("number of times fork observer added",
		      fib.callCount, forkObserver.addedCount);
	assertEquals ("number of forks (one less than number of processes)",
		      fib.callCount - 1, forkObserver.count);
    }

    /**
     * Test that the fork count from a sub-program that, in turn,
     * creates lots and lots of sub-processes matches the expected;
     * block each task for a short while.
     */
    public void testBlockingTaskForkedObserver ()
    {
	// An object that, when the child process exits, both sets a
	// flag to record that event, and requests that the event loop
	// stop.
	StopEventLoopWhenChildProcRemoved childRemoved
	    = new StopEventLoopWhenChildProcRemoved ();

	ProcCounter procCounter = new ProcCounter ();

	// Watch for any Task fork events, accumulating them as they
	// arrive.
	class ForkStopper
	    extends AutoAddTaskObserverBase
	    implements TaskObserver.Forked
	{
	    int count;
	    TaskSet forkedTasks = new TaskSet ();
	    public Action updateForked (Task task, Proc proc)
	    {
		count++;
		forkedTasks.add (task);
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	    void updateTaskAdded (Task task)
	    {
		task.requestAddForkedObserver (this);
	    }
	}
	ForkStopper forkStopper = new ForkStopper ();

	// Run a program that forks wildly.
	Manager.host.requestCreateAttachedContinuedProc
	    (null, "/dev/null", null, new String[] {
		"./prog/fib/fork",
		Integer.toString (n)
	    });

	Fibonacci fib = new Fibonacci (n);

	// Repeatedly run the event loop until the child exits (every
	// time there is a fork the event loop will stop).
	int forkCount = 0;
	int loopCount = 0;
	while (loopCount <= fib.callCount && !childRemoved.p) {
	    loopCount++;
	    assertRunUntilStop ("run \"fork\" until stop, number "
				+ forkCount + " of " + fib.callCount);
	    forkCount += forkStopper.forkedTasks.size ();
	    forkStopper.forkedTasks.unblock (forkStopper);
	    forkStopper.forkedTasks.clear ();
	}

	assertEquals ("number of child processes created",
		      fib.callCount, procCounter.getAdjustedNumberAdded ());
	assertEquals ("number of child processes destroyed",
		      fib.callCount, procCounter.getAdjustedNumberRemoved ());
	assertEquals ("number of times fork observer added",
		      fib.callCount, forkStopper.addedCount);
	assertEquals ("number of forks (one less than number of processes)",
		      fib.callCount - 1, forkStopper.count);
    }
}
