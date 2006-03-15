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

import java.util.logging.Level;

/**
 * The task state machine.
 */

class TaskState
    extends State
{
    /**
     * Return the initial state of a detached task.
     */
    static TaskState unattachedState ()
    {
	return unattached;
    }
    /**
     * Return the initial state of the Main task.
     */
    static TaskState mainState ()
    {
	return Start.waitForStop;
    }
    /**
     * Return the initial state of a cloned task.
     */
    static TaskState clonedState (Task parent)
    {
	if (parent.state == detaching)
	    return detaching;
	else if (parent.state == running)
	    return Start.waitForStop;
	else
	    throw new RuntimeException ("clone's parent in unexpected state "
					+ parent);
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
    TaskState processStoppedEvent (Task task)
    {
	throw unhandled (task, "processStoppedEvent");
    }
    TaskState processTrappedEvent (Task task)
    {
	throw unhandled (task, "processTrappedEvent");
    }
    TaskState processSyscalledEvent (Task task)
    {
	throw unhandled (task, "processSyscalledEvent");
    }
    TaskState processTerminatedEvent (Task task, boolean signal, int value)
    {
	throw unhandled (task, "processTerminatedEvent");
    }
    TaskState processTerminatingEvent (Task task, boolean signal, int value)
    {
	throw unhandled (task, "processTerminatingEvent");
    }
    TaskState processExecedEvent (Task task)
    {
	throw unhandled (task, "processExecedEvent");
    }
    TaskState processDisappearedEvent (Task task, Throwable w)
    {
	throw unhandled (task, "processDisappearedEvent");
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
    TaskState processClonedEvent (Task task, Task clone)
    {
	throw unhandled (task, "processClonedEvent");
    }
    TaskState processForkedEvent (Task task, Task fork)
    {
	throw unhandled (task, "processForkedEvent");
    }
    TaskState processRequestUnblock (Task task, TaskObserver observer)
    {
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
	logger.log (Level.FINE, "{0} processAttachedTerminated\n", task); 
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
		logger.log (Level.FINE, "{0} processPerformRemoval\n", task); 
		return destroyed;
	    }
	    TaskState processPerformAttach (Task task)
	    {
		logger.log (Level.FINE, "{0} processPerformAttach\n", task); 
		task.sendAttach ();
		return attaching;
	    }
	};

    /**
     * The task is in the process of being attached.
     */
    private static TaskState attaching = new TaskState ("attaching")
	{
	    TaskState processStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processStoppedEvent\n", task); 
		task.proc.performTaskAttachCompleted (task);
		return attached;
	    }
	    TaskState processTrappedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processTrappedEvent\n", task); 
		task.proc.performTaskAttachCompleted (task);
		return attached;
	    }
    	    TaskState processDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} processDisappearedEvent\n", task); 
		// Outch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		task.proc.performTaskAttachCompleted (task);
		task.proc.remove (task);
		return destroyed;
    	    }
	    TaskState processPerformDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} processPerformDetach\n", task); 
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
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
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
		logger.log (Level.FINE, "{0} processPerformDetach\n", task); 
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		logger.log (Level.FINE, "{0} processPerformAddObservation\n", task); 
		observation.add ();
		return attached;
	    }
	};

    /**
     * Task just starting out, wait for it to both become ready, and
     * to be unblocked, before continuing.
     */
    static class Start
	extends TaskState
    {
	Start (String name)
	{
	    super ("Start." + name);
	}
	private static TaskState attemptAttach (Task task)
	{
	    logger.log (Level.FINE, "{0} attemptAttach\n", task); 
	    task.sendSetOptions ();
	    if (task.blockers.size () > 0) {
		return Start.blocked;
	    }
	    if (task.notifyAttached () > 0) {
		return blockedContinue;
	    }
	    task.sendContinue (0);
	    return running;
	}
	TaskState processPerformAddObservation (Task task,
						Observation observation)
	{
	    logger.log (Level.FINE, "{0} processPerformAddObservation\n",
			task); 
	    observation.add ();
	    return this;
	}
	
	private static TaskState waitForStop = new Start ("waitForStop")
	    {
		TaskState processRequestUnblock (Task task,
						 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} processRequestUnblock\n", task); 
		    task.blockers.remove (observer);
		    return Start.waitForStop;
		}
		TaskState processTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} processTrappedEvent\n", task);
		    return attemptAttach (task);
		}
		TaskState processStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} processStoppedEvent\n", task);
		    return attemptAttach (task);
		}
	    };
	
	private static TaskState blocked = new Start ("blocked")
	    {
		TaskState processRequestUnblock (Task task,
						 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} processRequestUnblock\n", task); 
		    task.blockers.remove (observer);
		    return attemptAttach (task);
		}
	    };
    }

    // A manually stopped task.
    private static TaskState stopping = new TaskState ("stopping")
	{
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		return stopping;
	    }
	    TaskState processSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processSyscalledEvent\n", task); 
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return stoppingInSyscall;
	    }
	    TaskState processStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processStoppedEvent\n", task); 
		return stopped;
	    }
	    TaskState processTrappedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processTrappedEvent\n", task); 
		return paused;
	    }
	};

    // A manually stopped task currently in a syscall.
    private static TaskState stoppingInSyscall = new TaskState ("stoppingInSyscall")
	{
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		return stoppingInSyscall;
	    }
	    TaskState processSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processSyscalledEvent\n", task); 
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
		logger.log (Level.FINE, "{0} processPerformSignaled\n", task); 
		if (task.notifySignaled (sig) > 0) {
		    return new BlockedSignal (sig);
		}
		else {
		    task.sendContinue (sig);
		    return running;
		}
	    }
	    TaskState processSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processSyscalledEvent\n", task); 
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return runningInSyscall;
	    }
	    TaskState processTerminatingEvent (Task task, boolean signal,
					       int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return running;
	    }
	    TaskState processTerminatedEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatedEvent\n", task); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processExecedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processExecedEvent\n", task); 
		// Remove all tasks, retaining just this one.
		task.proc.retain (task);
		((LinuxProc)task.proc).getStat ().refresh();
		if (task.notifyExeced () > 0) {
		    return blockedContinue;
		}
		else {
		    task.sendContinue (0);
		    return running;
		}
	    }
    	    TaskState processDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} processDisappearedEvent\n", task); 
		return disappeared;
    	    }
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		task.sendStop ();
		return stopping;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
		return running;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} processPerformDetach\n", task); 
		task.sendStop ();
		return detaching;
	    }
	    TaskState processClonedEvent (Task task, Task clone)
	    {
		logger.log (Level.FINE, "{0} processClonedEvent\n", task); 
		if (task.notifyCloned (clone) > 0)
		    return blockedContinue;
		task.sendContinue (0);
		return running;
	    }
	    TaskState processForkedEvent (Task task, Task fork)
	    {
		logger.log (Level.FINE, "{0} processForkedEvent\n", task); 
		if (task.notifyForked (fork) > 0)
		    return blockedContinue;
		task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		logger.log (Level.FINE, "{0} processPerformAddObservation\n", task); 
		observation.add ();
		return running;
	    }
	    TaskState processRequestUnblock (Task task,
					     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} processRequestUnblock\n", task); 
		// XXX: What to do about a stray unblock?
		// observer.fail (new RuntimeException (task, "not blocked");
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
	    TaskState processSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processSyscalledEvent\n", task); 
		task.notifySyscallExit ();
		task.sendContinue (0);
		return running;
	    }
	    TaskState processTerminatingEvent (Task task, boolean signal,
					       int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return runningInSyscall;
	    }
	    TaskState processTerminatedEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatedEvent\n", task); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processRequestStop (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		task.sendStop ();
		return stoppingInSyscall;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
		return runningInSyscall;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} processPerformDetach\n", task); 
		task.sendStop ();
		return detachingInSyscall;
	    }
	};

    private static TaskState detaching = new TaskState ("detaching")
	{
	    TaskState processPerformAttach (Task task)
	    {
		logger.log (Level.FINE, "{0} processPerformAttach\n", task); 
		return attaching;
	    }
	    TaskState processStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processStoppedEvent\n", task); 
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processTerminatingEvent (Task task, boolean signal,
					       int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatingEvent\n", task); 
		if (signal)
		    task.sendDetach (value);
		else
		    task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processForkedEvent (Task task, Task fork)
	    {
		logger.log (Level.FINE, "{0} processForkedEvent\n", task);
		fork.sendDetach (0);
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processClonedEvent (Task task, Task clone)
	    {
		logger.log (Level.FINE, "{0} processClonedEvent\n", task);
		task.sendDetach (0);
		// Let the proc sort out the clone.
		task.proc.performTaskDetachCompleted (task, clone);
		return unattached;
	    }
	    TaskState processExecedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processExecedEvent\n", task);
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processPerformSignaled (Task task, int signal)
	    {
		logger.log (Level.FINE, "{0} processPerformSignaled\n", task);
		task.sendDetach (signal);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	};

    private static TaskState detachingInSyscall = new TaskState ("detachingInSyscall")
	{
	    TaskState processStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processStoppedEvent\n", task); 
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return unattached;
	    }
	    TaskState processSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processSyscalledEvent\n", task); 
		task.notifySyscallExit ();
		task.sendContinue (0);
		return detaching;
	    }
	    TaskState processTerminatingEvent (Task task, boolean signal,
					       int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatingEvent\n", task); 
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
	    TaskState processTrappedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processTrappedEvent\n", task); 
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
	    return "BlockedSignal,sig=" + sig;
	}
	TaskState processRequestUnblock (Task task, TaskObserver observer)
	{
	    logger.log (Level.FINE, "{0} processRequestUnblock\n", task); 
	    task.blockers.remove (observer);
	    if (task.blockers.size () > 0)
		return this; // Still blocked.
	    task.sendContinue (sig);
	    return running;
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
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		return stopped;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
		task.sendContinue (0);
		return running;
	    }
	    TaskState processRequestStepInstruction (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestStepInstruction\n", task); 
		task.sendStepInstruction (0);
		return stepping;
	    }
	    TaskState processPerformDetach (Task task)
	    {
		// XXX: Need to hang onto the signal that was about to
		// be delivered?
		logger.log (Level.FINE, "{0} processPerformDetach\n", task); 
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
		logger.log (Level.FINE, "{0} processRequestStop\n", task); 
		return paused;
	    }
	    TaskState processRequestContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
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
		logger.log (Level.FINE, "{0} processRequestContinue\n", task); 
		return unpaused;
	    }
	    TaskState processStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} processStoppedEvent\n", task); 
		task.sendContinue (0);
		return running;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		logger.log (Level.FINE, "{0} processPerformAddObservation\n", task); 
		observation.add ();
		return unpaused;
	    }
	};

    private static TaskState disappeared = new TaskState ("disappeared")
	{
	    TaskState processTerminatedEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatedEvent\n", task); 
		task.proc.remove (task);
		processAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState processTerminatingEvent (Task task, boolean signal,
					       int value)
	    {
		logger.log (Level.FINE, "{0} processTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		// 		if (signal)
		// 		    task.sendContinue (value);
		// 		else
		// 		    task.sendContinue (0);
		return disappeared;
	    }
    	    TaskState processDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} processDisappearedEvent\n", task); 
		return disappeared;
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
		logger.log (Level.FINE, "{0} processPerformAttach\n", task); 
		// Lie; the Proc wants to know that the operation has
		// been processed rather than the request was
		// successful.
		task.proc.performTaskAttachCompleted (task);
		return destroyed;
	    }
	    TaskState processPerformAddObservation (Task task,
						    Observation observation)
	    {
		logger.log (Level.FINE, "{0} processPerformAddObservation\n", task); 
		observation.fail (new RuntimeException ("unattached"));
		task.proc.performDeleteObservation (observation);
		return destroyed;
	    }
	};
}
