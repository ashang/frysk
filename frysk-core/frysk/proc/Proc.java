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

import java.util.*;

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

    public class TaskObservable
	extends Observable
    {
	protected void notify (Task task)
	{
	    setChanged ();
	    notifyObservers (task);
	}
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

    public TaskForcedStopObserver stopObserver = new TaskForcedStopObserver ();
    protected Map taskPool = new HashMap ();
    public TaskObservable taskAdded = new TaskObservable ();
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
    public TaskObservable taskRemoved = new TaskObservable ();
    /** Tempoary observer, for code needing an exit-status.  */
    public TaskEventObservable taskDestroyed = new TaskEventObservable ();
    void remove (Task task)
    {
	taskRemoved.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }

    // Other observable events, for the moment keep these in the proc
    // (should they be per-task?).
    public TaskEventObservable taskExiting = new TaskEventObservable ();
    public TaskEventObservable taskExeced = new TaskEventObservable ();
    public ProcEventObservable allStopped = new ProcEventObservable ();

    public abstract Auxv[] getAuxv ();

    public String toString ()
    {
	return ("[Proc"
		+ ",id=" + id
		+ "]");
    }
}
