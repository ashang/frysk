// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.proc.live;

import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import frysk.proc.Observation;

/**
 * A UNIX Process State
 *
 */

abstract class LinuxPtraceProcState extends State {
    private static final Log fine = LogFactory.fine(LinuxPtraceProcState.class);
    private static final Log finest = LogFactory.finest(LinuxPtraceProcState.class);

    LinuxPtraceProcState(String state) {
	super (state);
    }
    LinuxPtraceProcState handleRemoval(LinuxPtraceProc proc) {
	throw unhandled(proc, "handleRemoval");
    }
    LinuxPtraceProcState handleTaskAttachCompleted(LinuxPtraceProc proc, LinuxPtraceTask task) {
	throw unhandled(proc, "handleTaskAttachCompleted");
    }
    LinuxPtraceProcState handleTaskDetachCompleted(LinuxPtraceProc proc, LinuxPtraceTask task) {
	throw unhandled(proc, "handleTaskDetachCompleted");
    }
    LinuxPtraceProcState handleTaskDetachCompleted(LinuxPtraceProc proc, LinuxPtraceTask task, LinuxPtraceTask clone) {
	throw unhandled(proc, "handleTaskDetachCompleted/clone"); }
    LinuxPtraceProcState handleAddObservation(LinuxPtraceProc proc, Observation observation) {
	throw unhandled(proc, "handleAddObservation");
    }
    LinuxPtraceProcState handleDeleteObservation(LinuxPtraceProc proc, Observation observation) {
	throw unhandled(proc, "handleDeleteObservation");
    }
    LinuxPtraceProcState handleDetach(LinuxPtraceProc proc, boolean shouldRemoveObservers) {
	throw unhandled(proc, "handleDetach");
    }

    /**
     * Return the Proc's initial state.
     *
     * A new detached process materializes out of no where by
     * magically appearing in <tt>/proc</tt>; a new attached process
     * is always created via fork.  The latter, until an observer is
     * added is assumed to be about to detach.
     */
    static LinuxPtraceProcState initial (boolean starting) {
	fine.log("initial", starting);
	if (starting)
	    return new Detaching();
	else
	    return detached;
    }

    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    private static final LinuxPtraceProcState detached = new LinuxPtraceProcState ("detached")
	{
	    LinuxPtraceProcState handleRemoval(LinuxPtraceProc proc) {
		fine.log("handleRemoval", proc); 
		// XXX: What about a dieing proc's tasks, have a
		// dieing state and force a proc refresh?
		if (proc.parent != null)
		    proc.parent.remove (proc);
		return destroyed;
	    }
	    LinuxPtraceProcState handleAddObservation (LinuxPtraceProc proc,
						       Observation observation) {
	    	fine.log("handleAddObserver", proc); 
	    	return Attaching.initialState (proc, observation);
	    }

	    LinuxPtraceProcState handleDeleteObservation (LinuxPtraceProc proc,
					       Observation observation)
	    {
	    	fine.log("handleDeleteObservation", proc); 
		// Must be bogus; if there were observations then the
		// Proc wouldn't be in this state.
		observation.fail (new RuntimeException ("not attached"));
		return detached;
	    }
	};

