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
import frysk.sys.Signal;
import frysk.sys.Sig;

/**
 * Check that we can attach to a multi-tasked process.
 */

public class TestAttachThreads
    extends TestLib
{
    Task mainTask;
    int taskCreatedCount;
    int taskDestroyedCount;
    int taskDestroyedEventSig;

    // As soon as the process is created, attach a task created
    // observer.

    class ProcCreatedObserver
        implements Observer
    {
	Task task;
        public void update (Observable o, Object obj)
        {
            Proc proc = (Proc) obj;
            proc.taskDiscovered.addObserver (new TaskCreatedObserver ());
	    proc.taskDestroyed.addObserver (new TaskDestroyedObserver ());
        }
    }
 
    // Once the task has been created, schedule a terminate signal.

    class TaskCreatedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    Task task = (Task) obj;
	    assertEquals ("No termination before task creation", 0,
			  taskDestroyedCount);
	    taskCreatedCount++;
	    if (task.id.hashCode () == task.proc.id.hashCode ())
		mainTask = task;
	    if (taskCreatedCount == 3)
	        Manager.eventLoop.addTimerEvent (new KillTimerEvent (mainTask, 100));
	}
    }

    class TaskDestroyedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    taskDestroyedCount++;
	    // If it wasn't a terminate event, the task will fail.
	    TaskEvent.Terminated terminatedTaskEvent = (TaskEvent.Terminated) obj;
	    taskDestroyedEventSig = terminatedTaskEvent.signal;
	}
    }

    class KillTimerEvent
        extends frysk.event.TimerEvent
    {
        Task task;
	long milliseconds;
        KillTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
        {
            if (task != null)
                Signal.tkill (task.id.id, Sig.KILL);
        }
    }

    public void testAttachThreads ()
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
	assertEquals ("Task destroyed events = 3", 3,
		      taskDestroyedCount);
	assertEquals ("SIGKILL received", Sig.KILL,
		      taskDestroyedEventSig);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
