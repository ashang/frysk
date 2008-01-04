// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

import frysk.syscall.Syscall;
import frysk.syscall.SyscallTable;
import frysk.syscall.SyscallTableFactory;
import java.util.LinkedList;
import inua.eio.ByteBuffer;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Observer;
import java.util.Observable;
import frysk.isa.Register;
import frysk.isa.ISA;
import frysk.bank.RegisterBanks;

public abstract class Task {
    protected static final Logger logger = Logger.getLogger(ProcLogger.LOGGER_ID);

    /**
     * If known, as a result of tracing clone or fork, the task that
     * created this task.
     */
    final Task creator;

    /**
     * Return the task's corresponding TaskId.
     */
    public final TaskId getTaskId() {
	return id;
    }

    final TaskId id;

    /**
     * Return the task's process id.
     */
    public final int getTid() {
	return id.id;
    }

    /**
     * Return the task's (derived) name
     */
    public final String getName() {
	return "Task " + getTid();
    }

    /**
     * Return the state as a string; do not use!!!!
     */
    protected abstract String getStateFIXME();

    /**
     * Return's this Task's Instruction Set Architecture.
     */
    public final ISA getISA() {
	if (currentISA == null)
	    currentISA = sendrecISA();
	return currentISA;
    }
    private ISA currentISA;
    protected abstract ISA sendrecISA();

    /**
     * Returns this Task's Instruction Set Architecture.
     */
    public final Isa getIsa() {
	if (isa == null)
	    isa = sendrecIsa();
	return isa;
    }

    public final boolean hasIsa() {
	return (null != isa);
    }

    /**
     * This Task's Instruction Set Architecture.
     */
    private Isa isa;
 
    /**
     * Fetch this Task's Instruction Set Architecture.
     */
    protected abstract Isa sendrecIsa();

    private SyscallTable syscallTable;
    public final SyscallTable getSyscallTable() {
	if (syscallTable == null)
	    syscallTable = SyscallTableFactory.getSyscallTable(getISA());
	return syscallTable;
    }

    /**
     * Return the task's entry point address. This is the address of
     * the first instruction that the task will have executed. XXX:
     * Not yet implemented.
     */
    public long getEntryPointAddress() {
	return 0xdeadbeefL;
    }

    /**
     * Return the containing Proc.
     */
    public Proc getProc() {
	return proc;
    }
    private final Proc proc;

    /**
     * Create a new Task skeleton.
     */
    private Task(TaskId id, Proc proc, Task creator) {
	this.proc = proc;
	this.id = id;
	this.creator = creator;
	proc.add(this);
	proc.getHost().add(this);
    }

    /**
     * Create a new unattached Task.
     */
    protected Task(Proc proc, TaskId id) {
	this(id, proc, null);
	logger.log(Level.FINEST, "{0} new -- create unattached\n", this);
    }

    /**
     * Create a new attached clone of Task.
     */
    protected Task(Task task, TaskId cloneId) {
	this(cloneId, task.proc, task);
	logger.log(Level.FINE, "{0} new -- create attached clone\n", this);
    }

    /**
     * Create a new attached main Task of Proc. If Attached observer
     * is specified assume it should be attached, otherwize, assume
     * that, as soon as the task stops, it should be detached. Note
     * the chicken-egg problem here: to add the initial observation
     * the Proc needs the Task (which has the Observable).
     * Conversely, for a Task, while it has the Observable, it doesn't
     * have the containing proc.
     */
    protected Task(Proc proc, TaskObserver.Attached attached) {
	this(new TaskId(proc.getPid()), proc, proc.creator);
    }

    protected LinkedList queuedEvents = new LinkedList();

    /**
     * Add the specified observer to the observable.
     */
    protected abstract void handleAddObservation(TaskObservation observation);

    /**
     * Delete the specified observer from the observable.
     */
    protected abstract void handleDeleteObservation(TaskObservation observation);

    /**
     *  (Internal) Request that all observers from this task be
     *  removed.  Warning, should also be removed from the proc's
     *  observations.
     *
     * XXX: Should not be public.
     */
    public void removeObservers() {
	logger.log(Level.FINE, "{0} abandon", this);	 
		
	attachedObservers.removeAllObservers();
	clonedObservers.removeAllObservers();
	forkedObservers.removeAllObservers();
	terminatedObservers.removeAllObservers();
	terminatingObservers.removeAllObservers();
	execedObservers.removeAllObservers();
	syscallObservers.removeAllObservers();
	signaledObservers.removeAllObservers();
	instructionObservers.removeAllObservers();
	blockers.clear();
	pendingObservations.clear();
    }
  
    public class TaskEventObservable extends java.util.Observable {
	protected void notify(Object o) {
	    setChanged();
	    notifyObservers(o);
	}
    }

