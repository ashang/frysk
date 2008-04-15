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

import frysk.proc.TaskObserver.Terminating;
import frysk.isa.syscalls.SyscallTable;
import frysk.isa.syscalls.SyscallTableFactory;
import inua.eio.ByteBuffer;
import java.util.Set;
import java.util.HashSet;
import frysk.isa.registers.Register;
import frysk.isa.ISA;
import frysk.isa.banks.RegisterBanks;
import frysk.isa.signals.SignalTable;
import frysk.isa.signals.SignalTableFactory;
import frysk.rsl.Log;

public abstract class Task {
    private static final Log fine = Log.fine(Task.class);

    /**
     * Create a new Task skeleton.
     */
    private Task(int pid, Proc proc) {
	this.proc = proc;
	this.tid = pid;
	this.id = new TaskId(pid);
	proc.add(this);
    }

    /**
     * Create a new unattached Task.
     */
    protected Task(Proc proc, int pid) {
	this(pid, proc);
	fine.log("new -- create unattached");
    }

    /**
     * Create a new attached CLONE of TASK.
     */
    protected Task(Task cloningTask, int clone) {
	this(clone, cloningTask.getProc());
	fine.log(this, "new -- create attached clone");
    }

    /**
     * Create a newly created and attached main Task of Proc that is
     * the result of a fork.
     */
    protected Task(Proc proc) {
	this(proc.getPid(), proc);
    }

    /**
     * Return the task's corresponding TaskId.
     */
    TaskId getTaskId() {
	return id;
    }
    private final TaskId id;
    private final int tid;

    /**
     * Return the task's process id.
     */
    public final int getTid() {
	return tid;
    }

    /**
     * Return the task's (derived) name
     */
    public final String getName() {
	return "Task " + tid;
    }

    /**
     * Return the state as a string; do not use!!!!
     */
    protected abstract String getStateFIXME();

    /**
     * Return's this Task's Instruction Set Architecture; or throw a
     * NullPointerException if the ISA is not available.
     *
     * The isa is only available while the task is under observation
     * (attached) as an un-observed task can switch isa's (using
     * exec(2)) undetected.
     */
    public abstract ISA getISA();

    private SyscallTable syscallTable;
    public final SyscallTable getSyscallTable() {
	if (syscallTable == null)
	    syscallTable = SyscallTableFactory.getSyscallTable(getISA());
	return syscallTable;
    }

    /**
     * Return the containing Proc.
     */
    public Proc getProc() {
	return proc;
    }
    private final Proc proc;

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
		+ ",state=" + getStateFIXME()
		+ "}");
    }

    /**
     * XXX: Code using this needs a re-think.
     */
    public Set bogusUseOfInternalBlockersVariableFIXME() {
	return new HashSet();
    }

    /**
     * Request that the observer be removed from this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public abstract void requestUnblock(final TaskObserver observerArg);

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public abstract void requestAddClonedObserver(TaskObserver.Cloned o);
    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public abstract void requestDeleteClonedObserver(TaskObserver.Cloned o);

    /**
     * Add a TaskAttachedObserverXXX observer.
     */
    public abstract void requestAddAttachedObserver(TaskAttachedObserverXXX o);
    /**
     * Delete a TaskAttachedObserverXXX observer.
     */
    public abstract void requestDeleteAttachedObserver(TaskAttachedObserverXXX o);

    /**
     * Add a TaskObserver.Forked observer.
     */
    public abstract void requestAddForkedObserver(TaskObserver.Forked o);
    /**
     * Delete a TaskObserver.Forked observer.
     */
    public abstract void requestDeleteForkedObserver(TaskObserver.Forked o);

    /**
     * Add a TaskObserver.Terminated observer.
     */
    public abstract void requestAddTerminatedObserver(TaskObserver.Terminated o);
    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public abstract void requestDeleteTerminatedObserver(TaskObserver.Terminated o);

    /**
     * Add the Terminating observer to the TaskObserver pool.
     */
    public abstract void requestAddTerminatingObserver(Terminating o);
    /**
     * Delete the Terminating observer.
     */
    public abstract void requestDeleteTerminatingObserver(Terminating o);

    /**
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public abstract void requestAddExecedObserver(TaskObserver.Execed o);
    /**
     * Delete TaskObserver.Execed.
     */
    public abstract void requestDeleteExecedObserver(TaskObserver.Execed o);

    /**
     * Add TaskObserver.Syscalls to the TaskObserver pool.
     */
    public abstract void requestAddSyscallsObserver(TaskObserver.Syscalls o);
    /**
     * Delete TaskObserver.Syscall.
     */
    public abstract void requestDeleteSyscallsObserver(TaskObserver.Syscalls o);

    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public abstract void requestAddSignaledObserver(TaskObserver.Signaled o);
    /**
     * Delete TaskObserver.Signaled.
     */
    public abstract void requestDeleteSignaledObserver(TaskObserver.Signaled o);

    /**
     * Return the Task's memory.
     */
    public abstract ByteBuffer getMemory();

    /**
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public abstract void requestAddCodeObserver(TaskObserver.Code o, long a);
    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public abstract void requestDeleteCodeObserver(TaskObserver.Code o, long a);
  
    /**
     * Add TaskObserver.Watch to the TaskObserver pool.
     */
    public abstract void requestAddWatchObserver(TaskObserver.Watch o, long address, 
	    					int length, boolean writeOnly);

    /**
     * Delete TaskObserver.Watchfor the TaskObserver pool.
     */
    public abstract void requestDeleteWatchObserver(TaskObserver.Watch o, long address, 
	    					int length, boolean writeOnly);

    
    
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
     * Returns true if the observer has been added to task's
     * instruction observer list.
     */
    public abstract boolean isInstructionObserverAdded (TaskObserver.Instruction o);
    /**
     * Return the address of the instruction that this task will
     * execute next.
     */
    public abstract long getPC();
    /**
     * Set the address of the instruction that this task will execute
     * next.
     */
    public abstract void setPC(long addr);

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

    /**
     * Return this task's register banks.
     */
    protected abstract RegisterBanks getRegisterBanks();

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
     * Use a counter rather than a boolean because multiple caches may
     * depend on this count and no cache should be able to clear it.
     */
    public abstract int getMod();
 
    protected void clearIsa() {
	syscallTable = null;
	signalTable = null;
    }


    /**
     * Return a table of known (and unknown) signals for this ISA.
     */
    public SignalTable getSignalTable() {
	if (signalTable == null) {
	    signalTable = SignalTableFactory.getSignalTable(getISA());
	}
	return signalTable;
    }
    private SignalTable signalTable;

    public int compareTo(Object o) {
	Task other = (Task)o;
	int comp = getProc().compareTo(other.getProc());
	if (comp == 0)
	    comp = getTid() - other.getTid();
	return comp;
    }
}
