// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

package frysk.proc.dummy;

import inua.eio.ByteBuffer;
import frysk.proc.TaskObserver;
import frysk.proc.Isa;
import frysk.proc.TaskState;
import frysk.proc.TaskObservation;
import frysk.isa.ISA;
import frysk.bank.RegisterBanks;
import frysk.proc.Task;
import frysk.proc.Proc;

public class DummyTask extends Task {
    public DummyTask (Proc parent) {
	super (parent, (TaskObserver.Attached) null, null);
    }
    public String getStateString() {
	return "Attached";
    }
    protected Isa sendrecIsa() {
	return null;
    }
    protected ISA sendrecISA() {
	return null;
    }
    protected ByteBuffer sendrecMemory () {
	return null;
    }
    protected RegisterBanks sendrecRegisterBanks() {
	return null;
    }

    /**
     * The state of this task. During a state transition newState is
     * NULL.
     */
    private TaskState oldState;
    private TaskState newState;

    /**
     * Return the current state.
     */
    protected final TaskState getState() {
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
    protected final void set(TaskState newState) {
	this.newState = newState;
    }

    /**
     * Return the current state while at the same time marking that
     * the state is in flux. If a second attempt to change state
     * occurs before the current state transition has completed,
     * barf. XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    protected TaskState oldState() {
	if (newState == null)
	    throw new RuntimeException(this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }

    /**
     * (Internal) Add the specified observer to the observable.
     */
    protected void handleAddObservation(TaskObservation observation) {
	newState = oldState().handleAddObservation(this, observation);
    }

    /**
     * (Internal) Delete the specified observer from the observable.
     */
    protected void handleDeleteObservation(TaskObservation observation) {
	newState = oldState().handleDeleteObservation(this, observation);
    }

    public void handleUnblock(TaskObserver observer) {
	newState = oldState().handleUnblock(this, observer);
    }

    /**
     * (Internal) Requesting that the task go (or resume execution).
     */
    public void performContinue() {
	newState = oldState().handleContinue(this);
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * XXX: Should not be public.
     */
    public void performRemoval() {
	newState = oldState().handleRemoval(this);
    }

    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been completed. The task is left in the stopped state.
     *
     * XXX: Should not be public.
     */
    public void performAttach() {
	newState = oldState().handleAttach(this);
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already). Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     * @param shouldRemoveObservers whether to remove the observers as well.
     */
    public void performDetach(boolean shouldRemoveObservers) {
	newState = oldState().handleDetach(this, shouldRemoveObservers);
    }

    public void requestUnblock(final TaskObserver observerArg) {
	throw new RuntimeException("oops!");
    }
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddAttachedObserver(TaskObserver.Attached o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteAttachedObserver(TaskObserver.Attached o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("oops!");
    }
}
