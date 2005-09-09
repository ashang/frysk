// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

package frysk.proc;

import java.util.*;

abstract public class Task
{
    protected TaskId id;
    protected Proc proc;

    /**
     * Return the task's corresponding TaskId.
     */
    public TaskId getTaskId ()
    {
	return id;
    }

    /**
     * Return the task's process id.
     */
    public int getPid ()
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
    util.eio.ByteBuffer memory;
    util.eio.ByteBuffer[] registerBank;

    // Flags indicating the intended state of various trace options.
    // Typically the thread has to first be stopped before the option
    // can change -> number of state transitions.
    public boolean traceFork;
    public boolean traceExit;
    public boolean traceSyscall;  	// Trace syscall entry and exit

    /**
     * Create a new task, it is not attached (so RUNNABLE doesn't make
     * sense).
     */
    Task (Proc proc, TaskId id)
    {
	this.proc = proc;
	this.id = id;
	this.state = TaskState.unattached;
	proc.add (this);
	proc.host.add (this);
	proc.taskDiscovered.notify (this);
	proc.taskAdded.notify (this);
    }

    // all the info on a task
    Task (Proc proc, TaskId id, boolean runnable)
    {
	this.proc = proc;
	this.id = id;
	if (runnable)
	    state = TaskState.startRunning;
	else
	    state = TaskState.startStopped;
	proc.add (this);
	proc.host.add (this);
    }

    // Send operation to corresponding underlying [kernel] task.
    protected abstract void sendContinue (int sig);
    protected abstract void sendStepInstruction (int sig);
    protected abstract void sendStop ();
    protected abstract void sendSetOptions ();
    protected abstract void sendAttach ();

    protected LinkedList queuedEvents = new LinkedList ();

    TaskState state;

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
     * Request that the task be removed (it is no longer listed in the
     * system process table and, presumably, has exited).
     *
     * This method is package local.  Only proc/ internals should be
     * making this request.
     */
    void requestRemoval ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestRemoval (Task.this);
		}
	    });
    }
    /**
     * Request that the task attach itself (if it isn't already).
     * Notify parent once attached; the task is left in the stopped
     * state.
     */
    void requestAttach ()
    {
	Manager.eventLoop.appendEvent (new TaskEvent ()
	    {
		public void execute ()
		{
		    state = state.processRequestAttach (Task.this);
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
	return "[Task"
	    + ",id=" + id
	    + ",proc=" + proc
	    + ",state=" + state
	    + "]";
    }
}
