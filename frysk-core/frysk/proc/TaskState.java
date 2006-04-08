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
    static TaskState detachedState ()
    {
	return detached;
    }
    /**
     * Return the initial state of the Main task.
     */
    static TaskState mainState ()
    {
	return StartMainTask.waitForStop;
    }
    /**
     * Return the initial state of a cloned task.
     */
    static TaskState clonedState (TaskState parentState)
    {
	if (parentState == detaching)
	    return detaching;
	else if (parentState == running)
	    return StartMainTask.waitForStop;
	else
	    throw new RuntimeException ("clone's parent in unexpected state "
					+ parentState);
    }
    protected TaskState (String state)
    {
	super (state);
    }
    TaskState handleSignaledEvent (Task task, int sig)
    {
	throw unhandled (task, "handleSignaledEvent");
    }
    TaskState handleStoppedEvent (Task task)
    {
	throw unhandled (task, "handleStoppedEvent");
    }
    TaskState handleTrappedEvent (Task task)
    {
	throw unhandled (task, "handleTrappedEvent");
    }
    TaskState handleSyscalledEvent (Task task)
    {
	throw unhandled (task, "handleSyscalledEvent");
    }
    TaskState handleTerminatedEvent (Task task, boolean signal, int value)
    {
	throw unhandled (task, "handleTerminatedEvent");
    }
    TaskState handleTerminatingEvent (Task task, boolean signal, int value)
    {
	throw unhandled (task, "handleTerminatingEvent");
    }
    TaskState handleExecedEvent (Task task)
    {
	throw unhandled (task, "handleExecedEvent");
    }
    TaskState handleDisappearedEvent (Task task, Throwable w)
    {
	throw unhandled (task, "handleDisappearedEvent");
    }
    TaskState handleContinue (Task task)
    {
	throw unhandled (task, "RequestContinue");
    }
    TaskState handleRemoval (Task task)
    {
	throw unhandled (task, "handleRemoval");
    }
    TaskState handleAttach (Task task)
    {
	throw unhandled (task, "handleAttach");
    }
    TaskState handleDetach (Task task)
    {
	throw unhandled (task, "handleDetach");
    }
    TaskState handleClonedEvent (Task task, Task clone)
    {
	throw unhandled (task, "handleClonedEvent");
    }
    TaskState handleForkedEvent (Task task, Task fork)
    {
	throw unhandled (task, "handleForkedEvent");
    }
    TaskState handleUnblock (Task task, TaskObserver observer)
    {
	throw unhandled (task, "handleUnblock");
    }
    TaskState handleAddObserver (Task task, Observable observable,
				 Observer observer)
    {
	throw unhandled (task, "handleAddObserver");
    }
    TaskState handleDeleteObserver (Task task, Observable observable,
				    Observer observer)
    {
	throw unhandled (task, "handleDeleteObserver");
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
    protected static void handleAttachedTerminated (Task task, boolean signal,
						     int value)
    {
	logger.log (Level.FINE, "{0} handleAttachedTerminated\n", task); 
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
    private static final TaskState detached = new TaskState ("detached")
	{
	    TaskState handleRemoval (Task task)
	    {
		logger.log (Level.FINE, "{0} handleRemoval\n", task); 
		return destroyed;
	    }
	    TaskState handleAttach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		task.sendAttach ();
		return attaching;
	    }
	};

    /**
     * The task is in the process of being attached.
     */
    private static final TaskState attaching = new TaskState ("attaching")
	{
	    TaskState handleStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		return Attached.transitionTo (task);
	    }
	    TaskState handleTrappedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleTrappedEvent\n", task); 
		return Attached.transitionTo (task);
	    }
    	    TaskState handleDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		// Outch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		task.proc.performTaskAttachCompleted (task);
		task.proc.remove (task);
		return destroyed;
    	    }
	    TaskState handleTerminatedEvent (Task task, boolean signal,
					     int value)
    	    {
		logger.log (Level.FINE, "{0} processTerminatedEvent\n", task); 
		// Outch, the task terminated before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		task.proc.performTaskAttachCompleted (task);
		task.proc.remove (task);
		return destroyed;
    	    }
	    TaskState handleDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleDetach\n", task); 
		return detaching;
	    }
	};

    /**
     * The task is attached, and waiting to be either continued, or
     * unblocked.  This first continue is special, it is also the
     * moment that any observers get notified that the task has
     * transitioned into the attached state.
     */
    private static class Attached
	extends TaskState
    {
	private Attached (String name)
	{
	    super ("Attached." + name);
	}
	/**
	 * Transition to the initial attached sub-state.
	 */
	static TaskState transitionTo (Task task)
	{
	    logger.log (Level.FINE, "{0} Attached.transitionTo\n", task);
	    task.proc.performTaskAttachCompleted (task);
	    return Attached.waitForContinueOrUnblock;
	}
	/**
	 * In all Attached states, addObservation is allowed.
	 */
	TaskState handleAddObserver (Task task, Observable observable,
				     Observer observer)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task); 
	    observable.add (observer);
	    return this;
	}
	/**
	 * In all Attached states, deleteObservation is allowed.
	 */
	TaskState handleDeleteObserver (Task task, Observable observable,
					Observer observer)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
	    observable.delete (observer);
	    return this;
	}
	/**
	 * Once the task is both unblocked and continued, should
	 * transition to the running state.
	 */
	TaskState transitionToRunningState (Task task)
	{
	    task.sendSetOptions ();
	    if (task.notifyAttached () > 0)
		return blockedContinue;
	    else {
		task.sendContinue (0);
		return running;
	    }
	}
	/**
	 * Need either a continue or an unblock.
	 */
	private static final TaskState waitForContinueOrUnblock =
	    new Attached ("waitForContinueOrUnblock")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    return Attached.waitForContinueOrUnblock;
		}
		TaskState handleContinue (Task task)
		{
		    logger.log (Level.FINE, "{0} handleContinue\n", task); 
		    if (task.blockers.size () == 0)
			return transitionToRunningState (task);
		    return Attached.waitForUnblock;
		}
	    };
	/**
	 * Got continue, just need to clear the blocks.
	 */
	private static final TaskState waitForUnblock =
	    new Attached ("waitForUnblock")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    if (task.blockers.size () == 0)
			return transitionToRunningState (task);
		    return Attached.waitForUnblock;
		}
	    };
    }

    /**
     * Task just starting out, wait for it to both become ready, and
     * to be unblocked, before continuing.
     */
    static class StartMainTask
	extends TaskState
    {
	StartMainTask (String name)
	{
	    super ("StartMainTask." + name);
	}
	private static TaskState attemptAttach (Task task)
	{
	    logger.log (Level.FINE, "{0} attemptAttach\n", task); 
	    task.sendSetOptions ();
	    if (task.blockers.size () > 0) {
		return StartMainTask.blocked;
	    }
	    if (task.notifyAttached () > 0) {
		return blockedContinue;
	    }
	    task.sendContinue (0);
	    return running;
	}
	TaskState handleAddObserver (Task task, Observable observable,
				     Observer observer)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task); 
	    observable.add (observer);
	    return this;
	}
	
	private static final TaskState waitForStop =
	    new StartMainTask ("waitForStop")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    return StartMainTask.waitForStop;
		}
		TaskState handleTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return attemptAttach (task);
		}
		TaskState handleStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return attemptAttach (task);
		}
	    };
	
	private static final TaskState blocked = new StartMainTask ("blocked")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    return attemptAttach (task);
		}
	    };
    }

    /**
     * Keep the task running.
     */
    private static final TaskState running = new TaskState ("running")
	{
	    TaskState handleSignaledEvent (Task task, int sig)
	    {
		logger.log (Level.FINE, "{0} handleSignaledEvent\n", task); 
		if (task.notifySignaled (sig) > 0) {
		    return new BlockedSignal (sig);
		}
		else {
		    task.sendContinue (sig);
		    return running;
		}
	    }
	    TaskState handleSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
		task.notifySyscallEnter ();
		task.sendContinue (0);
		return runningInSyscall;
	    }
	    TaskState handleTerminatingEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return running;
	    }
	    TaskState handleTerminatedEvent (Task task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		task.proc.remove (task);
		handleAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState handleExecedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleExecedEvent\n", task); 
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
    	    TaskState handleDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		return disappeared;
    	    }
	    TaskState handleContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} handleContinue\n", task); 
		return running;
	    }
	    TaskState handleDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleDetach\n", task); 
		// Can't detach a running task, first need to stop it.
		task.sendStop ();
		return detaching;
	    }
	    TaskState handleClonedEvent (Task task, Task clone)
	    {
		logger.log (Level.FINE, "{0} handleClonedEvent\n", task); 
		if (task.notifyCloned (clone) > 0)
		    return blockedContinue;
		task.sendContinue (0);
		return running;
	    }
	    TaskState handleForkedEvent (Task task, Task fork)
	    {
		logger.log (Level.FINE, "{0} handleForkedEvent\n", task); 
		if (task.notifyForked (fork) > 0)
		    return blockedContinue;
		task.sendContinue (0);
		return running;
	    }
	    TaskState handleAddObserver (Task task, Observable observable,
					 Observer observer)
	    {
		logger.log (Level.FINE, "{0} handleAddObserver\n", task); 
		observable.add (observer);
		return running;
	    }
	    TaskState handleDeleteObserver (Task task, Observable observable,
					    Observer observer)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
		observable.delete (observer);
		return running;
	    }
	    TaskState handleUnblock (Task task,
				     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		// XXX: What to do about a stray unblock?
		// observer.fail (new RuntimeException (task, "not blocked");
		return running;
	    }
	};

    // Task is running inside a syscall.
    private static final TaskState runningInSyscall =
	new TaskState ("runningInSyscall")
	{
	    // XXX: We needn't look for signal events because the
	    // syscall will exit before we get the signal, however, we still.
	    // need to look for terminating and terminated events
	    // because an exit syscall will produce these events and
	    // never produce a syscall-exit event.

	    TaskState handleSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
		task.notifySyscallExit ();
		task.sendContinue (0);
		return running;
	    }
	    TaskState handleTerminatingEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return runningInSyscall;
	    }
	    TaskState handleTerminatedEvent (Task task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		task.proc.remove (task);
		handleAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState handleContinue (Task task)
	    {
		logger.log (Level.FINE, "{0} handleContinue\n", task); 
		return runningInSyscall;
	    }
	    TaskState handleDetach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleDetach\n", task); 
		task.sendStop ();
		return detachingInSyscall;
	    }
	};

    private static final TaskState detaching = new TaskState ("detaching")
	{
	    TaskState handleAttach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		return attaching;
	    }
	    TaskState handleStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		// This is what should happen, the task stops, the
		// task is detached.
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return detached;
	    }
	    TaskState handleTerminatingEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		// Oops, the task is terminating.  Skip over the
		// termination event allowing the stop or terminated
		// event behind it to bubble up.  Since nothing is
		// observing this task, no need to notify anything of
		// this event.
		if (signal)
		    task.sendContinue (value);
		else
		    task.sendContinue (0);
		return detaching;
	    }
	    TaskState handleTerminatedEvent (Task task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		task.proc.remove (task);
		// Lie, really just need to tell the proc that the
		// task is no longer lurking.
		task.proc.performTaskDetachCompleted (task);
		return destroyed;
	    }
	    TaskState handleForkedEvent (Task task, Task fork)
	    {
		logger.log (Level.FINE, "{0} handleForkedEvent\n", task);
		logger.log (Level.FINE, "... handleForkedEvent {0}\n", fork);
		// Oops, the task forked.  Skip that allowing the stop
		// event behind it to bubble up.  The owning proc will
		// have been informed of this via a separate code
		// path.
		task.sendContinue (0);
		return detaching;
	    }
	    TaskState handleClonedEvent (Task task, Task clone)
	    {
		logger.log (Level.FINE, "{0} handleClonedEvent\n", task);
		// Oops, the task cloned.  Skip that event allowing
		// the stop event behind it to bubble up.  The owning
		// proc will have been informed of this via a separate
		// code path.
		task.sendContinue (0);
		// XXX: What about telling the proc that the clone now
		// exists?
		return detaching;
	    }
	    TaskState handleExecedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleExecedEvent\n", task);
		// Oops, the [main] task did an exec.  Skip that event
		// allowing the stop event behind it to bubble up (I
		// hope there's a stop event?).
		task.sendContinue (0);
		return detaching;
	    }
	    TaskState handleSignaledEvent (Task task, int signal)
	    {
		logger.log (Level.FINE, "{0} handleSignaledEvent\n", task);
		// Oops, the task got the wrong signal.  Just continue
		// so that the stop event behind it can bubble up.
		task.sendContinue (signal);
		return detaching;
	    }
	};

    private static final TaskState detachingInSyscall =
	new TaskState ("detachingInSyscall")
	{
	    TaskState handleStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return detached;
	    }
	    TaskState handleSyscalledEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
		task.notifySyscallExit ();
		task.sendContinue (0);
		return detaching;
	    }
	    TaskState handleTerminatingEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		if (signal)
		    task.sendDetach (value);
		else
		    task.sendDetach (0);
		task.proc.performTaskDetachCompleted (task);
		return detached;
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
	TaskState handleUnblock (Task task, TaskObserver observer)
	{
	    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
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
    private static final TaskState blockedContinue = new BlockedSignal (0)
	{
	    public String toString ()
	    {
		return "blockedContinue";
	    }
	};

    private static final TaskState disappeared = new TaskState ("disappeared")
	{
	    TaskState handleTerminatedEvent (Task task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		task.proc.remove (task);
		handleAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    TaskState handleTerminatingEvent (Task task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		// 		if (signal)
		// 		    task.sendContinue (value);
		// 		else
		// 		    task.sendContinue (0);
		return disappeared;
	    }
    	    TaskState handleDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		return disappeared;
    	    }
	};

    private static final TaskState destroyed = new TaskState ("destroyed") 
	{
	    TaskState handleAttach (Task task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		// Lie; the Proc wants to know that the operation has
		// been processed rather than the request was
		// successful.
		task.proc.performTaskAttachCompleted (task);
		return destroyed;
	    }
	    TaskState handleAddObserver (Task task, Observable observable,
					 Observer observer)
	    {
		logger.log (Level.FINE, "{0} handleAddObserver\n", task); 
		observer.addFailed (task, new RuntimeException ("detached"));
		task.proc.requestDeleteObserver (task,
						 (TaskObservable) observable,
						 (TaskObserver) observer);
		return destroyed;
	    }
	    TaskState handleDeleteObserver (Task task, Observable observable,
					    Observer observer)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
		observable.delete (observer);
		return running;
	    }
	};
}
