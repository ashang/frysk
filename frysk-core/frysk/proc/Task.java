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
	newState = TaskState.detachedState ();
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
     * Create a new attached main Task of Proc.  If Attached observer
     * is specified assume it should be attached, otherwize, assume
     * that, as soon as the task stops, it should be detached.
     *
     * Note the chicken-egg problem here: to add the initial
     * observation the Proc needs the Task (which has the Observable).
     * Conversely, for a Task, while it has the Observable, it doesn't
     * have the containing proc.
     */
    protected Task (Proc proc, TaskObserver.Attached attached)
    {
	this (new TaskId (proc.getPid ()), proc);
	newState = TaskState.mainState ();
	if (attached != null) {
	    TaskObservation ob = new TaskObservation (this, attachedObservers,
						      attached)
		{
		    public void execute ()
		    {
			throw new RuntimeException ("oops!");
		    }
		};
	    proc.handleAddObservation (ob);
	}
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
     * Return the current state while at the same time marking that
     * the state is in flux.  If a second attempt to change state
     * occures before the current state transition has completed,
     * barf.  XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    private TaskState oldState ()
    {
	if (newState == null)
	    throw new RuntimeException (this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }

    /**
     * (Internal) Add the specified observer to the observable.
     */
    void handleAddObserver (Observable observable, Observer observer)
    {
	newState = oldState ().handleAddObserver (Task.this, observable,
						  observer);
    }
    /**
     * (Internal) Delete the specified observer from the observable.
     */
    void handleDeleteObserver (Observable observable, Observer observer)
    {
	newState = oldState ().handleDeleteObserver (Task.this, observable,
						     observer);
    }
    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    void performContinue ()
    {
	newState = oldState ().handleContinue (Task.this);
    }
    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     */
    void performRemoval ()
    {
	newState = oldState ().handleRemoval (Task.this);
    }
    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been completed.  The task is left in the stopped state.
     */
    void performAttach ()
    {
	newState = oldState ().handleAttach (Task.this);
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     */
    void performDetach ()
    {
	newState = oldState ().handleDetach (Task.this);
    }

    /**
     * (internal) This task cloned creating the new Task cloneArg.
     */
    void processClonedEvent (Task clone)
    {
	newState = oldState ().handleClonedEvent (this, clone);
    }
    /**
     * (internal) This Task forked creating an entirely new child
     * process containing one (the fork) task.
     */
    void processForkedEvent (Task fork)
    {
	newState = oldState ().handleForkedEvent (this, fork);
    }
    /**
     * (internal) This task stopped.
     */
    void processStoppedEvent ()
    {
	newState = oldState ().handleStoppedEvent (this);
    }
    /**
     * (internal) This task encountered a trap.
     */
    void processTrappedEvent ()
    {
	newState = oldState ().handleTrappedEvent (this);
    }
    /**
     * (internal) This task received a signal.
     */
    void processSignaledEvent (int sig)
    {
	newState = oldState ().handleSignaledEvent (this, sig);
    }

    /**
     * (internal) The task is in the process of terminating.  If
     * SIGNAL, VALUE is the signal, otherwize it is the exit status.
     */
    void processTerminatingEvent (boolean signal, int value)
    {
	newState = oldState ().handleTerminatingEvent (this, signal, value);
    }

    /**
     * (internal) The task has disappeared (due to an exit or some
     * other error operation).
     */
    void processDisappearedEvent (Throwable arg)
    {
	newState = oldState ().handleDisappearedEvent (this, arg);
    }

    /**
     * (internal) The task is performing a system call.
     */
    void processSyscalledEvent ()
    {
	newState = oldState ().handleSyscalledEvent (this);
    }

    /**
     * (internal) The task has terminated; if SIGNAL, VALUE is the
     * signal, otherwize it is the exit status.
     */
    void processTerminatedEvent (boolean signal, int value)
    {
	newState = oldState ().handleTerminatedEvent (this, signal, value);
    }

    /**
     * (internal) The task has execed, overlaying itself with another
     * program.
     */
    void processExecedEvent ()
    {
	newState = oldState ().handleExecedEvent (this);
    }

    public class TaskEventObservable
	extends java.util.Observable
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
		+ ",pid=" + proc.getPid ()
		+ ",tid=" + getTid ()
		+ ",state=" + getState ()
		+ "}");
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
		    newState = oldState ().handleUnblock (Task.this, observer);
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
	proc.requestAddObserver (this, clonedObservers, o);
    }
    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver (TaskObserver.Cloned o)
    {
	logger.log (Level.FINE, "{0} requestDeleteClonedObserver\n", this);
	proc.requestDeleteObserver (this, clonedObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddAttachedObserver\n", this);
	proc.requestAddObserver (this, attachedObservers, o);
    }
    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver (TaskObserver.Attached o)
    {
	logger.log (Level.FINE, "{0} requestDeleteAttachedObserver\n", this);
	proc.requestDeleteObserver (this, attachedObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddForkedObserver\n", this);
	proc.requestAddObserver (this, forkedObservers, o);
    }
    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver (TaskObserver.Forked o)
    {
	logger.log (Level.FINE, "{0} requestDeleteForkedObserver\n", this);
	proc.requestDeleteObserver (this, forkedObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddTerminatedObserver\n", this);
	proc.requestAddObserver (this, terminatedObservers, o);
    }
    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver (TaskObserver.Terminated o)
    {
	logger.log (Level.FINE, "{0} requestDeleteTerminatedObserver\n", this);
	proc.requestDeleteObserver (this, terminatedObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddTerminatingObserver\n", this);
	proc.requestAddObserver (this, terminatingObservers, o);
    }
    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver (TaskObserver.Terminating o)
    {
	logger.log (Level.FINE, "{0} requestDeleteTerminatingObserver\n", this);
	proc.requestDeleteObserver (this, terminatingObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddExecedObserver\n", this);
	proc.requestAddObserver (this, execedObservers, o);
    }
    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver (TaskObserver.Execed o)
    {
	logger.log (Level.FINE, "{0} requestDeleteExecedObserver\n", this);
	proc.requestDeleteObserver (this, execedObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddSyscallObserver\n", this);
	proc.requestAddObserver (this, syscallObservers, o);
    }
    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallObserver (TaskObserver.Syscall o)
    {
	logger.log (Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
	proc.requestDeleteObserver (this, syscallObservers, o);
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
	logger.log (Level.FINE, "{0} requestAddSignaledObserver\n", this);
	proc.requestAddObserver (this, signaledObservers, o);
    }
    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver (TaskObserver.Signaled o)
    {
	logger.log (Level.FINE, "{0} requestDeleteSignaledObserver\n", this);
	proc.requestDeleteObserver (this, signaledObservers, o);
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
