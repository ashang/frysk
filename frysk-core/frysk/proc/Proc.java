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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;

/**
 * A UNIX Process, containing tasks, memory, ...
 */

public abstract class Proc
{
    private static Logger logger = Logger.getLogger (Config.FRYSK_LOG_ID);
    protected ProcId id;
    public ProcId getId ()
    {
	return id;
    }
    
    Proc parent;
    public Proc getParent ()
    {
	// XXX: This needs to be made on-demand.
    	return this.parent;
    }

    Host host;
    public Host getHost ()
    {
	return host;
    }

    public int getPid ()
    {
	return id.hashCode ();
    }

    /**
     * Return the basename of the program that this process is
     * running.
     */
    public abstract String getCommand ();

    /**
     * Return the full path of the program that this process is
     * running.
     */
    public String getExe ()
    {
	if (exe == null)
	    exe = sendrecExe ();
	return exe;
    }
    private String exe;
    protected abstract String sendrecExe ();

    /**
     * Return the Proc's command line argument list
     */
    public String[] getCmdLine ()
    {
	if (argv == null)
	    argv = sendrecCmdLine ();
	return argv;
    }
    protected abstract String[] sendrecCmdLine ();
    private String[] argv;

    /**
     * Create a new Proc skeleton.  Since PARENT could be NULL,
     * explicitly specify the HOST.
     */
    private Proc (ProcId id, Proc parent, Host host)
    {
	this.host = host;
	this.id = id;
	this.parent = parent;
	// Keep parent informed.
	if (parent != null)
	    parent.add (this);
	// Keep host informed.
	host.add (this);
    }
    /**
     * Create a new, unattached, running, Proc.  Since PARENT could be
     * NULL, explicitly specify the HOST.
     */
    protected Proc (Host host, Proc parent, ProcId id)
    {
	this (id, parent, host);
	state = ProcState.initial (this, false);
	logger.log (Level.FINE, "{0} new - create unattached running proc\n",
		    this); 
    }
    /**
     * Create a new, attached, running, process forked by Task.
     */
    protected Proc (Task task, ProcId forkId)
    {
	this (forkId, task.proc, task.proc.host);
	state = ProcState.initial (this, true);
	logger.log (Level.FINE, "{0} new - create attached running proc\n",
		    this); 
    }

    abstract void sendRefresh ();

