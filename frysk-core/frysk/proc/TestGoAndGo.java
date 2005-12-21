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
 * Check that a running program can handle extraneous go requests.
 *
 * This creates a program that runs an infinite loop with a signal handler.
 * A timer is set up to detach from the program after a specified interval.
 * We then send a signal to the program to allow it to end.
 */

public class TestGoAndGo
    extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestGoAndGoInternals {
	Task mainTask;
	Task thread1;
	Task thread2;
	int taskCreatedCount;
	int taskDestroyedCount;
	int taskStopCount;
	int stopTimerEventCount;
	int goTimerEventCount;
	
	// Once the task has been created, schedule a terminate signal.
	
	class TaskCreatedObserver
	    implements Observer
	{
	    int pid;
	    TaskCreatedObserver (int pid)
	    {
		this.pid = pid;
	    }
	    int eventCount;
	    public void update (Observable o, Object obj)
	    {
		Task task = (Task) obj;
		if (task.proc.getPid () != pid)
		    return;
		assertEquals ("task destroyed count before task create", 0,
			      taskDestroyedCount);
		taskCreatedCount++;
		if (task.id.hashCode () == task.proc.id.hashCode ()) {
		    mainTask = task;
		}
		else if (thread1 == null)
		    thread1 = task;
		else if (task.id.hashCode () != thread1.id.hashCode ())
		    thread2 = task;
		task.requestedStopEvent.addObserver (taskEventObserver);
		if (taskCreatedCount == 3)
		    Manager.eventLoop.add (new StopTimerEvent (mainTask, 100));
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
		assertTrue ("terminated by signal", signal);
		taskDestroyedCount++;
		return Action.CONTINUE;
	    }
	}
	
	class TaskEventObserver
	    implements Observer
	{
	    public void update (Observable o, Object obj)
	    {
		if (++taskStopCount == 3) {
		    Manager.eventLoop.add (new GoTimerEvent (mainTask, 0));
		} else if (taskStopCount == 6) {
		    Manager.eventLoop.requestStop ();
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
		++stopTimerEventCount;
		// We ignore the task this timer is on and stop all threads
		mainTask.requestContinue ();  // Extraneous go request
		mainTask.requestStop ();
		thread1.requestContinue ();   // Extraneous go request
		thread1.requestStop ();
		thread2.requestStop ();
	    }
	}
	
	class GoTimerEvent
	    extends frysk.event.TimerEvent
	{
	    Task task;
	    long milliseconds;
	    GoTimerEvent (Task task, long milliseconds)
	    {
		super (milliseconds);
		this.task = task;
		this.milliseconds = milliseconds;
	    }
	    public void execute ()
	    {
		++goTimerEventCount;
		// We ignore the task this timer is on and stop all threads
		mainTask.requestContinue ();
		mainTask.requestContinue ();  // Extraneous go request
		thread1.requestContinue ();
		thread1.requestContinue ();   // Extraneous go request
		thread2.requestContinue ();
		Manager.eventLoop.add (new StopTimerEvent (mainTask, 0));
	    }
	}

	TestGoAndGoInternals (int pid)
	{
	    Manager.host.observableTaskAdded.addObserver (new TaskCreatedObserver (pid));
	    new TaskDestroyedObserver ();
	}
    }

    public void testGoAndGo ()
    {
	// Create threaded infinite loop
	int pid = XXX.infThreadLoop (2);
	Child child = new PidChildXXX (pid);
	TestGoAndGoInternals t = new TestGoAndGoInternals (pid);
	child.findProcUsingRefresh ().requestAttachedContinue ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("task creation events", 3, t.taskCreatedCount);
	assertEquals ("stop timer events", 2, t.stopTimerEventCount);
	assertEquals ("timer events", 1, t.goTimerEventCount);
	assertEquals ("forced stop events", 6, t.taskStopCount);
	assertEquals ("task destroyed events", 0, t.taskDestroyedCount);
    }
}
