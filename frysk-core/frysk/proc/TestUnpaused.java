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
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.XXX;

/**
 * Check that if a task is requested to stop and receives a natural
 * stop event first, that the task goes into the paused state.  When continued,
 * the task will go into the unpaused state and finally when the requested
 * SIGSTOP gets received, it will go back into the normal running state.
 *
 */

public class TestUnpaused
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
	    if (taskDestroyedCount > 0)
		throw new RuntimeException ("TaskTerminatedEvent before TaskCreatedEvent");
	    taskCreatedCount++;
	    if (task.id.id == task.proc.id.id)
		mainTask = task;
	    else if (thread1 == null)
		thread1 = task;
	    else if (task.id.hashCode () != thread1.id.hashCode ())
		thread2 = task;
	    if (taskCreatedCount == 3)
	        Manager.eventLoop.addTimerEvent (new StopTimerEvent (mainTask, 500));
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

    class StopEventObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    TaskEvent.Signaled ste = (TaskEvent.Signaled)obj;
	    assertEquals ("Expect task to be running", TaskState.running,
			  ste.task.state);
	    ste.task.requestStop ();  // Extraneous stop
	}
    }

    StopEventObserver stopEventObserver = new StopEventObserver ();

    class AllStoppedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    ProcEvent pe = (ProcEvent)obj;
	    LinuxProc p = (LinuxProc)pe.proc;
	    p.detach ();
	    Manager.eventLoop.requestStop ();
	}
    }

    class RunningCheckTimerEvent
        extends frysk.event.TimerEvent
    {
        Task task;
	long milliseconds;
        RunningCheckTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
	{
            if (task != null) {
		mainTask.requestContinue ();  // Extraneous go
		if (mainTask.state != TaskState.running) {
	 	    Manager.eventLoop.addTimerEvent (new RunningCheckTimerEvent (mainTask, 500));	
		    return;
		}
		else if (thread1.state != TaskState.running) {
	 	    Manager.eventLoop.addTimerEvent (new RunningCheckTimerEvent (mainTask, 500));
		    return;
		}
		else if (thread2.state != TaskState.running) {
	 	    Manager.eventLoop.addTimerEvent (new RunningCheckTimerEvent (mainTask, 500));
		    return;
		}
		mainTask.proc.allStopped.addObserver (new AllStoppedObserver ());
		mainTask.proc.stop ();
	    }
	}
    }

    class UnpausedTimerEvent
        extends frysk.event.TimerEvent
    {
        long numberOfTimerEvents = 0;
        Task task;
	long milliseconds;
        UnpausedTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
        {
            if (task != null) {
		assertEquals ("Main task unpaused", mainTask.state,
			      TaskState.unpaused);
		assertEquals ("Thread1 is unpaused", thread1.state,
			      TaskState.unpaused);
		assertEquals ("Thread2 is unpaused", thread2.state,
			      TaskState.unpaused);
		mainTask.stopEvent.addObserver (stopEventObserver);
		thread1.stopEvent.addObserver (stopEventObserver);
		thread2.stopEvent.addObserver (stopEventObserver);
	 	Manager.eventLoop.addTimerEvent (new RunningCheckTimerEvent (mainTask, 500));
	    }
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
		assertEquals ("Main task is paused", mainTask.state,
			      TaskState.paused);
		assertEquals ("Thread1 is paused", thread1.state,
			      TaskState.paused);
		assertEquals ("Thread2 is paused", thread2.state,
			      TaskState.paused);
		mainTask.requestContinue ();
		mainTask.requestContinue ();  // Extraneous go
		thread1.requestContinue ();
		thread1.requestContinue ();   // Extraneous go
		thread2.requestContinue ();
		Manager.eventLoop.addTimerEvent (new UnpausedTimerEvent (mainTask, 0));
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

    class StopTimerEvent
        extends frysk.event.TimerEvent
    {
        Task task;
	long milliseconds;
        StopTimerEvent (Task task, long milliseconds)
        {
            super (milliseconds);
            this.task = task;
	    this.milliseconds = milliseconds;
        }
        public void execute ()
        {
            if (task != null) {
		Signal.tkill (mainTask.id.id, Sig.TRAP);
		Signal.tkill (thread1.id.id, Sig.TRAP);
		Signal.tkill (thread2.id.id, Sig.TRAP);
		mainTask.requestStop ();
		thread1.requestStop ();
		thread2.requestStop ();
	    }
        }
    }

    public void testUnpaused ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	int pid = XXX.infThreadLoop (2);
	Manager.host.requestAttachProc (new ProcId (pid));

	// Register child to be removed at end of test
	registerChild (pid);	

        // Once a proc destroyed has been seen stop the event loop.
        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("TaskCreatedEvents received = 3", 3,
		      taskCreatedCount);
	assertEquals ("Forced StopTaskEvents received = 6", 6,
		      taskStopCount);
	assertEquals ("No TaskDestroyedEvents received", 0,
		      taskDestroyedCount);
    }
}