    /**
     * A process is being attached, this is broken down into
     * sub-states.
     */
    private static class Attaching
    {
	/**
	 * The initial attaching state, find the main task and tell it
	 * to attach.
	 */
	static LinuxPtraceProcState initialState (LinuxPtraceProc proc, Observation observation)
	{
	    fine.log("state", proc); 
	    if (!proc.addObservation (observation))
		observation.fail(new RuntimeException("not actually added"));
	    // Grab the main task; only bother with the refresh if the
	    // Proc has no clue as to its task list.
	    if (proc.getTasks().size() == 0)
		proc.sendRefresh ();
	    // Assumes that the main Task's ID == the Proc's ID.
	    LinuxPtraceTask mainTask
		= ((LinuxPtraceHost)proc.getHost()).getTask(proc.pid);
	    if (mainTask == null) {
		// The main task exited and a refresh managed to
		// update Proc removing it.
		observation.fail (new RuntimeException ("main task exited"));
		return detached;
	    }
	    // Tell the main task to get things started.
	    mainTask.performAttach ();
	    return new Attaching.ToMainTask (mainTask);
	}
	/**
	 * All tasks attached, set them running and notify any
	 * interested parties.
	 */
	private static LinuxPtraceProcState allAttached (LinuxPtraceProc proc)
	{
	    fine.log("allAttached", proc); 
	    for (Iterator i = proc.observationsIterator();
		 i.hasNext ();) {
		Observation observation = (Observation) i.next ();
		observation.handleAdd ();
	    }
	    // .., let them go, and mark this as
	    // attached/running.
	    for (Iterator i = proc.getTasks ().iterator ();
		 i.hasNext (); ) {
		LinuxPtraceTask t = (LinuxPtraceTask) i.next ();
		t.performContinue ();
	    }
	    proc.observableAttachedXXX.notify (proc);
	    return running;
	}
	/**
	 * In the process of attaching, the main task has been sent an
	 * attach request.  That task stopping indicates that the
	 * entire process is stopped and hence all the other tasks can
	 * be attached.
	 */
	private static class ToMainTask extends LinuxPtraceProcState {
	    private LinuxPtraceTask mainTask;
	    ToMainTask (LinuxPtraceTask mainTask)
	    {
		super ("Attaching.ToMainTask");
		this.mainTask = mainTask;
	    }
	    LinuxPtraceProcState handleTaskAttachCompleted (LinuxPtraceProc proc, LinuxPtraceTask task)
	    {
		fine.log("handleTaskAttachCompleted", proc); 
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
		    LinuxPtraceTask t = (LinuxPtraceTask) i.next ();
		    t.performAttach ();
		}
		// What next?  Nothing else wait for all the attaching
		// tasks.
		if (attachingTasks.size () == 0)
		    return allAttached (proc);
		else
		    return new Attaching.ToOtherTasks (attachingTasks);
		
	    }
	    LinuxPtraceProcState handleAddObservation (LinuxPtraceProc proc,
					    Observation observation)
	    {
		fine.log("handleAddObservation", proc); 
		// An extra observation, just add it to the list.
		proc.addObservation (observation);
		return this;
	    }
	    LinuxPtraceProcState handleDeleteObservation (LinuxPtraceProc proc,
					       Observation observation)
	    {
		fine.log("handleDeleteObservation", proc); 
		// If the observation was never added, this will
		// return false, but that is ok.
		proc.removeObservation (observation);
		observation.fail (new RuntimeException ("canceled"));
		if (proc.observationsSize() == 0) {
		    // None of the other tasks are attached, just need
		    // to detach the main one.
		    mainTask.performDetach (false);
		    return new Detaching (proc, mainTask);
		}
		return this;
	    }

	    LinuxPtraceProcState handleTaskDetachCompleted (LinuxPtraceProc proc, LinuxPtraceTask task)
	    {
		return this;
	    }
        
