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
import frysk.sys.XXX;

/**
 * Check that tasks of a process can be manually stopped.
 *
 */

public class TestStop
    extends TestLib
{
    Task mainTask;
    Task thread1;
    Task thread2;
    int pid;
    int taskCreatedCount;
    int taskDestroyedCount;
    int taskStopCount;

    // As soon as the process is created, attach a task created
    // observer.

    class ProcCreatedObserver
        implements Observer
    {
	Task task;
        public void update (Observable o, Object obj)
        {
            Proc proc = (Proc) obj;
	    pid = proc.id.hashCode ();
            proc.taskDiscovered.addObserver (new TaskCreatedObserver ());
	    proc.taskRemoved.addObserver (new TaskDestroyedObserver ());
        }
    }
 
    // Once the task has been created, schedule a terminate signal.

    class TaskCreatedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    Task task = (Task) obj;
	    assertEquals ("No terminated events before task creation", 0,
			  taskDestroyedCount);
	    taskCreatedCount++;
	    if (task.id.id == task.proc.id.id)
		mainTask = task;
	    else if (thread1 == null)
		thread1 = task;
	    else if (task.id.hashCode () != thread1.id.hashCode ())
		thread2 = task;
	    if (taskCreatedCount == 3)
	        Manager.eventLoop.addTimerEvent (new DetachTimerEvent (mainTask, 100));
	    task.requestedStopEvent.addObserver (taskEventObserver);
	}
    }

    class TaskDestroyedObserver
	implements Observer
    {
	int eventSig;
	public void update (Observable o, Object obj)
	{
	    taskDestroyedCount++;
	    // If it wasn't a terminate event, the task will fail.
	    TaskEvent.Terminated terminatedTaskEvent = (TaskEvent.Terminated) obj;
	    eventSig = terminatedTaskEvent.signal;
	}
    }

    class AllStoppedTimerEvent
        extends frysk.event.TimerEvent
    {
        long numberOfTimerEvents = 0;
        Task task;
	long milliseconds;
        AllStoppedTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
        {
            if (task != null) {
		assertEquals ("Main task stopped", TaskState.stopped,
			      mainTask.state);
		assertEquals ("Thread1 stopped", TaskState.stopped,
			      thread1.state);
		assertEquals ("Thread2 stopped", TaskState.stopped,
			      thread2.state);
		task.proc.detach ();
	    }
        }
    }

    class TaskEventObserver
 	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    TaskEvent e = (TaskEvent) obj;
	    if (++taskStopCount == 3) {
	        Manager.eventLoop.addTimerEvent (new AllStoppedTimerEvent (mainTask, 0));
	    }
 	}
    }

    TaskEventObserver taskEventObserver = new TaskEventObserver ();

    class DetachTimerEvent
        extends frysk.event.TimerEvent
    {
        long numberOfTimerEvents = 0;
        Task task;
	long milliseconds;
        DetachTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
        {
            if (task != null) {
		mainTask.requestStop ();
		thread1.requestStop ();
		thread2.requestStop ();
	    }
        }
    }

    public void testStop ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	// Create threaded infinite loop
	int pid = XXX.infThreadLoop (2);
	Manager.host.requestAttachProc (new ProcId (pid));

        // Register child to be removed at end of test
        registerChild (pid);

        // Once a proc destroyed has been seen stop the event loop.
        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("Task created events = 3", 3,
		      taskCreatedCount);
	assertEquals ("Stop events received = 3", 3,
		      taskStopCount);
	assertEquals ("No task destroyed events", 0,
		      taskDestroyedCount);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
