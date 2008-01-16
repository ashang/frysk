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

import java.util.LinkedList;
import frysk.isa.Register;
import frysk.isa.RegistersFactory;
import java.util.Iterator;
import java.util.Collection;
import frysk.proc.Action;
import frysk.proc.TaskEvent;
import frysk.proc.Manager;
import frysk.proc.TaskObserver.Terminating;
import frysk.proc.TaskObserver;
import frysk.proc.Proc;
import frysk.proc.TaskId;
import frysk.proc.Task;
import java.util.logging.Level;
import frysk.event.Event;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Ptrace.AddressSpace;
import frysk.sys.Signal;
import frysk.syscall.Syscall;
import frysk.isa.ISA;
import frysk.isa.ElfMap;
import java.io.File;
import frysk.bank.RegisterBanks;

/**
 * A Linux Task tracked using PTRACE.
 */

public class LinuxPtraceTask extends LiveTask {
    /**
     * Create a new unattached Task.
     */
    public LinuxPtraceTask(Proc proc, TaskId id) {
	super(proc, id);
	newState = LinuxPtraceTaskState.detachedState();
    }
    /**
     * Create a new attached clone of Task.
     */
    public LinuxPtraceTask(Task task, TaskId clone) {
	// XXX: shouldn't need to grub around in the old task's state.
	super(task, clone);
	newState = LinuxPtraceTaskState.clonedState(((LinuxPtraceTask)task).getState ());
    }
    /**
     * Create a new attached main Task of Proc.
     */
    public LinuxPtraceTask(LinuxPtraceProc proc,
			   TaskObserver.Attached attached) {
	super(proc, attached);
	newState = LinuxPtraceTaskState.mainState();
	if (attached != null) {
	    TaskObservation ob = new TaskObservation(this, attachedObservers,
						     attached, true) {
		    public void execute() {
			throw new RuntimeException("oops!");
		    }
		};
	    proc.handleAddObservation(ob);
	}
    }

    /**
     * Return the raw memory byte-buffer. This is the TEXT/DATA area.
     */
    ByteBuffer getRawMemory() {
	logger.log(Level.FINE, "Begin fillMemory\n", this);
	ByteOrder byteOrder = getISA().order();
	ByteBuffer memory = new AddressSpaceByteBuffer(getTid(),
						       AddressSpace.DATA);
	memory.order(byteOrder);
	logger.log(Level.FINE, "End fillMemory\n", this); 
	return memory;
    }

    /**
     * Return the Task's memory.
     */
    public ByteBuffer getMemory() {
	if (memory == null) {
	    logger.log(Level.FINE, "{0} exiting get memory\n", this);
	    int tid = getTid();
	    ByteOrder byteOrder = getISA().order();
	    BreakpointAddresses breakpoints = ((LinuxPtraceProc)getProc()).breakpoints;
	    memory = new LogicalMemoryBuffer(tid, AddressSpace.DATA,
					     breakpoints);
	    memory.order(byteOrder);
	}
	return memory;
    }
    private ByteBuffer memory;

    protected RegisterBanks sendrecRegisterBanks() {
	return PtraceRegisterBanksFactory.create(getISA(), getTid());
    }

    /**
     * Return the Task's ISA.
     *
     * Can this instead look at AUXV?
     */
    protected ISA sendrecISA () {
	// FIXME: This should use task.proc.getExe().  Only that
	// causes wierd failures; take a rain-check :-(
	return ElfMap.getISA(new File("/proc/" + getTid() + "/exe"));
    }

    /**
     * Return the Task's ISA.
     *
     * XXX: This code locally, and not the IsaFactory, and definitly
     * not via a PID should be determining the ISA of the process.
     */
    public Isa getIsaFIXME() {
	logger.log(Level.FINE, "{0} sendrecIsa\n", this);
	IsaFactory factory = IsaFactory.getSingleton();
	return factory.getIsa(getTid());
    }

