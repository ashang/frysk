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

import frysk.proc.TaskAttachedObserverXXX;
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
import frysk.sys.ptrace.ByteSpace;
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
    private final LinuxPtraceTask creator;

    /**
     * Create a new unattached Task.
     */
    public LinuxPtraceTask(Proc proc, ProcessIdentifier pid) {
	super(proc, pid);
	this.creator = null; // not known.
	((LinuxPtraceHost)proc.getHost()).putTask(tid, this);
	((LinuxPtraceProc)proc).addTask(this);
	newState = LinuxPtraceTaskState.detachedState();
    }
    /**
     * Create a new attached clone of Task.
     */
    public LinuxPtraceTask(LinuxPtraceTask cloningTask,
			   ProcessIdentifier clone) {
	super(cloningTask, clone);
	this.creator = cloningTask;
	((LinuxPtraceHost)getProc().getHost()).putTask(tid, this);
	((LinuxPtraceProc)cloningTask.getProc()).addTask(this);
	// XXX: shouldn't need to grub around in the old task's state.
	newState = LinuxPtraceTaskState.clonedState(cloningTask.getState());
    }
    /**
     * Create a new attached main Task of Proc.
     */
    public LinuxPtraceTask(LinuxPtraceTask forkingTask,
			   LinuxPtraceProc proc,
			   TaskAttachedObserverXXX attached) {
	super(proc);
	this.creator = forkingTask;
	((LinuxPtraceHost)proc.getHost()).putTask(tid, this);
	((LinuxPtraceProc)proc).addTask(this);
	newState = LinuxPtraceTaskState.mainState();
	// See the various FIXMEs below around the isa, ISA,
	// currentISA, getISA(), getIsaFIXME() and clearIsa(). The
	// current design is such that the isa isn't a constant of a
	// proc (actually task), which means we cannot remove
	// breakpoint instructions from the fork (the breakpoint
	// instruction is an intrinsic of the isa).  Luckily we know
	// that the fork will have the same isa as the task it forked
	// from, so we explicitly set it now. We cannot do the
	// breakpoint resetting here (the LinuxPtraceTask is created,
	// but not fully setup yet), we do that in the
	// LinuxPtraceTaskState.StartMainTask.wantToDetach class
	// handlers.
	currentISA = forkingTask.currentISA;
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
	fine.log(this, "Begin fillMemory");
	ByteOrder byteOrder = getISA().order();
	ByteBuffer memory = new ByteSpaceByteBuffer(tid, ByteSpace.DATA);
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
	    memory = new LogicalMemoryBuffer(tid, ByteSpace.DATA,
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
    void processClonedEvent(LinuxPtraceTask clone) {
	try {
	    newState = oldState().handleClonedEvent(this, clone);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) This Task forked creating an entirely new child process
     * containing one (the fork) task.
     */
    void processForkedEvent(LinuxPtraceTask fork) {
	try {
	    newState = oldState().handleForkedEvent(this, fork);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) This task stopped with SIGNAL pending.
     */
    void processStoppedEvent(Signal signal) {
	try {
	    fine.log(this, "stoppedEvent", signal);
	    newState = oldState().handleStoppedEvent(this, signal);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) The task is in the process of terminating. If SIGNAL
     * is non-ZERO the terminating signal, else STATUS is the exit
     * status.
     */
    void processTerminatingEvent(Signal signal, int value) {
	try {
	    newState = oldState().handleTerminatingEvent(this, signal, value);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) The task has disappeared (due to an exit or some other error
     * operation).
     */
    void processDisappearedEvent(Throwable arg) {
	newState = handleDisappearedEvent(arg);
    }
    private LinuxPtraceTaskState handleDisappearedEvent(Throwable arg) {
	// Don't call oldState() here; things are stuffed without also
	// worrying about double state transitions.
	return oldState.handleTerminatedEvent(this, Signal.KILL,
					      -Signal.KILL.intValue());
    }
    /**
     * (internal) The task is performing a system call.
     */
    void processSyscalledEvent() {
	try {
	    newState = oldState().handleSyscalledEvent(this);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) The task has terminated; if SIGNAL is non-NULL the
     * termination signal else STATUS contains the exit status.
     */
    void processTerminatedEvent(Signal signal, int value) {
	try {
	    newState = oldState().handleTerminatedEvent(this, signal, value);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    /**
     * (internal) The task has execed, overlaying itself with another program.
     */
    void processExecedEvent() {
	try {
	    newState = oldState().handleExecedEvent(this);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
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
	try {
	    Ptrace.attach(tid);
	    
	    // XXX: Linux kernel has a 'feature' that if a process is
	    // already stopped and ptrace requests that it be stopped
	    // (again) in order to attach to it, the signal (SIGCHLD)
	    // notifying frysk of the attach's pending waitpid event
	    // isn't generated.  XXX: This line sends another signal
	    // to frysk notifying about the attach's pending waitpid
	    // regardless of whether the task is running or
	    // stopped. This avoids hangs on attaching to a stopped
	    // process. Bug 3316.

	    Signal.CHLD.tkill(frysk.sys.Tid.get());
	} catch (Errno.Eperm e) {
	    // FIXME: Need to propogate this back up to the initiator
	    // of the attach some how.
	    fine.log(this, "cannot attach to process", e);
	}
	// NOTE: If there's an Errno.Esrch that is allowed to
	// propogate all the way up to the state engine which will
	// immediatly execute "disappeared" - since the task is gone
	// just get the hell out of here.
    }

    void sendDetach(Signal sig) {
	fine.log(this, "sendDetach");
	clearIsa();
	if (sig == Signal.STOP) {
	    fine.log(this, "sendDetach/signal STOP");
	    Signal.STOP.tkill(tid);
	    Ptrace.detach(tid, Signal.NONE);
	} else {
	    Ptrace.detach(tid, sig);
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
	// FIXME: This should use task.proc.getExeFile().getSysRootedPath().  Only that
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
	try {
	    newState = oldState().handleAddObservation(this, observation);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    
    /**
     * (Internal) Delete the specified observer from the observable.
     */
    void handleDeleteObservation(TaskObservation observation) {
	try {
	    newState = oldState().handleDeleteObservation(this, observation);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }
    
    void handleUnblock(TaskObserver observer) {
	try {
	    newState = oldState().handleUnblock(this, observer);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }

    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    void performContinue() {
	try {
	    newState = oldState().handleContinue(this);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * XXX: Should not be public.
     */
    void performRemoval() {
	try {
	    newState = oldState().handleRemoval(this);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }

    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been completed. The task is left in the stopped state.
     *
     * XXX: Should not be public.
     */
    void performAttach() {
	try {
	    newState = oldState().handleAttach(this);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     * @param shouldRemoveObservers whether to remove the observers as well.
     */
    void performDetach(boolean shouldRemoveObservers) {
	try {
	    newState = oldState().handleDetach(this, shouldRemoveObservers);
	} catch (Errno.Esrch e) {
	    newState = handleDisappearedEvent(e);
	}
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
     * Set of Watch observers.
     **/
    private final TaskObservable watchObservers = new TaskObservable(this);

    /**
     * Notify all Watchpoint observers of the triggered watchpoint. 
     * Return the number of blocking observers, or 0 if nothing blocks.
     */
    int notifyWatchpoint(long address, int  length, boolean writeOnly) {
	LinuxPtraceProc proc  = (LinuxPtraceProc) getProc();
	Collection observers = proc.watchpoints.getWatchObservers(this, address, length, writeOnly);
	
	for (Iterator z = observers.iterator(); z.hasNext();) {
		TaskObserver.Watch observer = (TaskObserver.Watch) z.next();
		if (observer.updateHit(this, address, length) == Action.BLOCK) {
		    blockers.add(observer);
		}
	}
	return blockers.size();
    }

    /**
     * Add a TaskObserver.Watch observer
     * (hardware data breakpoint only)
     */
    public void requestAddWatchObserver(TaskObserver.Watch o, long address, int length, boolean writeOnly) {
	fine.log(this,"requestAddWatchObserver");
	((LinuxPtraceProc)getProc()).requestAddWatchObserver(this, watchObservers, o, address, length, writeOnly);
    }

    /**
     * Delete a TaskObserver.Watch observer
     * (hardware data breakpoint only)
     */
    public void requestDeleteWatchObserver(TaskObserver.Watch o, long address, int length, boolean writeOnly) {
	fine.log(this, "requestDeleteWatcheObserver");
	((LinuxPtraceProc)getProc()).requestDeleteWatchObserver(this, watchObservers, o, address, length, writeOnly);
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
	for (Iterator i = creator.clonedObservers.iterator();
	     i.hasNext();) {
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
	    TaskAttachedObserverXXX observer = (TaskAttachedObserverXXX) i.next();
	    if (observer.updateAttached(this) == Action.BLOCK)
		blockers.add(observer);
	}
	return blockers.size();
    }
    /**
     * Add a TaskAttachedObserverXXX observer.
     */
    public void requestAddAttachedObserver(TaskAttachedObserverXXX o) {
	fine.log(this, "requestAddAttachedObserver");
	((LinuxPtraceProc)getProc()).requestAddObserver(this, attachedObservers, o);
    }
    /**
     * Delete a TaskAttachedObserverXXX observer.
     */
    public void requestDeleteAttachedObserver(TaskAttachedObserverXXX o) {
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
	for (Iterator i = creator.forkedObservers.iterator();
	     i.hasNext();) {
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
	LinuxPtraceProc proc = (LinuxPtraceProc) getProc();
	Collection observers = proc.breakpoints.getCodeObservers(address);
	if (observers == null)
	    return -1;

	// Sanity check
	if (steppingBreakpoint != null)
	  throw new RuntimeException("Already breakpoint stepping: "
				     + steppingBreakpoint);

	// Reset pc, some architectures might leave the pc right after
	// the breakpoint, but since we haven't actually executed the
	// real instruction yet we want it to be at the actual address
	// of the original instruction.
	setPC(address);

	// All logic for determining how and where to step the              
	// Breakpoint is determined by Proc and                             
	// Breakpoint.prepareStep() (called in sendContinue).               
	Breakpoint bp = Breakpoint.create(address,proc);

	// TODO: This should really move us to a new TaskState.             
	// Currently we rely on the Task.steppingBreakpoint                 
	// being set and the Breakpoint/Instruction having all              
	// the state necessary.                                             
	steppingBreakpoint = bp;

	Iterator i = observers.iterator();
	while (i.hasNext()) {
	    BreakpointAddresses.CodeObserver co;
	    co = (BreakpointAddresses.CodeObserver) i.next();
	    if (co.task.equals(this))
		if (co.observer.updateHit(this, address) == Action.BLOCK)
		    blockers.add(co.observer);
	}
	return blockers.size();
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
    public boolean isInstructionObserverAdded (TaskObserver.Instruction o) {
	fine.log(this, "isInstructionObserverAdded");
	return (instructionObservers.contains(o))? true: false;
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
