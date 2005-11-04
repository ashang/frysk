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
import java.util.Observable;
import java.util.LinkedList;

/**
 * A UNIX Process, containing tasks, memory, ...
 */

public abstract class Proc
{
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
     * Create a new, possibly attached, definitely running, Proc'ess.
     * Since PARENT could be NULL, explicitly specify the HOST.
     */
    private Proc (Host host, Proc parent, ProcId id, boolean attached)
    {
	this.host = host;
	this.id = id;
	this.parent = parent;
	state = ProcState.initial (this, attached);
	// Keep parent informed.
	if (parent != null)
	    parent.add (this);
	// Keep our manager informed.
	host.add (this);
	if (attached)
	    // XXX: Only do this when attached; when detached require
	    // a further system-poll to get the info.
	    sendNewAttachedTask (new TaskId (id.id));
    }
    /**
     * Create a new, unattached, running, Proc.  Since PARENT could be
     * NULL, explicitly specify the HOST.
     */
    protected Proc (Host host, Proc parent, ProcId id)
    {
	this (host, parent, id, false);
    }
    /**
     * Create a new, attached, running, process forked by Task.
     */
    protected Proc (Task task, ProcId forkId)
    {
	this (task.proc.host, task.proc, forkId, true);
    }
   
    /**
     * Create a new, definitely attached, definitely running, task.
     */
    abstract void sendNewAttachedTask (TaskId id);

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
		    state = state.processRequestAttachedContinue (Proc.this);
		}
	    });
    }
    /**
     * Request that the process be both attached and stopped.
     *
     * An un-attached process will be attached.  A running process
     * will be stopped.
     */
    public void requestAttachedStop ()
    {
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestAttachedStop (Proc.this);
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
     * completed its stop request.
     */
    void performTaskStopCompleted (final Task theTask)
    {
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
	childPool.add (child);
    }
    /**
     * Remove Proc from this processes children.
     */
    void remove (Proc child)
    {
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
    public class TaskObservable
	extends Observable
    {
	protected void notify (Task task)
	{
	    setChanged ();
	    notifyObservers (task);
	}
    }
    /**
     * Notify of the addition of a task attached to this process.
     *
     * This event indicates the presence of the task, not that it is
     * attached or detached.  When ever a task is attached a {@link
     * Task.observableAttachedContinue} or {@link
     * Task.observableAttachedStopped} event also occures.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public TaskObservable observableTaskAdded = new TaskObservable ();
    /**
     * Notify of the removal of a task attached to this process.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public TaskObservable observableTaskRemoved = new TaskObservable ();

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
    }
    /**
     * Remove Task from this Proc.
     */
    void remove (Task task)
    {
	observableTaskRemoved.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }
    /**
     * Remove all but Task from this Proc.
     */
    void retain (Task task)
    {
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
     * The process has transitioned to the attached / continue state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ProcObservable observableAttachedContinue = new ProcObservable ();

    /**
     * The process has transitioned to the attached / stop state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ProcObservable observableAttachedStop = new ProcObservable ();

    /**
     * The process has transitioned to the detached / continue state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ProcObservable observableDetachedContinue = new ProcObservable ();

    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",id=" + id
		+ ",state=" + state
		+ "}");
    }
}
