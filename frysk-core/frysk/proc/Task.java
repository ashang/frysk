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

import java.util.LinkedList;
import inua.eio.ByteBuffer;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;

abstract public class Task
{
    protected static final Logger logger = Logger.getLogger (Config.FRYSK_LOG_ID);
    protected TaskId id;
    protected Proc proc;

    /**
     * Return the task's corresponding TaskId.
     */
    public final TaskId getTaskId ()
    {
	return id;
    }

    /**
     * Return the task's process id.
     */
    public final int getTid ()
    {
	return id.id;
    }

    /**
     * Return the task's (derived) name
     */
    public final String getName ()
    {
	return "Task " + getTid();
    }
    
    /**
     * Returns this Task's Instruction Set Architecture.
     */
    public final Isa getIsa ()
    {
	if (isa == null)
	    isa = sendrecIsa ();
	return isa;
    }
    /**
     * This Task's Instruction Set Architecture.
     */
    private Isa isa;
    /**
     * Fetch this Task's Instruction Set Architecture.
     */
    abstract protected Isa sendrecIsa ();

    /**
     * Return the task's entry point address.
     *
     * This is the address of the first instruction that the task will
     * have executed.
     *
     * XXX: Not yet implemented.
     */
    public long getEntryPointAddress ()
    {
	return 0xdeadbeefL;
    }

    /**
     * Return the containing Proc.
     */
    public Proc getProc ()
    {
	return proc;
    }

    // Contents of a task.
    ByteBuffer memory;
    ByteBuffer[] registerBank;

    // XXX: Should be eliminated.  Flags indicating the intended state
    // of various trace options.  Typically the thread has to first be
    // stopped before the option can change -> number of state
    // transitions.
    public boolean traceSyscall;  	// Trace syscall entry and exit

    /**
     * Create a new Task skeleton.
     */
    private Task (TaskId id, Proc proc)
    {
	this.proc = proc;
	this.id = id;
	proc.add (this);
	proc.host.add (this);
    }
    /**
     * Create a new unattached Task.
     */
    protected Task (Proc proc, TaskId id)
    {
	this (id, proc);
	newState = TaskState.unattachedState ();
	logger.log (Level.FINE, "{0} new -- create unattached\n", this); 
    }
    /**
     * Create a new attached clone of Task.
     */
    protected Task (Task task, TaskId cloneId)
    {
	this (cloneId, task.proc);
	newState = TaskState.clonedState (task.getState ());
	logger.log (Level.FINE, "{0} new -- create attached clone\n", this); 
    }
    /**
     * Create a new attached main Task of Proc.
     */
    protected Task (Proc proc)
    {
	this (new TaskId (proc.getPid ()), proc);
	newState = TaskState.mainState ();
	logger.log (Level.FINE, "{0} new -- create attached main\n", this); 
    }

    // Send operation to corresponding underlying [kernel] task.
    protected abstract void sendContinue (int sig);
    protected abstract void sendStepInstruction (int sig);
    protected abstract void sendStop ();
    protected abstract void sendSetOptions ();
    protected abstract void sendAttach ();
    protected abstract void sendDetach (int sig);

    protected LinkedList queuedEvents = new LinkedList ();

    /**
     * The state of this task.  During a state transition newState is
     * NULL.
     */
    private TaskState oldState;
    private TaskState newState;
    /**
     * Return the current state while at the same time marking that
     * the state is in flux.  If a second attempt to change state
     * occures before the current state transition has completed,
     * barf.  XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    private TaskState oldState ()
    {
	if (newState == null)
	    throw new RuntimeException ("double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }
    /**
     * Return the current state.
     */
    TaskState getState ()
    {
	if (newState != null)
	    return newState;
	else
	    return oldState;
    }

    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    void performContinue ()
    {
	logger.log (Level.FINE, "{0} requestContinue\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processPerformContinue (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * This method is package local.  Only proc/ internals should be
     * making this request.
     */
    void performRemoval ()
    {
	logger.log (Level.FINE, "{0} performRemoval\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processPerformRemoval (Task.this);
		}
	    });
    }
    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been completed.  The task is left in the stopped state.
     */
    void performAttach ()
    {
	logger.log (Level.FINE, "{0} performAttach\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processPerformAttach (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     */
    void performDetach ()
    {
	logger.log (Level.FINE, "{0} performDetach\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processPerformDetach (Task.this);
		}
	    });
    }

