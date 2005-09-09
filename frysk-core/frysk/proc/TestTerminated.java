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
import frysk.sys.Sig;
import frysk.sys.Signal;

/**
 * Check that process termination event is detected.
 *
 * This creates a program that runs an infinite loop.
 * A timer is set up to SIGKILL the program after a specified interval.
 * We expect to see the TaskTerminatedEvent with the SIGKILL code.
 */

public class TestTerminated
    extends TestLib
{
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
	    assertEquals ("No TaskTerminatedEvents before task creation", 0,
			  taskDestroyedCount);
	    taskCreatedCount++;
	    assertEquals ("Only one task created", 1, taskCreatedCount);
	    Manager.eventLoop.addTimerEvent (new KillTimerEvent (task, 100));
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

    public void testTerminated ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	// Create infinite loop
	Manager.host.requestCreateProc (new String[]
	    {
		"./prog/terminated/infloop"
	    });

        // Once a proc destroyed has been seen stop the event loop.
        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("run \"infloop\" until exit");

	assertEquals ("Task created events received = 1", 1,
		      taskCreatedCount); 
	assertEquals ("Task destroyed event received", 1,
		      taskDestroyedCount);
	assertEquals ("SIGKILL was received", Sig.KILL,
		      taskDestroyedEventSig);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
