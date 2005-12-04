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
    abstract public String getCommand ();

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
	logger.log (Level.FINE, "create detached proc {0}\n", this); 
    }
    /**
     * Create a new, attached, running, process forked by Task.
     */
    protected Proc (Task task, ProcId forkId)
    {
	this (forkId, task.proc, task.proc.host);
	state = ProcState.initial (this, true);
	logger.log (Level.FINE, "create attached forked proc {0}\n", this); 
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
     * Request that the process be both attached and running.
     *
     * An un-attached process will be attached.  A stopped process
     * will resume execution.
     */
    public void requestAttachedContinue ()
    {
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    logger.log (Level.FINE, "request proc attach/run execute\n", ""); 
		    state = state.processRequestAttachedContinue (Proc.this);
		}
	    });
    }
    /**
     * Request that the process be both detached and left running.
     *
     * An attached process is detached.  If the attached process was
     * stopped, it's execution is resumed.
     */
    public void requestDetachedContinue ()
    {
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    logger.log (Level.FINE, "request process detach\n", ""); 
		    state = state.processRequestDetachedContinue (Proc.this);
		}
	    });
    }
    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh ()
    {
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    logger.log (Level.FINE, "request process task refresh\n", ""); 
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
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    logger.log (Level.FINE, "request proc removal\n", ""); 
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
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    logger.log (Level.FINE, "task stopped {0}\n", theTask); 
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
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    logger.log (Level.FINE, "task continued {0}\n", theTask); 
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
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Observation observation = observationArg;
		public void execute ()
		{
		    logger.log (Level.FINE, "add observer {0} \n", observationArg); 
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
	logger.log (Level.FINE, "add proc as new child {0}\n", child); 
	childPool.add (child);
    }
    /**
     * Remove Proc from this processes children.
     */
    void remove (Proc child)
    {
	logger.log (Level.FINE, "remove proc as child {0}\n", child); 
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
	    logger.log (Level.FINE, "notify observers {0}\n", o); 
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
    public ObservableXXX observableTaskAdded = new ObservableXXX ();
    /**
     * Notify of the removal of a task attached to this process.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableTaskRemoved = new ObservableXXX ();

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
	observableTaskAdded.notify (task);
	host.observableTaskAdded.notify (task);
    }
    /**
     * Remove Task from this Proc.
     */
    void remove (Task task)
    {
	logger.log (Level.FINE, "remove task from proc {0}\n", task); 
	observableTaskRemoved.notify (task);
	host.observableTaskRemoved.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }
    /**
     * Remove all but Task from this Proc.
     */
    void retain (Task task)
    {
	logger.log (Level.FINE, "remove all but task from proc {0}\n", task); 
	Collection tasks = taskPool.values();
	tasks.remove (task);
	taskPool.values().removeAll (tasks);
	host.removeTasks (tasks);
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

    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",id=" + id
		+ ",state=" + state
		+ ",command=" + getCommand ()
		+ "}");
    }
}
