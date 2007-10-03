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

/**
 * The task state machine.
 */

class TaskState
    extends State
{
    /**
     * Return the tasks initial state.
     */
    static TaskState initial (Task task, boolean attached)
    {
	if (attached)
	    return startRunning;
	else
	    return unattached;
    }

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
    TaskState processPerformSignaled (Task task, int sig)
    {
	throw unhandled (task, "PerformSignaled");
    }
    TaskState processPerformStopped (Task task)
    {
	throw unhandled (task, "PerformStopped");
    }
    TaskState processPerformTrapped (Task task)
    {
	throw unhandled (task, "PerformTrapped");
    }
    TaskState processPerformSyscalled (Task task)
    {
	throw unhandled (task, "PerformSyscalled");
    }
    TaskState processPerformTerminated (Task task, boolean signal, int value)
    {
	throw unhandled (task, "PerformTerminated");
    }
    TaskState processPerformTerminating (Task task, boolean signal, int value)
    {
	throw unhandled (task, "PerformTerminating");
    }
    TaskState processPerformExeced (Task task)
    {
	throw unhandled (task, "PerformExeced");
    }
    TaskState processPerformZombied (Task task)
    {
	throw unhandled (task, "PerformZombied");
    }
    TaskState processRequestStop (Task task)
    {
	throw unhandled (task, "RequestStop");
    }
    TaskState processRequestContinue (Task task)
    {
	throw unhandled (task, "RequestContinue");
    }
    TaskState processRequestStepInstruction (Task task)
    {
	throw unhandled (task, "RequestStepInstruction");
    }
    TaskState processPerformRemoval (Task task)
    {
	throw unhandled (task, "PerformRemoval");
    }
    TaskState processPerformAttach (Task task)
    {
	throw unhandled (task, "PerformAttach");
    }
    TaskState processPerformDetach (Task task)
    {
	throw unhandled (task, "PerformDetach");
    }
    TaskState processPerformStop (Task task)
    {
	throw unhandled (task, "PerformStop");
    }
    TaskState processPerformContinue (Task task)
    {
	throw unhandled (task, "PerformContinue");
    }
    TaskState processPerformCloned (Task task, Task clone)
    {
	throw unhandled (task, "PerformCloned");
    }
    TaskState processPerformForked (Task task, Proc fork)
    {
	throw unhandled (task, "PerformForked");
    }
    TaskState processRequestUnblock (Task task, TaskObserver observer)
    {
	throw unhandled (task, "RequestUnblock");
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
    protected static void processAttachedTerminated (Task task, boolean signal,
						     int value)
    {
	task.notifyTerminated (signal, value);
	// A process with no tasks is dead ...?
	if (task.proc.taskPool.size () == 0) {
	    task.proc.parent.remove (task.proc);
	    task.proc.host.remove (task.proc);
	}
    }

    /**
     * The task isn't attached (it was presumably detected using a
     * probe of the system process list).
     */
    private static TaskState unattached = new TaskState ("unattached")
	{
	    TaskState processPerformRemoval (Task task)
	    {
		return destroyed;
	    }
	    TaskState processPerformAttach (Task task)
	    {
		task.sendAttach ();
		return attaching;
	    }
	};

    /**
     * The task is in the process of being attached.
     */
    private static TaskState attaching = new TaskState ("attaching")
	{
	    TaskState processPerformStopped (Task task)
	    {
		task.proc.performTaskAttachCompleted (task);
		return attached;
	    }
    	    TaskState processPerformZombied (Task task)
    	    {
		// Outch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		task.proc.performTaskAttachCompleted (task);
		task.proc.remove (task);
		return destroyed;
    	    }
	};

    /**
     * The task is attached, and waiting to be continued.  This first
     * continue is special, it is also the moment that any observers
     * get notified that the task has transitioned into the attached
     * state.
     */
    private static TaskState attached = new TaskState ("attached")
	{
	    TaskState processRequestContinue (Task task)
	    {
		task.sendSetOptions ();
		if (task.notifyAttached () > 0)
		    return blockedContinue;
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
	    TaskState processPerformDetach (Task task)
	    {
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	};

    /**
     * Task just starting out, wait for it to become ready, and then
     * let it run.
     */
    private static TaskState startRunning = new TaskState ("startRunning")
	{
	    TaskState processPerformStopped (Task task)
	    {
		task.sendSetOptions ();
		task.sendContinue (0);
		task.notifyAttached ();
		return running;
	    }
	    TaskState processPerformTrapped (Task task)
	    {
		task.sendSetOptions ();
		if (task.notifyAttached () > 0)
		    return blockedContinue;
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformTerminated (Task task, boolean signal,
						int value)
	    {
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	};

    // A manually stopped task.
    private static TaskState stopping = new TaskState ("stopping")
	{
	    TaskState processRequestStop (Task task)
	    {
		return stopping;
	    }
	    TaskState processPerformStopped (Task task)
	    {
		// XXX: Not a standard observer.
		task.requestedStopEvent.notify (task);
		return stopped;
	    }
	    TaskState processPerformTrapped (Task task)
	    {
		// XXX: Not a standard observer.
		task.requestedStopEvent.notify (task);
		return paused;
	    }
	};

    // Keep the task running.
    private static TaskState running = new TaskState ("running")
	{
	    boolean isRunning ()
	    {
		return true;
	    }
	    TaskState processPerformSignaled (Task task, int sig)
	    {
		if (task.notifySignaled (sig) > 0) {
		    return new BlockedSignal (sig);
		}
		else {
		    task.sendContinue (sig);
		    return running;
		}
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallXXX ();
		task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformTerminated (Task task, boolean signal,
						int value)
	    {
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processPerformExeced (Task task)
	    {
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		if (task.notifyExeced () > 0) {
		    return blockedContinue;
		}
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
    	    TaskState processPerformZombied (Task task)
    	    {
		return zombied;
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
	    TaskState processPerformDetach (Task task)
	    {
		task.sendStop ();
		return detaching;
	    }
	    TaskState processPerformStop (Task task)
	    {
		task.sendStop ();
		return performingStop;
	    }
	    TaskState processPerformCloned (Task task, Task clone)
	    {
		if (task.notifyCloned (clone) > 0) {
		    return blockedContinue;
		}
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
	    TaskState processPerformForked (Task task, Proc fork)
	    {
		if (task.notifyForked (fork) > 0)
		    return blockedContinue;
		else
		    task.sendContinue (0);
		return running;
	    }
	};

    private static TaskState performingStop = new TaskState ("performingStop")
	{
	    TaskState processPerformStopped (Task task)
	    {
		task.proc.performTaskStopCompleted (task);
		return stopped;
	    }
	};

    private static TaskState detaching = new TaskState ("detaching")
	{
	    TaskState processPerformStopped (Task task)
	    {
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		if (signal)
		    task.sendDetach (value);
		else
		    task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	};

    private static TaskState stepping = new TaskState ("stepping")
	{
	    TaskState processPerformTrapped (Task task)
	    {
		// XXX: Not a standard observer.  Notify SIGTRAP
		// indicating that the step is done.
		task.stepEvent.notify (task);
		return stopped;
	    }
	};

    /**
     * The Task is blocked by a set of observers, remain in this state
     * until all the observers have unblocked themselves.  This state
     * preserves any pending signal so that, once unblocked, the
     * signal is delivered.
     */
    private static class BlockedSignal
	extends TaskState
    {
	int sig;
	BlockedSignal (int sig)
	{
	    super ("BlockedSignal");
	    this.sig = sig;
	}
	public String toString ()
	{
	    return "BlockedSignal=" + sig;
	}
	TaskState processRequestUnblock (Task task, TaskObserver observer)
	{
	    task.blockers.remove (observer);
	    if (task.blockers.size () == 0) {
		task.sendContinue (sig);
		return running;
	    }
	    else {
		return this; // Still blocked.
	    }
	}
    }
    /**
     * The task is in the blocked state with no pending signal.
     */
    private static TaskState blockedContinue = new BlockedSignal (0)
	{
	    public String toString ()
	    {
		return "blockedContinue";
	    }
	};

    private static TaskState stopped = new TaskState ("stopped")
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
	    TaskState processPerformContinue (Task task)
	    {
		// XXX: Need to save the stop signal so that the
		// continue is correct.
		task.sendContinue (0);
		task.proc.performTaskContinueCompleted (task);
		return running;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		// XXX: Need to hang onto the signal that was about to
		// be delivered?
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	};

    private static TaskState paused = new TaskState ("paused")
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
	};

    private static TaskState unpaused = new TaskState ("unpaused")
	{
	    boolean isRunning ()
	    {
		return true;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return unpaused;
	    }
	    TaskState processPerformStopped (Task task)
	    {
		task.sendContinue (0);
		return running;
	    }
	};

    private static TaskState zombied = new TaskState ("zombied")
	{
	    TaskState processPerformTerminated (Task task, boolean signal,
						int value)
	    {
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		task.notifyTerminating (signal, value);
// 		if (signal)
// 		    task.sendContinue (value);
// 		else
// 		    task.sendContinue (0);
		return zombied;
	    }
    	    TaskState processPerformZombied (Task task)
    	    {
		return zombied;
    	    }
	};

    private static TaskState destroyed = new TaskState ("destroyed") 
	{
	    boolean isStopped ()
	    {
		return true;
	    }
	    boolean isDead ()
	    {
		return true;
	    }
	    TaskState processPerformAttach (Task task)
	    {
		// Lie; the Proc wants to know that the operation has
		// been processed rather than the request was
		// successful.
		task.proc.performTaskAttachCompleted (task);
		return destroyed;
	    }
	};
}
