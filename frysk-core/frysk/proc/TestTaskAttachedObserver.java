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

import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.event.TimerEvent;
import frysk.sys.Errno;
import java.util.Observer;
import java.util.Observable;

/**
 * Test the TaskObserver.Attached observer.
 */

public class TestTaskAttachedObserver
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
	public void added (Throwable w)
	{
	    super.added (null); // lie
	    assertNotNull ("FailedObserver.added arg", w);
	    Manager.eventLoop.requestStop ();
	}
    }

    /**
     * Send .theSig to .thePid, and then keep probeing .thePid until
     * it no longer exists.
     */
    private void assertTaskGone (final int thePid, final int theSig)
    {
	Manager.eventLoop.add (new TimerEvent (0, 50)
	    {
		int pid = thePid;
		int sig = theSig;
		public void execute ()
		{
		    try {
			Signal.tkill (pid, sig);
			sig = 0;
		    }
		    catch (Errno.Esrch e) {
			Manager.eventLoop.requestStop ();
		    }
		}
	    });
	assertRunUntilStop ("task gone");
    }

    /**
     * Attach to the list of tasks.
     */
    private AttachedObserver attach (final Task[] tasks)
    {
	// Add the AttachedObserver to the task causing <em>frysk</em>
	// to attach to the Task's Proc..  Run the event loop until
	// the process reports back that the attach occured.
	AttachedObserver attachedObserver = new AttachedObserver ();
	for (int i = 0; i < tasks.length; i++)
	    tasks[i].requestAddAttachedObserver (attachedObserver);
	tasks[0].proc.observableAttached.addObserver (new Observer ()
	    {
		Proc proc = tasks[0].proc;
		public void update (Observable obj, Object arg)
		{
		    proc.observableAttached.deleteObserver (this);
		    Manager.eventLoop.requestStop ();
		}
	    });
	assertRunUntilStop ("attaching to task");
	assertEquals ("attached count", tasks.length,
		      attachedObserver.attachedCount);
	assertEquals ("deleted count", 0,
		      attachedObserver.deletedCount);
	return attachedObserver;
    }

    /**
     * Detach from the list of tasks.
     */
    private void detach (final Task[] tasks, AttachedObserver attachedObserver)
    {
	// Delete the AttachedObserver from the task causing
	// <em>frysk</em> to detach from the Task's Proc.  Run the
	// event loop until the Proc reports back that it has
	// detached.
	for (int i = 0; i < tasks.length; i++)
	    tasks[i].requestDeleteAttachedObserver (attachedObserver);
	tasks[0].proc.observableDetached.addObserver (new Observer ()
	    {
		Proc proc = tasks[0].proc;
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
	assertTaskGone (tasks[0].proc.getPid (), Sig.KILL);

	// Check that while the process has gone, <em>frysk</em>
	// hasn't noticed.
	assertTrue ("process still has tasks",
		    tasks[0].proc.getTasks ().size () > 0);
    }

    /** 
     * Attach and then Detach the list of tasks.
     */
    private void attachDetach (Task[] tasks)
    {
	AttachedObserver attachedObserver = attach (tasks);
	detach (tasks, attachedObserver);
    }

    /**
     * Test that adding an Attached observer to a detached processes's
     * main task causes an attach; and removing it causes the
     * corresponding detach.
     */
    public void testAttachDetachMainTask ()
    {
	// Create a detached child.
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true); // main
	assertNotNull ("main task", task);
	attachDetach (new Task[] { task });
    }

    /**
     * Check that it is possible to force the attach, and then detach
     * of the non-main task.
     */
    public void testAttachDetachOtherTask ()
    {
	// Create a detached child.
	Child child = new DaemonChild (2);
	Task task = child.findTaskUsingRefresh (false); // non-main
	assertNotNull ("non-main task", task);
	attachDetach (new Task[] { task });
    }

    /**
     * Check that a program with many many tasks can be attached, and detached.
     */
    public void testAttachDetachManyTasks ()
    {
	int count = 20;
	Child child = new DaemonChild (count);
	Proc proc = child.findProcUsingRefresh (true); // Tasks also.
	Task[] tasks = (Task[])proc.getTasks ().toArray (new Task[0]);
	assertTrue ("number of tasks", count == tasks.length - 1);
	attachDetach (tasks);
    }

    /**
     * Check that detaching from a task that has already started to
     * exit works.
     */
    public void testDetachExitingMainTask ()
    {
	// Create a detached child.
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);
 	Task[] tasks = new Task[] { task };

	// Attach to it.
	AttachedObserver attachedObserver = attach (tasks);

	// Now blow away the task.  Since the event queue is stopped
	// this signal will be delivered to the inferior first but
	// won't be processed until after .detach has started
	// detaching the task.  This results in the task in the
	// detaching state getting the terminating event instead of
	// the more typical stopped.
	Signal.kill (child.getPid (), Sig.KILL);

	detach (tasks, attachedObserver);
    }

    /**
     * Check that attaching to a dead main task fails.
     */
    public void testAttachDeadMainTask ()
    {
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	
	// Blow away the task; make certain that the Proc's task list
	// is refreshed so that the task is no longer present.
	assertTaskGone (task.getTid (), Sig.KILL);
	task.proc.sendRefresh ();
	assertEquals ("task count", 0, task.proc.getTasks ().size ());

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }

    /**
     * Check that attaching to a dead non-main task fails.
     */
    public void testAttachDeadOtherTask ()
    {
	DaemonChild child = new DaemonChild (1);
	Task task = child.findTaskUsingRefresh (false);
	
	// Blow away the task; make certain that the Proc's task list
	// is refreshed so that the task is no longer present.
	child.delTask (); // must be the other task.
	assertTaskGone (task.getTid (), 0);
	task.proc.sendRefresh ();
	assertEquals ("task count (down from two)", 1,
		      task.proc.getTasks ().size ());

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }

    /**
     * Check that attaching to a dieing task fails.
     */
    public void testAttachDieingMainTask ()
    {
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	
	// Blow away the task.
	assertTaskGone (task.getTid (), Sig.KILL);

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }

    /**
     * Check that attaching to a dieing non-main task fails.
     */
    public void testAttachDieingOtherTask ()
    {
	DaemonChild child = new DaemonChild (1);
	Task task = child.findTaskUsingRefresh (false);
	
	// Blow away the task.
	child.delTask ();
	assertTaskGone (task.getTid (), 0);

	// Try to add the observer to the now defunct task.  Should
	// successfully fail.
	FailedObserver failedObserver = new FailedObserver ();
	task.requestAddAttachedObserver (failedObserver);
	assertRunUntilStop ("fail to add observer");
	assertEquals ("added count", 1, failedObserver.addedCount);
    }

    /**
     * Check that an attach to an attached observer works.
     */
    public void testAttachToAttached ()
    {
	final Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);
	attach (new Task[] { task });
	class Terminate
	    extends TaskObserverBase
	    implements TaskObserver.Terminating
	{
	    Child c = child;
	    public void added (Throwable w)
	    {
		assertNull ("added arg", w);
		c.signal (Sig.TERM);
	    }
	    public Action updateTerminating (Task task, boolean signal,
					     int val)
	    {
		assertTrue ("signal", signal);
		assertEquals ("val", Sig.TERM, val);
		Manager.eventLoop.requestStop ();
		return Action.CONTINUE;
	    }
	}
	Terminate terminate = new Terminate ();
	task.requestAddTerminatingObserver (terminate);
	assertRunUntilStop ("terminated");
    }

    /**
     * Check that back-to-back add/add, delete/delete observers.
     */
    public void testBackToBackAttachAttachMainTask ()
    {
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);

	// .attach does an add, add a few more.
	AttachedObserver extra = new AttachedObserver ();
	task.requestAddAttachedObserver (extra);
	AttachedObserver attached = attach (new Task[] { task });
	assertEquals ("extra attached count", 1, extra.attachedCount);
	
	// .detach does a few deletes, delete a few more.
	task.requestDeleteAttachedObserver (extra);
	detach (new Task[] { task }, attached);
    }

    /**
     * Check back-to-back add/delete observers.
     */
    public void testBackToBackAttachDetachMainTask ()
    {
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);

	// pull an observer out from under the tasks feet.
	AttachedObserver extra = new AttachedObserver ()
	    {
		public void added (Throwable w)
		{
		    super.added (null);
		    assertNotNull ("added arg", w);
		}
	    };
	task.requestAddAttachedObserver (extra);
	task.requestDeleteAttachedObserver (extra);
	AttachedObserver attached = attach (new Task[] { task });
	
	// .detach does a few deletes, delete a few more.
	detach (new Task[] { task }, attached);
    }

    /**
     * Check that that an instantly canceled attach doesn't.
     */
    public void testDeletedAttach ()
    {
	Child child = new DaemonChild ();
	Task task = child.findTaskUsingRefresh (true);
	assertNotNull ("main task", task);

	// .attach does an add, add a few more.
	AttachedObserver extra = new AttachedObserver ()
	    {
		public void added (Throwable w)
		{
		    super.added (null);
		    assertNotNull ("added arg", w);
		    deletedCount++; // a lie
		}
		public void deleted ()
		{
		    fail ("deleted");
		}
	    };
	task.requestAddAttachedObserver (extra);
	detach (new Task[] { task }, extra);
    }
}