    /**
     * (internal) This task cloned creating the new Task cloneArg.
     */
    void receiveClonedEvent (final Task cloneArg)
    {
	logger.log (Level.FINE, "{0} receiveClonedEvent\n", this);
	Manager.eventLoop.add (new TaskEvent ()
	    {
		Task clone = cloneArg;
		public void execute ()
		{
		    newState = oldState ().processClonedEvent (Task.this, clone);
		}
	    });
    }

    /**
     * (internal) This Task forked creating an entirely new child
     * process containing one (the fork) task.
     */
    void receiveForkedEvent (final Task forkArg)
    {
	logger.log (Level.FINE, "{0} receiveForkedEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		Task fork = forkArg;
		public void execute ()
		{
		    newState = oldState ().processForkedEvent (Task.this, fork);
		}
	    });
    }

    /**
     * (internal) This task stopped.
     */
    void receiveStoppedEvent ()
    {
	logger.log (Level.FINE, "{0} receiveStoppedEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processStoppedEvent (Task.this);
		}
	    });
    }
    /**
     * (internal) This task encountered a trap.
     */
    void receiveTrappedEvent ()
    {
	logger.log (Level.FINE, "{0} receiveTrappedEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processTrappedEvent (Task.this);
		}
	    });
    }
    /**
     * (internal) This task received a signal.
     */
    void receiveSignaledEvent (final int sigArg)
    {
	logger.log (Level.FINE, "{0} receiveSignaledEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		int sig = sigArg;
		public void execute ()
		{
		    newState = oldState ().processPerformSignaled (Task.this, sig);
		}
	    });
    }

    /**
     * (internal) The task is in the process of terminating.  If
     * SIGNAL, VALUE is the signal, otherwize it is the exit status.
     */
    void receiveTerminatingEvent (final boolean signalArg, final int valueArg)
    {
	logger.log (Level.FINE, "{0} receiveTerminatingEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		boolean signal = signalArg;
		int value = valueArg;
		public void execute ()
		{
		    newState = oldState ().processTerminatingEvent (Task.this, signal,
							   value);
		}
	    });
    }

    /**
     * (internal) The task has disappeared (due to an exit or some
     * other error operation).
     */
    void receiveDisappearedEvent (final Throwable arg)
    {
	logger.log (Level.FINE, "{0} receiveDisappearedEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		Throwable w = arg;
		public void execute ()
		{
		    newState = oldState ().processDisappearedEvent (Task.this, w);
		}
	    });
    }

    /**
     * (internal) The task is performing a system call.
     */
    void receiveSyscalledEvent ()
    {
	logger.log (Level.FINE, "{0} receiveSyscalledEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processSyscalledEvent (Task.this);
		}
	    });
    }

    /**
     * (internal) The task has terminated; if SIGNAL, VALUE is the
     * signal, otherwize it is the exit status.
     */
    void receiveTerminatedEvent (final boolean signalArg, final int valueArg)
    {
	logger.log (Level.FINE, "{0} receiveTerminatedEvent\n", this); 
	Manager.eventLoop.add (new TaskEvent ()
	    {
		boolean signal = signalArg;
		int value = valueArg;
		public void execute ()
		{
		    newState = oldState ().processTerminatedEvent (Task.this, signal,
							  value);
		}
	    });
    }

    /**
     * (internal) The task has execed, overlaying itself with another
     * program.
     */
    void receiveExecedEvent ()
    {
	logger.log (Level.FINE, "{0} receiveExecedEvent\n", this);
	Manager.eventLoop.add (new TaskEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().processExecedEvent (Task.this);
		}
	    });
    }

    public class TaskEventObservable
	extends Observable
    {
	protected void notify (Object o)
	{
	    setChanged ();
	    notifyObservers (o);
	}
    }

    /**
     * Return a summary of the task's state.
     */
    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",id=" + id
		+ ",oldState=" + oldState
		+ ",newState=" + newState
		+ "}");
    }


    /**
     * Request that the observer be added to this task.  Pass the
     * request up to the Proc which may need to first perform an
     * attach.  Once attached, the Proc will pass the request back
     * down to .performAddObservation.
     */
    private void requestAddObserver (TaskObservable observable,
				     TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestAddObserver\n", this);
	proc.performAddObservation (new TaskObservation (this, observable,
							 observer));
    }
    /**
     * Delete TaskObserver from this tasks set of observers; also
     * delete it from the set of blockers.
     */
    private void requestDeleteObserver (final TaskObservable observable,
					final TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestDeleteObserver\n", this);
	proc.performDeleteObservation (new TaskObservation (this, observable,
							    observer));
    }
    /**
     * (Internal) Add the specified observation to the observer.
     */
    void performAddObservation (final Observation observationArg)
    {
	logger.log (Level.FINE, "{0} performAddObservation\n", this);
	Manager.eventLoop.add (new TaskEvent ()
	    {
		Observation observation = observationArg;
		public void execute ()
		{
		    newState = oldState ().processPerformAddObservation (Task.this,
								observation);
		}
	    });
    }

    /**
     * Set of interfaces currently blocking this task.
     */
    Set blockers = new HashSet ();
    /**
     * Return the current set of blockers as an array.  Useful when
     * debugging.
     */
    public TaskObserver[] getBlockers ()
    {
	return (TaskObserver[]) blockers.toArray (new TaskObserver[0]);
    }
    /**
     * Request that the observer be removed fro this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public void requestUnblock (final TaskObserver observerArg)
    {
	logger.log (Level.FINE, "{0} requestUnblock -- observer\n", this);
	Manager.eventLoop.add (new TaskEvent ()
	    {
		TaskObserver observer = observerArg;
		public void execute ()
		{
		    newState = oldState ().processRequestUnblock (Task.this, observer);
		}
	    });
    }

    /**
     * Set of Cloned observers.
     */
    private TaskObservable clonedObservers = new TaskObservable (this);
    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver (TaskObserver.Cloned o)
    {
	logger.log (Level.FINE, "{0} requestAddClonedObserver\n", this);
	requestAddObserver (clonedObservers, o);
    }
    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver (TaskObserver.Cloned o)
    {
	requestDeleteObserver (clonedObservers, o);
    }
    /**
     * Notify all cloned observers that this task cloned.  Return the
     * number of blocking observers.
     */
    int notifyCloned (Task clone)
    {
	for (Iterator i = clonedObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Cloned observer
		= (TaskObserver.Cloned) i.next ();
	    if (observer.updateCloned (this, clone) == Action.BLOCK) {
		blockers.add (observer);
		clone.blockers.add (observer);
	    }
	}
	return blockers.size ();
    }

    /**
     * Set of Attached observers.
     */
    private TaskObservable attachedObservers = new TaskObservable (this);
    /**
     * Add a TaskObserver.Attached observer.
     */
    public void requestAddAttachedObserver (TaskObserver.Attached o)
    {
	requestAddObserver (attachedObservers, o);
    }
    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver (TaskObserver.Attached o)
    {
	requestDeleteObserver (attachedObservers, o);
    }
    /**
     * Notify all Attached observers that this task attached.  Return
     * the number of blocking observers.
     */
    int notifyAttached ()
    {
	for (Iterator i = attachedObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Attached observer
		= (TaskObserver.Attached) i.next ();
	    if (observer.updateAttached (this) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }

    /**
     * Set of Forked observers.
     */
    private TaskObservable forkedObservers = new TaskObservable (this);
    /**
     * Add a TaskObserver.Forked observer.
     */
    public void requestAddForkedObserver (TaskObserver.Forked o)
    {
	requestAddObserver (forkedObservers, o);
    }
    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver (TaskObserver.Forked o)
    {
	requestDeleteObserver (forkedObservers, o);
    }
    /**
     * Notify all Forked observers that this task forked.  Return the
     * number of blocking observers.
     */
    int notifyForked (Task fork)
    {
	for (Iterator i = forkedObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Forked observer
		= (TaskObserver.Forked) i.next ();
	    if (observer.updateForked (this, fork) == Action.BLOCK) {
		blockers.add (observer);
		fork.blockers.add (observer);
	    }
	}
	return blockers.size ();
    }

    /**
     * Set of Terminated observers.
     */
    private TaskObservable terminatedObservers = new TaskObservable (this);
    /**
     * Add a TaskObserver.Terminated observer.
     */
    public void requestAddTerminatedObserver (TaskObserver.Terminated o)
    {
	requestAddObserver (terminatedObservers, o);
    }
    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver (TaskObserver.Terminated o)
    {
	logger.log (Level.FINE, "{0} requestDeleteTerminatedObserver\n", this);
	requestDeleteObserver (terminatedObservers, o);
    }
    /**
     * Notify all Terminated observers, of this Task's demise.  Return
     * the number of blocking observers.  (Does this make any sense?)
     */
    int notifyTerminated (boolean signal, int value)
    {
	for (Iterator i = terminatedObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Terminated observer
		= (TaskObserver.Terminated) i.next ();
	    if (observer.updateTerminated (this, signal, value) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }

    /**
     * Set of Terminating observers.
     */
    private TaskObservable terminatingObservers = new TaskObservable (this);
    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public void requestAddTerminatingObserver (TaskObserver.Terminating o)
    {
	requestAddObserver (terminatingObservers, o);
    }
    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver (TaskObserver.Terminating o)
    {
	requestDeleteObserver (terminatingObservers, o);
    }
    /**
     * Notify all Terminating observers, of this Task's demise.
     * Return the number of blocking observers.
     */
    int notifyTerminating (boolean signal, int value)
    {
	for (Iterator i = terminatingObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Terminating observer
		= (TaskObserver.Terminating) i.next ();
	    if (observer.updateTerminating (this, signal, value) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }

    /**
     * Set of Execed observers.
     */
    private TaskObservable execedObservers = new TaskObservable (this);
    /**
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public void requestAddExecedObserver (TaskObserver.Execed o)
    {
	requestAddObserver (execedObservers, o);
    }
    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver (TaskObserver.Execed o)
    {
	requestDeleteObserver (execedObservers, o);
    }
    /**
     * Notify all Execed observers, of this Task's demise.  Return the
     * number of blocking observers.
     */
    int notifyExeced ()
    {
	for (Iterator i = execedObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Execed observer
		= (TaskObserver.Execed) i.next ();
	    if (observer.updateExeced (this) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }

    /**
     * Set of Syscall observers.
     */
    private TaskObservable syscallObservers = new TaskObservable (this);
    /**
     * Add TaskObserver.Syscall to the TaskObserver pool.
     */
    public void requestAddSyscallObserver (TaskObserver.Syscall o)
    {
	requestAddObserver (syscallObservers, o);
    }
    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallObserver (TaskObserver.Syscall o)
    {
	requestDeleteObserver (syscallObservers, o);
    }
    /**
     * Notify all Syscall observers of this Task's entry into a system
     * call.  Return the number of blocking observers.
     */
    int notifySyscallEnter ()
    {
	for (Iterator i = syscallObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Syscall observer
		= (TaskObserver.Syscall) i.next ();
	    if (observer.updateSyscallEnter (this) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }
    /**
     * Notify all Syscall observers of this Task's exit from a system
     * call.  Return the number of blocking observers.
     */
    int notifySyscallExit ()
    {
	for (Iterator i = syscallObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Syscall observer
		= (TaskObserver.Syscall) i.next ();
	    if (observer.updateSyscallExit (this) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }


    /**
     * Set of Signaled observers.
     */
    private TaskObservable signaledObservers = new TaskObservable (this);
    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public void requestAddSignaledObserver (TaskObserver.Signaled o)
    {
	requestAddObserver (signaledObservers, o);
    }
    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver (TaskObserver.Signaled o)
    {
	requestDeleteObserver (signaledObservers, o);
    }
    /**
     * Notify all Signaled observers of the signal.  Return the number
     * of blocking observers.
     */
    int notifySignaled (int sig)
    {
	logger.log (Level.FINE, "{0} notifySignaled(int)\n", this);
	for (Iterator i = signaledObservers.iterator ();
	     i.hasNext (); ) {
	    TaskObserver.Signaled observer
		= (TaskObserver.Signaled) i.next ();
	    if (observer.updateSignaled (this, sig) == Action.BLOCK)
		blockers.add (observer);
	}
	return blockers.size ();
    }
}
