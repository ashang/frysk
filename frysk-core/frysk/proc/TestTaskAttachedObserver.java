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

import java.util.Iterator;
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
    /**
     * Attach, and then detach a task.
     */
    private void attachDetach (final Task task)
    {
	// Remember the number of tasks.
	int numberTasks = task.proc.getTasks ().size ();

	// Create an observer that records when it is attached or
	// detached.
	class AttachedObserver
	    extends TaskObserverBase
	    implements TaskObserver.Attached
	{
	    static final int UNDEFINED = 0;
	    static final int ATTACHED = 1;
	    static final int DETACHED = 2;
	    int state = UNDEFINED;
	    public Action updateAttached (Task task)
	    {
		state = ATTACHED;
		return Action.CONTINUE;
	    }
	    public void deleted ()
	    {
		state = DETACHED;
	    }
	}
	AttachedObserver attachedObserver = new AttachedObserver ();

	// Add the AttachedObserver to the task causing <em>frysk</em>
	// to attach to the Task's Proc..  Run the event loop until
	// the process reports back that the attach occured.
	task.requestAddAttachedObserver (attachedObserver);
	task.proc.observableAttached.addObserver (new Observer ()
	    {
		Proc proc = task.proc;
		public void update (Observable obj, Object arg)
		{
		    proc.observableAttached.deleteObserver (this);
		    Manager.eventLoop.requestStop ();
		}
	    });
	assertRunUntilStop ("attaching to task");
	assertEquals ("attached state", AttachedObserver.ATTACHED,
		      attachedObserver.state);

	// Delete the AttachedObserver from the task causing
	// <em>frysk</em> to detach from the Task's Proc.  Run the
	// event loop until the Proc reports back that it has
	// detached.
	task.requestDeleteAttachedObserver (attachedObserver);
	task.proc.observableDetached.addObserver (new Observer ()
	    {
		Proc proc = task.proc;
		public void update (Observable obj, Object arg)
		{
		    proc.observableAttached.deleteObserver (this);
		    Manager.eventLoop.requestStop ();
		}
	    });
	assertRunUntilStop ("detaching from task");
	assertEquals ("detached", AttachedObserver.DETACHED,
		      attachedObserver.state);

	// Finally, prove that the process really is detached - send
	// it a kill and then probe (using kill) the process until
	// that fails.
	Signal.kill (task.proc.getPid (), Sig.KILL);
	Manager.eventLoop.add (new TimerEvent (0, 50)
	    {
		int pid = task.proc.getPid ();
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
	assertEquals ("process task count", numberTasks,
		      task.proc.getTasks ().size ());
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
	attachDetach (task);
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
	Task task = null;
	for (Iterator i = proc.getTasks ().iterator (); i.hasNext (); ) {
	    task = (Task)i.next ();
	    if (task.getTid () != proc.getPid ())
		break;
	}
	assertNotNull ("found the non-main task", task);
	attachDetach (task);
    }
}
