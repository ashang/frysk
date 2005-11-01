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
 * Check that tasks can be manually stopped and restarted.
 */

public class TestStopAndGo
    extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestStopAndGoInternals {
	Task mainTask;
	Task thread1;
	Task thread2;
	int taskCreatedCount;
	int taskDestroyedCount;
	int taskStopCount;
	int stopTimerEventCount;
	int goTimerEventCount;
	
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
		assertTrue ("a signal", signal);
		taskDestroyedCount++;
		return Action.CONTINUE;
	    }
	}
	
	class TaskEventObserver
	    implements Observer
	{
	    int stopCount;
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
		mainTask.requestStop ();
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
		thread1.requestContinue ();
		thread2.requestContinue ();
		Manager.eventLoop.add (new StopTimerEvent (mainTask, 0));
	    }
	}

	TestStopAndGoInternals (int pid)
	{
	    Manager.host.observableProcAdded.addObserver (new ProcCreatedObserver (pid));
	    new TaskDestroyedObserver ();
	}
    }

    public void testStopAndGo ()
    {
	// Create threaded infinite loop
	int pid = XXX.infThreadLoop (2);
	Child child = new PidChild (pid);
	TestStopAndGoInternals tsag = new TestStopAndGoInternals (pid);
	child.findProcUsingRefresh ().requestAttachedContinue ();
                                                                                
	assertRunUntilStop ("XXX: run until?");

	assertEquals ("Task created events received = 3", 3,
		      tsag.taskCreatedCount);
	assertEquals ("StopTimerEvents = 2", 2,
		      tsag.stopTimerEventCount);
	assertEquals ("One GoTimerEvent triggered", 1,
		      tsag.goTimerEventCount);
	assertEquals ("Forced stop task events = 6", 6,
		      tsag.taskStopCount);
	assertEquals ("No task destroyed events received", 0,
		      tsag.taskDestroyedCount);
    }
}