    /**
     * Return a summary of the task's state.
     */
    public String toString() {
	return ("{" + super.toString()
		+ ",pid=" + proc.getPid()
		+ ",tid=" + getTid()
		+ "}");
    }

    /**
     * Set of interfaces currently blocking this task.
     *
     * XXX: Should not be public.
     */
    public Set blockers = new HashSet();

    /**
     * Return the current set of blockers as an array. Useful when
     * debugging.
     */
    public TaskObserver[] getBlockers() {
	return (TaskObserver[]) blockers.toArray(new TaskObserver[0]);
    }

    /**
     * Request that the observer be removed from this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public abstract void requestUnblock(final TaskObserver observerArg);

    /**
     * Set of Cloned observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable clonedObservers = new TaskObservable(this);

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public abstract void requestAddClonedObserver(TaskObserver.Cloned o);

    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public abstract void requestDeleteClonedObserver(TaskObserver.Cloned o);

    /**
     * Notify all cloned observers that this task cloned. Return the
     * number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyClonedParent(Task offspring) {
	for (Iterator i = clonedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Cloned observer = (TaskObserver.Cloned) i.next();
	    if (observer.updateClonedParent(this, offspring) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }

    /**
     * Notify all cloned observers that this task cloned. Return the
     * number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyClonedOffspring() {
	logger.log(Level.FINE, "{0} notifyClonedOffspring\n", this);
	for (Iterator i = creator.clonedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Cloned observer = (TaskObserver.Cloned) i.next();
	    if (observer.updateClonedOffspring(creator, this) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }

    /**
     * Set of Attached observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable attachedObservers = new TaskObservable(this);

    /**
     * Add a TaskObserver.Attached observer.
     */
    public abstract void requestAddAttachedObserver(TaskObserver.Attached o);

    /**
     * Delete a TaskObserver.Attached observer.
     */
    public abstract void requestDeleteAttachedObserver(TaskObserver.Attached o);

    /**
     * Notify all Attached observers that this task attached. Return
     * the number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyAttached() {
	logger.log(Level.FINE, "{0} notifyAttached\n", this);
	//Fill isa on attach.
	getIsa();
	for (Iterator i = attachedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Attached observer = (TaskObserver.Attached) i.next();
	    if (observer.updateAttached(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Set of Forked observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable forkedObservers = new TaskObservable(this);

    /**
     * Add a TaskObserver.Forked observer.
     */
    public abstract void requestAddForkedObserver(TaskObserver.Forked o);

    /**
     * Delete a TaskObserver.Forked observer.
     */
    public abstract void requestDeleteForkedObserver(TaskObserver.Forked o);