	    LinuxPtraceProcState handleDetach(LinuxPtraceProc proc, boolean shouldRemoveObservers)
	    {
		fine.log("handleDetach", proc);
		return new Detaching (proc, shouldRemoveObservers);
	    } 

	}
	/**
	 * In the process of attaching, the main task is attached, now
	 * waiting for the remaining tasks to indicate they, too, have
	 * attached (or at least processed the attach request).
	 */
	static private class ToOtherTasks extends LinuxPtraceProcState {
	    private Collection attachingTasks;
	    ToOtherTasks (Collection attachingTasks)
	    {
		super ("Attaching.ToOtherTasks");
		this.attachingTasks = attachingTasks;
	    }
	    LinuxPtraceProcState handleTaskAttachCompleted (LinuxPtraceProc proc, LinuxPtraceTask task)
	    {
		fine.log("handleTaskAttachCompleted", proc); 
		// As each task reports that it has been attached,
		// remove it from the pool, wait until there are none
		// left.
		attachingTasks.remove (task);
		if (attachingTasks.size () == 0)
		    return allAttached (proc);
		return this;
	    }
	    LinuxPtraceProcState handleAddObservation (LinuxPtraceProc proc,
					    Observation observation)
	    {
		fine.log("handleAddObservation", proc); 
		proc.addObservation (observation);
		return this;
	    }
	    LinuxPtraceProcState handleDeleteObservation (LinuxPtraceProc proc,
					       Observation observation)
	    {
		fine.log("handleDeleteObservation", proc); 
		proc.removeObservation (observation);
		observation.fail (new RuntimeException ("canceled"));
		if (proc.observationsSize () == 0)
		    return new Detaching (proc, false);
		return this;
	    }
	}
    }

    /**
     * In the process of detaching; waiting for all tasks to report
     * back that they have successfully detached.
     */
    private static class Detaching extends LinuxPtraceProcState {
	private Collection attachedTasks;
	/**
	 * A Proc just created using fork; task yet to be created and
	 * want to detach from it.
	 */
	Detaching() {
	    super("Detaching");
	    attachedTasks = new HashSet();
	}
	/**
	 * Start detaching the entire process.
	 * @param shouldRemoveObservers whether the observers on each task should 
	 * be removed.
	 */
	Detaching (LinuxPtraceProc proc, boolean shouldRemoveObservers)
	{
	    super ("Detaching");
	    attachedTasks = proc.getTasks ();
	    for (Iterator i = attachedTasks.iterator ();
		 i.hasNext (); ) {
		LinuxPtraceTask t = (LinuxPtraceTask) i.next ();
		t.performDetach (shouldRemoveObservers);
	    }
	}
	/**
	 * Either just starting up and assumed to be detaching or just
	 * attached to the main task.
	 */
	Detaching (LinuxPtraceProc proc, LinuxPtraceTask mainTask)
	{
	    super ("Detaching");
	    attachedTasks = new HashSet ();
	    attachedTasks.add (mainTask);
	}
	LinuxPtraceProcState handleTaskDetachCompleted (LinuxPtraceProc proc, LinuxPtraceTask task)
	{
	    fine.log("handleTaskDetachCompleted", proc, "task", task);
	    // As each task reports that it has detached, add it
	    // to the detached list.
	    attachedTasks.remove (task);
	    if (attachedTasks.size () > 0)
		// Still more tasks to detach.
		return this;
	    // All done, notify.
	    proc.observableDetachedXXX.notify (proc);
	    return detached;
	}
	LinuxPtraceProcState handleTaskDetachCompleted (LinuxPtraceProc proc, LinuxPtraceTask task,
					     LinuxPtraceTask clone)
	{
	    fine.log("handleTaskDetachCompleted", proc);
	    attachedTasks.remove (task);
	    // Doh, a clone, need to also wait for that to detach.
	    attachedTasks.add (clone);
	    return this;
	}
	LinuxPtraceProcState handleDetach(LinuxPtraceProc proc, boolean shouldRemoveObservers)
	{
	    //Already detaching, don't have to do anything different.
	    if (shouldRemoveObservers)
		return this;
      
	    return super.handleDetach(proc, shouldRemoveObservers);
	}
	LinuxPtraceProcState handleAddObservation (LinuxPtraceProc proc,
					Observation observation)
	{
	    fine.log("handleAddObservation", proc);
	    // Ulgh, detaching and a new observer arrived.
	    return Attaching.initialState (proc, observation);
	}
	LinuxPtraceProcState handleDeleteObservation (LinuxPtraceProc proc,
					   Observation observation)
	{
	    fine.log("handleDeleteObservation", proc);
	    // Ouch; request to remove what must be an already
	    // removed observation.
	    observation.fail (new RuntimeException ("canceled"));
	    return this;
	}
    }

    /**
     * The process has been destroyed.
     */
    private static final LinuxPtraceProcState destroyed = new LinuxPtraceProcState ("destroyed")
	{
	};

    /**
     * The process is running, add and remove observers.  If the
     * number of observers ever reaches zero, start detaching.
     */
    private static final LinuxPtraceProcState running = new LinuxPtraceProcState ("running")
	{
	    LinuxPtraceProcState handleAddObservation (LinuxPtraceProc proc,
					    Observation observation)
	    {
		fine.log("handleAddObservation", proc); 
		proc.addObservation(observation);
		observation.handleAdd ();
		return running;
	    }
	    LinuxPtraceProcState handleDeleteObservation (LinuxPtraceProc proc,
					       Observation observation)
	    {
		fine.log("handleDeleteObservation", proc); 
		if (proc.removeObservation (observation)) {
		    finest.log("handleDeleteObservation remove succeeded");
		    observation.handleDelete ();
		    if (proc.observationsSize () == 0) {
			finest.log("handleDeleteObservation size == 0, detaching");
			return new Detaching (proc, false);
		    }
		}
		else 
		    observation.fail (new RuntimeException ("not added"));
        
		return running;
	    }
	    LinuxPtraceProcState handleDetach(LinuxPtraceProc proc, boolean shouldRemoveObservers)
            {
		fine.log("handleDetach", proc);
		return new Detaching (proc, shouldRemoveObservers);
            }        
	};
}
