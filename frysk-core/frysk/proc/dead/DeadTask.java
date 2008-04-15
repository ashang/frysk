// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.proc.dead;

import frysk.proc.TaskAttachedObserverXXX;
import frysk.rsl.Log;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.TaskObserver;
import frysk.isa.ISA;
import frysk.isa.banks.RegisterBanks;

/**
 * A dead Host/Proc/Task is characterised by its lack of state, and an
 * in ability to respond to stateful requests such as add/remove
 * observers.
 */

abstract class DeadTask extends Task {
    private static final Log fine = Log.fine(DeadTask.class);

    DeadTask(Proc proc, int pid, ISA isa, RegisterBanks registerBanks) {
	super(proc, pid);
	this.isa = isa;
	this.registerBanks = registerBanks;
    }

    protected String getStateFIXME() {
	return "dead";
    }

    /**
     * Request that the observer be removed from this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public void requestUnblock(final TaskObserver observerArg) {
	fine.log(this, "requestUnblock is bogus");
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	fine.log(this, "requestAddClonedObserver is bogus");
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	throw new RuntimeException("requestDeleteClonedObserver");
    }

    /**
     * Add a TaskAttachedObserverXXX observer.
     */
    public void requestAddAttachedObserver(TaskAttachedObserverXXX o) {
	throw new RuntimeException("requestAddAttachedObserver");
    }

    /**
     * Delete a TaskAttachedObserverXXX observer.
     */
    public void requestDeleteAttachedObserver(TaskAttachedObserverXXX o) {
	throw new RuntimeException("requestDeleteAttachedObserver");
    }

    /**
     * Add a TaskObserver.Forked observer.
     */
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	fine.log(this, "requestAddForkedObserver is bogus");
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	throw new RuntimeException("requestDeleteForkedObserver");
    }

    /**
     * Add a TaskObserver.Terminated observer.
     */
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	fine.log(this, "requestAddTerminatedObserver is bogus");
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	throw new RuntimeException("requestDeleteTerminatedObserver");
    }

    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public void requestAddTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("requestAddTerminatingObserver");
    }

    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("requestDeleteTerminatingObserver");
    }

    /**
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public void requestAddExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("requestAddExecedObserver");
    }

    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("requestDeleteExecedObserver");
    }

    /**
     * Add TaskObserver.Syscalls to the TaskObserver pool.
     */
    public void requestAddSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("requestAddSyscallsObserver");
    }

    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("requestDeleteSyscallsObserver");
    }

    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("requestAddSignaledObserver");
    }

    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("requestDeleteSignaledObserver");
    }

    /**
     * Add TaskObserver.Watch to the TaskObserver pool.
     */
    public void requestAddWatchObserver(TaskObserver.Watch o, long address, 
	    				int length, boolean writeOnly) {
	throw new RuntimeException("requestAddWatchObserver");
    }

    /**
     * Delete TaskObserver.Watch from the TaskObserver pool.
     */
    public  void requestDeleteWatchObserver(TaskObserver.Watch o, long address, 
	    				int length, boolean writeOnly) {
	throw new RuntimeException("requestDeleteWatchObserver");
    }

    
    /**
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("requestAddCodeObserver");
    }

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("requestDeleteCodeObserver");
    }

    /**
     * Request the addition of a Instruction observer that will be
     * notified as soon as the task executes an instruction.
     * <code>o.updateExecuted</code> is called as soon as the Task
     * starts running again (is not blocked or stopped) and executes
     * the next instruction.
     */
    public void requestAddInstructionObserver(TaskObserver.Instruction o) {
	throw new RuntimeException("requestAddInstructionObserver");
    }

    /**
     * Delete TaskObserver.Instruction from the TaskObserver pool.
     */
    public void requestDeleteInstructionObserver(TaskObserver.Instruction o) {
	throw new RuntimeException("requestDeleteInstructionObserver");
    }
    
    public boolean isInstructionObserverAdded (TaskObserver.Instruction o) {
	throw new RuntimeException("isInstructionObserverAddded");
    }

    public int getMod() {
	return 1; // never changes.
    }

    public void setPC(long addr) {
	throw new RuntimeException("setPC: the task is dead");
    }

    public ISA getISA() {
	return isa;
    }
    private final ISA isa;

    protected RegisterBanks getRegisterBanks() {
	return registerBanks;
    }
    private final RegisterBanks registerBanks;
}
