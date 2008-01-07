// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.proc.live;

import frysk.proc.TaskObserver;
import frysk.proc.Observer;
import frysk.proc.Observable;
import frysk.proc.TaskObservation;
import frysk.proc.Breakpoint;
import java.util.Collection;
import java.util.Iterator;
import frysk.proc.Isa;
import java.util.logging.Level;
import frysk.proc.TaskObservable;
import frysk.sys.Signal;

/**
 * A Linux Task's State tracked using PTRACE.
 */

class LinuxPtraceTaskState extends State {
    LinuxPtraceTaskState(String state) {
	super(state);
    }

    LinuxPtraceTaskState handleSignaledEvent(LinuxPtraceTask task, int sig) {
	throw unhandled (task, "handleSignaledEvent");
    }
    LinuxPtraceTaskState handleStoppedEvent(LinuxPtraceTask task) {
	throw unhandled (task, "handleStoppedEvent");
    }
    LinuxPtraceTaskState handleTrappedEvent(LinuxPtraceTask task) {
	throw unhandled (task, "handleTrappedEvent");
    }
    LinuxPtraceTaskState handleSyscalledEvent(LinuxPtraceTask task) {
	throw unhandled (task, "handleSyscalledEvent");
    }
    LinuxPtraceTaskState handleTerminatedEvent(LinuxPtraceTask task,
					       boolean signal,
					       int value) {
	throw unhandled (task, "handleTerminatedEvent");
    }
    LinuxPtraceTaskState handleTerminatingEvent(LinuxPtraceTask task,
						boolean signal,
						int value) {
	throw unhandled(task, "handleTerminatingEvent");
    }
    LinuxPtraceTaskState handleExecedEvent(LinuxPtraceTask task) {
	throw unhandled(task, "handleExecedEvent");
    }
    LinuxPtraceTaskState handleDisappearedEvent(LinuxPtraceTask task, Throwable w) {
	throw unhandled(task, "handleDisappearedEvent");
    }
    LinuxPtraceTaskState handleContinue(LinuxPtraceTask task) {
	throw unhandled(task, "handleContinue");
    }
    LinuxPtraceTaskState handleRemoval(LinuxPtraceTask task) {
	throw unhandled(task, "handleRemoval");
    }
    LinuxPtraceTaskState handleAttach(LinuxPtraceTask task) {
	throw unhandled(task, "handleAttach");
    }
    LinuxPtraceTaskState handleDetach(LinuxPtraceTask task,
				      boolean shouldRemoveObservers) {
	throw unhandled(task, "handleDetach");
    }
    LinuxPtraceTaskState handleClonedEvent(LinuxPtraceTask task, LinuxPtraceTask clone) {
	throw unhandled(task, "handleClonedEvent");
    }
    LinuxPtraceTaskState handleForkedEvent(LinuxPtraceTask task, LinuxPtraceTask fork) {
	throw unhandled(task, "handleForkedEvent");
    }
    LinuxPtraceTaskState handleUnblock(LinuxPtraceTask task,
				       TaskObserver observer) {
	throw unhandled(task, "handleUnblock");
    }
    LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					      TaskObservation observation) {
	throw unhandled(task, "handleAddObservation");
    }
    LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
						 TaskObservation observation) {
	throw unhandled(task, "handleDeleteObservation");
    }

    /**
     * Return the initial state of a detached task.
     */
    static LinuxPtraceTaskState detachedState ()
    {
	return detached;
    }
    /**
     * Return the initial state of the Main task.
     */
    static LinuxPtraceTaskState mainState ()
    {
	return StartMainTask.wantToDetach;
    }
    /**
     * Return the initial state of a cloned task.
     */
    static LinuxPtraceTaskState clonedState(LinuxPtraceTaskState parentState) {
	if (parentState == detaching)
	    // XXX: Is this needed?  Surely the infant can detect that
	    // it should detach, and the parent handle that.
	    return detaching;
	else if (parentState == running
		 || parentState == inSyscallRunning)
	    return StartClonedTask.waitForStop;
	
	throw new RuntimeException ("clone's parent in unexpected state "
	                            + parentState);
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
    protected static void handleAttachedTerminated (LinuxPtraceTask task, boolean signal,
						    int value)
    {
	logger.log (Level.FINE, "{0} handleAttachedTerminated\n", task); 
	task.notifyTerminated (signal, value);
	// A process with no tasks is dead ...?
	if (task.getProc().taskPool.size () == 0) {
	    task.getProc().parent.remove (task.getProc());
	    task.getProc().getHost().remove (task.getProc());
	}
    }

    /**
     * The task isn't attached (it was presumably detected using a
     * probe of the system process list).
     */
    protected static final LinuxPtraceTaskState detached = new LinuxPtraceTaskState("detached")
	{
	    LinuxPtraceTaskState handleRemoval (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleRemoval\n", task); 
		return destroyed;
	    }
	    LinuxPtraceTaskState handleAttach (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		task.sendAttach ();
		return attaching;
	    }
	};

    /**
     * The task is in the process of being attached.
     */
    private static final LinuxPtraceTaskState attaching = new LinuxPtraceTaskState("attaching")
	{
	    private LinuxPtraceTaskState transitionToAttached (LinuxPtraceTask task, int signal)
	    {
		((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		return new Attached.WaitForContinueOrUnblock (signal);
	    }
	    LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		return transitionToAttached (task, 0);
	    }
	    LinuxPtraceTaskState handleSignaledEvent (LinuxPtraceTask task, int signal)
	    {
		logger.log (Level.FINE, "{0} handleSignaledEvent, signal: {1}\n ", new Object[] {task,new Integer(signal)}); 
		return transitionToAttached (task, signal);
	    }
	    LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleTrappedEvent\n", task); 
		return transitionToAttached (task, 0);
	    }
    	    LinuxPtraceTaskState handleDisappearedEvent (LinuxPtraceTask task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		// Ouch, the task disappeared before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		((LinuxPtraceProc)task.getProc()).remove (task);
		return destroyed;
    	    }
	    LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
					     int value)
    	    {
		logger.log (Level.FINE, "{0} processTerminatedEvent\n", task); 
		// Ouch, the task terminated before the attach
		// reached it, just abandon this one (but ack the
		// operation regardless).
		((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		((LinuxPtraceProc)task.getProc()).remove (task);
		return destroyed;
    	    }
	    LinuxPtraceTaskState handleDetach (LinuxPtraceTask task, boolean shouldRemoveObservers)
	    {
		logger.log (Level.FINE, "{0} handleDetach\n", task); 
		return detaching;
	    }

	    /**
	     * Unblocking in attaching state is really a noop since
	     * no observer should have been triggered yet, so no
	     * observer should be blocking yet (but we allow a stray
	     * unblock).
	     */
	    LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
				     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		// Sanity check
		if (task.blockers.remove(observer))
		  {
		    throw new RuntimeException
		      ("blocked observer in attaching state unblock? "
		       + observer);
		  }
		return this;
	    }

	    /**
	     * All observer can be added (but won't trigger yet) in
	     * attaching state.
	     */
	    LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					   TaskObservation observation)
	    {
	      logger.log (Level.FINE, "{0} handleAddObservation\n", task);
	      observation.add();
	      return this;
	    }
	  
	    /**
	     * Deleting an observer is always allowd in attaching state.
	     */
	    LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
					      TaskObservation observation)
	    {
	      logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
	      observation.delete();
	      return handleUnblock(task, observation.getTaskObserver());
	    }
	};

    /**
     * The task is attached, and waiting to be either continued, or
     * unblocked.  This first continue is special, it is also the
     * moment that any observers get notified that the task has
     * transitioned into the attached state.
     */
    private static class Attached extends LinuxPtraceTaskState {
	private Attached (String name)
	{
	    super ("Attached." + name);
	}
	/**
	 * In all Attached states, addObservation is allowed.
	 */
        LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
	    observation.add();
	    return this;
	}
	/**
	 * In all Attached states, deleteObservation is allowed.
	 */
        LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
	    observation.delete();
	    return handleUnblock(task, observation.getTaskObserver());
	}

        /**
	 * While attaching the LinuxPtraceTask disappeared, go to destroyed.
	 */
        LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
					 int value)
	{
	  logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task);
	  ((LinuxPtraceProc)task.getProc()).remove (task);
	  handleAttachedTerminated (task, signal, value);
	  return destroyed;
	}

	/**
	 * Once the task is both unblocked and continued, should
	 * transition to the running state.
	 */
        static LinuxPtraceTaskState transitionToRunningState(LinuxPtraceTask task, int signal)
	{
	    logger.log(Level.FINE, "transitionToRunningState\n");
	    task.sendSetOptions();
	    if (task.notifyAttached () > 0)
		return new BlockedSignal(signal, false);

	    return running.sendContinue(task, signal);
	}
	/**
	 * The blocked task has stopped, possibly with a pending
	 * signal, waiting on either a continue or an unblock.
	 */
	private static class WaitForContinueOrUnblock extends Attached {
	    final int signal;
	    WaitForContinueOrUnblock (int signal)
	    {
		super ("WaitForContinueOrUnblock");
		this.signal = signal;
	    }
	    LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
				     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		task.blockers.remove (observer);
		return Attached.waitForContinueOrUnblock;
	    }
	    LinuxPtraceTaskState handleContinue (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleContinue\n", task); 
		if (task.blockers.size () == 0)
		    return transitionToRunningState(task, signal);
		else
		    return new Attached.WaitForUnblock (signal);
	    }
	}
	private static final LinuxPtraceTaskState waitForContinueOrUnblock =
	    new Attached.WaitForContinueOrUnblock (0);

	/**
	 * Got continue, just need to clear the blocks.
	 */
	private static class WaitForUnblock extends Attached {
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
	    LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
				     TaskObserver observer)
	    {
		logger.log (Level.FINE, "{0} handleUnblock\n", task);
		logger.logp(Level.FINEST, "Attached.WaitForUnblock",
			    "handleUnblock", "{0} blockers",
			    new Integer(task.blockers.size()));
		task.blockers.remove (observer);
		if (task.blockers.size () == 0)
		    return transitionToRunningState(task, signal);
		return this;
	    }
	    LinuxPtraceTaskState handleDetach (LinuxPtraceTask task, boolean shouldRemoveObservers)
	    {
            
		logger.log (Level.FINE, "{0} handleDetach\n", task);
            
		if (shouldRemoveObservers)
		    task.removeObservers();
		// XXX: Otherwise check that observers are empty?
            
		task.sendDetach (0);
		((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
		return detached;
	    }
	}
    }

    /**
     * A new main Task, created via fork, just starting.  Go with the
     * assumption that it should detach.  Two events exist that
     * influence this assumption: the task stops; and the controlling
     * proc orders an attach.
     *
     * If the LinuxPtraceTask stops then, once the ForkedOffspring observers have
     * stopped blocking this, do the detach.  * ForkOffspring
     * observers of this).
     *
     * If, on the other hand, an attach order is received, change the
     * assumption to that the task should be attached and proceed on
     * that basis.
     */
    static class StartMainTask extends LinuxPtraceTaskState {
	StartMainTask (String name)
	{
	    super ("StartMainTask." + name);
	}
	/**
	 * StartMainTask out assuming that, once the process has
	 * stopped, ForkedOffspring observers have been notified, and
	 * all blocks have been removed, a detach should occure.
	 */
	static final LinuxPtraceTaskState wantToDetach =
	    new StartMainTask ("wantToDetach")
	    {
		LinuxPtraceTaskState handleAttach (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleAttach\n", task); 
		    ((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		    return StartMainTask.wantToAttach;
		}
		private LinuxPtraceTaskState blockOrDetach (LinuxPtraceTask task)
		{
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.detachBlocked;
		    task.sendDetach (0);
		    ((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
		    return detached;
		}
		LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrDetach (task);
		}
		LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrDetach (task);
		}
	    };
	/**
	 * The detaching LinuxPtraceTask has stopped and the ForkedOffspring
	 * observers have been notified; just waiting for all the
	 * ForkedOffspring blockers to be removed before finishing the
	 * detach.
	 */
	static final LinuxPtraceTaskState detachBlocked =
	    new StartMainTask ("detachBlocked")
	    {
		LinuxPtraceTaskState handleAttach (LinuxPtraceTask task)
		{
		    // Proc got around to telling us to be attached,
		    // since the task has already stopped, immediatly
		    // jump across to the blocked attached state
		    // (waiting for ForkedOffspring observers to
		    // unblock before finishing the attach).
		    logger.log (Level.FINE, "{0} handleAttach\n", task); 
		    ((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		    return StartMainTask.attachBlocked;
		}
		LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    logger.log (Level.FINER, "{0} handleUnblock number of blockers left {1}\n", new Object[]{task, new Integer(task.blockers.size())}); 

		    if (task.blockers.size () == 0) {
			// Ya! All the blockers have been removed.
			task.sendDetach (0);
			((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
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
	static final LinuxPtraceTaskState wantToAttach =
	    new StartMainTask ("wantToAttach")
	    {
		LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObservationr\n", task);
		    // XXX - This should most likely test needsSuspendedAction
		    observation.add();
		    return this;
		}
		LinuxPtraceTaskState blockOrAttach(LinuxPtraceTask task) {
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.attachBlocked;
		    return Attached.waitForContinueOrUnblock;
		}
		LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrAttach(task);
		}
		LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrAttach (task);
		}
		LinuxPtraceTaskState handleContinue (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleContinue\n", task);
		    return StartMainTask.wantToAttachContinue;
		}
	    };
	/**
	 * The task is all ready to run, but still waiting for it to
	 * stop so that it can be properly attached.
	 */
	static final LinuxPtraceTaskState wantToAttachContinue =
	    new StartMainTask ("wantToAttachContinue")
	    {
		LinuxPtraceTaskState blockOrAttachContinue(LinuxPtraceTask task,
							   int signal) {
		    // Mark this LinuxPtraceTask as just started.
		    // See Running.handleTrapped for more explanation. 
		    task.justStartedXXX = true;
		    if (task.notifyForkedOffspring () > 0)
			return StartMainTask.attachContinueBlocked;
		    return Attached.transitionToRunningState(task, signal);
		}
		LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return blockOrAttachContinue (task, 0);
		}
		LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return blockOrAttachContinue (task, 0);
		}
		LinuxPtraceTaskState handleSignaledEvent (LinuxPtraceTask task, int signal)
		{
		    logger.log (Level.FINE, "{0} handleSignaledEvent\n", task);
		    return blockOrAttachContinue (task, signal);
		}

	      // Adding or removing observers doesn't impact this state.
	      LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
						    TaskObservation to)
	      {
		logger.log (Level.FINE, "{0} handleAddObservation\n", task);
		to.add();
		return this;
	      }
	      LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
					      TaskObserver observer)
	      {
		logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		task.blockers.remove(observer);
		return this;
	      }
	    };
	/**
	 * The task has stopped; just waiting for all the blockers to
	 * be removed before finishing the attach.
	 */
	static final LinuxPtraceTaskState attachBlocked =
	    new StartMainTask ("attachBlocked")
	    {
		LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
		    observation.add();
		    return this;
                }
		LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    task.blockers.remove (observer);
		    if (task.blockers.size () == 0)
			return Attached.waitForContinueOrUnblock;
		    return StartMainTask.attachBlocked;
		}
		LinuxPtraceTaskState handleContinue (LinuxPtraceTask task)
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
	static final LinuxPtraceTaskState attachContinueBlocked =
	    new StartMainTask ("attachContinueBlocked")
	    {
		LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					       TaskObservation observation)
		{
		    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
		    observation.add();
		    return this;
		}
		LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
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
    static class StartClonedTask extends LinuxPtraceTaskState {
	StartClonedTask (String name)
	{
	    super ("StartClonedTask." + name);
	}
	private static LinuxPtraceTaskState attemptContinue (LinuxPtraceTask task)
	{
	    logger.log (Level.FINE, "{0} attemptContinue\n", task); 
	    task.sendSetOptions ();
	    if (task.notifyClonedOffspring () > 0)
		return StartClonedTask.blockedOffspring;
	    // XXX: Really notify attached here?
	    if (task.notifyAttached () > 0)
                return blockedContinue;
	    return running.sendContinue(task, 0);
	}
	LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
	    // XXX most likely need to check needsSuspendedAction
	    observation.add();
	    return this;
	}
	LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
	    // XXX most likely need to check needsSuspendedAction
	    observation.delete();
	    return this;
	}

	static final LinuxPtraceTaskState waitForStop =
	    new StartClonedTask ("waitForStop")
	    {
		LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
					 TaskObserver observer)
		{
		    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
		    // XXX: Should instead fail?
		    task.blockers.remove (observer);
		    return StartClonedTask.waitForStop;
		}
		LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
		    return attemptContinue (task);
		}
		LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
		{
		    logger.log (Level.FINE, "{0} handleStoppedEvent\n", task);
		    return attemptContinue (task);
		}
	    };
	
	private static final LinuxPtraceTaskState blockedOffspring =
	    new StartClonedTask ("blockedOffspring")
	    {
		LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
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
		    return running.sendContinue(task, 0);
		}
	    };
    }

    /**
     * Keep the task running.
     */
    private static class Running extends LinuxPtraceTaskState {
	// Whether or not we are running inside a syscall
	private final boolean insyscall;

	protected Running (String state,
			   boolean insyscall)
	{
	    super (state);
	    this.insyscall = insyscall;
	}
	
      void setupSteppingBreakpoint(LinuxPtraceTask task, long address)
      {
	// Reset pc, this should maybe be moved into the Breakpoint,
	// but if the breakpoint gets removed before we step it, and
	// the architecture puts the pc just behind the breakpoint
	// address, then there is no good other place to get at the
	// original pc location.
	task.setPC(address);

	// All logic for determining how and where to step the
	// Breakpoint is determined by Proc and
	// Breakpoint.prepareStep() (called in sendContinue).
	Breakpoint bp = Breakpoint.create(address, ((LinuxPtraceProc)task.getProc()));
	
	// TODO: This should really move us to a new TaskState.
	// Currently we rely on the Task.steppingBreakpoint
	// being set and the Breakpoint/Instruction having all
	// the state necessary.
	task.steppingBreakpoint = bp;
      }
      
        /**
	 * Tells the LinuxPtraceTask to continue, keeping in kind pending
	 * breakpoints, with or without syscall tracing.
	 * Returns the new Running (sub) state of the Task.
	 */
        Running sendContinue(LinuxPtraceTask task, int sig)
        {
	  Breakpoint bp = task.steppingBreakpoint;
	  logger.log (Level.FINE, "{0} sendContinue, {1}\n",
		      new Object[] { task, bp });
	  if (bp != null)
	    if (! bp.isInstalled())
	      {
		// Apparently the breakpoint was removed already
		// which means it was never stepped, stepDone()
		// will only do some bookkeeping, but not (re)set
		// the breakpoint.
		bp.stepDone(task);
		task.steppingBreakpoint = null;
		bp = null;
	      }
	    else
	      {
		// The breakpoint knows the instruction, address and
		// Proc and will negotiate with the Proc how to step
		// depending on the cababilities (canExecuteOutOfLine)
		// of the Instruction.
		bp.prepareStep(task);
	      }

	  
	  // Step when there is a breakpoint at the current location
	  // or there are Instruction observers installed.
	  if (bp != null
	      || task.instructionObservers.numberOfObservers() > 0)
	    {
	      task.sendStepInstruction(sig);
	      return stepping;
	    }
	  else
	    {
	      // Always reset this, only the first step is important.
	      // See Running.handleTrapped() for more.
	      task.justStartedXXX = false;
	      
	      if (task.syscallObservers.numberOfObservers() > 0)
		{
		  task.sendSyscallContinue(sig);
		  return this;
		}
	      else
		{
		  task.sendContinue(sig);
		  // If we were stepping, but no breakpoint step or instruction
		  // observers are installed we are running again.
		  return (this != stepping) ? this : running;
		}
	    }
        }

        /**
	 * Returns a blocked LinuxPtraceTaskState depending on whether we are
	 * syscall tracing the LinuxPtraceTask and being inside or outside a syscall.
	 */
        LinuxPtraceTaskState blockedContinue()
        {
	    if (insyscall)
		return syscallBlockedInSyscallContinue;
	    return blockedContinue;
	}

	LinuxPtraceTaskState handleSignaledEvent (LinuxPtraceTask task, int sig)
	{
	    logger.log (Level.FINE, "{0} handleSignaledEvent, signal: {1}\n", new Object[] {task, new Integer(sig)}); 
	    if (task.notifySignaled (sig) > 0) {
		return new BlockedSignal(sig, insyscall);
	    }
	    else
	      return sendContinue(task, sig);
	}
	LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
	{
	    Collection pendingObservations = task.pendingObservations;
	    // XXX Real stop event! - Do we want observers here?
	    // What state should the task be after being stopped?
	    if (pendingObservations.isEmpty())
		logger.log(Level.WARNING, "{0} Unhandled real stop event", task);

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
	    Running newState;
	    if (task.instructionObservers.numberOfObservers() > 0)
	      newState = insyscall ? inSyscallRunning : running;
	    else
	      newState = insyscall ? inSyscallRunning : running;

	    return newState.sendContinue(task, 0);
	}

	LinuxPtraceTaskState handleTerminatingEvent (LinuxPtraceTask task, boolean signal,
					  int value)
	{
	    logger.log(Level.FINE, "{0} handleTerminatingEvent\n", task); 
	    if (task.notifyTerminating (signal, value) > 0)
		{
		    if (signal)
			return new BlockedSignal(value, insyscall);
		    else
			return blockedContinue();
		}
        
	    if (signal)
	      return sendContinue(task, value);
	    else
	      return sendContinue(task, 0);
	}
	LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
					 int value)
	{
	    logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
	    ((LinuxPtraceProc)task.getProc()).remove (task);
	    handleAttachedTerminated (task, signal, value);
	    return destroyed;
	}
	LinuxPtraceTaskState handleExecedEvent (LinuxPtraceTask task)
	{
	    logger.log (Level.FINE, "{0} handleExecedEvent\n", task); 
	    // Remove all tasks, retaining just this one.
	    ((LinuxPtraceProc)task.getProc()).retain (task);
	    ((LinuxPtraceProc)task.getProc()).getStat().refresh();

	    // All breakpoints have been erased.  We need to explicitly
	    // tell those attached to the current Task.
	    ((LinuxPtraceProc)task.getProc()).breakpoints.removeAllCodeObservers();
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
		  return (task.syscallObservers.numberOfObservers() > 0
			    ? syscallBlockedInSyscallContinue
			    : blockedInExecSyscall);
		}
	    else
		{
		  // XXX logic is slightly confusing after exec() call.
		  sendContinue(task, 0);
		  return inSyscallRunning;
		}
	}
	LinuxPtraceTaskState handleDisappearedEvent (LinuxPtraceTask task, Throwable w)
	{
	    logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
	    return disappeared;
	}
	LinuxPtraceTaskState handleContinue (LinuxPtraceTask task)
	{
	    logger.log (Level.FINE, "{0} handleContinue\n", task); 
	    return this;
	}
	LinuxPtraceTaskState handleDetach (LinuxPtraceTask task, boolean shouldRemoveObservers)
	{
	    logger.log (Level.FINE, "{0} handleDetach\n", task); 
	    
	    if (shouldRemoveObservers)
		task.removeObservers();  
	    // XXX: Otherwise check if there are still observers and panic?
        
	    // Can't detach a running task, first need to stop it.                
	    task.sendStop ();
	    return detaching;
	}
	LinuxPtraceTaskState handleClonedEvent (LinuxPtraceTask task, LinuxPtraceTask clone)
	{
	    logger.log (Level.FINE, "{0} handleClonedEvent\n", task); 
	    if (task.notifyClonedParent (clone) > 0)
		return blockedContinue();
	    return sendContinue(task, 0);
	}
	LinuxPtraceTaskState handleForkedEvent (LinuxPtraceTask task, LinuxPtraceTask fork)
	{
	    logger.log (Level.FINE, "{0} handleForkedEvent\n", task); 
	    if (task.notifyForkedParent (fork) > 0)
		return blockedContinue();
	    return sendContinue(task, 0);
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
	LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
	{
	  logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
	  
	  Isa isa;
	  isa = task.getIsaFIXME();

	  // First see if this was just an indication the we stepped.
	  // And see if we were stepping a breakpoint.  Or whether we
	  // installed a breakpoint at the address.  Otherwise it is a
	  // real trap event and we should treat it like a trap
	  // signal.  There is a special case for bug #4663.  The
	  // first step onto the first instruction of a just started
	  // task sometimes doesn't set the right task stepped flag.
	  // So we check and immediately clear here.
	  if (isa.isTaskStepped(task) || task.justStartedXXX)
	    {
	      if (task.justStartedXXX)
		return stepping.handleTrappedEvent(task);
	      else
		{
		  System.err.println("Whoa! Wrong state for stepping: "
				     + this);
		  return sendContinue(task, 0);
		}
	    }
	  else
	    {
	      // Do we have a breakpoint installed here?
	      long address = isa.getBreakpointAddress(task);
	      int blockers = task.notifyCodeBreakpoint(address);
	      if (blockers >= 0)
		{
		  // Prepare for stepping the breakpoint
		  setupSteppingBreakpoint(task, address);
		  
		  if (blockers == 0)
		    return sendContinue(task, 0);
		  else
		    return blockedContinue();
		}
	      else {
		  // This is not a trap event generated by us.  And we
		  // also didn't send a step request.  Deliver the
		  // real Trap event to the Task.
		  return handleSignaledEvent(task, Signal.TRAP.intValue());
	      }
	    }
	}

	LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
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
	LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
					  TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
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
	LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task,
				 TaskObserver observer)
	{
	    logger.log (Level.FINE, "{0} handleUnblock\n", task); 
	    // XXX: What to do about a stray unblock?
	    // observer.fail (new RuntimeException (task, "not blocked");
	    return this;
	}

	LinuxPtraceTaskState handleSyscalledEvent(LinuxPtraceTask task)
	{
	    logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
	    if (task.syscallObservers.numberOfObservers() > 0)
		{
		    if (! insyscall && task.notifySyscallEnter() > 0)
			return syscallBlockedInSyscallContinue;
	    
		    if (insyscall && task.notifySyscallExit() > 0)
			return blockedContinue;

		    // Swap in/out syscall
		    Running newState = sendContinue(task, 0);
		    newState = (newState.insyscall
				? running
				: inSyscallRunning);
		    return newState;
		}
	    else
	      return sendContinue(task, 0);
	}
    }

  // LinuxPtraceTask is stepping (either because of instruction observers
  // or because of getting through a breakpoint).
  static final Stepping stepping = new Stepping();

  static final class Stepping extends Running {
    /**
     * Singleton indicating a stepping Running state
     * (not inside a syscall).
     */
    Stepping()
    {
      super("stepping", false);
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
    LinuxPtraceTaskState handleTrappedEvent (LinuxPtraceTask task)
    {
      logger.log (Level.FINE, "{0} handleTrappedEvent\n", task);
      
      Isa isa;
      isa = task.getIsaFIXME();
      
      // First see if this was just an indication the we stepped.
      // And see if we were stepping a breakpoint.  Or whether we
      // installed a breakpoint at the address.  Otherwise it is a
      // real trap event and we should treat it like a trap
      // signal.
      Breakpoint steppingBreakpoint = task.steppingBreakpoint;
      if (isa.isTaskStepped(task)
	  || steppingBreakpoint != null
	  || task.justStartedXXX)
	{
	  task.justStartedXXX = false;
	  
	  // Are we stepping a breakpoint? (This should be a new
	  // State).  Reset/Reinstall Intruction, all logic is in the
	  // Breakpoint and associated Instruction, which will fixup
	  // any registers for us.
	  if (steppingBreakpoint != null)
	    {
	      steppingBreakpoint.stepDone(task);
	      task.steppingBreakpoint = null;
	    }
	  
	  if (task.notifyInstruction() > 0)
	    return blockedContinue();
	  else
	    return sendContinue(task, 0);
	}
      else
	{
	  // Do we have a breakpoint installed here?
	  long address = isa.getBreakpointAddress(task);
	  int blockers = task.notifyCodeBreakpoint(address);
	  if (blockers >= 0)
	    {
	      // Sanity check
	      if (task.steppingBreakpoint != null)
		throw new RuntimeException("Already breakpoint stepping: "
					   + task.steppingBreakpoint);
	      
	      // Prepare for stepping the breakpoint
	      setupSteppingBreakpoint(task, address);
	      
	      if (blockers == 0)
		return sendContinue(task, 0);
	      else
		return blockedContinue();
	      
	    }
	  else
	    {
	      // This is not a trap event generated by us.
	      
	      // When we just send a step to the task there are
	      // two reasons we might get a Trap event that
	      // doesn't relate to an actual instruction
	      // step. Either we requested a step into a signal
	      // (which depending on architecture does or doesn't
	      // set the step debug flag).  Or a instruction that
	      // looks like it is generating a trap is encountered
	      // but that the kernel will handle (like a syscall
	      // trapping instruction) that we should ignore (this
	      // would be nice to use to support syscall tracking
	      // during stepping, but it doesn't happen on all
	      // architectures).
	      if ((task.sigSendXXX != 0
		   || task.syscallSigretXXX
		   || isa.hasExecutedSpuriousTrap(task)))
		return sendContinue(task, 0);
	      
	      // Deliver the real Trap event to the Task.  This is
	      // somewhat weird, we are either stepping a trapping
	      // instruction (breakpoint) that we didn't install, or
	      // something is sending this process an explicit trap
	      // signal.
	      return handleSignaledEvent(task, Signal.TRAP.intValue());
	    }
	}
    }

    private void checkBreakpointStepping(LinuxPtraceTask task)
    {
      // Since we were stepping we expected a trap event.
      // If we were stepping a breakpoint we have to check whether
      // or not the step occured before or after the breakpoint was
      // taken and make sure the breakpoint it put back in place.
      Breakpoint steppingBreakpoint = task.steppingBreakpoint;
      if (steppingBreakpoint != null)
	{
	  long pc = task.getPC();
	  long setupAddress = steppingBreakpoint.getSetupAddress();

	  // Check whether the breakpoint was actually stepped.
	  // In theory there are instructions that might not change the
	  // pc after execution, these are expected to not need fixups.
	  // If any of them would, then we can add an explicit abort()
	  // to Instruction so they can special case themselves.
	  if (pc != setupAddress)
	    steppingBreakpoint.stepDone(task);
	  else
	    steppingBreakpoint.stepAbort(task);
	}
    }

    LinuxPtraceTaskState handleSignaledEvent(LinuxPtraceTask task, int sig)
    {
      logger.log (Level.FINE, "{0} handleSignaledEvent, signal: {1}\n",
		  new Object[] {task, new Integer(sig)}); 
      checkBreakpointStepping(task);
      return super.handleSignaledEvent(task, sig);
    }

    LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
    {
      logger.log (Level.FINE, "{0} handleStoppedEvent\n", new Object[] {task}); 
      checkBreakpointStepping(task);
      return super.handleStoppedEvent(task);
    }
  }

    /**
     * Sharable instance of the running state.
     */
    protected static final Running running =
	new Running("running", false);

    // LinuxPtraceTask is running inside a syscall.
    protected static final Running inSyscallRunning =
	new Running("inSyscallRunning", true);

    protected static final LinuxPtraceTaskState detaching = new LinuxPtraceTaskState("detaching")
	{
	    LinuxPtraceTaskState handleAttach (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		return attaching;
	    }
	    LinuxPtraceTaskState handleStoppedEvent (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleStoppedEvent\n", task); 
		// This is what should happen, the task stops, the
		// task is detached.
		task.sendDetach (0);
		((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
		// FIXME: This static reference is totally bogus;
		// should be stored in the dynamic LinuxPtraceHost.
		frysk.proc.Task.taskStateDetached.notify(task);
		return detached;
	    }
	    LinuxPtraceTaskState handleTerminatingEvent (LinuxPtraceTask task, boolean signal,
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
	    LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		((LinuxPtraceProc)task.getProc()).remove (task);
		// Lie, really just need to tell the proc that the
		// task is no longer lurking.
		((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
		return destroyed;
	    }
	    LinuxPtraceTaskState handleDisappearedEvent (LinuxPtraceTask task, Throwable w)
	    {
		// Woops, it disappeared before we were really detached,
	        // pretend the detached happened anyway.
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		((LinuxPtraceProc)task.getProc()).remove (task);
		((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
		return destroyed;
	    }
	    LinuxPtraceTaskState handleForkedEvent (LinuxPtraceTask task, LinuxPtraceTask fork)
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
	    LinuxPtraceTaskState handleClonedEvent (LinuxPtraceTask task, LinuxPtraceTask clone)
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
	    LinuxPtraceTaskState handleExecedEvent (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleExecedEvent\n", task);
		// Oops, the [main] task did an exec.  Skip that event
		// allowing the stop event behind it to bubble up (I
		// hope there's a stop event?).
		task.sendContinue (0);
		return detaching;
	    }
	    LinuxPtraceTaskState handleSignaledEvent (LinuxPtraceTask task, int signal)
	    {
		logger.log (Level.FINE, "{0} handleSignaledEvent\n", task);
		// Oops, the task got the wrong signal.  Just continue
		// so that the stop event behind it can bubble up.
		task.sendContinue (signal);
		return detaching;
	    }
	    //XXX: why is this needed and why does it mean a syscallExit ?
	    LinuxPtraceTaskState handleSyscalledEvent (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleSyscalledEvent\n", task); 
		task.notifySyscallExit ();
		task.sendContinue (0);
		return detaching;
	    }
	};
  
    /**
     * The LinuxPtraceTask is blocked by a set of observers, remain in
     * this state until all the observers have unblocked themselves.
     * This state preserves any pending signal so that, once
     * unblocked, the signal is delivered.
     */
    private static class BlockedSignal extends LinuxPtraceTaskState {
        final int sig;
        final boolean insyscall;

	BlockedSignal(int sig, boolean insyscall)
	{
	    super ("BlockedSignal");
	    this.sig = sig;
	    this.insyscall = insyscall;
	}
	public String toString() {
	    return "BlockedSignal,sig=" + sig;
	}
	LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleAddObservation\n", task);
	    observation.add();
	    return this;
	}
	LinuxPtraceTaskState handleUnblock (LinuxPtraceTask task, TaskObserver observer)
	{
	    if (logger.isLoggable(Level.FINEST)) {
		logger.log(Level.FINEST, "{0} handleUnblock {1}\n",
			   new Object[] {task, observer});
	    } else {
		logger.log (Level.FINE, "{0} handleUnblock\n", task);
	    }
	  task.blockers.remove (observer);
	  if (task.blockers.size () > 0)
	    return this; // Still blocked.
	  Running newState;
	  if (task.instructionObservers.numberOfObservers() > 0)
	    newState = insyscall ? inSyscallRunning : running;
	  if (task.syscallObservers.numberOfObservers() > 0)
	    newState = insyscall ? inSyscallRunning : running;
	  else
	    newState = running;
	  return newState.sendContinue(task, sig);
	}
	
	LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task, TaskObservation observation)
	{
	    logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
	    observation.delete();
	    return handleUnblock(task, observation.getTaskObserver());
	}
      
	LinuxPtraceTaskState handleDetach (LinuxPtraceTask task, boolean shouldRemoveObservers)
	{
        
	    logger.log (Level.FINE, "{0} handleDetach\n", task);
        
	    if (shouldRemoveObservers)
		task.removeObservers();
	    // XXX: Otherwise check that observers are empty?
        
	    task.sendDetach (0);
	    ((LinuxPtraceProc)task.getProc()).performTaskDetachCompleted (task);
	    return detached;
	}

        LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
						int value)
	{
	    logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
	    ((LinuxPtraceProc)task.getProc()).remove (task);
	    handleAttachedTerminated (task, signal, value);
	    return destroyed;
	}
    }
    
    /**
     * The task is in the blocked state with no pending signal.
     * It is a common case that a task is blocked with no pending
     * signal so this instance can be shared.
     */
    private static final LinuxPtraceTaskState blockedContinue =
	new BlockedSignal(0, false)
	{
	    public String toString() {
		return "blockedContinue";
	    }
	};
    
    /**
     * A task blocked after SyscallEnter notification or while runningInSyscall
     * by an event other than syscalledEvent
     */
    static class SyscallBlockedInSyscall extends BlockedSignal{
	SyscallBlockedInSyscall(int sig)
	{
	    super(sig, true);
	}

	public String toString() {
	    return "SyscallBlockedInSyscall";
	}	    
    }
    
    /**
     * A shareable instance of SyscallBlockedInSyscall where the signal
     * to be delivered is 0.
     */
    private static final LinuxPtraceTaskState syscallBlockedInSyscallContinue = new SyscallBlockedInSyscall(0){
	    public String toString() {
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
    private static final LinuxPtraceTaskState blockedInExecSyscall =
	new BlockedSignal(0, true)
        {
	    public String toString() {
		return "blockedInExecSyscall";
	    }	    
	};
    
    private static final LinuxPtraceTaskState disappeared = new LinuxPtraceTaskState("disappeared")
	{
	    LinuxPtraceTaskState handleTerminatedEvent (LinuxPtraceTask task, boolean signal,
					     int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatedEvent\n", task); 
		((LinuxPtraceProc)task.getProc()).remove (task);
		handleAttachedTerminated (task, signal, value);
		return destroyed;
	    }
	    LinuxPtraceTaskState handleTerminatingEvent (LinuxPtraceTask task, boolean signal,
					      int value)
	    {
		logger.log (Level.FINE, "{0} handleTerminatingEvent\n", task); 
		task.notifyTerminating (signal, value);
		return disappeared;
	    }
    	    LinuxPtraceTaskState handleDisappearedEvent (LinuxPtraceTask task, Throwable w)
    	    {
		logger.log (Level.FINE, "{0} handleDisappearedEvent\n", task); 
		return disappeared;
    	    }
	};

    static LinuxPtraceTaskState getDestroyed()
    {
	return destroyed;
    }
    
    private static final LinuxPtraceTaskState destroyed
	= new LinuxPtraceTaskState("destroyed") {
	    LinuxPtraceTaskState handleAttach (LinuxPtraceTask task)
	    {
		logger.log (Level.FINE, "{0} handleAttach\n", task); 
		// Lie; the Proc wants to know that the operation has
		// been processed rather than the request was
		// successful.
		((LinuxPtraceProc)task.getProc()).performTaskAttachCompleted (task);
		return destroyed;
	    }
	    LinuxPtraceTaskState handleAddObservation(LinuxPtraceTask task,
					   TaskObservation observation)
	    {
		logger.log (Level.FINE, "{0} handleAddObservation\n", task);
		Observable observable = observation.getTaskObservable();
		Observer observer = observation.getTaskObserver();
		observer.addFailed (task, new RuntimeException ("destroyed"));
		((LinuxPtraceProc)task.getProc()).requestDeleteObserver (task,
						 (TaskObservable) observable,
						 (TaskObserver) observer);
		return destroyed;
	    }
	    LinuxPtraceTaskState handleDeleteObservation(LinuxPtraceTask task,
					      TaskObservation observation)
	    {
		logger.log (Level.FINE, "{0} handleDeleteObservation\n", task); 
		observation.delete();
		return destroyed;
	    }
	};
}
