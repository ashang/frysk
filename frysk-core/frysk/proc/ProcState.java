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
import java.util.Collection;
import java.util.HashSet;

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
    static ProcState initial (Proc proc, boolean attached)
    {
	if (attached)
	    return startRunning;
	else
	    return unattached;
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
    ProcState processPerformAddObservation (Proc proc,
					    Observation observation)
    {
	throw unhandled (proc, "PerformAddObservation");
    }
    ProcState processPerformDeleteObservation (Proc proc,
					       Observation observation)
    {
	throw unhandled (proc, "PerformDeleteObservation");
    }

    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    private static ProcState unattached = new ProcState ("unattached")
	{
	    private ProcState processRequestAttach (Proc proc, boolean stop)
	    {
		return Attaching.state (proc, null, stop);
	    }
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		return processRequestAttach (proc, false);
	    }
	    ProcState processRequestRefresh (Proc proc)
	    {
		proc.sendRefresh ();
		return unattached;
	    }
	    ProcState processPerformRemoval (Proc proc)
	    {
		// XXX: What about a dieing proc's tasks, have a
		// dieing state and force a proc refresh?
		if (proc.parent != null)
		    proc.parent.remove (proc);
		return destroyed;
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		return Attaching.state (proc, observation, false);
	    }
	};

    /**
     * A process is being attached, this is broken down into
     * sub-states.
     */
    private static class Attaching
    {
	/**
	 * The initial attaching state.
	 */
	static ProcState state (Proc proc, Observation observation,
				boolean stop)
	{
	    if (observation != null)
		proc.observations.add (observation);
	    // Grab the main task; only bother with the refresh if the
	    // Proc has no clue as to its task list.
	    if (proc.taskPool.size () == 0)
		proc.sendRefresh ();
	    // Assumes that the main Task's ID == the Proc's ID.
	    Task mainTask = Manager.host.get (new TaskId (proc.getPid ()));
	    if (mainTask == null) {
		// The main task exited and a refresh managed to
		// update Proc removing it.
		observation.fail (new RuntimeException ("main task exited"));
		return unattached;
	    }
	    mainTask.performAttach ();
	    return new Attaching.ToMainTask (mainTask, false);
	}
	/**
	 * All tasks attached, set them running and notify any
	 * interested parties.
	 */
	private static ProcState allAttached (Proc proc, boolean stop)
	{
	    for (Iterator i = proc.observations.iterator ();
		 i.hasNext ();) {
		Observation observation = (Observation) i.next ();
		observation.requestAdd ();
	    }
	    if (stop)
		return stopped;
	    // .., let them go, and mark this as
	    // attached/running.
	    for (Iterator i = proc.getTasks ().iterator ();
		 i.hasNext (); ) {
		Task t = (Task) i.next ();
		t.requestContinue ();
	    }
	    proc.observableAttached.notify (proc);
	    return running;
	}
	/**
	 * In the process of attaching, the main task has been sent an
	 * attach request, waiting for it to finish.
	 */
	private static class ToMainTask
	    extends ProcState
	{
	    private boolean stop;
	    private Task mainTask;
	    ToMainTask (Task mainTask, boolean stop)
	    {
		super ("Attaching.ToMainTask");
		this.stop = stop;
		this.mainTask = mainTask;
	    }
	    ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
	    {
		// With the main task attached, it is possible to get
		// an up-to-date list of tasks.  Remove from it the
		// tasks already being attached.  Ask them to also
		// attach.
		proc.sendRefresh ();
		Collection attachingTasks = proc.getTasks ();
		attachingTasks.remove (task);
		// Attach to those remaining attaching tasks.
		for (Iterator i = attachingTasks.iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performAttach ();
		}
		// What next?  Nothing else wait for all the attaching
		// tasks.
		if (attachingTasks.size () == 0)
		    return allAttached (proc, stop);
		else
		    return new ToOtherTasks (attachingTasks, stop);
		
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		proc.observations.add (observation);
		return this;
	    }
	    ProcState processPerformDeleteObservation (Proc proc,
						       Observation observation)
	    {
		proc.observations.remove (observation);
		observation.fail (new RuntimeException ("canceled"));
		if (proc.observations.size () == 0) {
		    Collection attachedTasks = new HashSet ();
		    attachedTasks.add (mainTask);
		    return Detaching.state (proc, attachedTasks);
		}
		else
		    return this;
	    }
	}

	/**
	 * In the process of attaching, the main task is attached, now
	 * waiting for the remaining tasks to indicate they, too, have
	 * attached (or at least processed the attach request).
	 */
	static private class ToOtherTasks
	    extends ProcState
	{
	    private Collection attachingTasks;
	    private boolean stop;
	    ToOtherTasks (Collection attachingTasks, boolean stop)
	    {
		super ("Attaching.ToOtherTasks");
		this.attachingTasks = attachingTasks;
		this.stop = stop;
	    }
	    ProcState processPerformTaskAttachCompleted (Proc proc, Task task)
	    {
		// As each task reports that it has been attached,
		// remove it from the pool, wait until there are none
		// left.
		attachingTasks.remove (task);
		if (attachingTasks.size () > 0)
		    return this;
		return allAttached (proc, stop);
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		proc.observations.add (observation);
		return this;
	    }
	    ProcState processPerformDeleteObservation (Proc proc,
						       Observation observation)
	    {
		proc.observations.remove (observation);
		observation.fail (new RuntimeException ("canceled"));
		if (proc.observations.size () > 0)
		    return this;
		return Detaching.state (proc);
	    }
	}
    }

    /**
     * In the process of detaching; waiting for all tasks to report
     * back that they have successfully detached.
     */
    private static class Detaching
    {
	/**
	 * Start detaching from the process, only need to detach the
	 * subset of tasks.
	 */
	static private ProcState state (Proc proc, Collection attachedTasks)
	{
	    for (Iterator i = attachedTasks.iterator ();
		 i.hasNext (); ) {
		Task t = (Task) i.next ();
		t.performDetach ();
	    }
	    return new Detaching.AllTasks (attachedTasks);
	}
	/**
	 * Start detaching the process.
	 */
	static private ProcState state (Proc proc)
	{
	    return state (proc, proc.getTasks ());
	}
	private static class AllTasks
	    extends ProcState
	{
	    private Collection attachedTasks;
	    AllTasks (Collection attachedTasks)
	    {
		super ("DetachingAllTasks");
		this.attachedTasks = attachedTasks;
	    }
	    ProcState processPerformTaskDetachCompleted (Proc proc, Task task)
	    {
		// As each task reports that it has detached, remove
		// it from the list.  Once there are none left the
		// detach is done.
		attachedTasks.remove (task);
		if (attachedTasks.size () > 0)
		    return this;
		// All done, notify.
		proc.observableDetached.notify (proc);
		return unattached;
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		// Ulgh, detaching and a new observer arrived.
		return Attaching.state (proc, observation, false);
	    }
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
		proc.observableAttached.notify (proc);
		return running;
	    } 
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		return Detaching.state (proc);
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		proc.observations.add (observation);
		observation.requestAdd ();
		return running;
	    }
	    ProcState processPerformDeleteObservation (Proc proc,
						       Observation observation)
	    {
		observation.delete ();
		proc.observations.remove (observation);
		if (proc.observations.size () == 0)
		    return Detaching.state (proc);
		else
		    return running;
	    }
	};

    private static ProcState startRunning = new ProcState ("startRunning")
	{
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		proc.observableAttached.notify (proc);
		return running;
	    } 
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		return Detaching.state (proc);
	    }
	    ProcState processPerformAddObservation (Proc proc,
						    Observation observation)
	    {
		proc.observations.add (observation);
		observation.requestAdd ();
		return startRunning;
	    }
	};

    private static ProcState stopped = new ProcState ("stopped")
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    ProcState processRequestAttachedContinue (Proc proc)
	    {
		Collection stoppedTasks = proc.getTasks ();
		for (Iterator i = stoppedTasks.iterator ();
		     i.hasNext (); ) {
		    Task t = (Task) i.next ();
		    t.performContinue ();
		}
		return new ContinuingAllTasks (stoppedTasks);
	    }
	    ProcState processRequestDetachedContinue (Proc proc)
	    {
		return Detaching.state (proc);
	    }
	};

    /**
     * All tasks have been sent a continue request, wait for each in
     * turn to report back that the request has been processed.
     */
    private static class ContinuingAllTasks
	extends ProcState
    {
	private Collection stoppedTasks;
	ContinuingAllTasks (Collection stoppedTasks)
	{
	    super ("ContinupingAllTasks");
	    this.stoppedTasks = stoppedTasks;
	}
	ProcState processPerformTaskContinueCompleted (Proc proc, Task task)
	{
	    // Wait until all the tasks have processed the continue
	    // request.
	    stoppedTasks.remove (task);
	    if (stoppedTasks.size () > 0)
		return this;
	    // All continuped.  XXX: Not a standard observer.
	    proc.observableAttached.notify (proc);
	    return running;
	}
    }
}
