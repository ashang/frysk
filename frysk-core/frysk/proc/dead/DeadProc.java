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
import frysk.proc.Action;
import frysk.proc.Breakpoint;
import frysk.proc.ProcEvent;
import frysk.proc.ProcState;

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
     * The current state of this Proc, during a state transition
     * newState is null.
     */
    private ProcState oldState;
    private ProcState newState;

    /**
     * Return the current state as a string.
     */
    protected String getStateFIXME() {
	if (newState != null)
	    return newState.toString();
	else
	    return oldState.toString();
    }
    protected void setStateFIXME(ProcState state) {
	newState = state;
    }

    /**
     * Return the current state while at the same time marking that
     * the state is in flux. If a second attempt to change state
     * occurs before the current state transition has completed,
     * barf. XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    private ProcState oldState() {
	if (newState == null)
	    throw new RuntimeException(this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }
  
    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh() {
	logger.log(Level.FINE, "{0} requestRefresh\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleRefresh(DeadProc.this);
		}
	    });
    }

    /**
     * (Internal) Tell the process that is no longer listed in the
     * system table remove itself.
     *
     * XXX: This should not be public.
     */
    public void performRemoval() {
	logger.log(Level.FINEST, "{0} performRemoval -- no longer in /proc\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleRemoval(DeadProc.this);
		}
	    });
    }

    /**
     *(Internal) Tell the process that the corresponding task has
     * completed its attach.
     *
     * XXX: Should not be public.
     */
    public void performTaskAttachCompleted (final Task theTask) {
	logger.log(Level.FINE, "{0} performTaskAttachCompleted\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;

		public void execute() {
		    newState = oldState().handleTaskAttachCompleted(DeadProc.this, task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     *
     * XXX: Should not be public.
     */
    public void performTaskDetachCompleted(final Task theTask) {
	logger.log(Level.FINE, "{0} performTaskDetachCompleted\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;
		public void execute() {
		    newState = oldState().handleTaskDetachCompleted(DeadProc.this, task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    protected void performTaskDetachCompleted(final Task theTask, final Task theClone) {
	logger.log(Level.FINE, "{0} performTaskDetachCompleted/clone\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;

		Task clone = theClone;

		public void execute() {
		    newState = oldState().handleTaskDetachCompleted(DeadProc.this, task, clone);
		}
	    });
    }

    protected void performDetach() {
	logger.log(Level.FINE, "{0} performDetach\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleDetach(DeadProc.this, true);
		}
	    });
    }

    /**
     * (internal) Tell the process to add the specified Observation,
     * attaching the process if necessary.
     */
    protected void handleAddObservation(TaskObservation observation) {
	newState = oldState().handleAddObservation(this, observation);
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestAddObserver(Task task, TaskObservable observable,
			    TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddObservation\n", this);
	Manager.eventLoop.add(new TaskObservation(task, observable, observer, true) {
		public void execute() {
		    handleAddObservation(this);
		}
	    });
    }

    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting a Syscall observer.
     */
    final class SyscallAction implements Runnable {
	private final Task task;

	private final boolean addition;

	SyscallAction(Task task, boolean addition) {
	    this.task = task;
	    this.addition = addition;
	}

	public void run() {
	    int syscallobs = task.syscallObservers.numberOfObservers();
	    if (addition) {
		if (syscallobs == 0)
		    task.startTracingSyscalls();
	    } else {
		if (syscallobs == 0)
		    task.stopTracingSyscalls();
	    }
	}
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary. Adds a syscallObserver
     * which changes the task to syscall tracing mode of necessary.
     *
     * XXX: Should not be public.
     */
    public void requestAddSyscallObserver(final Task task, TaskObservable observable,
				   TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
	SyscallAction sa = new SyscallAction(task, true);
	TaskObservation to = new TaskObservation(task, observable, observer, sa,
						 true) {
		public void execute() {
		    handleAddObservation(this);
		}
		public boolean needsSuspendedAction() {
		    return task.syscallObservers.numberOfObservers() == 0;
		}
	    };
	Manager.eventLoop.add(to);
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
		    newState = oldState().handleDeleteObservation(DeadProc.this, this);
		}
	    });
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteSyscallObserver(final Task task,
				      TaskObservable observable,
				      TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
	SyscallAction sa = new SyscallAction(task, false);
	TaskObservation to = new TaskObservation(task, observable, observer, sa,
						 false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(DeadProc.this,
								  this);
		}

		public boolean needsSuspendedAction() {
		    return task.syscallObservers.numberOfObservers() == 1;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting a Code observer.
     */
    final class BreakpointAction implements Runnable {
	private final TaskObserver.Code code;

	private final Task task;

	private final long address;

	private final boolean addition;

	BreakpointAction(TaskObserver.Code code, Task task, long address,
			 boolean addition) {
	    this.code = code;
	    this.task = task;
	    this.address = address;
	    this.addition = addition;
	}

	public void run() {
	    if (addition) {
		boolean mustInstall = breakpoints.addBreakpoint(code, address);
		if (mustInstall) {
		    Breakpoint breakpoint;
		    breakpoint = Breakpoint.create(address, DeadProc.this);
		    breakpoint.install(task);
		}
	    } else {
		boolean mustRemove = breakpoints.removeBreakpoint(code, address);
		if (mustRemove) {
		    Breakpoint breakpoint;
		    breakpoint = Breakpoint.create(address, DeadProc.this);
		    breakpoint.remove(task);
		}
	    }
	}
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
				final long address) {
	logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
	BreakpointAction bpa = new BreakpointAction(observer, task, address, true);
	TaskObservation to;
	to = new TaskObservation(task, observable, observer, bpa, true) {
		public void execute() {
		    handleAddObservation(this);
		}
		public boolean needsSuspendedAction() {
		    return breakpoints.getCodeObservers(address) == null;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified Code
     * Observation, detaching from the process if necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteCodeObserver(Task task, TaskObservable observable,
				   TaskObserver.Code observer,
				   final long address)    {
	logger.log(Level.FINE, "{0} requestDeleteCodeObserver\n", this);
	BreakpointAction bpa = new BreakpointAction(observer, task, address, false);
	TaskObservation to;
	to = new TaskObservation(task, observable, observer, bpa, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(DeadProc.this, this);
		}

		public boolean needsSuspendedAction() {
		    return breakpoints.getCodeObservers(address).size() == 1;
		}
	    };

	Manager.eventLoop.add(to);
    }

    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting an Instruction observer. No
     * particular actions are needed, but we must make sure the Task
     * is suspended.
     */
    final static class InstructionAction implements Runnable {
	public void run()
	{
	    // There is nothing in particular we need to do. We just want
	    // to make sure the Task is stopped so we can send it a step
	    // instruction or, when deleted, start resuming the process
	    // normally.

	    // We do want an explicit updateExecuted() call, after adding
	    // the observer, but while still suspended. This is done by
	    // overriding the add() method in the TaskObservation
	    // below. No such action is required on deletion.
	}
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
    public void requestAddInstructionObserver(final Task task,
				       TaskObservable observable,
				       TaskObserver.Instruction observer) {
	logger.log(Level.FINE, "{0} requestAddInstructionObserver\n", this);
	TaskObservation to;
	InstructionAction ia = new InstructionAction();
	to = new TaskObservation(task, observable, observer, ia, true) {
		public void execute() {
		    handleAddObservation(this);
		}

		public boolean needsSuspendedAction() {
		    return task.instructionObservers.numberOfObservers() == 0;
		}

		// Makes sure that the observer is properly added and then,
		// while the Task is still suspended, updateExecuted() is
		// called. Giving the observer a chance to inspect and
		// possibly block the Task.
		public void add() {
		    super.add();
		    TaskObserver.Instruction i = (TaskObserver.Instruction) observer;
		    if (i.updateExecuted(task) == Action.BLOCK)
			task.blockers.add(observer);
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified Instruction
     * Observation, detaching and/or suspending from the process if
     * necessary.
     *
     * XXX: Should not be public.
     */
    public void requestDeleteInstructionObserver(final Task task,
					  TaskObservable observable,
					  TaskObserver.Instruction observer) {
	logger.log(Level.FINE, "{0} requestDeleteInstructionObserver\n", this);
	TaskObservation to;
	InstructionAction ia = new InstructionAction();
	to = new TaskObservation(task, observable, observer, ia, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(DeadProc.this, this);
		}
		public boolean needsSuspendedAction() {
		    return task.instructionObservers.numberOfObservers() == 1;
		}
	    };
	Manager.eventLoop.add(to);
    }
}
