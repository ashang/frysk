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
import frysk.sys.Sig;
import java.util.Iterator;

/**
 * Check that assembler instruction stepping works.
 */

public class TestStep
    extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestStepInternals {
	Task mainTask;
	Task thread1;
	Task thread2;
	long numberOfTimerEvents;
	int taskCreatedCount;
	int taskDestroyedCount;
	int taskStopCount;
	int stepEventMatchCount;
	
	class StopEventObserver
	    extends TaskObserverBase
	    implements TaskObserver.Signaled
	{
	    boolean startedLoop;
	    public Action updateSignaled (Task task, int sig)
	    {
		if (sig == Sig.SEGV) {
		    startedLoop = true;	     
		    Manager.eventLoop.add (new DetachTimerEvent (mainTask, 500));
		}
		return Action.CONTINUE;
	    }
	}
	
	StopEventObserver stopEventObserver = new StopEventObserver ();
	
	class TaskCreatedObserver
	    implements Observer
	{
	    public void update (Observable o, Object obj)
	    {
		Task task = (Task) obj;
		if (!isChildOfMine (task.proc))
		    return;
		registerChild (task.getTid ());
		assertEquals ("No terminated event before task creation", 0,
			      taskDestroyedCount);
		taskCreatedCount++;
		if (task.id.hashCode () == task.proc.id.hashCode ())
		    mainTask = task;
		else if (thread1 == null) {
		    thread1 = task;
		    thread1.requestAddSignaledObserver (stopEventObserver);
		}
		else if (task.id.hashCode () != thread1.id.hashCode ())
		    thread2 = task;
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
	
	class StepEventObserver
	    implements Observer
	{
	    int stepCount;
	    long firstPc;
	    public void update (Observable o, Object obj)
	    {
		Task t = (Task) obj;
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
		mainTask.requestContinue ();
		thread2.requestContinue ();
		thread1.requestStepInstruction ();
	    }
	}
	
	class TaskEventObserver
	    implements Observer
	{
	    public void update (Observable o, Object obj)
	    {
		if (++taskStopCount == 3) {
		    Manager.eventLoop.add (new AllStoppedTimerEvent (mainTask, 0));
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
		    Iterator i = task.proc.taskPool.values().iterator ();
		    while (i.hasNext ()) {
			Task t = (Task)i.next ();
			t.requestStop ();
		    }
		}
	    }
	}

	TestStepInternals ()
	{
	    Manager.host.observableTaskAdded.addObserver (new TaskCreatedObserver ());
	    new TaskDestroyedObserver ();
	}
    }

    public void testStep ()
    {
	new StopEventLoopWhenChildProcRemoved ();
	TestStepInternals t = new TestStepInternals ();

	// Create threaded infinite loop
	Manager.host.requestCreateAttachedProc
	    (new String[] {
                "./prog/step/infThreadLoop"
            });

	assertRunUntilStop ("run \"infThreadLoop\" until exit");

	assertEquals ("Task created events = 3", 3,
		      t.taskCreatedCount);
	assertTrue ("At least 3 stop events received",
		    t.taskStopCount >= 3);
	assertTrue ("At least 5 loops occurred",
		    t.stepEventMatchCount >= 5);
	assertEquals ("No task destroyed events", 0,
		      t.taskDestroyedCount);
    }
}
