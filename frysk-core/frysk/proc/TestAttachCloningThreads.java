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
import frysk.sys.XXX;
import frysk.sys.Sig;
import frysk.sys.Signal;

/**
 * Test attaching to a process that continuously clones and joins children
 * which in turn clone and join children themselves.
 *
 */

public class TestAttachCloningThreads
    extends TestLib
{
    Task mainTask;

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
        }
    }
 
    // Once the task has been created, schedule a terminate signal.

    class TaskCreatedObserver
	implements Observer
    {
	int eventCount;
	public void update (Observable o, Object obj)
	{
	    Task task = (Task) obj;
	    eventCount++;
	    if (task.id.hashCode () == task.proc.id.hashCode ())
		mainTask = task;
	    if (eventCount == 200)
	        Manager.eventLoop.addTimerEvent (new KillTimerEvent (mainTask, 100));
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

    public void testAttachCloningThreads ()
    {
        Manager.procDiscovered.addObserver (new ProcCreatedObserver ());

	// Create threaded infinite loop
	int pid = XXX.infCloneLoop ();
	Manager.host.requestAttachProc (new ProcId (pid));

        // Register child to be removed at end of test
        registerChild (pid);

        // Once a proc destroyed has been seen stop the event loop.
        new StopEventLoopOnProcDestroy ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
