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

import java.util.Set;
import java.util.HashSet;
import java.util.Observer;
import java.util.Observable;
import java.util.Iterator;

/**
 * Check that clone (task create and delete) events are detected.
 *
 * This creates a program that, in turn, creates lots and lots of
 * tasks.  It then checks that the number of task create and delete
 * events matches the expected.
 */

public class TestTaskClonedObserver
    extends TestLib
{
    static final int fibCount = 10;

    /**
     * Adds the supplied TaskObserver to any of the child Proc's
     * Task's.
     */
    class AddTaskObserver
    {
	TaskObserver observer;
	AddTaskObserver (TaskObserver o)
	{
	    observer = o;
	    Manager.host.observableProcAdded.addObserver (new Observer ()
		{
		    public void update (Observable obj, Object o)
		    {
			Proc proc = (Proc) o;
			if (!isChildOfMine (proc))
			    return;
			proc.observableTaskAdded.addObserver (new Observer ()
			    {
				public void update (Observable obj, Object o)
				{
				    Task task = (Task) o;
				    task.requestAddObserver (observer);
				}
			    });
		    }
		});
	}
    }

    /**
     * Test that Task.requestAddObserver (Clone) can be used to track
     * all clones by a child process.
     */
    public void testTaskCloneObserver ()
    {
	addStopEventLoopOnChildProcRemovedObserver ();
	class CloneCounter
	    implements TaskObserver.Cloned
	{
	    int count;
	    public void added (Throwable w)
	    {
		assertNull ("addObserver ack", w);
	    }
	    public void deleted ()
	    {
	    }
	    public boolean updateCloned (Task task, Task clone)
	    {
		count++;
		return false;
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
     */
    public void testBlockingTaskCloneObserver ()
    {
	// An object that, when the child process exits, both sets a
	// flag to record that event, and requests that the event loop
	// stop.
	class ChildRemoved
	{
	    boolean p = false;
	    ChildRemoved ()
	    {
		Manager.host.observableProcRemoved.addObserver (new Observer ()
		    {
			public void update (Observable o, Object obj)
			{
			    Proc proc = (Proc) obj;
			    if (!isChildOfMine (proc))
				return;
			    // Shut things down.
			    Manager.eventLoop.requestStop ();
			    p = true;
			}
		    });
	    }
	}
	ChildRemoved childRemoved = new ChildRemoved ();

	// An object that, for every Task that clones, puts the
	// cloning task into a blocked state, and requests that the
	// event loop stop.  The blocked tasks are accumulated in
	// .blockedTasks.  The blocked Tasks can be unblocked using the
	// .unblockTasks method.
	class CloneStopper
	    implements TaskObserver.Cloned
	{
	    // Maintain a set of tasks that were recently blocked.
	    Set blockedTasks = new HashSet ();
	    // Remember if/when the clone stopper is acked.
	    int addedAcks;
	    public void added (Throwable w)
	    {
		assertNull ("addObserver ack", w);
		addedAcks++;
	    }
	    int deletedAcks;
	    public void deleted ()
	    {
		deletedAcks++;
	    }
	    public boolean updateCloned (Task task, Task clone)
	    {
		blockedTasks.add (task);
		Manager.eventLoop.requestStop ();
		return true;
	    }
	    void unblockTasks ()
	    {
		for (Iterator i = blockedTasks.iterator (); i.hasNext(); ) {
		    Task task = (Task) i.next ();
		    task.requestUnblock (this);
		}
		blockedTasks.clear ();
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
	    cloneCount += cloneStopper.blockedTasks.size ();
	    cloneStopper.unblockTasks ();
	}

	// The first task, included in fib.callCount isn't included in
	// the clone count.
	assertEquals ("number of times cloneObserver added",
		      fib.callCount, cloneStopper.addedAcks);
	assertEquals ("number of times cloneObserver deleted",
		      0, cloneStopper.deletedAcks);
	assertEquals ("Number of clones", fib.callCount - 1, cloneCount);
	assertTrue ("Child exited", childRemoved.p);
	assertTrue ("At least two iterations of the clone loop",
		    loopCount > 2);
    }
}
