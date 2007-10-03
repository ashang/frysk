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

import java.util.*;
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.XXX;

/**
 * Check that a task will go into paused state.  This occurs when a
 * task is requested to stop and the task stops due to another event
 * already in the pipe.  The task goes into paused state to denote
 * that the original stop request is still out there.
 */

public class TestPaused
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
	    assertEquals ("No terminated event before task creation", 0,
			  taskDestroyedCount);
	    taskCreatedCount++;
	    if (task.id.id == task.proc.id.id)
		mainTask = task;
	    else if (thread1 == null)
		thread1 = task;
	    else if (task.id.hashCode () != thread1.id.hashCode ())
		thread2 = task;
	    if (taskCreatedCount == 3)
	        Manager.eventLoop.addTimerEvent (new DetachTimerEvent (mainTask, 500));
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
		assertEquals ("Main task is paused", TaskState.paused,
			      mainTask.state);
		mainTask.requestStop ();  // Extraneous stop
		assertEquals ("Thread 1 is paused", TaskState.paused,
			      thread1.state);
		assertEquals ("Thread 2 is paused", TaskState.paused,
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
		Signal.tkill (mainTask.id.id, Sig.TRAP);
		Signal.tkill (thread1.id.id, Sig.TRAP);
		Signal.tkill (thread2.id.id, Sig.TRAP);
		mainTask.requestStop ();
		mainTask.requestStop ();  // Extraneous stop
		thread1.requestStop ();
		thread2.requestStop ();
	    }
        }
    }

    public void testPaused ()
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

	assertEquals ("TaskCreatedEvents received = 3", 3,
		      taskCreatedCount);
	assertEquals ("Forced StopTaskEvents received = 3", 3,
		      taskStopCount);
	assertEquals ("No TaskDestroyedEvents received", 0,
		      taskDestroyedCount);
	assertEquals ("No tasks left", 0,
		      Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0,
		      Manager.host.procPool.size ());
	Signal.kill (pid, Sig.KILL);
    }
}
