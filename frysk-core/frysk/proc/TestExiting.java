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
 * Check that we can detect a program that is exiting (i.e. before
 * it has already exited).
 *
 * This creates a program that is in an infinite loop.  We set up
 * a timer to kill the program with a SIGKILL.  We expect to get
 * an exiting event before we get the exited event.
 */

public class TestExiting
    extends TestLib
{
    int taskExitingCount;
    int taskTerminatedCount;
    int taskExitingEventSig;
    int taskTerminatedEventSig;

    class ProcCreatedObserver
        implements Observer
    {
	Task task;
        public void update (Observable o, Object obj)
        {
            Proc proc = (Proc) obj;
	    proc.taskDiscovered.addObserver (new TaskCreatedObserver ());
	    proc.taskDestroyed.addObserver (new TaskTerminatedObserver ());
	    proc.taskExiting.addObserver (new TaskExitingObserver ());
        }
    }

    class TaskCreatedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    Task task = (Task) obj;
	    task.traceExit = true;
	    Manager.eventLoop.addTimerEvent (new KillTimerEvent (task, 100));
	}
    }

    class TaskTerminatedObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    taskTerminatedCount++;
	    TaskEvent.Terminated taskEvent = (TaskEvent.Terminated) obj;
	    taskTerminatedEventSig = taskEvent.signal;
	}
    }

    class TaskExitingObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    taskExitingCount++;
	    assertEquals ("No termination before exiting", 0,
			  taskTerminatedCount);
	    TaskEvent.Exiting taskEvent = (TaskEvent.Exiting) obj;
	    taskExitingEventSig = taskEvent.signal;
	}
    }

    class KillTimerEvent
        extends frysk.event.TimerEvent
    {
        long numberOfTimerEvents = 0;
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


    public void testExiting ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	// Create infinite loop
	Manager.host.requestCreateProc (new String[]
	    {
		"./prog/terminated/infloop"
	    });

        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("run \"infloop\" until exit");

	assertEquals ("Exiting event received", 1,
		      taskExitingCount);
	assertEquals ("Termination event received", 1,
		      taskTerminatedCount);
	assertEquals ("SIGILL received while exiting", Sig.KILL,
		      taskExitingEventSig);
	assertEquals ("SIGILL received at termination", Sig.KILL,
		      taskTerminatedEventSig);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
