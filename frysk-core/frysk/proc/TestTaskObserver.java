// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.util.Observer;
import java.util.Observable;
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Errno;
import frysk.event.TimerEvent;

/**
 * Generic observer tests - that the framework functions ok.
 */

public class TestTaskObserver
    extends TestLib
{
    /*
     * Create an observer that records when it is attached or
     * detached.
     */
    class AttachedObserver
	extends TaskObserverBase
	implements TaskObserver.Attached
    {
	int attachedCount;
	public Action updateAttached (Task task)
	{
	    attachedCount++;
	    return Action.CONTINUE;
	}
    }


    /**
     * An observer that is ok with a failed add attempt.
     */
    class FailedObserver
	extends AttachedObserver
    {
	public void addedTo (Object o)
	{
	    fail ("addedTo");
	}
	public void addFailed (Object o, Throwable w)
	{
	    super.addedTo (null); // lie
	    Manager.eventLoop.requestStop ();
	}
    }

    /**
     * Send .theSig to .thePid, and then keep probeing .thePid until
     * it no longer exists.
     */
    private void assertTaskGone (final int thePid, final Sig theSig)
    {
	Manager.eventLoop.add (new TimerEvent (0, 50)
	    {
		int pid = thePid;
		Sig sig = theSig;
		public void execute ()
		{
		    try {
			Signal.tkill (pid, sig);
			sig = Sig.NONE;
		    }
		    catch (Errno.Esrch e) {
			Manager.eventLoop.requestStop ();
		    }
		}
	    });
	assertRunUntilStop ("task gone");
    }

    /**
     * Attach the specified observer to the list of tasks.
     */
    private void attach (final Task[] tasks, AttachedObserver attachedObserver)
    {
	// Add the AttachedObserver to the task causing <em>frysk</em>
	// to attach to the Task's Proc..  Run the event loop until
	// the process reports back that the attach occurred.
	for (int i = 0; i < tasks.length; i++)
	    tasks[i].requestAddAttachedObserver (attachedObserver);
	tasks[0].getProc().observableAttached.addObserver (new Observer ()
	    {
		Proc proc = tasks[0].getProc();
		public void update (Observable obj, Object arg)
		{
		    proc.observableAttached.deleteObserver (this);
		    Manager.eventLoop.requestStop ();
		}
	    });
	assertRunUntilStop ("attaching to task");
    }
    /**
     * Attach AttachedObserver to the list of tasks.
     */
    private AttachedObserver attach (Task[] tasks)
    {
	AttachedObserver attachedObserver = new AttachedObserver ();
	attach (tasks, attachedObserver);
	assertEquals ("attached count", tasks.length,
		      attachedObserver.attachedCount);
	assertEquals ("deleted count", 0,
		      attachedObserver.deletedCount);
	return attachedObserver;
    }

    /**
     * Detach from the list of tasks.
     */
    private void detach (final Task[] tasks,
			 AttachedObserver attachedObserver,
			 boolean shouldStillHaveTasks)
    {
	// Delete the AttachedObserver from the task causing
	// <em>frysk</em> to detach from the Task's Proc.  Run the
	// event loop until the Proc reports back that it has
	// detached.
	for (int i = 0; i < tasks.length; i++)
	    tasks[i].requestDeleteAttachedObserver (attachedObserver);
	tasks[0].getProc().observableDetached.addObserver (new Observer ()
	    {
		Proc proc = tasks[0].getProc();
		public void update (Observable obj, Object arg)
		{
		    proc.observableAttached.deleteObserver (this);
		    Manager.eventLoop.requestStop ();
		}
	    });
	assertRunUntilStop ("detaching from task");
	assertEquals ("deleted count", tasks.length,
		      attachedObserver.deletedCount);

	// Finally, prove that the process really is detached - send
	// it a kill and then probe (using kill) the process until
	// that fails.
	assertTaskGone (tasks[0].getProc().getPid (), Sig.KILL);

	// Check that while the process has gone, <em>frysk</em>
	// hasn't noticed.
	if (shouldStillHaveTasks)
	    assertTrue ("process still has tasks",
			tasks[0].getProc().getTasks ().size () > 0);
    }

    /** 
     * Attach and then Detach the list of tasks.
     */
    private void attachDetach (Task[] tasks)
    {
	AttachedObserver attachedObserver = attach (tasks);
	detach (tasks, attachedObserver, true);
    }


    /**
     * Test that adding an observer to a detached processes causes an
     * attach; and removing it causes the corresponding detach.
     */
    public void attachDetachTask (int count, boolean main)
    {
	// Create a detached child.
	Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("task", task);
	attachDetach (new Task[] { task });
     }
    /** {@link #attachDetachTask} */
    public void testAttachDetachMainTask ()
    {
	attachDetachTask (0, true);
    }
    /** {@link #attachDetachTask} */
    public void testAttachDetachOtherTask ()
    {
	attachDetachTask (1, false);
    }
    /** {@link #attachDetachTask} */
    public void testAttachDetachManyTasks ()
    {
	attachDetachTask (20, true);
    }

    /**
     * Check that detaching from a task that has already started to
     * exit works.
     */
    public void detachExitingTask (int count, boolean main)
    {
	// Create a detached child.
	Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("task", task);

	// Attach to it.
	AttachedObserver attachedObserver = attach (new Task[] { task });

	// Now blow away the task.  Since the event queue is stopped
	// this signal will be delivered to the inferior first but
	// won't be processed until after .detach has started
	// detaching the task.  This results in the task in the
	// detaching state getting the terminating event instead of
	// the more typical stopped.
	Signal.kill (task.getTid (), Sig.KILL);

	detach (new Task[] { task }, attachedObserver, false);
    }
    /** {@link #detachExitingTask} */
    public void testDetachExitingMainTask ()
    {
	if (unresolvedOnUtrace(3737))
	    return;
	detachExitingTask (0, true);
    }
    /** {@link #detachExitingTask} */
    public void testDetachExitingOtherTask ()
    {
	if (unresolvedOnUtrace(3737))
	    return;
	detachExitingTask (1, false);
    }

    /**
     * Check that attaching to a dead main task fails.
     */
    public void attachDeadTask (int count, boolean main)
    {
	Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("task", task);
	
	// Blow away the task; make certain that the Proc's task list
	// is refreshed so that the task is no longer present.
	assertTaskGone (task.getTid (), Sig.KILL);
	task.getProc().sendRefresh ();
	assertEquals ("task count", 0, task.getProc().getTasks ().size ());

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }
    /** {@link #attachDeadTask} */
    public void testAttachDeadMainTask ()
    {
	attachDeadTask (0, true);
    }
    /** {@link #attachDeadTask} */
    public void testAttachDeadOtherTask ()
    {
	attachDeadTask (1, false);
    }

    /**
     * Check that attaching to a dieing task fails.
     */
    public void attachDieingTask (int count, boolean main)
    {
	AckProcess child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	
	// Blow away the task.
	if (main)
	    assertTaskGone (task.getTid (), Sig.KILL);
	else {
	    child.assertSendDelCloneWaitForAcks ();
	    assertTaskGone (task.getTid (), Sig.NONE);
	}	    

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }
    /** {@link #attachDieingTask} */
    public void testAttachDieingMainTask ()
    {
	attachDieingTask (0, true);
    }
    /** {@link #attachDieingTask} */
    public void testAttachDieingOtherTask ()
    {
	attachDieingTask (1, false);
    }

    /**
     * Check that an attach to an attached observer works.
     */
    public void attachToAttachedTask (int count, boolean main)
    {
	final Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("task", task);
	attach (new Task[] { task });
	class Terminate
	    extends TaskObserverBase
	    implements TaskObserver.Terminating
	{
	    Child c = child;
	    public void addedTo (Object o)
	    {
		c.signal (Sig.TERM);
	    }
	    public Action updateTerminating (Task task, boolean signal,
					     int val)
	    {
		assertTrue ("signal", signal);
		assertEquals ("val", Sig.TERM_, val);
		Manager.eventLoop.requestStop ();
		return Action.CONTINUE;
	    }
	}
	Terminate terminate = new Terminate ();
	task.requestAddTerminatingObserver (terminate);
	assertRunUntilStop ("terminated");
    }
    /** {@link #attachToAttachedTask} */
    public void testAttachToAttachedMainTask ()
    {
	attachToAttachedTask (0, true);
    }
    /** {@link #attachToAttachedTask} */
    public void testAttachToAttachedOtherTask ()
    {
	attachToAttachedTask (1, false);
    }

    /**
     * Check that back-to-back add/add, delete/delete observers.
     */
    public void backToBackAttachAttachTask (int count, boolean main)
    {
	Child child = new AckDaemonProcess ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);

	// .attach does an add, add a few more.
	AttachedObserver extra = new AttachedObserver ();
	task.requestAddAttachedObserver (extra);
	AttachedObserver attached = attach (new Task[] { task });
	assertEquals ("extra attached count", 1, extra.attachedCount);
	
	// .detach does a few deletes, delete a few more.
	task.requestDeleteAttachedObserver (extra);
	detach (new Task[] { task }, attached, true);
    }
    /** {@link #backToBackAttachAttachTask} */
    public void testBackToBackAttachAttachMainTask ()
    {
	backToBackAttachAttachTask (0, true);	
    }
    /** {@link #backToBackAttachAttachTask} */
    public void testBackToBackAttachAttachOtherTask ()
    {
	backToBackAttachAttachTask (1, false);
    }

    /**
     * Check back-to-back add/delete observers.
     */
    public void backToBackAttachDetachTask (int count, boolean main)
    {
	Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("main task", task);

	// pull an observer out from under the tasks feet.
	AttachedObserver extra = new AttachedObserver ()
	    {
		public void addedTo (Object o)
		{
		    fail ("addedTo");
		}
		public void addFailed (Object o, Throwable w)
		{
		    super.addedTo (null); // A lie.
		}
	    };
	task.requestAddAttachedObserver (extra);
	task.requestDeleteAttachedObserver (extra);
	AttachedObserver attached = attach (new Task[] { task });
	
	// .detach does a few deletes, delete a few more.
	detach (new Task[] { task }, attached, true);
    }
    /** {@link #backToBackAttachDetachTask} */
    public void testBackToBackAttachDetachMainTask ()
    {
	backToBackAttachDetachTask (0, true);
    }
    /** {@link #backToBackAttachDetachTask} */
    public void testBackToBackAttachDetachOtherTask ()
    {
	backToBackAttachDetachTask (1, false);
    }

    /**
     * Check that that an instantly canceled attach doesn't.
     */
    public void deletedAttachTask (int count, boolean main)
    {
	Child child = new AckDaemonProcess (count);
	Task task = child.findTaskUsingRefresh (main);
	assertNotNull ("main task", task);

	// .attach does an add, add a few more.
	AttachedObserver extra = new AttachedObserver ()
	    {
		public void addedTo (Object o)
		{
		    fail ("addedTo");
		}
		public void addFailed (Object o, Throwable w)
		{
		    super.addedTo (o); // A lie.
		    deletedCount++; // A bigger lie.
		}
		public void deletedFrom (Object o)
		{
		    fail ("deletedFrom");
		}
	    };
	task.requestAddAttachedObserver (extra);
	detach (new Task[] { task }, extra, true);
    }
    /** {@link #deletedAttachTask} */
    public void testDeletedAttachMainTask ()
    {
	deletedAttachTask (0, true);
    }
    /** {@link #deletedAttachTask} */
    public void testDeletedAttachOtherTask ()
    {
	deletedAttachTask (1, false);
    }

    /**
     * Check that removing a non-existant observer doesn't cause a
     * panic.
     */
    private void deleteUnattachedObserver (Child child, boolean main)
    {
	TaskObserver.Attached unattachedObserver = new TaskObserver.Attached ()
	    {
		public Action updateAttached (Task task)
		{
		    fail ("updateAttached called with " + task);
		    return null;
		}
		public void addedTo (Object task)
		{
		    fail ("addedTo task called with " + task);
		}
		public void deletedFrom (Object task)
		{
		    fail ("deletedFrom task called with " + task);
		}
		public void addFailed (Object task, Throwable reason)
		{
		    Manager.eventLoop.requestStop ();
		}
	    };
	Task task = child.findTaskUsingRefresh (main);
	task.requestDeleteAttachedObserver (unattachedObserver);
	assertRunUntilStop ("delete failing");
    }	    
    /** {@link #deleteUnattachedObserver} */
    public void testDeleteUnattachedFromAttachedMain ()
    {
	Child child = new AttachedAckProcess ();
	deleteUnattachedObserver (child, true);
    }
    /** {@link #deleteUnattachedObserver} */
    public void testDeleteUnattachedFromDetachedMain ()
    {
	Child child = new AckDaemonProcess ();
	deleteUnattachedObserver (child, true);
    }
}