    /**
     * The current state of this process.
     */
    /* XXX: private */ ProcState state;
    /**
     * Return the state represented as a simple string.
     */
    public String getStateString ()
    {
	return state.toString ();
    }

    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh ()
    {
	logger.log (Level.FINE, "{0} requestRefresh\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestRefresh (Proc.this);
		}
	    });
    }
    /**
     * (Internal) Tell the process that is no longer listed in the
     * system table remove itself.
     */
    void performRemoval ()
    {
	logger.log (Level.FINE, "{0} performRemoval -- no longer in /proc\n",
		    this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformRemoval (Proc.this);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its attach.
     */
    void performTaskAttachCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskAttachCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    state = state.processPerformTaskAttachCompleted (Proc.this,
								     task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    void performTaskDetachCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskDetachCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    state = state.processPerformTaskDetachCompleted (Proc.this,
								     task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    void performTaskDetachCompleted (final Task theTask, final Task theClone)
    {
	logger.log (Level.FINE, "{0} performTaskDetachCompleted/clone\n",
		    this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		Task clone = theClone;
		public void execute ()
		{
		    state = state.processPerformTaskDetachCompleted
			(Proc.this, task, clone);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its stop request.
     */
    void performTaskStopCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskStopCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    state = state.processPerformTaskStopCompleted (Proc.this,
								   task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the contained task has
     * completed the continue request.
     */
    void performTaskContinueCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskContinueCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    state = state.processPerformTaskContinueCompleted
			(Proc.this, task);
		}
	    });
    }

    /**
     * The set of observations that currently apply to this task.
     */
    Set observations = new HashSet ();

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     */
    void performAddObservation (final Observation observationArg)
    {
	logger.log (Level.FINE, "{0} performAddObservation\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Observation observation = observationArg;
		public void execute ()
		{
		    state = state.processPerformAddObservation
			(Proc.this, observation);
		}
	    });
    }
    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     */
    void performDeleteObservation (final Observation observationArg)
    {
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Observation observation = observationArg;
		public void execute ()
		{
		    state = state.processPerformDeleteObservation
			(Proc.this, observation);
		}
	    });
    }

    boolean isStopped ()
    {
	return state.isStopped ();
    }

    /**
     * Table of this processes child processes.
     */
    private Set childPool = new HashSet ();
    /**
     * Add Proc as a new child
     */
    void add (Proc child)
    {
	logger.log (Level.FINE, "{0} add(Proc) -- a child process\n", this); 
	childPool.add (child);
    }
    /**
     * Remove Proc from this processes children.
     */
    void remove (Proc child)
    {
	logger.log (Level.FINE, "{0} remove(Proc) -- a child proces\n", this); 
	childPool.remove (child);
    }
    /**
     * Get the children as an array.
     */
    public LinkedList getChildren ()
    {
	return new LinkedList (childPool);
    }

    /**
     * XXX: Temporary until .observable's are converted to
     * .requestAddObserver.
     */
    public class ObservableXXX
	extends Observable
    {
	void notify (Object o)
	{
	    logger.log (Level.FINE, "{0} notify -- all observers\n", o); 
	    setChanged ();
	    notifyObservers (o);
	}
    }

    /**
     * Notify of the addition of a task attached to this process.
     *
     * This event indicates the presence of the task, not that it is
     * attached or detached.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableTaskAddedXXX = new ObservableXXX ();
    /**
     * Notify of the removal of a task attached to this process.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableTaskRemovedXXX = new ObservableXXX ();

    /**
     * Pool of tasks belonging to this Proc.
     */
    Map taskPool = new HashMap ();
    /**
     * Add the Task to this Proc.
     */
    void add (Task task)
    {
	taskPool.put (task.id, task);
	observableTaskAddedXXX.notify (task);
	host.observableTaskAdded.notify (task);
    }
    /**
     * Remove Task from this Proc.
     */
    void remove (Task task)
    {
	logger.log (Level.FINE, "{0} remove(Task) -- within this Proc\n",
		    this); 
	observableTaskRemovedXXX.notify (task);
	host.observableTaskRemoved.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }
    /**
     * Remove all but Task from this Proc.
     */
    void retain (Task task)
    {
	logger.log (Level.FINE, "{0} retain(Task) -- remove all but task\n",
		    this); 
	HashMap new_tasks = new HashMap();
	new_tasks = (HashMap)((HashMap)taskPool).clone ();
	new_tasks.values().remove( task);
	taskPool.values().removeAll (new_tasks.values());
	host.removeTasks (new_tasks.values());
    }
    
    /**
     * Return this Proc's Task's as a list.
     */
    public LinkedList getTasks ()
    {
	return new LinkedList (taskPool.values ());
    }

    /**
     * The Process Auxiliary Vector.
     */
    public Auxv[] getAuxv ()
    {
	if (auxv == null) {
	    auxv = sendrecAuxv ();
	}
	return auxv;
    }
    private Auxv[] auxv;
    /**
     * Extract the auxv from the inferior.
     */
    abstract Auxv[] sendrecAuxv ();

    /**
     * The process has transitioned to the attached state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableAttached = new ObservableXXX ();

    /**
     * The process has transitioned to the detached.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableDetached = new ObservableXXX ();

    /**
     * Add Tasks Observer.
     * @param tasksObserver observer to be added.
     */
    public void requestAddTasksObserver(final ProcObserver.Tasks tasksObserver)
    {
    	logger.log (Level.FINE, "{0} requestAddTasksObserver \n", this); 
    	Manager.eventLoop.add (new ProcEvent ()
	    {
    		ProcObserver.Tasks theObserver = tasksObserver;
		public void execute ()
		{
		    state = state.processRequestAddTasksObserver(Proc.this,
								 theObserver);
		}
	    });
    }
    
    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",id=" + id
		+ ",state=" + state
		+ ",command=" + getCommand ()
		+ "}");
    }
}
