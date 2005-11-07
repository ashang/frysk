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
	assertEquals ("attached count (no change)", tasks.length,
		      attachedObserver.attachedCount);
	assertEquals ("deleted count", tasks.length,
		      attachedObserver.deletedCount);

	// Finally, prove that the process really is detached - send
	// it a kill and then probe (using kill) the process until
	// that fails.
	Signal.kill (tasks[0].proc.getPid (), Sig.KILL);
	Manager.eventLoop.add (new TimerEvent (0, 50)
	    {
		int pid = tasks[0].proc.getPid ();
		public void execute ()
		{
		    try {
			Signal.kill (pid, 0);
		    }
		    catch (Errno.Esrch e) {
			Manager.eventLoop.requestStop ();
		    }
		}
	    });
	assertRunUntilStop ("process gone");

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
	final Proc proc = child.findProcUsingRefresh (true); // Tasks also.
	Task task = (Task) proc.getTasks ().getFirst ();
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
	final Proc proc = child.findProcUsingRefresh (true); // Tasks also.
	Task[] tasks = (Task[])proc.getTasks ().toArray (new Task[0]);
	Task task = null;
	for (int i = 0; i < tasks.length; i++) {
	    if (tasks[i].getTid () != proc.getPid ()) {
		task = tasks[i];
		break;
	    }
	}
	assertNotNull ("found the non-main task", task);
	attachDetach (new Task[] { task });
    }

    /**
     * Check that a program with many many tasks can be attached, and detached.
     */
    public void testAttachDetachManyTasks ()
    {
	int count = 100;
	Child child = new DaemonChild (count);
	final Proc proc = child.findProcUsingRefresh (true); // Tasks also.
	Task[] tasks = (Task[])proc.getTasks ().toArray (new Task[0]);
	assertTrue ("number of tasks", count == tasks.length - 1);
	attachDetach (tasks);
    }
}
