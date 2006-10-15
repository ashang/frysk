// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

/**
 * Observable events generated by a Task.
 */

public interface TaskObserver
    extends Observer
{
    /**
     * Interface used to notify of Task clone events.
     */
    public interface Cloned
	extends TaskObserver
    {
	/**
	 * Called when the Task (the parent) has cloned, creating a
	 * clone Task (the offspring).  Return Action.BLOCK if this
	 * observer wants the parent Task to block.
	 */
	Action updateClonedParent (Task task, Task clone);
	/**
	 * Called when the Task (the offspring) that was created by a
	 * fork has stopped at its first instruction.
	 */
	Action updateClonedOffspring (Task parent, Task offspring);
    }

    /**
     * Interface used to notify of Task forked (creating a new child
     * process that contains one Task) events.
     */
    public interface Forked
	extends TaskObserver
    {
	/**
	 * Called when the Task (the parent) has forked, creating a
	 * child Proc containing a single Task (the offspring).
	 * Return Action.BLOCK if the observer wants the parent task
	 * to block.
	 */
	Action updateForkedParent (Task parent, Task offspring);
	/**
	 * Called when the Task (the offspring) that was created by a
	 * fork has stopped at its first instruction.
	 */
	Action updateForkedOffspring (Task parent, Task offspring);
    }

    /**
     * Interface used to notify of a Task exec (overlaying the process
     * image with that of a new program).
     */
    public interface Execed
	extends TaskObserver
    {
	/**
	 * Called AFTER the Task has execed.  Return Action.BLOCK if
	 * the observer wants the task to block.
	 */
	Action updateExeced (Task task);
    }

    /**
     * Interface used to notify of a Task that is terminating.
     */
    public interface Terminating
	extends TaskObserver
    {
	/**
	 * Called while the Task is terminating; while the process
	 * still exists not much other than examining it can be
	 * performed.  If SIGNAL, the termination was forced using
	 * signal VALUE, otherwize the termination is due to an
	 * _exit(2) call.
	 */
	Action updateTerminating (Task task, boolean signal,
					   int value);
    }

    /**
     * Interface used to notify that Task has terminated (the task no
     * longer exits).
     */
    public interface Terminated
	extends TaskObserver
    {
	/**
	 * Called once the Task has terminated; the process no longer
	 * exists.  If SIGNAL, the termination was forced using signal
	 * VALUE, otherwize the termination is due to an _exit(2)
	 * call.
	 */
	Action updateTerminated (Task task, boolean signal,
					  int value);
    }

    /**
     * Interface used to notify that a Task has a pending signal.
     */
    public interface Signaled
	extends TaskObserver
    {
	/**
	 * The SIGNAL is pending delivery to the task.  Return
	 * Action.BLOCK to block the task's further execution.
	 *
	 * XXX: This gets weird.  At present and in theory, a client
	 * wanting to discard a signal would need to sequence the
	 * following: tell the task to scrub discard the signal; tell
	 * the task to remove this observer from the set of blockers;
	 * return Action.BLOCK so that this task is added to the set
	 * of blockers.  Perhaps it would be better to always add an
	 * observer to the blocker pool and then require explict
	 * removal.
	 */
	Action updateSignaled (Task task, int signal);
    }

    /**
     * Interface used to notify of a Task either entering, or exiting
     * a system call.
     */
    public interface Syscall
	extends TaskObserver
    {
	/**
	 * The Task is entering a system call.  Return Action.BLOCK to
	 * block the task's further execution.
	 */
	Action updateSyscallEnter (Task task);
	/**
	 * The task is exiting a system call.  Return Action.BLOCK to
	 * block the task's further execution.
	 */
	Action updateSyscallExit (Task task);
    }

    /**
     * Interface used to notify that a Task has executed a single
     * instruction. <code>updateExecuted</code> is called as soon as
     * the Instruction observer is added to the Task. And whenever the
     * Task starts running again (isn't blocked or suspended) it will
     * be called on each instruction being executed.
     * <p>
     * This TaskObserver can also be used for executing code that
     * needs the Task to be (temporarily) blocked or suspended as soon
     * as possible. <code>updateExecuted()</code> will be called as
     * soon as this observer has been properly added, and at that time
     * the Task is suspended to make it possible to inspect the Task
     * state. If no other action is request, the method can then just
     * delete the observer from the Task again.
     */
    public interface Instruction
	extends TaskObserver
    {
	/**
	 * The task has started executing or has executed another
	 * instruction.  Return Action.BLOCK to block the task's
	 * further execution.  When Action.CONTINUE is returned
	 * this method will be called as soon as one instruction
	 * has been executed.
	 */
	Action updateExecuted (Task task);
    }

    /**
     * Interface used to notify of a Task that the task's execution
     * has reached a specific point in the code address space.
     */
    public interface Code
	extends TaskObserver
    {
	/**
	 * The task has hit the breakpoint.  Return Action.BLOCK to
	 * block the task's further execution.  Note that all Tasks of
	 * a Proc share their breakpoints, so this method needs to
	 * check the actual Task that got hit.
	 */
      Action updateHit (Task task, long address);
    }

    /**
     * Interface used to notify of a Task that has has been attached,
     * and is about to resume execution in that state.  Only after a
     * Task has been attached can its internals be manipulated
     * (registers, memory, auxv).
     *
     * Adding this observer does not cause a Task to be attached.
     *
     * XXX: Place holder until a better named observer comes to be.
     */
    public interface Attached
	extends TaskObserver
    {
	/**
	 * The Task is attached.
	 */
	Action updateAttached (Task task);
    }
}
