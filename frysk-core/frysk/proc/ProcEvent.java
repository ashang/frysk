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

import frysk.event.Event;

abstract class ProcEvent
    implements Event
{
    protected Proc proc;
    protected TaskId taskId; // Random task within Proc.
    ProcEvent ()
    {
    }
    ProcEvent (Proc proc)
    {
	this.proc = proc;
    }
    public Proc getProc ()
    {
	return proc;
    }
    /**
     * Create a proc event when the proc may not yet be known.
     *
     * See TaskEvent.ProcEvent(TaskId) for further discussion.
     */
    ProcEvent (TaskId taskId)
    {
	this.taskId = taskId;
    }
    /**
     * Find the Proc that owns the task.
     */
    protected Proc getProcFromTaskId ()
    {
	Task task = Manager.host.get (taskId);
	if (task == null)
	    return null;
	return proc = task.proc;
    }
    public String toString ()
    {
	return ("[ProcEvent"
		+ ",proc" + proc
		+ "]");
    }

    static class AllStopped
	extends ProcEvent
    {
	AllStopped (Proc proc)
	{
	    super (proc);
	}
	public void execute ()
	{
	    proc.state = proc.state.process (proc, this);
	}
	public String toString ()
	{
	    return ("[AllStopped"
		    + super.toString ()
		    + "]");
	}
    }

    /**
     * A task cloned.
     *
     * This takes the TaskId of the cloning task, and not the
     * containing proc.  See TaskEvent.Cloned for further discussion.
     */
    static class TaskCloned
	extends ProcEvent
    {
	protected TaskId cloneId;
	TaskCloned (TaskId taskId, TaskId cloneId)
	{
	    super (taskId);
	    this.cloneId = cloneId;
	}
	public TaskId getCloneId ()
	{
	    return cloneId;
	}
	public void execute ()
	{
 	    proc = getProcFromTaskId ();
 	    if (proc == null)
 		return;
 	    proc.state = proc.state.process (proc, this);
	}
	public String toString ()
	{
	    return ("[TaskCloned"
		    + super.toString ()
		    + "]");
	}
    }

    /**
     * A task forked.
     *
     * This takes the TaskId of the forking task, and not the
     * containing proc.  See TaskEvent.Cloned for further discussion.
     */
    static class TaskForked
	extends ProcEvent
    {
	protected ProcId forkId;
	TaskForked (TaskId taskId, ProcId forkId)
	{
	    super (taskId);
	    this.forkId = forkId;
	}
	public ProcId getForkId ()
	{
	    return forkId;
	}
	public void execute ()
	{
 	    proc = getProcFromTaskId ();
 	    if (proc == null)
 		return;
 	    proc.state = proc.state.process (proc, this);
	}
	public String toString ()
	{
	    return ("[TaskForked"
		    + super.toString ()
		    + ",forkId=" + forkId
		    + "]");
	}
    }

    /**
     * A task attached.
     */
    static class TaskAttached
	extends ProcEvent
    {
	Task task;
	TaskAttached (Proc proc, Task task)
	{
	    super (proc);
	    this.task = task;
	}
	public void execute ()
	{
 	    proc.state = proc.state.process (proc, this);
	}
	public String toString ()
	{
	    return ("{TaskAttached" + super.toString () + "}");
	}
    }
}