    /**
     * Notify all Forked observers that this task forked. Return the
     * number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyForkedParent(Task offspring) {
	for (Iterator i = forkedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Forked observer = (TaskObserver.Forked) i.next();
	    if (observer.updateForkedParent(this, offspring) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }

    /**
     * Notify all Forked observers that this task's new offspring,
     * created using fork, is sitting at the first instruction.
     *
     * XXX: Should not be public.
     */
    public int notifyForkedOffspring() {
	for (Iterator i = creator.forkedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Forked observer = (TaskObserver.Forked) i.next();
	    if (observer.updateForkedOffspring(creator, this) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }

    /**
     * Set of Terminated observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable terminatedObservers = new TaskObservable(this);

    /**
     * Add a TaskObserver.Terminated observer.
     */
    public abstract void requestAddTerminatedObserver(TaskObserver.Terminated o);

    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public abstract void requestDeleteTerminatedObserver(TaskObserver.Terminated o);

    /**
     * Notify all Terminated observers, of this Task's demise. Return
     * the number of blocking observers.(Does this make any sense?)
     *
     * XXX: Should not be public.
     */
    public int notifyTerminated(boolean signal, int value) {
	logger.log(Level.FINE, "{0} notifyTerminated\n", this);
	for (Iterator i = terminatedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Terminated observer = (TaskObserver.Terminated) i.next();
	    if (observer.updateTerminated(this, signal, value) == Action.BLOCK) {
		logger.log(Level.FINER,
			   "{0} notifyTerminated adding {1} to blockers\n",
			   new Object[] { this, observer });
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }

    /**
     * Set of Terminating observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable terminatingObservers = new TaskObservable(this);

    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public abstract void requestAddTerminatingObserver(TaskObserver.Terminating o);

    /**
     * Delete TaskObserver.Terminating.
     */
    public abstract void requestDeleteTerminatingObserver(TaskObserver.Terminating o);

    /**
     * Notify all Terminating observers, of this Task's demise. Return
     * the number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyTerminating(boolean signal, int value) {
	for (Iterator i = terminatingObservers.iterator(); i.hasNext();) {
	    TaskObserver.Terminating observer = (TaskObserver.Terminating) i.next();
	    if (observer.updateTerminating(this, signal, value) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Set of Execed observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable execedObservers = new TaskObservable(this);

    /**
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public abstract void requestAddExecedObserver(TaskObserver.Execed o);

    /**
     * Delete TaskObserver.Execed.
     */
    public abstract void requestDeleteExecedObserver(TaskObserver.Execed o);

    /**
     * Notify all Execed observers, of this Task's demise. Return the
     * number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyExeced() {
	//Flush the isa in case it has changed between exec's.
	clearIsa();
	//XXX: When should the isa be rebuilt?
	for (Iterator i = execedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Execed observer = (TaskObserver.Execed) i.next();
	    if (observer.updateExeced(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Set of Syscall observers. Checked in TaskState.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable syscallObservers = new TaskObservable(this);

    /**
     * Add TaskObserver.Syscalls to the TaskObserver pool.
     */
    public abstract void requestAddSyscallsObserver(TaskObserver.Syscalls o);

    /**
     * Delete TaskObserver.Syscall.
     */
    public abstract void requestDeleteSyscallsObserver(TaskObserver.Syscalls o);

    /**
     * Notify all Syscall observers of this Task's entry into a system
     * call.  Return the number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifySyscallEnter() {
	logger.log(Level.FINE,
		   "{0} notifySyscallEnter\n", this);
	Syscall syscall = getSyscallTable().getSyscall(this);
	for (Iterator i = syscallObservers.iterator(); i.hasNext();) {
	    TaskObserver.Syscalls observer = (TaskObserver.Syscalls) i.next();
	    if (observer.updateSyscallEnter(this, syscall) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Notify all Syscall observers of this Task's exit from a system
     * call. Return the number of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifySyscallExit() {
	logger.log(Level.FINE,
		   "{0} notifySyscallExit {1}\n", this);
	for (Iterator i = syscallObservers.iterator(); i.hasNext();) {
	    TaskObserver.Syscalls observer = (TaskObserver.Syscalls) i.next();
	    if (observer.updateSyscallExit(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Set of Signaled observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable signaledObservers = new TaskObservable(this);

    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public abstract void requestAddSignaledObserver(TaskObserver.Signaled o);

    /**
     * Delete TaskObserver.Signaled.
     */
    public abstract void requestDeleteSignaledObserver(TaskObserver.Signaled o);

    /**
     * Notify all Signaled observers of the signal. Return the number
     * of blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifySignaled(int sig) {
	logger.log(Level.FINE, "{0} notifySignaled(int)\n", this);
	for (Iterator i = signaledObservers.iterator(); i.hasNext();) {
	    TaskObserver.Signaled observer = (TaskObserver.Signaled) i.next();
	    if (observer.updateSignaled(this, sig) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    private ByteBuffer memory;
    protected abstract ByteBuffer sendrecMemory();
    /**
     * Return the Task's memory.
     */
    public ByteBuffer getMemory() {
	logger.log(Level.FINE, "{0} entering get memory {1}\n",new Object[] {this, memory});
	if (memory == null )
	    memory = sendrecMemory();
	logger.log(Level.FINE, "{0} exiting get memory {1}\n", new Object[] {this, memory});
	return this.memory;
    }

    /**
     * Returns the memory as seen by frysk-core. That includes things
     * like inserted breakpoint instructions bytes which are filtered
     * out by <code>getMemory()</code> (which is what you normally
     * want unless you are interested in frysk-core specifics).  <p>
     * Default implementation calls <code>getMemory()</code>, need to
     * be overriden by subclasses for which the raw memory view and
     * the logical memory view are different.
     */
    public ByteBuffer getRawMemory() {
	return getMemory();
    }

    /**
     * Set of Code observers.
     *
     * XXX: Should not be public.
     */
    public final TaskObservable codeObservers = new TaskObservable(this);
  
    /**
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public abstract void requestAddCodeObserver(TaskObserver.Code o, long a);

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public abstract void requestDeleteCodeObserver(TaskObserver.Code o, long a);
  
    /**
     * Whether we are currently stepping over a breakpoint.  Used in
     * the running task state when a trap event occurs after a step
     * has been issued. Null when no step is being performed.
     *
     * XXX: This variable belongs in the Linux/PTRACE state machine.
     */
    public Breakpoint steppingBreakpoint;

    /**
     * Whether we have just started the Task. Set in
     * wantToAttachContinue.blockOrAttachContinue() and immediately
     * reset in sendContinue() unless we request a step or
     * Running.handleTrappedEvent() when the first step is
     * received. This is a temporary hack to work around bug
     * #4663. Needs to be merged with SteppingState (see step_send).
     */
    public boolean just_started;

    /**
     * The signal, or zero, send last to the task.
     *
     * XXX: This should be a state in Linux/PTRACE state machine.
     */
    public int sig_send;

    /**
     * When the last request to the process was a step request,
     * whether it was a request to step a sigreturn syscall.  Set by
     * sendStepInstruction().
     *
     * XXX: This should be a state in Linux/PTRACE state machine.
     */
    public boolean syscall_sigret;

    /**
     * Notify all Code observers of the breakpoint. Return the number
     * of blocking observers or -1 if no Code observer were installed
     * on this address.
     *
     * XXX: Should not be public.
     */
    public int notifyCodeBreakpoint(long address) {
	logger.log(Level.FINE, "{0} notifyCodeBreakpoint({1})\n",
		   new Object[] { this, Long.valueOf(address) });
    
	Collection observers = proc.breakpoints.getCodeObservers(address);
	if (observers == null)
	    return -1;

	Iterator i = observers.iterator();
	while (i.hasNext()) {
	    TaskObserver.Code observer = (TaskObserver.Code) i.next();
	    if (codeObservers.contains(observer))
		if (observer.updateHit(this, address) == Action.BLOCK)
		    blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * Set of Instruction observers.
     *
     * XXX: Should not be public.
     */
    public TaskObservable instructionObservers = new TaskObservable(this);
  
    /**
     * Request the addition of a Instruction observer that will be
     * notified as soon as the task executes an instruction.
     * <code>o.updateExecuted</code> is called as soon as the Task
     * starts running again (is not blocked or stopped) and executes
     * the next instruction.
     */
    public abstract void requestAddInstructionObserver(TaskObserver.Instruction o);

    /**
     * Delete TaskObserver.Instruction from the TaskObserver pool.
     */
    public abstract void requestDeleteInstructionObserver(TaskObserver.Instruction o);
  
    /**
     * Notify all Instruction observers. Returns the total number of
     * blocking observers.
     *
     * XXX: Should not be public.
     */
    public int notifyInstruction() {
	logger.log(Level.FINE, "{0} notifyInstruction()\n", this);
	Iterator i = instructionObservers.iterator();
	while (i.hasNext()) {
	    TaskObserver.Instruction observer;
	    observer = (TaskObserver.Instruction) i.next();
	    if (observer.updateExecuted(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }

    /**
     * List containing the TaskObservations that are pending addition
     * or deletion (in order that they were requested). Will be dealt
     * with as soon as a stop event is received during one of the
     * running states.
     *
     * XXX: Should not be public.
     */
    public LinkedList pendingObservations = new LinkedList();

    /**
     * Return the Task's Register as a long.
     */
    public long getRegister(Register register) {
	return getRegisterBanks().get(register);
    }
    /**
     * Store the long value in the Task's register.
     */
    public void setRegister(Register register, long value) {
	getRegisterBanks().set(register, value);
    }
    /**
     * Access bytes OFFSET:LENGTH of the Task's register read/writing
     * it into the byte buffer from START.
     */
    public void access(Register register, int offset, int length,
		       byte[] bytes, int start, boolean write) {
	getRegisterBanks().access(register, offset, length, bytes,
				  start, write);
    }

    private RegisterBanks registerBanks;
    protected abstract RegisterBanks sendrecRegisterBanks();
    RegisterBanks getRegisterBanks() {
	if (registerBanks == null)
	    registerBanks = sendrecRegisterBanks();
	return registerBanks;
    }

    /**
     * Return the machine's register banks as an array of ByteBuffers.
     *
     * XXX: This is wrong.  Clients cannot assume internal register
     * layout.
     */
    public ByteBuffer[] getRegisterBuffersFIXME() {
	return getRegisterBanks().getBanksFIXME();
    }
  
    /**
     * The process has transitioned to the detached.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public static TaskStateObservable taskStateDetached = new TaskStateObservable ();
  
    static {
	taskStateDetached.addObserver(new Observer() {
		public void update (Observable o, Object arg) {
		    if (arg instanceof Task) {
			Task task = (Task) arg;
			task.clearIsa();
		    }
		}
	    });
    }
 
    /**
     * Use a counter rather than a boolean because multiple caches may
     * depend on this count and no cache should be able to clear it.
     */
    private int modCount = 0;
 
    public void incrementMod() {
	modCount++;
    }
 
    public int getMod() {
	return modCount;
    }
 
    public void clearIsa() {
	isa = null;
	memory = null;
	registerBanks = null;
	syscallTable = null;
	currentISA = null;
    }
  
    /**
     * XXX: Temporary until .observable's are converted to
     * .requestAddObserver.
     */
    public static class TaskStateObservable extends Observable {
	public void notify(Object o) {
	    logger.log(Level.FINE, "{0} notify -- all observers\n", o); 
	    setChanged();
	    notifyObservers(o);
	}
    }
}
