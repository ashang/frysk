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

class TaskState
    extends State
{
    protected TaskState (String state)
    {
	super (state);
    }
    boolean isRunning ()
    {
	return false;
    }
    boolean isStopped ()
    {
	return false;
    }
    boolean isDead ()
    {
	return false;
    }
    TaskState process (Task task, TaskEvent.Signaled event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Stopped event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Trapped event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Syscall event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Exited event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Exiting event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Terminated event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Cloned event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Forked event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Execed event)
    {
	throw unhandled (task, event);
    }
    TaskState process (Task task, TaskEvent.Zombied event)
    {
	throw unhandled (task, event);
    }
    TaskState processRequestStop (Task task)
    {
	throw unhandled (task);
    }
    TaskState processRequestContinue (Task task)
    {
	throw unhandled (task);
    }
    TaskState processRequestStepInstruction (Task task)
    {
	throw unhandled (task);
    }
    TaskState processRequestRemoval (Task task)
    {
	throw unhandled (task);
    }
    TaskState processRequestAttach (Task task)
    {
	throw unhandled (task);
    }

    /**
     * An attached task was destroyed, notify observers and, when the
     * containing process has no tasks left, assume its also exited so
     * should be destroyed.
     *
     * XXX: Linux doesn't provide a way for detecting the transition
     * from "process with no tasks" (aka zombie or defunct) to
     * destroyed.  This code unfortunatly bypasses the zombied state.
     *
     * XXX: GCJ botches the code gen for a call to this method, from
     * an anonymous inner class, when this method isn't static.
     */
    protected static void processAttachedDestroy (Task task, TaskEvent event)
    {
	task.proc.taskDestroyed.notify (event);
	// A process with no tasks is dead ...?
	if (task.proc.taskPool.size () == 0) {
	    if (task.proc.parent != null)
		task.proc.parent.remove (task.proc);
	    task.proc.host.remove (task.proc);
	}
    }

    /**
     * The task isn't attached (it was presumably detected using a
     * probe of the system process list).
     */
    static TaskState unattached = new TaskState ("unattached")
	{
	    TaskState processRequestRemoval (Task task)
	    {
		return destroyed;
	    }
	    TaskState processRequestAttach (Task task)
	    {
		task.sendAttach ();
		return attaching;
	    }
	};

    /**
     * The task is in the process of being attached.
     */
    static TaskState attaching = new TaskState ("attaching")
	{
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		// XXX: Need to hang onto the signal that was about to
		// be delivered?
		Manager.eventLoop.appendEvent (new ProcEvent.TaskAttached (task.proc, task));
		task.sendSetOptions ();
		return stopped;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		// Outch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		Manager.eventLoop.appendEvent (new ProcEvent.TaskAttached (task.proc, task));		
		task.proc.remove (task);
		return destroyed;
    	    }
	};

    /**
     * Task just starting out, wait for it to become ready, and then
     * let it run.
     */
    static TaskState startRunning = new TaskState ("startRunning")
	{
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		task.proc.taskDiscovered.notify (task);
		task.proc.taskAdded.notify (task);
		task.sendSetOptions ();
		task.stopEvent.notify (event);
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		task.proc.taskDiscovered.notify (task);
		task.proc.taskAdded.notify (task);
		task.sendSetOptions ();
		task.stopEvent.notify (event);
		task.sendContinue (0);
		return running;
	    }
	    TaskState processRequestStop (Task task)
	    {
		task.sendSetOptions ();
		return stopping;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return startRunning;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		// This can happen if the whole process gets killed.
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};
    /**
     * Task just starting out, wait for it to become ready, but put it
     * into the stopped start.
     */
    static TaskState startStopped = new TaskState ("startStopped")
	{
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		task.proc.taskDiscovered.notify (task);
		task.proc.taskAdded.notify (task);
		task.sendSetOptions ();
		return stopped;
	    }
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		task.proc.taskDiscovered.notify (task);
		task.proc.taskAdded.notify (task);
		task.sendSetOptions ();
		return stopped;
	    }
	    TaskState processRequestStop (Task task)
	    {
		task.sendSetOptions ();
		return stopping;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return startRunning;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		// This can happen if the whole process gets killed.
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    // A manually stopped task.
    static TaskState stopping = new TaskState ("stopping")
	{
	    TaskState processRequestStop (Task task)
	    {
		return stopping;
	    }
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		task.requestedStopEvent.notify (event);
		return stopped;
	    }
	    TaskState process (Task task, TaskEvent.Signaled event)
	    {
		task.requestedStopEvent.notify (event);
		// For any other stop, we distinguish that we are
		// stopped, but not as we expected.
		task.stopEvent.notify (event);
		return paused;
	    }
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		task.requestedStopEvent.notify (event);
		// For any other stop, we distinguish that we are
		// stopped, but not as we expected.
		task.stopEvent.notify (event);
		return paused;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		task.syscallEvent.notify (event);
		task.sendContinue (0);
		return stopping;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		task.proc.taskExiting.notify (event);
		task.sendContinue (event.signal);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		return stopping;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		return stopping;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		task.proc.taskExeced.notify (event);
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		return stopping;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    // Keep the task running.
    static TaskState running = new TaskState ("running")
	{
	    boolean isRunning ()
	    {
		return true;
	    }
	    TaskState processRequestStop (Task task)
	    {
		task.sendStop ();
		return stopping;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		task.stopEvent.notify (event);
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Signaled event)
	    {
		task.stopEvent.notify (event);
		task.sendContinue (event.signal);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		task.stopEvent.notify (event);
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		task.syscallEvent.notify (event);
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		task.proc.taskExiting.notify (event);
		task.sendContinue (event.signal);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		task.proc.taskExeced.notify (event);
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		task.sendContinue (0);
		return running;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState stepping = new TaskState ("stepping")
	{
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		// We are waiting for a SIGTRAP to indicate step done.
		task.stepEvent.notify (event);
		return stopped;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		task.syscallEvent.notify (event);
		task.sendContinue (0);
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		task.proc.taskExiting.notify (event);
		task.sendContinue (event.signal);
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		task.proc.taskExeced.notify (event);
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		return stopped;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState steppingPaused = new TaskState ("steppingPaused")
	{
	    TaskState process (Task task, TaskEvent.Signaled event)
	    {
		// For any other stop, we process the event
		// and continue, but we are not stepped yet.
		// We don't notify for SIGSTOP events.
		task.stopEvent.notify (event);
		task.sendStepInstruction (event.signal);
		return steppingPaused;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		task.syscallEvent.notify (event);
		task.sendContinue (0);
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		task.proc.taskExiting.notify (event);
		task.sendContinue (event.signal);
		return steppingPaused;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		return steppingPaused;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		return steppingPaused;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		task.proc.taskExeced.notify (event);
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		return stopped;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState stopped = new TaskState ("stopped")
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    TaskState processRequestStop (Task task)
	    {
		return stopped;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		task.sendContinue (0);
		return running;
	    }
	    TaskState processRequestStepInstruction (Task task)
	    {
		task.sendStepInstruction (0);
		return stepping;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState paused = new TaskState ("paused")
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    TaskState processRequestStop (Task task)
	    {
		return paused;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		task.sendContinue (0);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState unpaused = new TaskState ("unpaused")
	{
	    boolean isRunning ()
	    {
		return true;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		task.sendContinue (0);
		return running;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		task.syscallEvent.notify (event);
		task.sendContinue (0);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		task.proc.taskExiting.notify (event);
		task.sendContinue (event.signal);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		task.sendContinue (0);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		task.sendContinue (0);
		return unpaused;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		task.proc.taskExeced.notify (event);
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		task.sendContinue (0);
		return unpaused;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState zombied = new TaskState ("zombied")
	{
	    TaskState process (Task task, TaskEvent.Stopped event)
	    {
		// Ignore.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Trapped event)
	    {
		// Ignore.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Forked event)
	    {
		// Ignore.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Cloned event)
	    {
		// Ignore.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Execed event)
	    {
		// Too late to do anything.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Exiting event)
	    {
		// Too late to do anything.
		return zombied;
	    }
	    TaskState process (Task task, TaskEvent.Exited event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Terminated event)
	    {
		task.proc.remove (event.task);
		processAttachedDestroy (task, event);
		return destroyed;
	    }
    	    TaskState process (Task task, TaskEvent.Zombied event)
    	    {
		return zombied;
    	    }
	};

    static TaskState destroyed = new TaskState ("destroyed") 
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    boolean isDead ()
	    {
		return true;
	    }
	    TaskState process (Task task, TaskEvent.Signaled event)
	    {
		// Ignore any subsequent stop events which may
		// have been forced by us.
		return destroyed;
	    }
	    TaskState process (Task task, TaskEvent.Syscall event)
	    {
		// Process any leftover syscall event.
		task.syscallEvent.notify (event);
		return destroyed;
	    }
	    TaskState processRequestAttach (Task task)
	    {
		// Lie; the Proc wants to know that the operation has
		// been processed more than it succeeded.
		Manager.eventLoop.appendEvent (new ProcEvent.TaskAttached (task.proc, task));
		return destroyed;
	    }
	};
}
