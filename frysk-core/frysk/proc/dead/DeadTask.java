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
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.TaskState;
import frysk.proc.TaskId;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObservation;
import frysk.proc.TaskEvent;

/**
 * A dead Host/Proc/Task is characterised by its lack of state, and an
 * in ability to respond to stateful requests such as add/remove
 * observers.
 */

abstract class DeadTask extends Task {
    DeadTask(Proc proc, TaskId taskId, TaskState initialState) {
	super(proc, taskId, initialState);
    }

    protected String getStateFIXME() {
	return "dead";
    }

    /**
     * Set the new state.
     */
    protected final void set(TaskState newState) {
	// ignore
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
	logger.log(Level.FINE, "{0} requestUnblock -- observer\n", this);
	Manager.eventLoop.add(new TaskEvent(this) {
		final TaskObserver observer = observerArg;
		protected void execute(Task task) {
		    task.handleUnblock(observer);
		}
	    });
    }

    /**
     * Add a TaskObserver.Cloned observer.
     */
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	logger.log(Level.FINE, "{0} requestAddClonedObserver\n", this);
	getProc().requestAddObserver(this, clonedObservers, o);
    }

    /**
     * Delete a TaskObserver.Cloned observer.
     */
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	logger.log(Level.FINE, "{0} requestDeleteClonedObserver\n", this);
	getProc().requestDeleteObserver(this, clonedObservers, o);
    }

    /**
     * Add a TaskObserver.Attached observer.
     */
    public void requestAddAttachedObserver(TaskObserver.Attached o) {
	logger.log(Level.FINE, "{0} requestAddAttachedObserver\n", this);
	getProc().requestAddObserver(this, attachedObservers, o);
    }

    /**
     * Delete a TaskObserver.Attached observer.
     */
    public void requestDeleteAttachedObserver(TaskObserver.Attached o) {
	logger.log(Level.FINE, "{0} requestDeleteAttachedObserver\n", this);
	getProc().requestDeleteObserver(this, attachedObservers, o);
    }

    /**
     * Add a TaskObserver.Forked observer.
     */
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	logger.log(Level.FINE, "{0} requestAddForkedObserver\n", this);
	getProc().requestAddObserver(this, forkedObservers, o);
    }

    /**
     * Delete a TaskObserver.Forked observer.
     */
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	logger.log(Level.FINE, "{0} requestDeleteForkedObserver\n", this);
	getProc().requestDeleteObserver(this, forkedObservers, o);
    }

    /**
     * Add a TaskObserver.Terminated observer.
     */
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	logger.log(Level.FINE, "{0} requestAddTerminatedObserver\n", this);
	getProc().requestAddObserver(this, terminatedObservers, o);
    }

    /**
     * Delete a TaskObserver.Terminated observer.
     */
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	logger.log(Level.FINE, "{0} requestDeleteTerminatedObserver\n", this);
	getProc().requestDeleteObserver(this, terminatedObservers, o);
    }

    /**
     * Add TaskObserver.Terminating to the TaskObserver pool.
     */
    public void requestAddTerminatingObserver(TaskObserver.Terminating o) {
	logger.log(Level.FINE, "{0} requestAddTerminatingObserver\n", this);
	getProc().requestAddObserver(this, terminatingObservers, o);
    }

    /**
     * Delete TaskObserver.Terminating.
     */
    public void requestDeleteTerminatingObserver(TaskObserver.Terminating o) {
	logger.log(Level.FINE, "{0} requestDeleteTerminatingObserver\n", this);
	getProc().requestDeleteObserver(this, terminatingObservers, o);
    }

    /**
     * Add TaskObserver.Execed to the TaskObserver pool.
     */
    public void requestAddExecedObserver(TaskObserver.Execed o) {
	logger.log(Level.FINE, "{0} requestAddExecedObserver\n", this);
	getProc().requestAddObserver(this, execedObservers, o);
    }

    /**
     * Delete TaskObserver.Execed.
     */
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	logger.log(Level.FINE, "{0} requestDeleteExecedObserver\n", this);
	getProc().requestDeleteObserver(this, execedObservers, o);
    }

    /**
     * Add TaskObserver.Syscalls to the TaskObserver pool.
     */
    public void requestAddSyscallsObserver(TaskObserver.Syscalls o) {
	logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
	getProc().requestAddSyscallObserver(this, syscallObservers, o);
    }

    /**
     * Delete TaskObserver.Syscall.
     */
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	logger.log(Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
	getProc().requestDeleteSyscallObserver(this, syscallObservers, o);
    }

    /**
     * Add TaskObserver.Signaled to the TaskObserver pool.
     */
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	logger.log(Level.FINE, "{0} requestAddSignaledObserver\n", this);
	getProc().requestAddObserver(this, signaledObservers, o);
    }

    /**
     * Delete TaskObserver.Signaled.
     */
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	logger.log(Level.FINE, "{0} requestDeleteSignaledObserver\n", this);
	getProc().requestDeleteObserver(this, signaledObservers, o);
    }

  
    /**
     * Add TaskObserver.Code to the TaskObserver pool.
     */
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
	getProc().requestAddCodeObserver(this, codeObservers, o, a);
    }

    /**
     * Delete TaskObserver.Code for the TaskObserver pool.
     */
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	logger.log(Level.FINE, "{0} requestDeleteCodeObserver\n", this);
	getProc().requestDeleteCodeObserver(this, codeObservers, o, a);
    }
}
