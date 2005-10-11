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

import frysk.event.Event;

public abstract class TaskEvent
    implements Event
{
    protected Task task;
    protected TaskId taskId;
    TaskEvent ()
    {
    }
    TaskEvent (Task task)
    {
	this.task = task;
    }
    /**
     * Construct an event that doesn't yet have a corresponding task.
     *
     * In the case of an event generated by the host, because those
     * events are first accumulated and then processed in batch, at
     * the time of event creation the actual Task may not yet exist.
     * For instance, the Clone event that causes the cloned task to be
     * added to the process may not have been executed at the time
     * that the clone'd tasks stop event gets created..  Handle this
     * by delaying the search for the actual Task until the task
     * execution point.
     *
     * Similarly, in the case of zombied tasks, events stray events
     * for tasks can be received.  Handle those by allowing such
     * dropped events to fall to the floor.
     */
    TaskEvent (TaskId taskId)
    {
	this.taskId = taskId;
    }
    public Task getTask ()
    {
	return this.task;
    }
    public String toString ()
    {
	return ("[TaskEvent"
		+ ",task" + task
		+ "]");
    }

    /**
     * This task has forked, creating a new child process and task.
     */
    static class Execed
	extends TaskEvent
    {
	Execed (TaskId taskId)
	{
	    super (taskId);
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Execed" + super.toString ()
		    + "]");
	}
    }

    /**
     * The task has exited (no more process)
     */
    static class Exited
	extends TaskEvent
    {
	protected int status;
	Exited (TaskId taskId, int status)
	{
	    super (taskId);
	    this.status = status;
	}
	public int getStatus ()
	{
	    return status;
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Exited" + super.toString ()
		    + ",status=" + status
		    + "]");
	}
    }

    /**
     * The task is exiting.
     */
    static class Exiting
	extends TaskEvent
    {
	protected int signal;
	Exiting (TaskId taskId, int signal)
	{
	    super (taskId);
	    this.signal = signal;
	}
	public int getSignal ()
	{
	    return signal;
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Exiting" + super.toString ()
		    + ",signal=" + signal
		    + "]");
	}
    }

    /**
     * This task has terminated and is in a zombie state.
     */
    static class Zombied
	extends TaskEvent
    {
	Zombied (TaskId taskId)
	{
	    super (taskId);
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Zombied" + super.toString ()
		    + "]");
	}
    }

    /**
     * The TASK is about to receive a SIGNAL.
     */
    static class Signaled
	extends TaskEvent
    {
	protected int signal;
	Signaled (Task task, int signal)
	{
	    super (task);
	    this.signal = signal;
	}
	public int getSignal ()
	{
	    return signal;
	}
	public void execute ()
	{
	    throw new RuntimeException ("should not happen");
	}
	public String toString ()
	{
	    return ("[Signaled"
		    + super.toString ()
		    + ",signal=" + signal
		    + "]");
	}
    }

    /**
     * The task stopped using Sig.STOP.
     */
    static class Stopped
	extends TaskEvent
    {
	Stopped (Task task)
	{
	    super (task);
	}
	public void execute ()
	{
	    throw new RuntimeException ("should not happen");
	}
	public String toString ()
	{
	    return ("[Stopped"
		    + super.toString ()
		    + "]");
	}
    }

    /**
     * The task is either entering or exiting a system call.
     */
    static class Syscall
	extends TaskEvent
    {
	Syscall (TaskId taskId)
	{
	    super (taskId);
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Syscall" + super.toString ()
		    + "]");
	}
    }

    /**
     * The task was terminated (due to a signal)
     */
    static class Terminated
	extends TaskEvent
    {
	protected int signal;
	Terminated (TaskId taskId, int signal)
	{
	    super (taskId);
	    this.signal = signal;
	}
	public void execute ()
	{
	    task = Manager.host.get (taskId);
	    if (task == null)
		return;
	    task.state = task.state.process (task, this);
	}
	public String toString ()
	{
	    return ("[Terminated" + super.toString ()
		    + ",signal=" + signal
		    + "]");
	}
    }

    /**
     * The task encountered a trap event.
     *
     * This could be due to the attempted execution of a breakpoint, or
     * single stepping an instruction.
     */
    static class Trapped
	extends TaskEvent
    {
	Trapped (Task task)
	{
	    super (task);
	}
	public void execute ()
	{
	    throw new RuntimeException ("should not happen");
	}
	public String toString ()
	{
	    return ("[Trapped"
		    + super.toString ()
		    + "]");
	}
    }
}
