// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.proc.ptrace;

import frysk.proc.TaskObserver;
import frysk.proc.LinuxPtraceTask;
import frysk.proc.Proc;
import frysk.proc.TaskId;
import frysk.proc.Task;
import java.util.logging.Level;
import frysk.proc.Manager;
import frysk.proc.TaskEvent;

/**
 * A Linux Task tracked using PTRACE.
 */

public class LinuxTask
    extends LinuxPtraceTask
{
    /**
     * Create a new unattached Task.
     */
    public LinuxTask (Proc proc, TaskId id)
    {
	super(proc, id, LinuxTaskState.detachedState());
    }
    /**
     * Create a new attached clone of Task.
     */
    public LinuxTask (Task task, TaskId clone)
    {
	// XXX: shouldn't need to grub around in the old task's state.
	super(task, clone,
	      LinuxTaskState.clonedState(((LinuxTask)task).getState ()));
    }
    /**
     * Create a new attached main Task of Proc.
     */
    public LinuxTask (Proc proc, TaskObserver.Attached attached)
    {
	super(proc, attached, LinuxTaskState.mainState());
    }

    /**
     * (internal) This task cloned creating the new Task cloneArg.
     */
    void processClonedEvent (Task clone)
    {
	set(oldState().handleClonedEvent(this, clone));
    }
    /**
     * (internal) This Task forked creating an entirely new child process
     * containing one (the fork) task.
     */
    void processForkedEvent (Task fork)
    {
	set(oldState().handleForkedEvent(this, fork));
    }
    /**
     * (internal) This task stopped.
     */
    void processStoppedEvent ()
    {
	set(oldState().handleStoppedEvent(this));
    }
    /**
     * (internal) This task encountered a trap.
     */
    void processTrappedEvent ()
    {
	set(oldState().handleTrappedEvent(this));
    }
    /**
     * (internal) This task received a signal.
     */
    void processSignaledEvent (int sig)
    {
	set(oldState().handleSignaledEvent(this, sig));
    }
    /**
     * (internal) The task is in the process of terminating. If SIGNAL, VALUE is
     * the signal, otherwize it is the exit status.
     */
    void processTerminatingEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatingEvent(this, signal, value));
    }
    /**
     * (internal) The task has disappeared (due to an exit or some other error
     * operation).
     */
    void processDisappearedEvent (Throwable arg)
    {
	set(oldState().handleDisappearedEvent(this, arg));
    }
    /**
     * (internal) The task is performing a system call.
     */
    void processSyscalledEvent ()
    {
	set(oldState().handleSyscalledEvent(this));
    }
    /**
     * (internal) The task has terminated; if SIGNAL, VALUE is the signal,
     * otherwize it is the exit status.
     */
    void processTerminatedEvent (boolean signal, int value)
    {
	set(oldState().handleTerminatedEvent(this, signal, value));
    }
    /**
     * (internal) The task has execed, overlaying itself with another program.
     */
    void processExecedEvent ()
    {
	set(oldState().handleExecedEvent(this));
    }

    /**
     * Must inject disappeared events back into the event loop so that
     * they can be processed in sequence. Calling
     * receiveDisappearedEvent directly would cause a recursive state
     * transition.
     */
    protected void postDisappearedEvent (final Throwable arg)
    {
	logger.log(Level.FINE, "{0} postDisappearedEvent\n", this);
	Manager.eventLoop.add(new TaskEvent()
	    {
		final Throwable w = arg;
		public void execute ()
		{
		    processDisappearedEvent(w);
		}
	    });
    }

}
