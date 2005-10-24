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
 * Check that clone (task create and delete) events are detected.
 */

public class TestTaskClonedObserver
    extends TestLib
{
    static final int fibCount = 10;

    /**
     * Test that Task.requestAddObserver (Clone) can be used to track
     * all clones by a child process.
     *
     * This creates a program that, in turn, creates lots and lots of
     * tasks.  It then checks that the number of task create and
     * delete events matches the expected.
     */
    public void testTaskCloneObserver ()
    {
	new StopEventLoopWhenChildProcRemoved ();
	class CloneCounter
	    extends TaskObserverBase
	    implements TaskObserver.Cloned
	{
	    int count;
	    public Action updateCloned (Task task, Task clone)
	    {
		count++;
		return Action.CONTINUE;
	    }
	}
	CloneCounter cloneCounter = new CloneCounter ();
	new AddTaskObserver (cloneCounter);

	Manager.host.requestCreateAttachedContinuedProc
	    (null, "/dev/null", null, new String[] {
		"./prog/fib/clone",
		Integer.toString (fibCount)
	    });
	
	assertRunUntilStop ("run \"clone\" to exit");

 	Fibonacci fib = new Fibonacci (fibCount);
	// The first task, included in fib.callCount isn't included in
	// the clone count.
	assertEquals ("Number of clones", fib.callCount - 1,
		      cloneCounter.count);
    }


    /**
     * Test that Task.requestAddObserver (Clone) can be used to hold a
     * Tasks at the clone point.
     *
     * This creates a program that, in turn, creates lots and lots of
     * tasks.  It then checks that the number of task create and
     * delete events matches the expected.
     */
    public void testBlockingTaskCloneObserver ()
    {
	// An object that, when the child process exits, both sets a
	// flag to record that event, and requests that the event loop
	// stop.
	StopEventLoopWhenChildProcRemoved childRemoved
	    = new StopEventLoopWhenChildProcRemoved ();

	// An object that, for every Task that clones, puts the
	// cloning task into a blocked state, and requests that the
	// event loop stop.  The blocked tasks are accumulated in
	// .blockedTasks.  The blocked Tasks can be unblocked using the
	// .unblockTasks method.
	class CloneStopper
	    extends TaskObserverBase
	    implements TaskObserver.Cloned
	{
	    public Action updateCloned (Task task, Task clone)
	    {
		addTask (task);
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	}
	CloneStopper cloneStopper = new CloneStopper ();
	new AddTaskObserver (cloneStopper);

	// Compute the expected number of tasks (this includes the
	// main task).
 	Fibonacci fib = new Fibonacci (fibCount);

	Manager.host.requestCreateAttachedContinuedProc
	    (null, "/dev/null", null, new String[] {
		"./prog/fib/clone",
		Integer.toString (fibCount)
	    });
	
	// Repeatedly run the event loop until the child exits (every
	// time there is a clone the event loop will stop).
	int cloneCount = 0;
	int loopCount = 0;
	while (loopCount <= fib.callCount && !childRemoved.p) {
	    loopCount++;
	    assertRunUntilStop ("run \"clone\" until stop, number "
				+ cloneCount + " of " + fib.callCount);
	    cloneCount += cloneStopper.countTasks ();
	    cloneStopper.unblockTasks ();
	    cloneStopper.clearTasks ();
	}

	// The first task, included in fib.callCount isn't included in
	// the clone count.
	assertEquals ("number of times cloneObserver added",
		      fib.callCount, cloneStopper.addedCount);
	assertEquals ("number of times cloneObserver deleted",
		      0, cloneStopper.deletedCount);
	assertEquals ("Number of clones", fib.callCount - 1, cloneCount);
	assertTrue ("Child exited", childRemoved.p);
	assertTrue ("At least two iterations of the clone loop",
		    loopCount > 2);
    }
}