    /**
     * (internal) This task cloned creating the new Task cloneArg.
     */
    void processClonedEvent (Task clone)
    {
	set(oldState().handleClonedEvent(this, (LinuxPtraceTask)clone));
    }
    /**
     * (internal) This Task forked creating an entirely new child process
     * containing one (the fork) task.
     */
    void processForkedEvent (Task fork)
    {
	set(oldState().handleForkedEvent(this, (LinuxPtraceTask)fork));
    }
    /**
     * (internal) This task stopped.
     */
    void processStoppedEvent ()
    {
	set(oldState().handleStoppedEvent(this));
    }
    /**
     * (internal) This task encountered a trap.
     */
    void processTrappedEvent ()
    {
	set(oldState().handleTrappedEvent(this));
    }
    /**
     * (internal) This task received a signal.
     */
    void processSignaledEvent (int sig)
    {
	set(oldState().handleSignaledEvent(this, sig));
    }
    /**
     * (internal) The task is in the process of terminating. If SIGNAL, VALUE is
     * the signal, otherwize it is the exit status.
     */
    void processTerminatingEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatingEvent(this, signal, value));
    }
    /**
     * (internal) The task has disappeared (due to an exit or some other error
     * operation).
     */
    void processDisappearedEvent (Throwable arg)
    {
	set(oldState().handleDisappearedEvent(this, arg));
    }
    /**
     * (internal) The task is performing a system call.
     */
    void processSyscalledEvent ()
    {
	set(oldState().handleSyscalledEvent(this));
    }
    /**
     * (internal) The task has terminated; if SIGNAL, VALUE is the signal,
     * otherwize it is the exit status.
     */
    void processTerminatedEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatedEvent(this, signal, value));
    }
    /**
     * (internal) The task has execed, overlaying itself with another program.
     */
    void processExecedEvent ()
    {
	set(oldState().handleExecedEvent(this));
    }

    /**
     * Must inject disappeared events back into the event loop so that
     * they can be processed in sequence. Calling
     * receiveDisappearedEvent directly would cause a recursive state
     * transition.
     */
    protected void postDisappearedEvent (final Throwable arg)
    {
	logger.log(Level.FINE, "{0} postDisappearedEvent\n", this);
	Manager.eventLoop.add(new Event()
	    {
		final Throwable w = arg;
		public void execute ()
		{
		    processDisappearedEvent(w);
		}
	    });
    }

    public void sendContinue (int sig)
    {
	logger.log(Level.FINE, "{0} sendContinue\n", this);
	sigSendXXX = sig;
        incrementMod();
	try
	    {
		Ptrace.cont(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }
    public void sendSyscallContinue (int sig)
    {
	logger.log(Level.FINE, "{0} sendSyscallContinue\n", this);
	sigSendXXX = sig;
        incrementMod();
	try
	    {
		Ptrace.sysCall(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendStepInstruction (int sig)
    {
	logger.log(Level.FINE, "{0} sendStepInstruction\n", this);
	sigSendXXX = sig;
        incrementMod();
	syscallSigretXXX = getIsaFIXME().isAtSyscallSigReturn(this);
	try
	    {
		Ptrace.singleStep(getTid(), sig);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendStop ()
    {
	logger.log(Level.FINE, "{0} sendStop\n", this);
	Signal.STOP.tkill(getTid());
    }

    private int ptraceOptions;
    public void sendSetOptions ()
    {
	logger.log(Level.FINE, "{0} sendSetOptions\n", this);
	try
	    {
		// XXX: Should be selecting the trace flags based on the
		// contents of .observers.
		ptraceOptions |= Ptrace.optionTraceClone();
		ptraceOptions |= Ptrace.optionTraceFork();
		ptraceOptions |= Ptrace.optionTraceExit();
		// ptraceOptions |= Ptrace.optionTraceSysgood (); not set by default
		ptraceOptions |= Ptrace.optionTraceExec();
		Ptrace.setOptions(getTid(), ptraceOptions);
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendAttach ()
    {
	logger.log(Level.FINE, "{0} sendAttach\n", this);
	try
	    {
		Ptrace.attach(getTid());

		/*
		 * XXX: Linux kernel has a 'feature' that if a process is already
		 * stopped and ptrace requests that it be stopped (again) in order to
		 * attach to it, the signal (SIGCHLD) notifying frysk of the attach's
		 * pending waitpid event isn't generated.
		 */

		/*
		 * XXX: This line sends another signal to frysk
		 * notifying about the attach's pending waitpid
		 * regardless of whether the task is running or
		 * stopped. This avoids hangs on attaching to a
		 * stopped process. Bug 3316.
		 */
		Signal.CHLD.tkill(frysk.sys.Tid.get());
	    }
	catch (Errno.Eperm e)
	    {
		logger.log(Level.FINE, "{" + e.toString()
			   + "} Cannot attach to process\n");
	    }
	catch (Errno.Esrch e)
	    {
		postDisappearedEvent(e);
	    }
    }

    public void sendDetach(int sig) {
	logger.log(Level.FINE, "{0} sendDetach\n", this);
	clearIsa();
	try {
	    Ptrace.detach(getTid(), sig);
	} catch (Exception e) {
	    // Ignore problems trying to detach, most of the time the
	    // problem is the process has already left the cpu queue
	}
    }

    public void startTracingSyscalls ()
    {
	logger.log(Level.FINE, "{0} startTracingSyscalls\n", this);
	ptraceOptions |= Ptrace.optionTraceSysgood();
	this.sendSetOptions();
    }

    public void stopTracingSyscalls ()
    {
	logger.log(Level.FINE, "{0} stopTracingSyscalls\n", this);
	ptraceOptions &= ~ (Ptrace.optionTraceSysgood());
	this.sendSetOptions();
    }


    /**
     * The state of this task. During a state transition newState is
     * NULL.
     */
    private LinuxPtraceTaskState oldState;
    private LinuxPtraceTaskState newState;

    /**
     * Return the current state.
     */
    protected final LinuxPtraceTaskState getState() {
	if (newState != null)
	    return newState;
	else
	    return oldState;
    }
    protected String getStateFIXME() {
	return getState().toString();
    }

    /**
     * Set the new state.
     */
    void set(LinuxPtraceTaskState newState) {
	this.newState = newState;
    }

    /**
     * Return the current state while at the same time marking that
     * the state is in flux. If a second attempt to change state
     * occurs before the current state transition has completed,
     * barf. XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    protected LinuxPtraceTaskState oldState() {
	if (newState == null)
	    throw new RuntimeException(this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }
    
    /**
     * (Internal) Add the specified observer to the observable.
     */
    void handleAddObservation(TaskObservation observation) {
	newState = oldState().handleAddObservation(this, observation);
    }
    
    /**
     * (Internal) Delete the specified observer from the observable.
     */
    void handleDeleteObservation(TaskObservation observation) {
	newState = oldState().handleDeleteObservation(this, observation);
    }
    
    void handleUnblock(TaskObserver observer) {
	newState = oldState().handleUnblock(this, observer);
    }

    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    void performContinue() {
	newState = oldState().handleContinue(this);
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * XXX: Should not be public.
     */
    void performRemoval() {
	newState = oldState().handleRemoval(this);
    }

    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been completed. The task is left in the stopped state.
     *
     * XXX: Should not be public.
     */
    void performAttach() {
	newState = oldState().handleAttach(this);
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     * @param shouldRemoveObservers whether to remove the observers as well.
     */
    void performDetach(boolean shouldRemoveObservers) {
	newState = oldState().handleDetach(this, shouldRemoveObservers);
    }

    /**
     * Request that the observer be removed from this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public void requestUnblock(final TaskObserver observerArg) {
	logger.log(Level.FINE, "{0} requestUnblock -- observer\n", this);
	Manager.eventLoop.add(new TaskEvent(this) {
		final TaskObserver observer = observerArg;
		protected void execute(Task task) {
		    ((LinuxPtraceTask)task).handleUnblock(observer);
		}
	    });
    }


    /**
     * Set of Cloned observers.
     */
    private final TaskObservable clonedObservers = new TaskObservable(this);
    /**
     * Notify all cloned observers that this task cloned. Return the
     * number of blocking observers.
     */
    int notifyClonedParent(Task offspring) {
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
     */
    int notifyClonedOffspring() {
	logger.log(Level.FINE, "{0} notifyClonedOffspring\n", this);
	LinuxPtraceTask creator = (LinuxPtraceTask)this.getCreator();
	for (Iterator i = creator.clonedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Cloned observer = (TaskObserver.Cloned) i.next();
	    if (observer.updateClonedOffspring(creator, this) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }
    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	logger.log(Level.FINE, "{0} requestAddClonedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, clonedObservers, o);
    }
    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	logger.log(Level.FINE, "{0} requestDeleteClonedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, clonedObservers, o);
    }

    /**
     * Set of Attached observers.
     *
     * XXX: Should not be public.
     */
    private final TaskObservable attachedObservers = new TaskObservable(this);
    /**
     * Notify all Attached observers that this task attached. Return
     * the number of blocking observers.
     */
    int notifyAttached() {
	logger.log(Level.FINE, "{0} notifyAttached\n", this);
	//Fill isa on attach.
	getIsaFIXME();
	for (Iterator i = attachedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Attached observer = (TaskObserver.Attached) i.next();
	    if (observer.updateAttached(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }
    /**
     * Add a TaskObserver.Attached observer.
     */
    public void requestAddAttachedObserver(TaskObserver.Attached o) {
	logger.log(Level.FINE, "{0} requestAddAttachedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, attachedObservers, o);
    }
    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver(TaskObserver.Attached o) {
	logger.log(Level.FINE, "{0} requestDeleteAttachedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, attachedObservers, o);
    }


    /**
     * Set of Forked observers.
     */
    private final TaskObservable forkedObservers = new TaskObservable(this);
    /**
     * Notify all Forked observers that this task forked. Return the
     * number of blocking observers.
     */
    int notifyForkedParent(Task offspring) {
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
     */
    int notifyForkedOffspring() {
	LinuxPtraceTask creator = (LinuxPtraceTask)this.getCreator();
	for (Iterator i = creator.forkedObservers.iterator(); i.hasNext();) {
	    TaskObserver.Forked observer = (TaskObserver.Forked) i.next();
	    if (observer.updateForkedOffspring(creator, this) == Action.BLOCK) {
		blockers.add(observer);
	    }
	}
	return blockers.size();
    }
    /**
     * Add a TaskObserver.Forked observer.
     */
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	logger.log(Level.FINE, "{0} requestAddForkedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, forkedObservers, o);
    }
    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	logger.log(Level.FINE, "{0} requestDeleteForkedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, forkedObservers, o);
    }

    /**
     * Set of Terminated observers.
     */
    private final TaskObservable terminatedObservers = new TaskObservable(this);
    /**
     * Notify all Terminated observers, of this Task's demise. Return
     * the number of blocking observers.(Does this make any sense?)
     */
    int notifyTerminated(boolean signal, int value) {
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
     * Add a TaskObserver.Terminated observer.
     */
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	logger.log(Level.FINE, "{0} requestAddTerminatedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, terminatedObservers, o);
    }
    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	logger.log(Level.FINE, "{0} requestDeleteTerminatedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, terminatedObservers, o);
    }

    /**
     * Set of Terminating observers.
     */
    private final TaskObservable terminatingObservers = new TaskObservable(this);
    /**
     * Notify all Terminating observers, of this Task's demise. Return
     * the number of blocking observers.
     */
    int notifyTerminating(boolean signal, int value) {
	for (Iterator i = terminatingObservers.iterator(); i.hasNext();) {
	    TaskObserver.Terminating observer = (TaskObserver.Terminating) i.next();
	    if (observer.updateTerminating(this, signal, value) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }
    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public void requestAddTerminatingObserver(Terminating o) {
	logger.log(Level.FINE, "{0} requestAddTerminatingObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, terminatingObservers, o);
    }
    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver(Terminating o) {
	logger.log(Level.FINE, "{0} requestDeleteTerminatingObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, terminatingObservers, o);
    }

    /**
     * Set of Execed observers.
     */
    private final TaskObservable execedObservers = new TaskObservable(this);
    /**
     * Notify all Execed observers, of this Task's demise. Return the
     * number of blocking observers.
     */
    int notifyExeced() {
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
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public void requestAddExecedObserver(TaskObserver.Execed o) {
	logger.log(Level.FINE, "{0} requestAddExecedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, execedObservers, o);
    }

    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	logger.log(Level.FINE, "{0} requestDeleteExecedObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, execedObservers, o);
    }

    /**
     * Set of Syscall observers. Checked in TaskState.
     *
     * FIXME: LinuxPtraceProc screws around with this; should be
     * private.
     */
    final TaskObservable syscallObservers = new TaskObservable(this);
    /**
     * Notify all Syscall observers of this Task's entry into a system
     * call.  Return the number of blocking observers.
     */
    int notifySyscallEnter() {
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
     */
    int notifySyscallExit() {
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
     * Add TaskObserver.Syscalls to the TaskObserver pool.
     */
    public void requestAddSyscallsObserver(TaskObserver.Syscalls o) {
	logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddSyscallObserver(this, syscallObservers, o);
    }
    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	logger.log(Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteSyscallObserver(this, syscallObservers, o);
    }


    /**
     * Set of Signaled observers.
     */
    private final TaskObservable signaledObservers = new TaskObservable(this);
    /**
     * Notify all Signaled observers of the signal. Return the number
     * of blocking observers.
     */
    int notifySignaled(int sig) {
	logger.log(Level.FINE, "{0} notifySignaled(int)\n", this);
	for (Iterator i = signaledObservers.iterator(); i.hasNext();) {
	    TaskObserver.Signaled observer = (TaskObserver.Signaled) i.next();
	    if (observer.updateSignaled(this, sig) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }
    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	logger.log(Level.FINE, "{0} requestAddSignaledObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddObserver(this, signaledObservers, o);
    }
    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	logger.log(Level.FINE, "{0} requestDeleteSignaledObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, signaledObservers, o);
    }

  
    /**
     * Set of Code observers.
     *
     * FIXME: Should be private only LinuxPtraceTaskState grubs around
     * with this variable.
     */
    final TaskObservable codeObservers = new TaskObservable(this);
    /**
     * Notify all Code observers of the breakpoint. Return the number
     * of blocking observers or -1 if no Code observer were installed
     * on this address.
     */
    int notifyCodeBreakpoint(long address) {
	logger.log(Level.FINE, "{0} notifyCodeBreakpoint({1})\n",
		   new Object[] { this, Long.valueOf(address) });
	Collection observers = ((LinuxPtraceProc)getProc()).breakpoints.getCodeObservers(address);
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
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddCodeObserver(this, codeObservers, o, a);
    }

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	logger.log(Level.FINE, "{0} requestDeleteCodeObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteCodeObserver(this, codeObservers, o, a);
    }

  
    /**
     * Set of Instruction observers.
     *
     * FIXME: LinuxPtraceProc and LinuxPtraceTaskState grub around
     * with this; chould not be public.
     */
    final TaskObservable instructionObservers = new TaskObservable(this);
    /**
     * Notify all Instruction observers. Returns the total number of
     * blocking observers.
     */
    int notifyInstruction() {
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
     * Request the addition of a Instruction observer that will be
     * notified as soon as the task executes an instruction.
     * <code>o.updateExecuted</code> is called as soon as the Task
     * starts running again (is not blocked or stopped) and executes
     * the next instruction.
     */
    public void requestAddInstructionObserver(TaskObserver.Instruction o) {
	logger.log(Level.FINE, "{0} requestAddInstructionObserver\n", this);
	((LinuxPtraceProc)getProc()).requestAddInstructionObserver(this, instructionObservers, o);
    }
    /**
     * Delete TaskObserver.Instruction from the TaskObserver pool.
     */
    public void requestDeleteInstructionObserver(TaskObserver.Instruction o) {
	logger.log(Level.FINE, "{0} requestDeleteInstructionObserver\n", this);
	((LinuxPtraceProc)getProc()).requestDeleteInstructionObserver(this, instructionObservers, o);
    }
  
    /**
     * List containing the TaskObservations that are pending addition
     * or deletion (in order that they were requested). Will be dealt
     * with as soon as a stop event is received during one of the
     * running states.
     */
    final LinkedList pendingObservations = new LinkedList();

    /**
     * (Internal) Request that all observers from this task be
     * removed.  Warning, should also be removed from the proc's
     * observations.
     */
    void removeObservers() {
	logger.log(Level.FINE, "{0} removeObservers", this);	 
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

    /**
     * Whether we have just started the Task. Set in
     * wantToAttachContinue.blockOrAttachContinue() and immediately
     * reset in sendContinue() unless we request a step or
     * Running.handleTrappedEvent() when the first step is
     * received.
     *
     * XXX: This is a temporary hack to work around bug #4663. Needs
     * to be merged with SteppingState (see step_send).
     */
    boolean justStartedXXX;

    /**
     * The signal, or zero, send last to the task.
     *
     * XXX: This should be a state in Linux/PTRACE state machine.
     */
    public int sigSendXXX;

    /**
     * When the last request to the process was a step request,
     * whether it was a request to step a sigreturn syscall.  Set by
     * sendStepInstruction().
     *
     * XXX: This should be a state in Linux/PTRACE state machine.
     */
    public boolean syscallSigretXXX;

    private int modCount = 0;
    public void incrementMod() {
	modCount++;
    }
    public int getMod() {
	return modCount;
    }
    private Register pcRegister;
    private Register pcRegister() {
	if (pcRegister == null)
	    pcRegister = RegistersFactory
		.getRegisters(getISA())
		.getProgramCounter();
	return pcRegister;
    }
    public long getPC() {
	return getRegister(pcRegister());
    }
    public void setPC(long addr) {
	setRegister(pcRegister(), addr);
    }

    public void clearIsa() {
	super.clearIsa();
	pcRegister = null;
	memory = null;
    }

    /**
     * Whether we are currently stepping over a breakpoint.  Used in
     * the running task state when a trap event occurs after a step
     * has been issued. Null when no step is being performed.
     *
     * XXX: This variable belongs in the Linux/PTRACE state machine.
     */
    public Breakpoint steppingBreakpoint;

}
