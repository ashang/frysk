// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
 * Root Task Observer.
 */

public interface TaskObserver
{
    /**
     * Acknowledge the request to add this Observer to the Task's set
     * of observers was processed.  Throwable is non-NULL if the
     * request failed.  Failure could be due to the Task having
     * exited, or that permission to add the observable was not
     * granted.
     */
    void added (Throwable e);

    /**
     * Acknowledge the request to delete this Observer from the tasks'
     * set of observers was processed.
     */
    void deleted ();

    /**
     * Interface used to notify of Task clone events.
     */
    public interface Cloned
	extends TaskObserver
    {
	/**
	 * Called when this TASK has cloned, creating CLONE.  Return
	 * true if this observer wants this task to block.
	 */
	boolean updateCloned (Task task, Task clone);
    }

    /**
     * Interface used to notify of Task forked (creating a new child
     * process) events.
     */
    public interface Forked
	extends TaskObserver
    {
	/**
	 * Called when the Task has forked, creating a child Proc.
	 * Return true if the observer wants the task to block.
	 */
	boolean updateForked (Task task, Proc child);
    }

    /**
     * Interface used to notify of a Task exec (overlaying the process
     * image with that of a new program).
     */
    public interface Execed
	extends TaskObserver
    {
	/**
	 * Called AFTER the Task has execed.  Return true if the
	 * observer wants the task to block.
	 */
	boolean updateExeced (Task task);
    }

    /**
     * Interface used to notify of a Task exiting.
     */
    public interface Exiting
	extends TaskObserver
    {
	/**
	 * Called while the Task is in the process of exiting, it
	 * still exists but not much other than examining it can be
	 * performed.  A +ve status indicates a normal exit, a -ve
	 * status indicates termination due to a signal.  Return true
	 * if the observer wants the task to block.
	 */
	boolean updateExiting (Task task, int status);
    }

    /**
     * Interface used to notify of an exited Task (the task no
     * longer exits).
     */
    public interface Exited
	extends TaskObserver
    {
	/**
	 * Called AFTER the Task has execed.
	 */
	boolean updateExeced (Task task, int status);
    }

    /**
     * Interface used to notify of a terminated Task (the task no
     * longer exists).
     */
    public interface Terminated
	extends TaskObserver
    {
	/**
	 * Called AFTER the Task has terminated.
	 */
	boolean updateTerminated (Task task, int status);
    }

    /**
     * Interface used to notify that a Task has a pending signal.
     */
    public interface Signaled
	extends TaskObserver
    {
	/**
	 * The SIGNAL is pending delivery to the task.  Return true to
	 * block the task's further execution.
	 */
	boolean updateTerminated (Task task, int signal);
    }

    /**
     * Interface used to notify of a Task either entering, or exiting
     * a system call.
     */
    public interface Syscall
	extends TaskObserver
    {
	/**
	 * The Task is entering a system call.  Return true to block
	 * the task's further execution.
	 */
	boolean updateSysEnter (Task task, int syscall);
	/**
	 * The task is exiting a system call.  Return true to block
	 * the task's further execution.
	 */
	boolean updateSysExit (Task task, int syscall);
    }

    /**
     * Interface used to notify that a Task has executed a single
     * instruction.
     */
    public interface Step
	extends TaskObserver
    {
	/**
	 * The task has executed one instruction.
	 */
	boolean updateStep (Task task);
    }

    /**
     * Interface used to notify of a Task that the task's execution
     * has reached a specific address.
     */
    public interface Breakpoint
	extends TaskObserver
    {
	/**
	 * Address of the breakpoint.
	 */
	long getAddress ();
	/**
	 * The task has hit the breakpoint.
	 */
	boolean updateBreakpoint (Task task);
    }
}
