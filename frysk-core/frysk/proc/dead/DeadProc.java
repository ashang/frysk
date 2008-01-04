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
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Host;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObservable;
import frysk.proc.Manager;
import frysk.proc.TaskObservation;
import frysk.proc.ProcEvent;

/**
 * A dead Host/Proc/Task is characterised by its lack of state, and an
 * in ability to respond to stateful requests such as add/remove
 * observers.
 */

abstract class DeadProc extends Proc {
    DeadProc(Host host, Proc parent, ProcId id) {
	super(host, parent, id);
    }

    /**
     * Return the current state as a string.
     */
    protected String getStateFIXME() {
	return "dead";
    }
  
    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh() {
	logger.log(Level.FINE, "{0} requestRefresh\n", this);
	Manager.eventLoop.add(new ProcEvent(this) {
		public void execute() {
		    proc.sendRefresh ();
		}
	    });
    }

    protected void performDetach() {
	logger.log(Level.FINE, "{0} performDetach\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestAddObserver(Task task, TaskObservable observable,
				   TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddObserver\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary. Adds a syscallObserver
     * which changes the task to syscall tracing mode of necessary.
     *
     * XXX: Should not be public.
     */
    public void requestAddSyscallObserver(Task task,
					  TaskObservable observable,
					  TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary. Removes a
     * syscallObserver exiting the task from syscall tracing mode of
     * necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteObserver(Task task, TaskObservable observable,
				      TaskObserver observer) {
	Manager.eventLoop.add(new TaskObservation(task, observable,
						  observer, false) {
		public void execute() {
		    // Must be bogus; if there were observations then
		    // the Proc wouldn't be in this state.
		    fail(new RuntimeException ("not attached"));
		}
	    });
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteSyscallObserver(Task task,
					     TaskObservable observable,
					     TaskObserver observer) {
	throw new RuntimeException("the process is already dead");
    }

    /**
     * (Internal) Tell the process to add the specified Code
     * Observation, attaching to the process if necessary. Adds a
     * TaskCodeObservation to the eventloop which instructs the task
     * to install the breakpoint if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestAddCodeObserver(Task task, TaskObservable observable,
				       TaskObserver.Code observer,
				       long address) {
	logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * (Internal) Tell the process to delete the specified Code
     * Observation, detaching from the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteCodeObserver(Task task, TaskObservable observable,
					  TaskObserver.Code observer,
					  long address)    {
	throw new RuntimeException("the process is already dead");
    }

    /**
     * (Internal) Tell the process to add the specified Instruction
     * Observation, attaching and/or suspending the process if
     * necessary. As soon as the observation is added and the task
     * isn't blocked it will inform the Instruction observer of every
     * step of the task.
     *
     * XXX: Should not be public.
     */
    public void requestAddInstructionObserver(Task task,
					      TaskObservable observable,
					      TaskObserver.Instruction observer) {
	logger.log(Level.FINE, "{0} requestAddInstructionObserver\n", this);
	// XXX: Fake out for now. What kind of observers would you put
	// on a core file? Might need a brain dead attached state in
	// this scenario for compataibility.
    }

    /**
     * (Internal) Tell the process to delete the specified Instruction
     * Observation, detaching and/or suspending from the process if
     * necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteInstructionObserver(Task task,
						 TaskObservable observable,
						 TaskObserver.Instruction observer) {
	throw new RuntimeException("the process is already dead");
    }
}
