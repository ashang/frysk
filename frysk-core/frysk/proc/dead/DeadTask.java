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

import java.util.logging.Level;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.TaskId;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObservation;

/**
 * A dead Host/Proc/Task is characterised by its lack of state, and an
 * in ability to respond to stateful requests such as add/remove
 * observers.
 */

abstract class DeadTask extends Task {
    DeadTask(Proc proc, TaskId taskId) {
	super(proc, taskId);
    }

    protected String getStateFIXME() {
	return "dead";
    }

    /**
     * (Internal) Add the specified observer to the observable.
     */
    protected void handleAddObservation(TaskObservation observation) {
	throw new RuntimeException("oops!");
    }

    /**
     * (Internal) Delete the specified observer from the observable.
     */
    protected void handleDeleteObservation(TaskObservation observation) {
	throw new RuntimeException("oops!");
    }

    public void handleUnblock(TaskObserver observer) {
	throw new RuntimeException("oops!");
    }

    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    public void performContinue() {
	throw new RuntimeException("oops!");
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * XXX: Should not be public.
     */
    public void performRemoval() {
	throw new RuntimeException("oops!");
    }

    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been completed. The task is left in the stopped state.
     *
     * XXX: Should not be public.
     */
    public void performAttach() {
	throw new RuntimeException("oops!");
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     * @param shouldRemoveObservers whether to remove the observers as well.
     */
    public void performDetach(boolean shouldRemoveObservers) {
	throw new RuntimeException("oops!");
    }

    /**
     * Request that the observer be removed from this tasks set of
     * blockers; once there are no blocking observers, this task
     * resumes.
     */
    public void requestUnblock(final TaskObserver observerArg) {
	logger.log(Level.FINE, "{0} requestUnblock is bogus\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	logger.log(Level.FINE, "{0} requestAddClonedObserver is bogus\n", this);
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
     * Add a TaskObserver.Attached observer.
     */
    public void requestAddAttachedObserver(TaskObserver.Attached o) {
	throw new RuntimeException("requestAddAttachedObserver");
    }

    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver(TaskObserver.Attached o) {
	throw new RuntimeException("requestDeleteAttachedObserver");
    }

    /**
     * Add a TaskObserver.Forked observer.
     */
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	logger.log(Level.FINE, "{0} requestAddForkedObserver is bogus\n", this);
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
	logger.log(Level.FINE, "{0} requestAddTerminatedObserver is bogus\n", this);
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
}
