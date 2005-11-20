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

import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;

/**
 * The task state machine.
 */

class TaskState
    extends State
{
    private static Logger logger = Logger.getLogger (Config.FRYSK_LOG_ID);
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
	logger.log (Level.FINE, "signal task {0}\n", task); 
	throw unhandled (task, "PerformSignaled");
    }
    TaskState processPerformStopped (Task task)
    {
	logger.log (Level.FINE, "stop task {0}\n", task); 
	throw unhandled (task, "PerformStopped");
    }
    TaskState processPerformTrapped (Task task)
    {
	logger.log (Level.FINE, "trap task {0}\n", task); 
	throw unhandled (task, "PerformTrapped");
    }
    TaskState processPerformSyscalled (Task task)
    {
	logger.log (Level.FINE, "syscall task {0}\n", task); 
	 throw unhandled (task, "PerformSyscalled");
     }
    TaskState processPerformTerminated (Task task, boolean signal, int value)
    {
	logger.log (Level.FINE, "terminated task {0}\n", task); 
	throw unhandled (task, "PerformTerminated");
    }
    TaskState processPerformTerminating (Task task, boolean signal, int value)
    {
	logger.log (Level.FINE, "terminating task {0}\n", task); 
	throw unhandled (task, "PerformTerminating");
    }
    TaskState processPerformExeced (Task task)
    {
	logger.log (Level.FINE, "exec task {0}\n", task); 
	throw unhandled (task, "PerformExeced");
    }
    TaskState processPerformDisappeared (Task task, Throwable w)
    {
	logger.log (Level.FINE, "zombie task {0}\n", task); 
	throw unhandled (task, "PerformZombied");
    }
    TaskState processRequestStop (Task task)
    {
	logger.log (Level.FINE, "stop task {0}\n", task); 
	throw unhandled (task, "RequestStop");
    }
    TaskState processRequestContinue (Task task)
    {
	logger.log (Level.FINE, "continue task {0}\n", task); 
	throw unhandled (task, "RequestContinue");
    }
    TaskState processRequestStepInstruction (Task task)
    {
	logger.log (Level.FINE, "step insn task {0}\n", task); 
	throw unhandled (task, "RequestStepInstruction");
    }
    TaskState processPerformRemoval (Task task)
    {
	logger.log (Level.FINE, "remove task {0}\n", task); 
	throw unhandled (task, "PerformRemoval");
    }
    TaskState processPerformAttach (Task task)
    {
	logger.log (Level.FINE, "attach task {0}\n", task); 
	throw unhandled (task, "PerformAttach");
    }
    TaskState processPerformDetach (Task task)
    {
	logger.log (Level.FINE, "detach task {0}\n", task); 
	throw unhandled (task, "PerformDetach");
    }
    TaskState processPerformStop (Task task)
    {
	logger.log (Level.FINE, "stop task {0}\n", task); 
	throw unhandled (task, "PerformStop");
    }
    TaskState processPerformContinue (Task task)
    {
	logger.log (Level.FINE, "continue task {0}\n", task); 
	throw unhandled (task, "PerformContinue");
    }
    TaskState processPerformCloned (Task task, Task clone)
    {
	logger.log (Level.FINE, "clone task {0}\n", task); 
	throw unhandled (task, "PerformCloned");
    }
    TaskState processPerformForked (Task task, Task fork)
    {
	logger.log (Level.FINE, "fork task {0}\n", task); 
	throw unhandled (task, "PerformForked");
    }
    TaskState processRequestUnblock (Task task, TaskObserver observer)
    {
	logger.log (Level.FINE, "unblock task {0}\n", task); 
	throw unhandled (task, "RequestUnblock");
    }
    TaskState processPerformAddObservation (Task task,
					    Observation observation)
    {
	throw unhandled (task, "PerformAddObservation");
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
	logger.log (Level.FINE, "destroy {0}\n", task); 
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
		logger.log (Level.FINE, "remove {0}\n", task); 
		return destroyed;
	    }
	    TaskState processPerformAttach (Task task)
	    {
		logger.log (Level.FINE, "attach {0}\n", task); 
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
		logger.log (Level.FINE, "stop {0}\n", task); 
		task.proc.performTaskAttachCompleted (task);
		return attached;
	    }
    	    TaskState processPerformDisappeared (Task task, Throwable w)
    	    {
		// Outch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		logger.log (Level.FINE, "zombie {0}\n", task); 
		task.proc.performTaskAttachCompleted (task);
		task.proc.remove (task);
		return destroyed;
    	    }
	    TaskState processPerformDetach (Task task)
	    {
		return detaching;
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
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		observation.add ();
		return attached;
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
		logger.log (Level.FINE, "stop {0}\n", task); 
		task.sendSetOptions ();
		task.sendContinue (0);
		task.notifyAttached ();
		return running;
	    }
	    TaskState processPerformTrapped (Task task)
	    {
		logger.log (Level.FINE, "trap {0}\n", task); 
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
		logger.log (Level.FINE, "terminate {0}\n", task); 
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
		logger.log (Level.FINE, "terminate {0}\n", task); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		observation.add ();
		return startRunning;
	    }
	};

    // A manually stopped task.
    private static TaskState stopping = new TaskState ("stopping")
	{
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", task); 
		return stopping;
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return stoppingInSyscall;
	    }
	    TaskState processPerformStopped (Task task)
	    {
		// XXX: Not a standard observer.
		logger.log (Level.FINE, "stop {0}\n", task); 
		task.requestedStopEvent.notify (task);
		return stopped;
	    }
	    TaskState processPerformTrapped (Task task)
	    {
		// XXX: Not a standard observer.
		logger.log (Level.FINE, "trap {0}\n", task); 
		task.requestedStopEvent.notify (task);
		return paused;
	    }
	};

    // A manually stopped task currently in a syscall.
    private static TaskState stoppingInSyscall = new TaskState ("stoppingInSyscall")
	{
	    TaskState processRequestStop (Task task)
	    {
		return stoppingInSyscall;
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallExit ();
		task.sendContinue (0);
		return stopping;
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
		logger.log (Level.FINE, "signal {0}\n", this); 
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
		logger.log (Level.FINE, "syscall {0}\n", this); 
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return runningInSyscall;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		logger.log (Level.FINE, "terminate {0}\n", this); 
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
		logger.log (Level.FINE, "terminate {0}\n", this); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processPerformExeced (Task task)
	    {
		// Remove all tasks, retaining just this one.
		logger.log (Level.FINE, "exec {0}\n", this); 
		task.proc.retain (task);
		if (task.notifyExeced () > 0) {
		    return blockedContinue;
		}
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
    	    TaskState processPerformDisappeared (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "zombie {0}\n", this); 
		return zombied;
    	    }
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", this); 
		task.sendStop ();
		return stopping;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "continue {0}\n", this); 
		return running;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		logger.log (Level.FINE, "detach {0}\n", this); 
		task.sendStop ();
		return detaching;
	    }
	    TaskState processPerformStop (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", this); 
		task.sendStop ();
		return performingStop;
	    }
	    TaskState processPerformCloned (Task task, Task clone)
	    {
		logger.log (Level.FINE, "clone {0}\n", this); 
		if (task.notifyCloned (clone) > 0) {
		    return blockedContinue;
		}
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
	    TaskState processPerformForked (Task task, Task fork)
	    {
		logger.log (Level.FINE, "fork {0}\n", this); 
		if (task.notifyForked (fork) > 0)
		    return blockedContinue;
		else
		    task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		observation.add ();
		return running;
	    }
	};

    // Task is running inside a syscall.
    private static TaskState runningInSyscall = new TaskState ("runningInSyscall")
	{
	    // XXX: We needn't look for signal events because the
	    // syscall will exit before we get the signal, however, we still.
	    // need to look for terminating and terminated events
	    // because an exit syscall will produce these events and
	    // never produce a syscall-exit event.

	    boolean isRunning ()
	    {
		return true;
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallExit ();
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
		return runningInSyscall;
	    }
	    TaskState processPerformTerminated (Task task, boolean signal,
						int value)
	    {
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processRequestStop (Task task)
	    {
		task.sendStop ();
		return stoppingInSyscall;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		return runningInSyscall;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		task.sendStop ();
		return detachingInSyscall;
	    }
	};

    private static TaskState performingStop = new TaskState ("performingStop")
	{
	    TaskState processPerformStopped (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", this); 
		task.proc.performTaskStopCompleted (task);
		return stopped;
	    }
	};

    private static TaskState detaching = new TaskState ("detaching")
	{
	    TaskState processPerformAttach (Task task)
	    {
		return attaching;
	    }
	    TaskState processPerformStopped (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", this); 
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return detachingInSyscall;
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

    private static TaskState detachingInSyscall = new TaskState ("detachingInSyscall")
	{
	    TaskState processPerformStopped (Task task)
	    {
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processPerformSyscalled (Task task)
	    {
		task.notifySyscallExit ();
		task.sendContinue (0);
		return detaching;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		logger.log (Level.FINE, "terminate {0}\n", this); 
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
		logger.log (Level.FINE, "trap {0}\n", this); 
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
		logger.log (Level.FINE, "stop {0}\n", this); 
		return stopped;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "continue {0}\n", this); 
		task.sendContinue (0);
		return running;
	    }
	    TaskState processRequestStepInstruction (Task task)
	    {
		logger.log (Level.FINE, "step insn {0}\n", this); 
		task.sendStepInstruction (0);
		return stepping;
	    }
	    TaskState processPerformContinue (Task task)
	    {
		// XXX: Need to save the stop signal so that the
		// continue is correct.
		logger.log (Level.FINE, "continue {0}\n", this); 
		task.sendContinue (0);
		task.proc.performTaskContinueCompleted (task);
		return running;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		// XXX: Need to hang onto the signal that was about to
		// be delivered?
		logger.log (Level.FINE, "detach {0}\n", this); 
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
		logger.log (Level.FINE, "stop {0}\n", this); 
		return paused;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "continue {0}\n", this); 
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
		logger.log (Level.FINE, "continue {0}\n", this); 
		return unpaused;
	    }
	    TaskState processPerformStopped (Task task)
	    {
		logger.log (Level.FINE, "stop {0}\n", this); 
		task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		observation.add ();
		return unpaused;
	    }
	};

    private static TaskState zombied = new TaskState ("zombied")
	{
	    TaskState processPerformTerminated (Task task, boolean signal,
						int value)
	    {
		logger.log (Level.FINE, "terminate {0}\n", this); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processPerformTerminating (Task task, boolean signal,
						 int value)
	    {
		logger.log (Level.FINE, "terminate {0}\n", this); 
		task.notifyTerminating (signal, value);
// 		if (signal)
// 		    task.sendContinue (value);
// 		else
// 		    task.sendContinue (0);
		return zombied;
	    }
    	    TaskState processPerformDisappeared (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "zombie {0}\n", this); 
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
		logger.log (Level.FINE, "attach {0}\n", this); 
		task.proc.performTaskAttachCompleted (task);
		return destroyed;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		observation.fail (new RuntimeException ("unattached"));
		task.proc.performDeleteObservation (observation);
		return destroyed;
	    }
	};
}
