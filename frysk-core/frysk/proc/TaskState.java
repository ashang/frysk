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

import java.util.Collection;
import java.util.Iterator;

import frysk.sys.Sig;

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
	return StartMainTask.wantToDetach;
    }
    /**
     * Return the initial state of a cloned task.
     */
    static TaskState clonedState (TaskState parentState)
    {
	if (parentState == detaching)
	    // XXX: Is this needed?  Surely the infant can detect that
	    // it should detach, and the parent handle that.
	    return detaching;
	else if (parentState == running
		 || parentState == inSyscallRunning
		 || parentState == syscallRunning
		 || parentState == inSyscallRunningTraced)
	    return StartClonedTask.waitForStop;
    
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
	throw unhandled (task, "handleContinue");
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
    TaskState handleAddObservation (Task task, TaskObservation observation)
    {
	throw unhandled (task, "handleAddObservation");
    }
    TaskState handleDeleteObservation (Task task, TaskObservation observation)
    {
	throw unhandled (task, "handleDeleteObservation");
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
	    private TaskState transitionToAttached (Task task, int signal)
	    {
		task.proc.performTaskAttachCompleted (task);
		return new Attached.WaitForContinueOrUnblock (signal);
	    }
	    TaskState handleStoppedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		return transitionToAttached (task, 0);
	    }
	    TaskState handleSignaledEvent (Task task, int signal)
	    {
		logger.log (Level.FINE, "{0} handleSignaledEvent, signal: {1}\n ", new Object[] {task,new Integer(signal)}); 
		return transitionToAttached (task, signal);
	    }
	    TaskState handleTrappedEvent (Task task)
	    {
		logger.log (Level.FINE, "{0} handleTrappedEvent\n", task); 
		return transitionToAttached (task, 0);
	    }
    	    TaskState handleDisappearedEvent (Task task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		// Ouch, the task disappeared before the attach
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
		// Ouch, the task terminated before the attach
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
	 * In all Attached states, addObservation is allowed.
	 */
        TaskState handleAddObservation(Task task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
	    observation.add();
	    return this;
	}
	/**
	 * In all Attached states, deleteObservation is allowed.
	 */
        TaskState handleDeleteObservation(Task task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
	    observation.delete();
	    return this;
	}
	/**
	 * Once the task is both unblocked and continued, should
	 * transition to the running state.
	 */
        static TaskState transitionToRunningState(Task task, int signal)
	{
	    logger.log(Level.FINE, "transitionToRunningState\n");
	    task.sendSetOptions();
	    if (task.notifyAttached () > 0)
	      return new BlockedSignal(signal, false);
	    if (task.instructionObservers.numberOfObservers() > 0)
	      {
		task.sendStepInstruction(signal);
		return running;
	      }
	    else if (task.syscallObservers.numberOfObservers() > 0)
	      {
		task.sendSyscallContinue(signal);
		return syscallRunning;
	      }
	    else
	      {
		task.sendContinue(signal);
		return running;
	      }
	}
	/**
	 * The blocked task has stopped, possibly with a pending
	 * signal, waiting on either a continue or an unblock.
	 */
	private static class WaitForContinueOrUnblock
	    extends Attached
	{
	    final int signal;
	    WaitForContinueOrUnblock (int signal)
	    {
		super ("WaitForContinueOrUnblock");
		this.signal = signal;
	    }
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
		  return transitionToRunningState(task, signal);
		else
		  return new Attached.WaitForUnblock (signal);
	    }
	}
	private static final TaskState waitForContinueOrUnblock =
	    new Attached.WaitForContinueOrUnblock (0);

	/**
	 * Got continue, just need to clear the blocks.
	 */
	private static class WaitForUnblock
	    extends Attached
	{
	    final int signal;

	    WaitForUnblock (int signal)
	    {
		super ("WaitForUnblock");
		this.signal = signal;
	    }
	    public String toString ()
	    {
		if (signal == 0)
		    return super.toString ();
		else
		    return super.toString () + ",signal=" + signal;
	    }
	    TaskState handleUnblock (Task task,
				     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		task.blockers.remove (observer);
		if (task.blockers.size () == 0)
		  return transitionToRunningState(task, signal);
		return this;
	    }
	}
    }

    /**
     * A new main Task, created via fork, just starting.  Go with the
     * assumption that it should detach.  Two events exist that
     * influence this assumption: the task stops; and the controlling
     * proc orders an attach.
     *
     * If the Task stops then, once the ForkedOffspring observers have
     * stopped blocking this, do the detach.  * ForkOffspring
     * observers of this).
     *
     * If, on the other hand, an attach order is received, change the
     * assumption to that the task should be attached and proceed on
     * that basis.
     */
    static class StartMainTask
	extends TaskState
    {
	StartMainTask (String name)
	{
	    super ("StartMainTask." + name);
	}
	/**
	 * StartMainTask out assuming that, once the process has
	 * stopped, ForkedOffspring observers have been notified, and
	 * all blocks have been removed, a detach should occure.
	 */
	private static TaskState wantToDetach =
	    new StartMainTask ("wantToDetach")
	    {
		TaskState handleAttach (Task task)
		{
		    logger.log (Level.FINE, "{0} handleAttach\n", task); 
		    task.proc.performTaskAttachCompleted (task);
		    return StartMainTask.wantToAttach;
		}
		private TaskState blockOrDetach (Task task)
		{
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.detachBlocked;
		    task.sendDetach (0);
		    task.proc.performTaskDetachCompleted (task);
		    return detached;
		}
		TaskState handleTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrDetach (task);
		}
		TaskState handleStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrDetach (task);
		}
	    };
	/**
	 * The detaching Task has stopped and the ForkedOffspring
	 * observers have been notified; just waiting for all the
	 * ForkedOffspring blockers to be removed before finishing the
	 * detach.
	 */
	private static TaskState detachBlocked =
	    new StartMainTask ("detachBlocked")
	    {
		TaskState handleAttach (Task task)
		{
		    // Proc got around to telling us to be attached,
		    // since the task has already stopped, immediatly
		    // jump across to the blocked attached state
		    // (waiting for ForkedOffspring observers to
		    // unblock before finishing the attach).
		    logger.log (Level.FINE, "{0} handleAttach\n", task); 
		    task.proc.performTaskAttachCompleted (task);
		    return StartMainTask.attachBlocked;
		}
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
            logger.log (Level.FINER, "{0} handleUnblock number of blockers left {1}\n", new Object[]{task, new Integer(task.blockers.size())}); 

            if (task.blockers.size () == 0) {
		      // Ya! All the blockers have been removed.
		      task.sendDetach (0);
		      task.proc.performTaskDetachCompleted (task);
		      return detached;
		    }
		    return StartMainTask.detachBlocked;
		}
	    };
	/**
	 * The process has told this task that it must attach; before
	 * advancing need to wait for the task to stop; once that
	 * occures any ForkedOffspring observers can be notififed and
	 * the task progress to being attached.
	 */
	private static TaskState wantToAttach =
	    new StartMainTask ("wantToAttach")
	    {
		TaskState handleAddObservation(Task task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
		    // XXX - This should most likely test needsSuspendedAction
		    observation.add();
		    return this;
		}
		TaskState blockOrAttach (Task task)
		{
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.attachBlocked;
		    return Attached.waitForContinueOrUnblock;
		}
		TaskState handleTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrAttach (task);
		}
		TaskState handleStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrAttach (task);
		}
		TaskState handleContinue (Task task)
		{
		    logger.log (Level.FINE, "{0} handleContinue\n", task);
		    return StartMainTask.wantToAttachContinue;
		}
	    };
	/**
	 * The task is all ready to run, but still waiting for it to
	 * stop so that it can be properly attached.
	 */
	private static TaskState wantToAttachContinue =
	    new StartMainTask ("wantToAttachContinue")
	    {
		TaskState blockOrAttachContinue (Task task, int signal)
		{
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.attachContinueBlocked;
		    return Attached.transitionToRunningState(task, signal);
		}
		TaskState handleTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrAttachContinue (task, 0);
		}
		TaskState handleStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrAttachContinue (task, 0);
		}
		TaskState handleSignaledEvent (Task task, int signal)
		{
		    logger.log (Level.FINE, "{0} handleSignaledEvent\n", task);
		    return blockOrAttachContinue (task, signal);
		}
	    };
	/**
	 * The task has stopped; just waiting for all the blockers to
	 * be removed before finishing the attach.
	 */
	private static TaskState attachBlocked =
	    new StartMainTask ("attachBlocked")
	    {
		TaskState handleAddObservation(Task task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
		    observation.add();
		    return this;
                }
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    if (task.blockers.size () == 0)
			return Attached.waitForContinueOrUnblock;
		    return StartMainTask.attachBlocked;
		}
		TaskState handleContinue (Task task)
		{
		    logger.log (Level.FINE, "{0} handleContinue\n", task);
		    return StartMainTask.attachContinueBlocked;
		}
	    };
	/**
	 * The task has stopped; all ready to get the task running
	 * only the ForkedOffspring observer blocked it.  Need to wait
	 * for that to clear before continuing on to the properly
	 * attached state.
	 */
	private static TaskState attachContinueBlocked =
	    new StartMainTask ("attachContinueBlocked")
	    {
		TaskState handleAddObservation(Task task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
		    observation.add();
		    return this;
		}
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task);
		    task.blockers.remove (observer);
		    if (task.blockers.size () > 0)
			return StartMainTask.attachContinueBlocked;
		    return Attached.transitionToRunningState(task, 0);
		}
	    };
    }

    /**
     * A cloned task just starting out, wait for it to stop, and for
     * it to be unblocked.  A cloned task is never continued.
     */
    static class StartClonedTask
	extends TaskState
    {
	StartClonedTask (String name)
	{
	    super ("StartClonedTask." + name);
	}
	private static TaskState attemptContinue (Task task)
	{
	    logger.log (Level.FINE, "{0} attemptContinue\n", task); 
	    task.sendSetOptions ();
	    if (task.notifyClonedOffspring () > 0)
		return StartClonedTask.blockedOffspring;
	    // XXX: Really notify attached here?
	    if (task.notifyAttached () > 0)
                return blockedContinue;
	    // XXX - What about syscall or instruction observers?
	    task.sendContinue (0);
	    return running;
	}
	TaskState handleAddObservation(Task task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
	    // XXX most likely need to check needsSuspendedAction
	    observation.add();
	    return this;
	}
	TaskState handleDeleteObservation(Task task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
	    // XXX most likely need to check needsSuspendedAction
	    observation.delete();
	    return this;
	}

    private static final TaskState waitForStop =
	    new StartClonedTask ("waitForStop")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    // XXX: Should instead fail?
		    task.blockers.remove (observer);
		    return StartClonedTask.waitForStop;
		}
		TaskState handleTrappedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return attemptContinue (task);
		}
		TaskState handleStoppedEvent (Task task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return attemptContinue (task);
		}
	    };
	
	private static final TaskState blockedOffspring =
	    new StartClonedTask ("blockedOffspring")
	    {
		TaskState handleUnblock (Task task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    if (task.blockers.size () > 0)
			return StartClonedTask.blockedOffspring;
		    // XXX: Really notify attached here?
		    if (task.notifyAttached () > 0)
		      {
			return blockedContinue;
		      }
		    if (task.instructionObservers.numberOfObservers() > 0)
		      {
			task.sendStepInstruction(0);
			return running;
		      }
		    else if (task.syscallObservers.numberOfObservers() > 0)
		      {
			task.sendSyscallContinue (0);
			return syscallRunning;
		      }
		    else
		      {
			task.sendContinue (0);
			return running;
		      }
		}
	    };
    }

    /**
     * Keep the task running.
     */
    private static class Running
	extends TaskState
    {
      // Whether or not we are tracing syscalls
      private final boolean syscalltracing;

      // Whether or not we are running inside a syscall
      private final boolean insyscall;

      protected Running (String state,
			 boolean syscalltracing, boolean insyscall)
	{
	    super (state);
	    this.syscalltracing = syscalltracing;
	    this.insyscall = insyscall;
	}
	
        /**
	 * Tells the Task to continue, with or without syscall tracing.
	 */
        private void sendContinue(Task task, int sig)
        {
	  if (task.instructionObservers.numberOfObservers() > 0)
	    task.sendStepInstruction(sig);
	  else if (syscalltracing)
	    task.sendSyscallContinue(sig);
	  else
	    task.sendContinue(sig);
        }

        /**
	 * Returns a blocked TaskState depending on whether we are
	 * syscall tracing the Task and being inside or outside a syscall.
	 */
        private TaskState blockedContinue()
        {
	  if (insyscall)
	    return syscallBlockedInSyscallContinue;
	  return blockedContinue;
	}

	TaskState handleSignaledEvent (Task task, int sig)
	{
	    logger.log (Level.FINE, "{0} handleSignaledEvent, signal: {1}\n", new Object[] {task, new Integer(sig)}); 
	    if (task.notifySignaled (sig) > 0) {
	      return new BlockedSignal(sig, insyscall);
	    }
	    else {
	        sendContinue(task, sig);
		return this;
	    }
	}
	TaskState handleStoppedEvent (Task task)
	{
	  Collection pendingObservations = task.pendingObservations;
	  // XXX Real stop event! - Do we want observers here?
	  // What state should the task be after being stopped?
	  if (pendingObservations.isEmpty())
	    logger.log(Level.WARNING, "{1} Unhandled real stop event", task);

	  Iterator it = pendingObservations.iterator();
	  while (it.hasNext())
	    {
	      TaskObservation observation = (TaskObservation) it.next();
	      if (observation.isAddition())
		observation.add();
	      else
		observation.delete();
	      it.remove();
	    }

	  // the observation.add() could have added a block.
	  if (task.blockers.size () > 0)
	    return blockedContinue();

	  // See how to continue depending on the kind of observers.
	  if (task.instructionObservers.numberOfObservers() > 0)
	    {
	      task.sendStepInstruction(0);
	      return insyscall ? inSyscallRunning : running;
	    }
	  else if (task.syscallObservers.numberOfObservers() > 0)
	    {
	      task.sendSyscallContinue(0);
	      return insyscall ? inSyscallRunningTraced : syscallRunning;
	    }
	  else
	    {
	      task.sendContinue(0);
	      return insyscall ? inSyscallRunning : running;
	    }
	}

	TaskState handleTerminatingEvent (Task task, boolean signal,
					  int value)
	{
	  logger.log(Level.FINE, "{0} handleTerminatingEvent\n", task); 
	  if(task.notifyTerminating (signal, value) > 0)
	    {
	      if (signal)
		return new BlockedSignal(value, insyscall);
	      else
		return blockedContinue();
	    }
        
	  if (signal)
	    sendContinue(task, value);
	  else
	    sendContinue(task, 0);
        
	  return this;
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

	    // All breakpoints have been erased.  We need to explicitly
	    // tell those attached to the current Task.
	    task.proc.breakpoints.removeAllCodeObservers();
	    Iterator it = task.codeObservers.iterator();
	    while (it.hasNext())
	      ((TaskObserver.Code) it.next()).deletedFrom(task);

	    // XXX - Do we really need to remove all?
	    // Remove just the code observers?
	    it = task.pendingObservations.iterator();
	    while (it.hasNext())
	      ((TaskObservation) it.next()).delete();

	    if (task.notifyExeced () > 0)
	      {
		return (syscalltracing
			? syscallBlockedInSyscallContinue
			: blockedInExecSyscall);
	      }
	    else
	      {
		if (task.instructionObservers.numberOfObservers() > 0)
		  {
		    task.sendStepInstruction(0);
		    return inSyscallRunning;
		  }
		if (syscalltracing)
		  {
		    task.sendSyscallContinue(0);
		    return inSyscallRunningTraced;
		  }
		else
		  {
		    sendContinue(task, 0);
		    return inSyscallRunning;
		  }
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
	    return this;
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
	    if (task.notifyClonedParent (clone) > 0)
	      return blockedContinue();
	    sendContinue(task, 0);
	    return this;
	}
	TaskState handleForkedEvent (Task task, Task fork)
	{
	    logger.log (Level.FINE, "{0} handleForkedEvent\n", task); 
	    if (task.notifyForkedParent (fork) > 0)
	      return blockedContinue();
	    sendContinue(task, 0);
	    return this;
	}

      /**
       * Handles traps caused by breakpoints or instruction
       * stepping. If there are any Code observers at the address of
       * the trap they get notified. If none of the Code observers
       * blocks we continue over the breakpoint (breakpoint stepping
       * state), otherwise we block till all blocking observers are
       * happy (breakpoint stopped state). If there are no Code observers
       * installed at the address, but we are stepping then all instruction
       * observers are notified. Otherwise it is a real trap event and we
       * pass it on to the task itself.
       */
      TaskState handleTrappedEvent (Task task)
      {
	logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);

	// To be fully correct we should also check that the 'current'
	// instruction is right 'after' the breakpoint. Otherwise it could
	// actually be a trap generated by the instruction we are currently
	// stepping. FIXME.
	Breakpoint steppingBreakpoint = task.steppingBreakpoint;
	if (steppingBreakpoint != null)
	  {
	    steppingBreakpoint.stepDone(task);
	    task.steppingBreakpoint = null;
	    sendContinue(task, 0);
	    return this;
	  }

	long address;
	try
	  {
	    address = task.getIsa().getBreakpointAddress(task);
	  }
	catch (TaskException tte)
	  {
	    // XXX - Now what - did the process die suddenly?
	    throw new RuntimeException(tte);
	  }

	int blockers = task.notifyCodeBreakpoint(address);
	if (blockers == -1)
	  {
	    // Maybe we were stepping this Task
	    if (task.instructionObservers.numberOfObservers() > 0)
	      {
		if (task.notifyInstruction() > 0)
		  return blockedContinue();
		else
		  {
		    sendContinue(task, 0);
		    return this;
		  }
	      }
	    else
	      {
		// This is not a trap event generated by us.
		return handleSignaledEvent (task, Sig.TRAP_);
	      }
	  }
	else if (blockers == 0)
	  {
	    try
	      {
		Breakpoint bp = Breakpoint.create(address, task.getProc());
		bp.prepareStep(task);
		task.sendStepInstruction(0);
		task.steppingBreakpoint = bp;
		return this;
	      }
	    catch (TaskException te)
	      {
		// Argh, major trouble! No way to recover from this one...
		throw new RuntimeException(te);
	      }
	  }
	else
	  return blockedContinue();
      }

	TaskState handleAddObservation(Task task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
	    if (! observation.needsSuspendedAction())
	      observation.add();
	    else
	      {
		Collection pending = task.pendingObservations;
		if (pending.isEmpty())
		  task.sendStop();
		pending.add(observation);
	      }
	    return this;
	}
	TaskState handleDeleteObservation(Task task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
	    if (! observation.needsSuspendedAction())
	      observation.delete();
	    else
	      {
		Collection pending = task.pendingObservations;
		if (pending.isEmpty())
		  task.sendStop();
		pending.add(observation);
	      }
	    return this;
	}
	TaskState handleUnblock (Task task,
				 TaskObserver observer)
	{
	    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
	    // XXX: What to do about a stray unblock?
	    // observer.fail (new RuntimeException (task, "not blocked");
	    return this;
	}

      TaskState handleSyscalledEvent(Task task)
      {
	logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
	if (syscalltracing)
	  {
	    if (! insyscall && task.notifySyscallEnter() > 0)
	      return syscallBlockedInSyscallContinue;
	    
	    if (insyscall && task.notifySyscallExit() > 0)
	      return blockedContinue;

	    sendContinue(task, 0);
	    return insyscall ? syscallRunning : inSyscallRunningTraced;
	  }
	else
	  {
	    sendContinue(task, 0);
	    return this;
	  }
      }
    }
  
    /**
     * Sharable instance of the running state.
     */
    private static final TaskState running =
      new Running("running", false, false);

    /**
     * Sharable instance of the syscallRunning state.
     */
    private static final TaskState syscallRunning =
      new Running("syscallRunning", true, false);
    
    // Task is running inside a syscall.
    private static final TaskState inSyscallRunning =
      new Running("inSyscallRunning", true, false);

    // Task is running inside a syscall.
    private static final TaskState inSyscallRunningTraced =
      new Running("inSyscallRunningTraced", true, true);

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
	    //XXX: why is this needed and why does it mean a syscallExit ?
	    TaskState handleSyscalledEvent (Task task)
	    {
	      logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
	      task.notifySyscallExit ();
	      task.sendContinue (0);
	      return detaching;
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
        final int sig;
        final boolean insyscall;

      BlockedSignal(int sig, boolean insyscall)
	{
	    super ("BlockedSignal");
	    this.sig = sig;
	    this.insyscall = insyscall;
	}
	public String toString ()
	{
	    return "BlockedSignal,sig=" + sig;
	}
	TaskState handleAddObservation(Task task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObserver\n", task);
	    observation.add();
	    return this;
	}
	TaskState handleUnblock (Task task, TaskObserver observer)
	{
	    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
	    task.blockers.remove (observer);
	    if (task.blockers.size () > 0)
		return this; // Still blocked.
	    if (task.instructionObservers.numberOfObservers() > 0)
	      {
		task.sendStepInstruction(sig);
		return running;
	      }
	    if (task.syscallObservers.numberOfObservers() > 0)
	      {
		task.sendSyscallContinue(sig);
		return insyscall ? inSyscallRunningTraced : syscallRunning;
	      }
	    else
	      {
		task.sendContinue (sig);
		return running;
	      }
	}
	
      TaskState handleDeleteObservation(Task task, TaskObservation observation)
      {
        logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
        observation.delete();
        return this;
      }
      
      TaskState handleDetach (Task task)
      {
        
        logger.log (Level.FINE, "{0} handleDetach\n", task);
        
        task.sendDetach (0);
        task.proc.performTaskDetachCompleted (task);
        return detached;
      }
    }
    
    /**
     * The task is in the blocked state with no pending signal.
     * It is a common case that a task is blocked with no pending
     * signal so this instance can be shared.
     */
  private static final TaskState blockedContinue =
      new BlockedSignal(0, false)
    {
        public String toString ()
        {
        return "blockedContinue";
        }
    };
    
    /**
     * A task blocked after SyscallEnter notification or while runningInSyscall
     * by an event other than syscalledEvent
     */
    public static class SyscallBlockedInSyscall extends BlockedSignal{
      SyscallBlockedInSyscall(int sig)
      {
        super(sig, true);
      }

	    public String toString ()
	    {
		return "SyscallBlockedInSyscall";
	    }	    
	}
    
    /**
     * A shareable instance of SyscallBlockedInSyscall where the signal
     * to be delivered is 0.
     */
  private static final TaskState syscallBlockedInSyscallContinue = new SyscallBlockedInSyscall(0){
        public String toString ()
        {
        return "syscallBlockedInSyscall";
        }
    };
    
    /**
     * A task recieves an execedEvent and the client decides to block
     * the task -> this state. This state is needed because if in this state
     * the client desides to switch over to syscall tacing mode the next event
     * is expected to be a syscall event representing the exec syscall exit not
     * entry. so if the switch to syscall tracing mode occurs the correct state
     * (runningInSyscall) is returned
     */
    private static final TaskState blockedInExecSyscall =
      new BlockedSignal(0, true)
        {
	    public String toString ()
	    {
		return "blockedInExecSyscall";
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
	    TaskState handleAddObservation(Task task,
					   TaskObservation observation)
	    {
		logger.log (Level.FINE, "{0} handleAddObserver\n", task);
		Observable observable = observation.getTaskObservable();
		Observer observer = observation.getTaskObserver();
		observer.addFailed (task, new RuntimeException ("detached"));
		task.proc.requestDeleteObserver (task,
						 (TaskObservable) observable,
						 (TaskObserver) observer);
		return destroyed;
	    }
	    TaskState handleDeleteObservation(Task task,
					      TaskObservation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObserver\n", task); 
		observation.delete();
		return destroyed;
	    }
	};
}
