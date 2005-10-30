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

import java.util.Observer;
import java.util.Observable;
import frysk.sys.XXX;

/**
 * Check that task can be stopped and that extraneous stop requests
 * are properly ignored.
 *
 * This creates a program that runs an infinite loop with a signal handler.
 * A timer is set up to detach from the program after a specified interval.
 * We then stop all tasks and when all tasks are verified to be stopped
 * we finally detach.
 */

public class TestStopAndStop
    extends TestLib
{
    Task mainTask;
    Task thread1;
    Task thread2;
    int taskCreatedCount;
    int taskDestroyedCount;
    int taskStopCount;

    // As soon as the process is created, attach a task created
    // observer.

    class ProcCreatedObserver
        implements Observer
    {
	int pid;
	ProcCreatedObserver (int pid)
	{
	    this.pid = pid;
	}
        public void update (Observable o, Object obj)
        {
            Proc proc = (Proc) obj;
	    if (proc.id.hashCode () != pid)
		return;
            proc.observableTaskAdded.addObserver (new TaskCreatedObserver ());
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
	        Manager.eventLoop.add (new DetachTimerEvent (mainTask, 100));
	    task.requestedStopEvent.addObserver (taskEventObserver);
	}
    }

    class TaskDestroyedObserver
	extends AutoAddTaskObserverBase
	implements TaskObserver.Terminated
    {
	void updateTaskAdded (Task task)
	{
	    task.requestAddTerminatedObserver (this);
	}
	public Action updateTerminated (Task task, boolean signal,
					int value)
	{
	    assertTrue ("a signal", signal);
	    taskDestroyedCount++;
	    return Action.CONTINUE;
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
		assertEquals ("main task state", "stopped",
			      mainTask.getStateString ());
		mainTask.requestStop (); // Extraneous stop
		thread1.requestStop ();  // Extraneous stop
		assertEquals ("task 1 state", "stopped",
			      thread1.getStateString ());
		assertEquals ("task 2 state", "stopped",
			      thread2.getStateString ());
		Manager.eventLoop.requestStop ();
	    }
        }
    }

    class TaskEventObserver
 	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    TaskEvent e = (TaskEvent) obj;
	    e.task.requestStop ();  // Extraneous stop
	    if (++taskStopCount == 3) {
	        Manager.eventLoop.add (new AllStoppedTimerEvent (mainTask, 0));
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
		mainTask.requestStop (); // Extraneous stop right away
		thread1.requestStop ();
		thread2.requestStop ();
	    }
        }
    }

    public void testStopAndStop ()
    {
	int pid = XXX.infThreadLoop (2);
	Child child = new PidChild (pid);

        Manager.host.observableProcAdded.addObserver (new ProcCreatedObserver (pid));
	new TaskDestroyedObserver ();
	child.findProcUsingRefresh ().requestAttachedContinue ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("Task created events = 3", 3,
		      taskCreatedCount);
	assertEquals ("Forced stop task events = 3", 3,
		      taskStopCount);
	assertEquals ("No task destroyed events", 0,
		      taskDestroyedCount);
    }
}
