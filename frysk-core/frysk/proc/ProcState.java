// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

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
    ProcState processRequestRemoval (Proc proc)
    {
	throw unhandled (proc);
    }
    ProcState processRequestAttachedStop (Proc proc)
    {
	throw unhandled (proc);
    }
    ProcState processRequestAttachedContinue (Proc proc)
    {
	throw unhandled (proc);
    }
    ProcState processRequestDetachedContinue (Proc proc)
    {
	throw unhandled (proc);
    }
    ProcState processRequestRefresh (Proc proc)
    {
	throw unhandled (proc);
    }

    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    static ProcState unattached = new ProcState ("unattached")
	{
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		return new AttachingToMainTask (proc);
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
	AttachingToMainTask (Proc proc)
	{
	    super ("AttachingWaitingForMainTask");
	    // XXX: Instead of doing a full refresh, should instead
	    // just pull out (with a local refresh) the main task.
	    proc.sendRefresh ();
	    // Grab the main task and attach to that.
	    Task task = Manager.host.get (new TaskId (proc.getPid ()));
	    task.requestAttach ();
	}
	ProcState process (Proc proc, ProcEvent.TaskAttached event)
	{
	    // Get an up-to-date list of all tasks.  Now that the main
	    // task has stopped, all other tasks should be frozen.
	    proc.sendRefresh ();
	    if (proc.taskPool.size () == 1) {
		event.task.requestContinue ();
		return running;
	    }
	    else
		return new AttachingToOtherTasks (proc, event);
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
	Map unattachedTasks;
	AttachingToOtherTasks (Proc proc, ProcEvent.TaskAttached event)
	{
	    super ("AttachingWaitingForOtherTasks");
	    // Track the number of un-attached tasks using a local
	    // map.  As each task reports that its been attached the
	    // map is srunk.  Remove the main task as that has already
	    // been attached.
	    unattachedTasks = (Map) (((HashMap)proc.taskPool).clone ());
	    unattachedTasks.remove (new TaskId (proc.getPid ()));
	    // Ask the other tasks to attach.
	    for (Iterator i = proc.taskPool.values ().iterator ();
		 i.hasNext (); ) {
		Task t = (Task) i.next ();
		if (t.getPid () == proc.getPid ())
		    continue;
		t.requestAttach ();
	    }
	}
	ProcState process (Proc proc, ProcEvent.TaskAttached event)
	{
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
	    return running;
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
