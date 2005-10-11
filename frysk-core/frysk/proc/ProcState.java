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
import java.util.Map;
import java.util.HashMap;

/**
 * A UNIX Process State
 *
 */

abstract class ProcState
    extends State
{
    /**
     * Return the Proc's initial state.
     */
    static ProcState initial (Proc proc, boolean attached, boolean running)
    {
	if (attached) {
	    if (running)
		return startRunning;
	    else
		return startStopped;
	}
	else {
	    return unattached;
	}
    }

    protected ProcState (String state)
    {
	super (state);
    }
    boolean isStopped ()
    {
	return false;
    }
    ProcState processPerformRemoval (Proc proc)
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
    ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "PerformTaskAttachCompleted");
    }
    ProcState processPerformTaskDetachCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "PerformTaskDetachCompleted");
    }
    ProcState processPerformTaskStopCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "PerformTaskStopCompleted");
    }
    ProcState processPerformTaskContinueCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "PerformTaskContinueCompleted");
    }

    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    private static ProcState unattached = new ProcState ("unattached")
	{
	    private ProcState processRequestAttach (Proc proc, boolean stop)
	    {
		// XXX: Instead of doing a full refresh, should
		// instead just pull out (with a local refresh) the
		// main task.
		proc.sendRefresh ();
		// Grab the main task and attach to that.
		Task task = Manager.host.get (new TaskId (proc.getPid ()));
		task.performAttach ();
		return new AttachingToMainTask (stop);
	    }
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		return processRequestAttach (proc, false);
	    }
	    ProcState processRequestAttachedStop (Proc proc)
	    {
		return processRequestAttach (proc, true);
	    }
	    ProcState processRequestRefresh (Proc proc)
	    {
		proc.sendRefresh ();
		return unattached;
	    }
	    ProcState processPerformRemoval (Proc proc)
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
    private static class AttachingToMainTask
	extends ProcState
    {
	private boolean stop;
	AttachingToMainTask (boolean stop)
	{
	    super ("AttachingWaitingForMainTask");
	    this.stop = stop;
	}
	ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
	{
	    // Get an up-to-date list of all tasks.  Now that the main
	    // task has stopped, all other tasks should be frozen.
	    proc.sendRefresh ();
	    if (proc.taskPool.size () == 1) {
		if (stop) {
		    proc.observableAttachedStop.notify (proc);
		    return stopped;
		}
		else {
		    task.requestContinue ();
		    proc.observableAttachedContinue.notify (proc);
		    return running;
		}
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
		    if (t.getTid () == proc.getPid ())
			continue;
		    t.performAttach ();
		}
		return new AttachingToOtherTasks (unattachedTasks, stop);
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
	private boolean stop;
	AttachingToOtherTasks (Map unattachedTasks, boolean stop)
	{
	    super ("AttachingWaitingForOtherTasks");
	    this.unattachedTasks = unattachedTasks;
	    this.stop = stop;
	}
	ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
	{
	    // As each task reports that it has been attached, remove
	    // it from the pool, wait until there are none left.
	    unattachedTasks.remove (task.id);
	    if (unattachedTasks.size () > 0)
		return this;
	    // All attached ...
	    if (stop) {
		proc.observableAttachedStop.notify (proc);
		return stopped;
	    }
	    // .., let them go, and mark this as
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
    private static class DetachingAllTasks
	extends ProcState
    {
	private Map attachedTasks;
	DetachingAllTasks (Map attachedTasks)
	{
	    super ("DetachingAllTasks");
	    this.attachedTasks = attachedTasks;
	}
	ProcState processPerformTaskDetachCompleted (Proc proc, Task task)
	{
	    // As each task reports that it has detached, remove it
	    // from the list.  Once there are none left the detach is
	    // done.
	    attachedTasks.remove (task.id);
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
    private static ProcState destroyed = new ProcState ("destroyed")
	{
	};

    private static ProcState running = new ProcState ("running")
	{
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		proc.observableAttachedContinue.notify (proc);
		return running;
	    } 
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		Map attachedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performDetach ();
		}
		return new DetachingAllTasks (attachedTasks);
	    }
	    ProcState processRequestAttachedStop (Proc proc)
	    {
		Map runningTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performStop ();
		}
		return new StoppingAllTasks (runningTasks);
	    }
	};

    private static ProcState startStopped = new ProcState ("startStopped")
	{
	    ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
	    {
		proc.observableAttachedStop.notify (proc);
		return stopped;
	    }
	};

    private static ProcState startRunning = new ProcState ("startRunning")
	{
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		proc.observableAttachedContinue.notify (proc);
		return running;
	    } 
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		Map attachedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performDetach ();
		}
		return new DetachingAllTasks (attachedTasks);
	    }
	    ProcState processRequestAttachedStop (Proc proc)
	    {
		Map runningTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performStop ();
		}
		return new StoppingAllTasks (runningTasks);
	    }
	};

    /**
     * All the tasks have been sent a stop request, waiting for each,
     * in turn, to acknowledge.
     */
    private static class StoppingAllTasks
	extends ProcState
    {
	private Map runningTasks;
	StoppingAllTasks (Map runningTasks)
	{
	    super ("StoppingAllTasks");
	    this.runningTasks = runningTasks;
	}
	ProcState processPerformTaskStopCompleted (Proc proc, Task task)
	{
	    // Wait until all the tasks have processed the stop
	    // request.
	    runningTasks.remove (task.id);
	    if (runningTasks.size () > 0)
		return this;
	    // All stopped.
	    proc.observableAttachedStop.notify (proc);
	    return stopped;
	}
    }

    private static ProcState stopped = new ProcState ("stopped")
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    ProcState processRequestAttachedStop (Proc proc)
	    {
		proc.observableAttachedStop.notify (proc);
		return stopped;
	    }
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		Map stoppedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performContinue ();
		}
		return new ContinuingAllTasks (stoppedTasks);
	    }
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		Map attachedTasks
		    = (Map) (((HashMap)proc.taskPool).clone ());
		for (Iterator i = proc.taskPool.values ().iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performDetach ();
		}
		return new DetachingAllTasks (attachedTasks);
	    }
	};

    /**
     * All tasks have been sent a continue request, wait for each in
     * turn to report back that the request has been processed.
     */
    private static class ContinuingAllTasks
	extends ProcState
    {
	private Map stoppedTasks;
	ContinuingAllTasks (Map stoppedTasks)
	{
	    super ("ContinupingAllTasks");
	    this.stoppedTasks = stoppedTasks;
	}
	ProcState processPerformTaskContinueCompleted (Proc proc, Task task)
	{
	    // Wait until all the tasks have processed the continue
	    // request.
	    stoppedTasks.remove (task.id);
	    if (stoppedTasks.size () > 0)
		return this;
	    // All continuped.
	    proc.observableAttachedContinue.notify (proc);
	    return running;
	}
    }
}
