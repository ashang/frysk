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

/**
 * Check that assembler instruction stepping works.
 *
 */

public class TestStep
    extends TestLib
{
    Task mainTask;
    Task thread1;
    Task thread2;
    int pid;
    long numberOfTimerEvents;
    int taskCreatedCount;
    int taskDestroyedCount;
    int taskStopCount;
    int stepEventMatchCount;

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
	    // Register pid for removal at end of test
	    registerChild (pid);
            proc.taskDiscovered.addObserver (new TaskCreatedObserver ());
	    proc.taskRemoved.addObserver (new TaskDestroyedObserver ());
        }
    }
 
    class StopEventObserver
 	implements Observer
    {
	boolean startedLoop;
	public void update (Observable o, Object obj)
	{
	    if (obj instanceof TaskEvent.Signaled) {
		TaskEvent.Signaled e = (TaskEvent.Signaled) obj;
		if (e.signal == Sig.SEGV) {
		    startedLoop = true;	     
		    Manager.eventLoop.addTimerEvent (new DetachTimerEvent (mainTask, 500));
		}
	    }
	}
    }

    StopEventObserver stopEventObserver = new StopEventObserver ();

    class TaskCreatedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    Task task = (Task) obj;
	    assertEquals ("No terminated event before task creation", 0,
			  taskDestroyedCount);
	    taskCreatedCount++;
	    if (task.id.hashCode () == task.proc.id.hashCode ())
		mainTask = task;
	    else if (thread1 == null) {
		thread1 = task;
		thread1.stopEvent.addObserver (stopEventObserver);
	    }
	    else if (task.id.hashCode () != thread1.id.hashCode ())
		thread2 = task;
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

    class StepEventObserver
 	implements Observer
    {
	int stepCount;
	long firstPc;
	public void update (Observable o, Object obj)
	{
	    TaskEvent e = (TaskEvent) obj;
	    Task t = e.task;
	    long pc = t.getIsa().pc (t);
	    if (stepCount == 0) {
		firstPc = pc;
	    }
	    if (pc == firstPc)
		++stepEventMatchCount;
	    if (++stepCount >= 40) {
		Manager.eventLoop.requestStop ();
	    }
	    else
		t.requestStepInstruction ();
 	}
    }

    class AllStoppedTimerEvent
        extends frysk.event.TimerEvent
    {
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
	    thread1.stepEvent.addObserver (new StepEventObserver ());
	    long pc = thread1.getIsa().pc (thread1);
	    if (pc >= 0x900000) {
	    	mainTask.requestContinue ();
	    	thread2.requestContinue ();
	    	thread1.requestStepInstruction ();
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
	        task.proc.stop ();
	    }
        }
    }

    public void testStep ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	// Create threaded infinite loop
	Manager.host.requestCreateProc (new String[]
	    {
                "./prog/step/infThreadLoop"
            });

        // Once a proc destroyed has been seen stop the event loop.
        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("run \"infThreadLoop\" until exit");

	assertEquals ("Task created events = 3", 3,
		      taskCreatedCount);
	assertTrue ("At least 3 stop events received",
		    taskStopCount >= 3);
	assertTrue ("At least 5 loops occurred",
		    stepEventMatchCount >= 5);
	assertEquals ("No task destroyed events", 0,
		      taskDestroyedCount);
    }
}
