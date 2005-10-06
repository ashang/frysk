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

import java.util.LinkedList;
import inua.eio.ByteBuffer;

abstract public class Task
{
    protected TaskId id;
    protected Proc proc;

    /**
     * Return the task's corresponding TaskId.
     */
    public final TaskId getTaskId ()
    {
	return id;
    }

    /**
     * Return the task's process id.
     */
    public final int getTid ()
    {
	return id.id;
    }

    /**
     * Returns the tasks' InstructionSetArchitecture.
     *
     * XXX: To simplify bootstrap, instead of making this opaque,
     * provide a default.  It is assumed that derived tasks extend
     * both the isa and the mechanism for getting the field.
     */
    public Isa getIsa ()
    {
	return bootstrapIsa;
    }
    private static Isa bootstrapIsa = new Isa ();

    /**
     * Return the task's entry point address.
     *
     * This is the address of the first instruction that the task will
     * have executed.
     *
     * XXX: Not yet implemented.
     */
    public long getEntryPointAddress ()
    {
	return 0xdeadbeefL;
    }

    /**
     * Return the containing Proc.
     */
    public Proc getProc ()
    {
	return proc;
    }

    // Contents of a task.
    ByteBuffer memory;
    ByteBuffer[] registerBank;

    // Flags indicating the intended state of various trace options.
    // Typically the thread has to first be stopped before the option
    // can change -> number of state transitions.
    public boolean traceFork;
    public boolean traceExit;
    public boolean traceSyscall;  	// Trace syscall entry and exit

    /**
     * Create a new, possibly attached, possibly running, task.
     */
    private Task (Proc proc, TaskId id, boolean attached, boolean running)
    {
	this.proc = proc;
	this.id = id;
	state = TaskState.initial (this, attached, running);
	proc.add (this);
	proc.host.add (this);
	// XXX: For backward compatibility.
	if (!attached)
	    proc.taskDiscovered.notify (this);
    }

    /**
     * Create a new task, it is not attached (so RUNNING doesn't make
     * sense).
     */
    Task (Proc proc, TaskId id)
    {
	this (proc, id, false, true);
    }

    /**
     * Create a new task, it is attached.
     */
    Task (Proc proc, TaskId id, boolean running)
    {
	this (proc, id, true, running);
    }

    // Send operation to corresponding underlying [kernel] task.
    protected abstract void sendContinue (int sig);
    protected abstract void sendStepInstruction (int sig);
    protected abstract void sendStop ();
    protected abstract void sendSetOptions ();
    protected abstract void sendAttach ();
    protected abstract void sendDetach (int sig);

    protected LinkedList queuedEvents = new LinkedList ();

    /**
     * The current state of this task.
     */
    /* XXX: private */ TaskState state;
    /**
     * Return the state represented as a simple string.
     */
    public String getStateString ()
    {
	return state.toString ();
    }

    /**
     * Event requesting that the task stop.
     */
    void requestStop ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestStop (Task.this);
		}
	    });
    }

    /**
     * Requesting that the task go (or resume execution).
     */
    void requestContinue ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestContinue (Task.this);
		}
	    });
    }

    /**
     * Request that the task step a single instruction.
     */
    void requestStepInstruction ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestStepInstruction (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to remove itself (it is no longer
     * listed in the system process table and, presumably, has
     * exited).
     *
     * This method is package local.  Only proc/ internals should be
     * making this request.
     */
    void performRemoval ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformRemoval (Task.this);
		}
	    });
    }
    /**
     * (Internal) Tell the task to attach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been completed.  The task is left in the stopped state.
     */
    void performAttach ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformAttach (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to detach itself (if it isn't
     * already).  Notify the containing process once the operation has
     * been processed; the task is allowed to run free.
     */
    void performDetach ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformDetach (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to stop itself.  Notify the containing
     * process once the operation has been processed.
     */
    void performStop ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformStop (Task.this);
		}
	    });
    }

    /**
     * (Internal) Tell the task to continue itself.  Notify the
     * containing process once the operation has been processed.
     */
    void performContinue ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processPerformContinue (Task.this);
		}
	    });
    }

    boolean isStopped ()
    {
	return state.isStopped ();
    }
    boolean isRunning ()
    {
	return state.isRunning ();
    }
    boolean isDead ()
    {
	return state.isDead ();
    }

    public TaskEventObservable syscallEvent = new TaskEventObservable ();
    public TaskEventObservable stopEvent = new TaskEventObservable ();
    public TaskEventObservable stepEvent = new TaskEventObservable ();
    public TaskEventObservable requestedStopEvent = new TaskEventObservable ();

    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",id=" + id
		+ ",proc=" + proc
		+ ",state=" + state
		+ "}");
    }
}
