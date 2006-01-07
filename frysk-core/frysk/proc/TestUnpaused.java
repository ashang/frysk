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
import frysk.sys.Signal;
import frysk.sys.Sig;

/**
 * Check that if a task is requested to stop and receives a natural
 * stop event first, that the task goes into the paused state.  When continued,
 * the task will go into the unpaused state and finally when the requested
 * SIGSTOP gets received, it will go back into the normal running state.
 */

public class TestUnpaused
    extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestUnpausedInternals {
	Task mainTask;
	Task thread1;
	Task thread2;
	int taskCreatedCount;
	int taskDestroyedCount;
	int taskStopCount;

	// Once the task has been created, schedule a terminate signal.
	
	class TaskCreatedObserver
	    implements Observer
	{
	    int pid;
	    TaskCreatedObserver (int pid)
	    {
		this.pid = pid;
	    }
	    public void update (Observable o, Object obj)
	    {
		Task task = (Task) obj;
		if (task.proc.getPid () != pid)
		    return;
		registerChild (task.getTid ());
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
		    Manager.eventLoop.add (new StopTimerEvent (mainTask, 500));
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
	
	class StopEventObserver
	    extends TaskObserverBase
	    implements TaskObserver.Signaled
	{
	    public Action updateSignaled (Task task, int sig)
	    {
		assertEquals ("task state", "running",
			      task.getStateString ());
		task.requestStop ();  // Extraneous stop
		return Action.CONTINUE;
	    }
	}
	
	StopEventObserver stopEventObserver = new StopEventObserver ();
	
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
		    if (!"running".equals (mainTask.getStateString ())) {
			Manager.eventLoop.add (new RunningCheckTimerEvent (mainTask, 500));	
			return;
		    }
		    else if (!"running".equals (thread1.getStateString ())) {
			Manager.eventLoop.add (new RunningCheckTimerEvent (mainTask, 500));
			return;
		    }
		    else if (!"running".equals (thread2.getStateString ())) {
			Manager.eventLoop.add (new RunningCheckTimerEvent (mainTask, 500));
			return;
		    }
		    Manager.eventLoop.requestStop ();
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
		    assertEquals ("main task state", "unpaused",
				  mainTask.getStateString ());
		    assertEquals ("task 1 state", "unpaused",
				  thread1.getStateString ());
		    assertEquals ("task 2 state", "unpaused",
				  thread2.getStateString ());
		    mainTask.requestAddSignaledObserver (stopEventObserver);
		    thread1.requestAddSignaledObserver (stopEventObserver);
		    thread2.requestAddSignaledObserver (stopEventObserver);
		    Manager.eventLoop.add (new RunningCheckTimerEvent (mainTask, 500));
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
		    assertEquals ("main task state", "paused",
				  mainTask.getStateString ());
		    assertEquals ("task 1 state", "paused",
				  thread1.getStateString ());
		    assertEquals ("task 2 state", "paused",
				  thread2.getStateString ());
		    mainTask.requestContinue ();
		    mainTask.requestContinue ();  // Extraneous go
		    thread1.requestContinue ();
		    thread1.requestContinue ();   // Extraneous go
		    thread2.requestContinue ();
		    Manager.eventLoop.add (new UnpausedTimerEvent (mainTask, 0));
		}
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

	TestUnpausedInternals (int pid)
	{
	    host.observableTaskAdded.addObserver (new TaskCreatedObserver (pid));
	    new TaskDestroyedObserver ();
	}
    }
	
    public void testUnpaused ()
    {
	Child child = new AckDaemonProcess (2, true);
	TestUnpausedInternals tu = new TestUnpausedInternals (child.getPid ());
	child.findProcUsingRefresh ().requestAttachedContinue ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("task created count", 3, tu.taskCreatedCount);
	assertEquals ("forced task stop count", 3, tu.taskStopCount);
	assertEquals ("task destroyed count", 0, tu.taskDestroyedCount);
    }
}
