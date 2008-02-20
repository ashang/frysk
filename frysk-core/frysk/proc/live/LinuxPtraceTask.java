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

import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import frysk.isa.registers.Register;
import frysk.isa.registers.RegistersFactory;
import java.util.Iterator;
import java.util.Collection;
import frysk.proc.Action;
import frysk.proc.TaskEvent;
import frysk.proc.Manager;
import frysk.proc.TaskObserver.Terminating;
import frysk.proc.TaskObserver;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.event.Event;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import frysk.sys.Errno;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ptrace.Ptrace;
import frysk.sys.ptrace.AddressSpace;
import frysk.sys.Signal;
import frysk.isa.syscalls.Syscall;
import frysk.isa.ISA;
import frysk.isa.ElfMap;
import java.io.File;
import frysk.isa.banks.RegisterBanks;
import frysk.rsl.Log;

/**
 * A Linux Task tracked using PTRACE.
 */

public class LinuxPtraceTask extends LiveTask {
    private static final Log fine = Log.fine(LinuxPtraceTask.class);

    /**
     * Create a new unattached Task.
     */
    public LinuxPtraceTask(Proc proc, ProcessIdentifier pid) {
	super(proc, pid);
	((LinuxPtraceHost)proc.getHost()).putTask(tid, this);
	newState = LinuxPtraceTaskState.detachedState();
	this.watchpoints = new WatchpointAddresses(this);
    }
    /**
     * Create a new attached clone of Task.
     */
    public LinuxPtraceTask(Task task, ProcessIdentifier clone) {
	// XXX: shouldn't need to grub around in the old task's state.
	super(task, clone);
	((LinuxPtraceHost)getProc().getHost()).putTask(tid, this);
	newState = LinuxPtraceTaskState.clonedState(((LinuxPtraceTask)task).getState ());
	this.watchpoints = new WatchpointAddresses(this);
    }
    /**
     * Create a new attached main Task of Proc.
     */
    public LinuxPtraceTask(LinuxPtraceProc proc,
			   TaskObserver.Attached attached) {
	super(proc, attached);
	((LinuxPtraceHost)proc.getHost()).putTask(tid, this);
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
	this.watchpoints = new WatchpointAddresses(this);

    }

    /**
     * Return the raw memory byte-buffer. This is the TEXT/DATA area.
     */
    ByteBuffer getRawMemory() {
	fine.log(this, "Begin fillMemory");
	ByteOrder byteOrder = getISA().order();
	ByteBuffer memory = new AddressSpaceByteBuffer(tid,
						       AddressSpace.DATA);
	memory.order(byteOrder);
	fine.log(this, "End fillMemory"); 
	return memory;
    }

    /**
     * Return the Task's memory.
     */
    public ByteBuffer getMemory() {
	if (memory == null) {
	    fine.log(this, "exiting get memory");
	    ByteOrder byteOrder = getISA().order();
	    BreakpointAddresses breakpoints = ((LinuxPtraceProc)getProc()).breakpoints;
	    memory = new LogicalMemoryBuffer(tid, AddressSpace.DATA,
					     breakpoints);
	    memory.order(byteOrder);
	}
	return memory;
    }
    private ByteBuffer memory;

    protected RegisterBanks getRegisterBanks() {
	if (registerBanks == null)
	    registerBanks = PtraceRegisterBanksFactory.create(getISA(), tid);
	return registerBanks;
    }
    private RegisterBanks registerBanks;

    /**
     * Return the Task's ISA.
     *
     * Can this instead look at AUXV?
     */
    public ISA getISA() {
	ISA scratch = currentISA;
	if (scratch == null)
	    throw new NullPointerException("ISA unavailable; task "
					   + this + " has no observers");
	return scratch;
    }
    private ISA currentISA;

