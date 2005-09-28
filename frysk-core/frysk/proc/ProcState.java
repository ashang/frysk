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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * A UNIX Process State
 *
 */

abstract class ProcState
    extends State
{
    protected ProcState (String state)
    {
	super (state);
    }
    boolean isStopped ()
    {
	return false;
    }
    ProcState stop (Proc proc)
    {
	throw new RuntimeException (proc + " " + this + " unhandled stop");
    }
    ProcState go (Proc proc)
    {
	throw new RuntimeException (proc + " " + this + " unhandled go");
    }
    ProcState process (Proc proc, ProcEvent.AllStopped event)
    {
	throw unhandled (proc, event);
    }
    ProcState process (Proc proc, ProcEvent.TaskCloned event)
    {
	throw unhandled (proc, event);
    }
    ProcState process (Proc proc, ProcEvent.TaskForked event)
    {
	throw unhandled (proc, event);
    }
    ProcState process (Proc proc, ProcEvent.TaskAttached event)
    {
	throw unhandled (proc, event);
    }
    ProcState process (Proc proc, ProcEvent.TaskDetached event)
    {
	throw unhandled (proc, event);
    }
    ProcState processRequestRemoval (Proc proc)
    {
	throw unhandled (proc, "RequestRemoval");
    }
    ProcState processRequestAttachedStop (Proc proc)
    {
	throw unhandled (proc, "RequestAttachedStop");
    }
    ProcState processRequestAttachedContinue (Proc proc)
    {
	throw unhandled (proc, "RequestAttachedContinue");
    }
    ProcState processRequestDetachedContinue (Proc proc)
    {
	throw unhandled (proc, "RequestDetachedContinue");
    }
    ProcState processRequestRefresh (Proc proc)
    {
	throw unhandled (proc, "RequestRefresh");
    }

    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    static ProcState unattached = new ProcState ("unattached")
	{
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		// XXX: Instead of doing a full refresh, should
		// instead just pull out (with a local refresh) the
		// main task.
		proc.sendRefresh ();
		// Grab the main task and attach to that.
		Task task = Manager.host.get (new TaskId (proc.getPid ()));
		task.requestAttach ();
		return new AttachingToMainTask ();
	    }
	    ProcState processRequestRefresh (Proc proc)
	    {
		proc.sendRefresh ();
		return unattached;
	    }
	    ProcState processRequestRemoval (Proc proc)
	    {
		if (proc.taskPool.size () > 0) {
		    throw new RuntimeException ("XXX: What about a dieing proc's tasks, have a dieing state and force a proc refresh?");
		}
		else {
		    if (proc.parent != null)
			proc.parent.remove (proc);
		    return destroyed;
		}
	    }
	};

    /**
     * In the process of attaching, the main task has been sent an
     * attach request, waiting for it to finish.
     */
    static class AttachingToMainTask
	extends ProcState
    {
	AttachingToMainTask ()
	{
	    super ("AttachingWaitingForMainTask");
	}
	ProcState process (Proc proc, ProcEvent.TaskAttached event)
	{
	    // Get an up-to-date list of all tasks.  Now that the main
	    // task has stopped, all other tasks should be frozen.
	    proc.sendRefresh ();
	    if (proc.taskPool.size () == 1) {
		event.task.requestContinue ();
		proc.observableAttachedContinue.notify (proc);
		return running;
	    }
	    else {
		// Track the number of un-attached tasks using a local
		// map.  As each task reports that its been attached
		// the map is srunk.  Remove the main task as that has
		// already been attached.
		Map unattachedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		unattachedTasks.remove (new TaskId (proc.getPid ()));
		// Ask the other tasks to attach.
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    if (t.getPid () == proc.getPid ())
			continue;
		    t.requestAttach ();
		}
		return new AttachingToOtherTasks (proc, unattachedTasks);
	    }
	}
    }

    /**
     * In the process of attaching, the main task is attached, now
     * waiting for the remaining tasks to indicate they, too, have
     * attached (or at least processed the attach request).
     */
    class AttachingToOtherTasks
	extends ProcState
    {
	private Map unattachedTasks;
	AttachingToOtherTasks (Proc proc, Map unattachedTasks)
	{
	    super ("AttachingWaitingForOtherTasks");
	    this.unattachedTasks = unattachedTasks;
	}
	ProcState process (Proc proc, ProcEvent.TaskAttached event)
	{
	    // As each task reports that it has been attached, remove
	    // it from the pool, wait until there are none left.
	    unattachedTasks.remove (event.task.id);
	    if (unattachedTasks.size () > 0)
		return this;
	    // All attached, let them go, and mark this as
	    // attached/running.
	    for (Iterator i = proc.taskPool.values().iterator ();
		 i.hasNext (); ) {
		Task t = (Task) i.next ();
		t.requestContinue ();
	    }
	    proc.observableAttachedContinue.notify (proc);
	    return running;
	}
    }

    /**
     * In the process of detaching; waiting for all tasks to report
     * back that they have successfully detached.
     */
    static class DetachingAllTasks
	extends ProcState
    {
	private Map attachedTasks;
	DetachingAllTasks (Map attachedTasks)
	{
	    super ("DetachingAllTasks");
	    this.attachedTasks = attachedTasks;
	}
	ProcState process (Proc proc, ProcEvent.TaskDetached event)
	{
	    // As each task reports that it has detached, remove it
	    // from the list.  Once there are none left the detach is
	    // done.
	    attachedTasks.remove (event.task.id);
	    if (attachedTasks.size () > 0)
		return this;
	    // All done, notify.
	    proc.observableDetachedContinue.notify (proc);
	    return unattached;
	}
    }

    /**
     * The process has been destroyed.
     */
    static ProcState destroyed = new ProcState ("destroyed")
	{
	};

    static ProcState running = new ProcState ("running")
	{
	    ProcState stop (Proc proc)
	    {
		// Check if we are already stopped.
		Collection allTasks = proc.taskPool.values();
		Iterator i = allTasks.iterator ();
		boolean allStopped = true;
		while (i.hasNext ()) {
		    Task t = (Task)i.next ();
		    if (!t.isStopped ()) {
			allStopped = false;
			break;
		    }
		}
		if (!allStopped) {
		    // Must stop tasks that are not yet stopped
		    proc.stopAllTasks ();
		    return stopping;
		}
		// Otherwise we are stopped as expected, notify observers
		ProcEvent.AllStopped event = new ProcEvent.AllStopped (proc);
		event.execute ();
		// XXX: ???
		return running;
	    }
	    ProcState go (Proc proc)
	    {
		proc.startAllTasks ();
		return running;
	    }
	    ProcState process (Proc proc, ProcEvent.AllStopped event)
	    {
		return proc.state;
	    }
	    ProcState process (Proc proc, ProcEvent.TaskCloned event)
	    {
		proc.newTask (event.getCloneId (), true);
		// The clone has already been added to the tree.
		return running;
	    }
	    ProcState process (Proc proc, ProcEvent.TaskForked event)
	    {
		proc.sendNewAttachedChild (event.getForkId ());
		// The process has already been added to the tree.
		return running;
	    }
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		Map attachedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.requestDetach ();
		}
		return new DetachingAllTasks (attachedTasks);
	    }
	};

    static ProcState stopping = new ProcState ("stopping")
	{
	    ProcState stop (Proc proc)
	    {
		return stopping;
	    }
	    ProcState go (Proc proc)
	    {
		proc.startAllTasks ();
		return running;
	    }
	    ProcState process (Proc proc, ProcEvent.AllStopped event)
	    {
		proc.allStopped.notify (event);
		// Only change state if observer did not restart process
		if (proc.state == stopping)
		    return stopped;
		return proc.state;
	    }
	};

    static ProcState stopped = new ProcState ("stopped")
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    ProcState process (Proc proc, ProcEvent.AllStopped event)
	    {
		proc.allStopped.notify (event);
		return stopped;
	    }
	    ProcState stop (Proc proc)
	    {
		// Check if we are already stopped.
		Collection allTasks = proc.taskPool.values();
		Iterator i = allTasks.iterator ();
		boolean allStopped = true;
		while (i.hasNext ()) {
		    Task t = (Task)i.next ();
		    if (!t.isStopped ()) {
			allStopped = false;
			break;
		    }
		}
		if (!allStopped) {
		    // Must stop tasks that are not yet stopped
		    proc.stopAllTasks ();
		    return stopping;
		}
		// Otherwise we are stopped as expected, notify observers
		ProcEvent.AllStopped event = new ProcEvent.AllStopped (proc);
		event.execute ();
		// XXX: ???
		return running;
	    }
	};
}
