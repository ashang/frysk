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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

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

    abstract Task newTask (TaskId id, boolean runnable);

    Proc (Host host, ProcId id, boolean attached)
    {
	this.host = host;
	this.id = id;
	if (attached)
	    state = ProcState.running;
	else
	    state = ProcState.unattached;
	// Keep our manager informed.
	host.add (this);
	// ... and add the main task.
	if (attached)
	    // XXX: Only do this when attached; when detached require
	    // a further system-poll to get the info.
	    newTask (new TaskId (id.id), true);
    }
    Proc (Proc parent, ProcId id, boolean attached)
    {
	this (parent.host, id, attached);
	this.parent = parent;
	parent.add (this);
    }
    /**
     * Create a new detached process.
     */
    Proc (Host host, Proc parent, ProcId procId)
    {
	this.host = host;
	this.id = procId;
	state = ProcState.unattached;
	// If there is a parent (process 1 is parentless) add this to
	// its list of children.
	if (parent != null) {
	    this.parent = parent;
	    parent.add (this);
	}
	// Keep the host informed.
	host.add (this);
    }
    
    protected void startAllTasks ()
    {
	Collection tasks = taskPool.values ();
	Iterator i = tasks.iterator ();
	while (i.hasNext ()) {
	    Task t = (Task)i.next ();
	    t.requestContinue ();
	}
    }

    protected void stopAllTasks ()
    {
	Collection tasks = taskPool.values ();
	Iterator i = tasks.iterator ();
	while (i.hasNext ()) {
	    Task t = (Task)i.next ();
	    t.requestStop ();
	}
    }

    abstract void sendAttach ();
    abstract void sendNewAttachedChild (ProcId childId);
    abstract void sendRefresh ();

    ProcState state = ProcState.unattached;

    /**
     * Request that the process be both attached and running.
     *
     * An un-attached process will be attached.  A stopped process
     * will resume execution.
     */
    public void requestAttachedContinue ()
    {
	Manager.eventLoop.appendEvent (new ProcEvent ()
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
	Manager.eventLoop.appendEvent (new ProcEvent ()
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
	Manager.eventLoop.appendEvent (new ProcEvent ()
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
	Manager.eventLoop.appendEvent (new ProcEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestRefresh (Proc.this);
		}
	    });
    }
    /**
     * Request that a process that is no longer listed in the system
     * table remove itself.
     */
    public void requestRemoval ()
    {
	Manager.eventLoop.appendEvent (new ProcEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestRemoval (Proc.this);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its attach.
     */
    void performTaskAttachCompleted (final Task theTask)
    {
	Manager.eventLoop.appendEvent (new ProcEvent ()
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
	Manager.eventLoop.appendEvent (new ProcEvent ()
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
     * Use requestAttachedStop.
     */
    void stop ()
    {
	state = state.stop (this);
    }
    /**
     * Use requestAttachedCont.
     */
    void go ()
    {
	state = state.go (this);
    }

    boolean isStopped ()
    {
	return state.isStopped ();
    }

    protected Set children = new HashSet ();
    void add (Proc child)
    {
	children.add (child);
    }
    void remove (Proc child)
    {
	children.remove (child);
    }

    public void detach ()
    {
	host.removeTasks (taskPool.values ());
	if (parent != null)
            parent.remove (this);
	host.remove (this);
    }

    static class TaskForcedStopObserver
        implements Observer
    {
        public void update (Observable o, Object obj)
        {
            TaskEvent e = (TaskEvent) obj;
	    Task task = e.task;
	    Proc proc = task.proc;
	    Collection allTasks = proc.taskPool.values();
	    Iterator i = allTasks.iterator ();
	    boolean allStopped = true;
	    while (i.hasNext ()) {
		Task t = (Task)i.next ();
		if (!t.isStopped () 
		    && t.id.hashCode () != task.id.hashCode ()) {
		    allStopped = false;
		    break;
		}
	    }
	    if (allStopped) {
		// all stopped
		ProcEvent.AllStopped event = new ProcEvent.AllStopped (proc);
		event.execute ();
	    }
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

    public TaskForcedStopObserver stopObserver = new TaskForcedStopObserver ();
    protected Map taskPool = new HashMap ();
    public TaskObservable taskDiscovered = new TaskObservable ();
    void add (Task task)
    {
	taskPool.put (task.id, task);
	task.requestedStopEvent.addObserver (stopObserver);
	// ... and any thing monitoring this process.
    }
    // Eliminate all but TASK.
    void retain (Task task)
    {
	Collection tasks = taskPool.values();
	tasks.remove (task);
	taskPool.values().removeAll (tasks);
	host.removeTasks (tasks);
    }
    /** Tempoary observer, for code needing an exit-status.  */
    public TaskEventObservable taskDestroyed = new TaskEventObservable ();
    void remove (Task task)
    {
	observableTaskRemoved.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }

    // Other observable events, for the moment keep these in the proc
    // (should they be per-task?).
    public TaskEventObservable taskExiting = new TaskEventObservable ();
    public TaskEventObservable taskExeced = new TaskEventObservable ();
    public ProcEventObservable allStopped = new ProcEventObservable ();

    public abstract Auxv[] getAuxv ();

    /**
     * The process has transitioned to the attached / continue state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ProcObservable observableAttachedContinue = new ProcObservable ();

    /**
     * The process has transitioned to the detached / continue state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ProcObservable observableDetachedContinue = new ProcObservable ();

    public String toString ()
    {
	return ("[Proc"
		+ ",id=" + id
		+ "]");
    }
}