    /**
     * Return the Task's ISA.
     *
     * XXX: This code locally, and not the IsaFactory, and definitly
     * not via a PID should be determining the ISA of the process.
     */
    public Isa getIsaFIXME() {
	fine.log(this, "sendrecIsa");
	IsaFactory factory = IsaFactory.getSingleton();
	return factory.getIsa(tid);
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
     * (internal) This task stopped with SIGNAL pending.
     */
    void processStoppedEvent(Signal signal) {
	fine.log(this, "stoppedEvent", signal);
	set(oldState().handleStoppedEvent(this, signal));
    }
    /**
     * (internal) The task is in the process of terminating. If SIGNAL
     * is non-ZERO the terminating signal, else STATUS is the exit
     * status.
     */
    void processTerminatingEvent(Signal signal, int value) {
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
     * (internal) The task has terminated; if SIGNAL is non-NULL the
     * termination signal else STATUS contains the exit status.
     */
    void processTerminatedEvent(Signal signal, int value) {
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
    protected void postDisappearedEvent(final Throwable arg) {
	fine.log(this, "postDisappearedEvent");
	Manager.eventLoop.add(new Event()
	    {
		final Throwable w = arg;
		public void execute ()
		{
		    processDisappearedEvent(w);
		}
	    });
    }

    void sendContinue(Signal sig) {
	fine.log(this, "sendContinue");
	sigSendXXX = sig;
        incrementMod();
	try {
	    Ptrace.cont(tid, sig);
	} catch (Errno.Esrch e) {
	    postDisappearedEvent(e);
	}
    }
    void sendSyscallContinue(Signal sig) {
	fine.log(this, "sendSyscallContinue");
	sigSendXXX = sig;
        incrementMod();
	try {
	    Ptrace.sysCall(tid, sig);
	} catch (Errno.Esrch e) {
	    postDisappearedEvent(e);
	}
    }
    void sendStepInstruction(Signal sig) {
	fine.log(this, "sendStepInstruction");
	sigSendXXX = sig;
        incrementMod();
	syscallSigretXXX = getIsaFIXME().isAtSyscallSigReturn(this);
	try {
	    Ptrace.singleStep(tid, sig);
	} catch (Errno.Esrch e) {
	    postDisappearedEvent(e);
	}
    }

    public void sendStop () {
	fine.log(this, "sendStop");
	Signal.STOP.tkill(tid);
    }


    public void sendAttach () {
	fine.log(this, "sendAttach");
	try
	    {
		Ptrace.attach(tid);

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
	    } catch (Errno.Eperm e) {
	    fine.log(this, "cannot attach to process", e);
	    } catch (Errno.Esrch e) {
		postDisappearedEvent(e);
	    }
    }

    void sendDetach(Signal sig) {
	fine.log(this, "sendDetach");
	clearIsa();
	try {
	    if (sig == Signal.STOP) {
		fine.log(this, "sendDetach/signal STOP");
		Signal.STOP.tkill(tid);
		Ptrace.detach(tid, Signal.NONE);
	    } else {
		Ptrace.detach(tid, sig);
	    }
	} catch (Exception e) {
	    // Ignore problems trying to detach, most of the time the
	    // problem is the process has already left the cpu queue
	}
    }

    // XXX: Should be selecting the trace flags based on the contents
    // of .observers?  Ptrace.optionTraceSysgood not set by default
    private long ptraceOptions = (Ptrace.OPTION_CLONE
				  | Ptrace.OPTION_FORK
				  | Ptrace.OPTION_EXIT
				  | Ptrace.OPTION_EXEC);
    void initializeAttachedState() {
	fine.log(this, "initializeAttachedState");
	Ptrace.setOptions(tid, ptraceOptions);
	// FIXME: This should use task.proc.getExe().  Only that
	// causes wierd failures; take a rain-check :-(
	currentISA = ElfMap.getISA(new File("/proc/" + tid + "/exe"));
    }
    void startTracingSyscalls() {
	fine.log(this, "startTracingSyscalls");
	ptraceOptions |= Ptrace.OPTION_SYSGOOD;
	Ptrace.setOptions(tid, ptraceOptions);
    }
    void stopTracingSyscalls() {
	fine.log(this, "stopTracingSyscalls");
	ptraceOptions &= ~Ptrace.OPTION_SYSGOOD;
	Ptrace.setOptions(tid, ptraceOptions);
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
	if (getState() != null)
	    return getState().toString();
	else
	    return "<null>";
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
	fine.log(this, "requestUnblock -- observer");
	Manager.eventLoop.add(new TaskEvent(this) {
		final TaskObserver observer = observerArg;
		protected void execute(Task task) {
		    ((LinuxPtraceTask)task).handleUnblock(observer);
		}
	    });
    }

    /**
     * Set of interfaces currently blocking this task.
     *
     * Package-private.
     */
    final Set blockers = new HashSet();
    public Set bogusUseOfInternalBlockersVariableFIXME() {
	return blockers;
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
	fine.log(this, "notifyClonedOffspring");
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
     * Add a TaskObserver.Watch observer
     * (hardware only)
     */
    public void requestAddWatchObserver(TaskObserver.Watch o, long address, int length) {
	fine.log(this,"requestAddWatchObserver");
	requestAddWatchObserver(this, codeObservers, o, address, length);
    }

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public void requestDeleteWatchObserver(TaskObserver.Watch o, long address, int length) {
	fine.log(this, "requestDeleteWatcheObserver");
	requestDeleteWatchObserver(this, codeObservers, o, address, length);
    }

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	fine.log(this, "requestAddClonedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, clonedObservers, o);
    }
    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	fine.log(this, "requestDeleteClonedObserver");
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
	fine.log(this, "notifyAttached");
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
	fine.log(this, "requestAddAttachedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, attachedObservers, o);
    }
    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver(TaskObserver.Attached o) {
	fine.log(this, "requestDeleteAttachedObserver");
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
	fine.log(this, "requestAddForkedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, forkedObservers, o);
    }
    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	fine.log(this, "requestDeleteForkedObserver");
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, forkedObservers, o);
    }

    /**
     * Set of Terminated observers.
     */
    private final TaskObservable terminatedObservers = new TaskObservable(this);
    /**
     * Notify all Terminated observers, of this Task's demise. Return
     * the number of blocking observers (does this make any sense?);
     * or -1 if there were no observers.
     */
    int notifyTerminated(boolean sig, int value) {
	frysk.isa.signals.Signal signal
	    = sig ? getSignalTable().get(value) : null;
	fine.log(this, "notifyTerminated signal", signal, "value", value);
	if (terminatedObservers.numberOfObservers() > 0) {
	    for (Iterator i = terminatedObservers.iterator(); i.hasNext();) {
		TaskObserver.Terminated observer
		    = (TaskObserver.Terminated) i.next();
		if (observer.updateTerminated(this, signal, value)
		    == Action.BLOCK) {
		    fine.log(this, "notifyTerminated adding", observer, "to blockers");
		    blockers.add(observer);
		}
	    }
	    return blockers.size();
	} else {
	    return -1;
	}
    }
    /**
     * Add a TaskObserver.Terminated observer.
     */
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	fine.log(this, "requestAddTerminatedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, terminatedObservers, o);
    }
    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	fine.log(this, "requestDeleteTerminatedObserver");
	((LinuxPtraceProc)getProc()).requestDeleteObserver(this, terminatedObservers, o);
    }

    /**
     * Set of Terminating observers.
     */
    private final TaskObservable terminatingObservers = new TaskObservable(this);
    /**
     * Notify all Terminating observers, of this Task's demise. Return
     * the number of blocking observers; or -1 of there are no
     * observers.
     */
    int notifyTerminating(boolean sig, int value) {
	frysk.isa.signals.Signal signal
	    = sig ? getSignalTable().get(value) : null;
	fine.log(this, "notifyTerminating signal", sig, "value", value);
	if (terminatingObservers.numberOfObservers() > 0) {
	    for (Iterator i = terminatingObservers.iterator(); i.hasNext();) {
		TaskObserver.Terminating observer
		    = (TaskObserver.Terminating) i.next();
		if (observer.updateTerminating(this, signal, value)
		    == Action.BLOCK) {
		    fine.log(this, "notifyTerminating adding", observer, "to blockers");
		    blockers.add(observer);
		}
	    }
	    return blockers.size();
	} else {
	    return -1;
	}
    }
    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public void requestAddTerminatingObserver(Terminating o) {
	fine.log(this, "requestAddTerminatingObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, terminatingObservers, o);
    }
    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver(Terminating o) {
	fine.log(this, "requestDeleteTerminatingObserver");
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
	initializeAttachedState();
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
	fine.log(this, "requestAddExecedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, execedObservers, o);
    }

    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	fine.log(this, "requestDeleteExecedObserver");
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
	fine.log(this, "notifySyscallEnter");
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
	fine.log(this, "notifySyscallExit");
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
	fine.log(this, "requestAddSyscallObserver");
	((LinuxPtraceProc)getProc()).requestAddSyscallObserver(this, syscallObservers, o);
    }
    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	fine.log(this, "requestDeleteSyscallObserver");
	((LinuxPtraceProc)getProc()).requestDeleteSyscallObserver(this, syscallObservers, o);
    }


    /**
     * Set of Signaled observers.
     */
    private final TaskObservable signaledObservers = new TaskObservable(this);
    /**
     * Notify all Signaled observers of the signal. Return the number
     * of blocking observers; or -1 if there are no observers.
     */
    int notifySignaled(int sig) {
	frysk.isa.signals.Signal signal = getSignalTable().get(sig);
	fine.log(this, "notifySignaled signal", sig);
	if (signaledObservers.numberOfObservers() > 0) {
	    for (Iterator i = signaledObservers.iterator(); i.hasNext();) {
		TaskObserver.Signaled observer
		    = (TaskObserver.Signaled) i.next();
		if (observer.updateSignaled(this, signal) == Action.BLOCK) {
		    fine.log(this, "notifySignaled adding", observer, "to blockers");
		    blockers.add(observer);
		}
	    }
	    return blockers.size();
	} else {
	    return -1;
	}

    }
    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	fine.log(this, "requestAddSignaledObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, signaledObservers, o);
    }
    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	fine.log(this, "requestDeleteSignaledObserver");
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
	fine.log(this, "notifyCodeBreakpoint address", address);
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
     * Class describing the action to take on the suspended Task
     * before adding or deleting a Watchpoint observer.
     */
    final class WatchpointAction implements Runnable {
	private final TaskObserver.Watch watch;

	private final Task task;

	private final long address;
	
	private final int length;

	private final boolean addition;

	WatchpointAction(TaskObserver.Watch watch, Task task, long address, int length,
			 boolean addition) {
	    this.watch = watch;
	    this.task = task;
	    this.address = address;
	    this.addition = addition;
	    this.length = length;
	}

	public void run() {
	    if (addition) {
		boolean mustInstall = watchpoints.addBreakpoint(watch, address, length);
		if (mustInstall) {
		    Watchpoint watchpoint;
		    watchpoint = Watchpoint.create(address, length, LinuxPtraceTask.this);
		    watchpoint.install(task);
		}
	    } else {
		boolean mustRemove = watchpoints.removeBreakpoint(watch, address, length);
		if (mustRemove) {
		    Watchpoint watchpoint;
		    watchpoint = Watchpoint.create(address, length, LinuxPtraceTask.this);
		    watchpoint.remove(task);
		}
	    }
	}
    }

    public final WatchpointAddresses watchpoints;

    /**
     * (Internal) Tell the task to add the specified Watchpoint
     * observation, attaching to the task if necessary. Adds a
     * TaskCodeObservation to the eventloop which instructs the task
     * to install the breakpoint if necessary.
     */
    void requestAddWatchObserver(Task task, TaskObservable observable,
				TaskObserver.Watch observer,
				final long address,
				final int length) {
	WatchpointAction watchAction = new WatchpointAction(observer, task, address, length, true);
	TaskObservation to;
	to = new TaskObservation((LinuxPtraceTask) task, observable, observer,
				 watchAction, true) {
		public void execute() {
		    handleAddObservation(this);
		}
		public boolean needsSuspendedAction() {
		    return watchpoints.getCodeObservers(address, length) == null;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified Watchpoint
     * observation, detaching from the process if necessary.
     */
    void requestDeleteWatchObserver(Task task, TaskObservable observable,
				   TaskObserver.Watch observer,
				   final long address,
				   final int length)    {
	WatchpointAction watchAction = new WatchpointAction(observer, task, address, length, false);
	TaskObservation to;
	to = new TaskObservation((LinuxPtraceTask)task, observable, observer, 
				 watchAction, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(LinuxPtraceTask.this, this);
		}

		public boolean needsSuspendedAction() {
		    return watchpoints.getCodeObservers(address, length).size() == 1;
		}
	    };

	Manager.eventLoop.add(to);
    }

    
    /**
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	fine.log(this, "requestAddCodeObserver");
	((LinuxPtraceProc)getProc()).requestAddCodeObserver(this, codeObservers, o, a);
    }

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	fine.log(this, "requestDeleteCodeObserver");
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
	fine.log(this, "notifyInstruction");
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
	fine.log(this, "requestAddInstructionObserver");
	((LinuxPtraceProc)getProc()).requestAddInstructionObserver(this, instructionObservers, o);
    }
    /**
     * Delete TaskObserver.Instruction from the TaskObserver pool.
     */
    public void requestDeleteInstructionObserver(TaskObserver.Instruction o) {
	fine.log(this, "requestDeleteInstructionObserver");
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
	fine.log(this, "removeObservers");	 
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
    Signal sigSendXXX = Signal.NONE;

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

    protected void clearIsa() {
	fine.log(this, "clearIsa");
	super.clearIsa();
	pcRegister = null;
	memory = null;
	currentISA = null;
	registerBanks = null;
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
