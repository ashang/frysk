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

import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;

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
	logger.log (Level.FINE, "{0} initial\n", proc); 
	if (attached)
	    return running;
	else
	    return detached;
    }

    protected ProcState (String state)
    {
	super (state);
    }
    ProcState handleRemoval (Proc proc)
    {
	throw unhandled (proc, "handleRemoval");
    }
    ProcState handleRefresh (Proc proc)
    {
	throw unhandled (proc, "handleRefresh");
    }
    ProcState handleTaskAttachCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "handleTaskAttachCompleted");
    }
    ProcState handleTaskDetachCompleted (Proc proc, Task task)
    {
	throw unhandled (proc, "handleTaskDetachCompleted");
    }
    ProcState handleTaskDetachCompleted (Proc proc, Task task,
					 Task clone)
    {
	throw unhandled (proc, "handleTaskDetachCompleted/clone");
    }
    ProcState handleAddObservation (Proc proc,
				    Observation observation)
    {
	throw unhandled (proc, "handleAddObservation");
    }
    ProcState handleDeleteObservation (Proc proc,
				       Observation observation)
    {
	throw unhandled (proc, "handleDeleteObservation");
    }
    ProcState handleAddTasksObserver(Proc proc,
				     ProcObserver.Tasks tasksObserver)
    {
	throw unhandled (proc, "handleAddTasksObserver");
    }


    /**
     * The process is running free (or at least was the last time its
     * status was checked).
     */
    private static final ProcState detached = new ProcState ("detached")
	{
	    ProcState handleRefresh (Proc proc)
	    {
		logger.log (Level.FINE, "{0} handleRefresh\n", proc); 
		proc.sendRefresh ();
		return detached;
	    }
	    ProcState handleRemoval (Proc proc)
	    {
		logger.log (Level.FINE, "{0} handleRemoval\n", proc); 
		// XXX: What about a dieing proc's tasks, have a
		// dieing state and force a proc refresh?
		if (proc.parent != null)
		    proc.parent.remove (proc);
		return destroyed;
	    }
	    ProcState handleAddObservation (Proc proc,
					    Observation observation)
	    {
	    	logger.log (Level.FINE, "{0} handleAddObserver \n", proc); 
	    	return Attaching.state (proc, observation);
	    }

	    ProcState handleDeleteObservation (Proc proc,
					       Observation observation)
	    {
	    	logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
		// Must be bogus; if there were observations then the
		// Proc wouldn't be in this state.
		observation.fail (new RuntimeException ("not attached"));
		return detached;
	    }

	    ProcState handleAddTasksObserver (final Proc proc,
					      final ProcObserver.Tasks tasksObserver)
	    { 
	    	logger.log (Level.FINE, "{0} handleAddTasksObserver \n", proc); 
	    	proc.sendRefresh();
	    	class ClonedObserver
		    implements TaskObserver.Cloned
		{
		    ProcObserver.Tasks theObserver;
		    ClonedObserver(){
			this.theObserver = tasksObserver;
		    }
	    		
		    public Action updateCloned(Task task, Task clone) {
			theObserver.taskAdded(clone);
			clone.requestAddClonedObserver(this);
			return Action.CONTINUE;
		    }

		    public void addedTo(Object observable) {
		    }

		    public void addFailed(Object observable, Throwable w)
		    {
			throw new RuntimeException("How did this happen ?!");
		    }

		    public void deletedFrom(Object observable)
		    {
			theObserver.taskRemoved((Task)observable);
		    }
	    	}
	    	
	    	
	    	final Task mainTask = Manager.host.get (new TaskId (proc.getPid ()));
		if (mainTask == null) {
		    tasksObserver.addFailed(proc, new RuntimeException("Process lost"));
		    return detached;
		}

	    	final ClonedObserver clonedObserver = new ClonedObserver();
		mainTask.requestAddAttachedObserver(new TaskObserver.Attached()
		    {
		    	Proc theProc = proc;
		    	Task theMainTask = mainTask;
		    	ClonedObserver theClonedObserver = clonedObserver;
		    	
			public void deletedFrom(Object observable) {
			}
			
			public void addFailed(Object observable, Throwable w)
			{
			    // TODO Auto-generated method stub
			    throw new RuntimeException("You fogot to implement this method :D ");
			}
			
			public void addedTo(Object observable) {
			}
			
			public Action updateAttached(Task task)
			{
			    Iterator iterator = theProc.getTasks().iterator();
			    while (iterator.hasNext()) {
				Task myTask = (Task) iterator.next();
				task.requestAddClonedObserver(theClonedObserver);
				tasksObserver.existingTask(myTask);
			    }
			    theMainTask.requestDeleteAttachedObserver(this);
			    return Action.CONTINUE;
			}
		    });
		tasksObserver.addedTo(proc);
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
	 * The initial attaching state.
	 */
	static ProcState state (Proc proc, Observation observation)
	{
	    logger.log (Level.FINE, "{0} state\n", proc); 
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
		return detached;
	    }
	    mainTask.performAttach ();
	    return new Attaching.ToMainTask (mainTask);
	}
	/**
	 * All tasks attached, set them running and notify any
	 * interested parties.
	 */
	private static ProcState allAttached (Proc proc)
	{
	    logger.log (Level.FINE, "{0} allAttached\n", proc); 
	    for (Iterator i = proc.observations.iterator ();
		 i.hasNext ();) {
		Observation observation = (Observation) i.next ();
		observation.handleAdd ();
	    }
	    // .., let them go, and mark this as
	    // attached/running.
	    for (Iterator i = proc.getTasks ().iterator ();
		 i.hasNext (); ) {
		Task t = (Task) i.next ();
		t.performContinue ();
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
	    private Task mainTask;
	    ToMainTask (Task mainTask)
	    {
		super ("Attaching.ToMainTask");
		this.mainTask = mainTask;
	    }
	    ProcState handleTaskAttachCompleted (Proc proc, Task task)
	    {
		logger.log (Level.FINE, "{0} handleTaskAttachCompleted\n", proc); 
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
		    return allAttached (proc);
		else
		    return new ToOtherTasks (attachingTasks);
		
	    }
	    ProcState handleAddObservation (Proc proc,
					    Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleAddObservation\n", proc); 
		proc.observations.add (observation);
		return this;
	    }
	    ProcState handleDeleteObservation (Proc proc,
					       Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
		// If the observation was never added, this will
		// return false, but that is ok.
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
	    ToOtherTasks (Collection attachingTasks)
	    {
		super ("Attaching.ToOtherTasks");
		this.attachingTasks = attachingTasks;
	    }
	    ProcState handleTaskAttachCompleted (Proc proc, Task task)
	    {
		logger.log (Level.FINE, "{0} handleTaskAttachCompleted\n", proc); 
		// As each task reports that it has been attached,
		// remove it from the pool, wait until there are none
		// left.
		attachingTasks.remove (task);
		if (attachingTasks.size () > 0)
		    return this;
		return allAttached (proc);
	    }
	    ProcState handleAddObservation (Proc proc,
					    Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleAddObservation\n", proc); 
		proc.observations.add (observation);
		return this;
	    }
	    ProcState handleDeleteObservation (Proc proc,
					       Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
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
	    ProcState handleTaskDetachCompleted (Proc proc, Task task)
	    {
		logger.log (Level.FINE,
			    "{0} handleTaskDetachCompleted\n",
			    proc);
		// As each task reports that it has detached, add it
		// to the detached list.
		attachedTasks.remove (task);
		if (attachedTasks.size () > 0)
		    // Still more tasks to detach.
		    return this;
		// All done, notify.
		proc.observableDetached.notify (proc);
		return detached;
	    }
	    ProcState handleTaskDetachCompleted (Proc proc, Task task,
						 Task clone)
	    {
		logger.log (Level.FINE,
			    "{0} handleTaskDetachCompleted\n",
			    proc);
		attachedTasks.remove (task);
		// Doh, a clone, need to also wait for that to detach.
		attachedTasks.add (clone);
		return this;
	    }
	    ProcState handleAddObservation (Proc proc,
					    Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleAddObservation\n",
			    proc);
		// Ulgh, detaching and a new observer arrived.
		return Attaching.state (proc, observation);
	    }
	    ProcState handleDeleteObservation (Proc proc,
					       Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n",
			    proc);
		// Outch; request to remove what must be an already
		// removed observation.
		observation.fail (new RuntimeException ("canceled"));
		return this;
	    }
	}
    }

    /**
     * The process has been destroyed.
     */
    private static final ProcState destroyed = new ProcState ("destroyed")
	{
	};

    private static final ProcState running = new ProcState ("running")
	{
	    ProcState handleAddObservation (Proc proc,
					    Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
		proc.observations.add (observation);
		observation.handleAdd ();
		return running;
	    }
	    ProcState handleDeleteObservation (Proc proc,
					       Observation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
		if (proc.observations.remove (observation)) {
		    observation.handleDelete ();
		    if (proc.observations.size () == 0)
			return Detaching.state (proc);
		}
		else
		    observation.fail (new RuntimeException ("not added"));
		return running;
	    }
	};
}
