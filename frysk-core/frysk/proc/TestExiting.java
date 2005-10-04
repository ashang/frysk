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
	    registerChild (proc.getId ().hashCode ());
	    proc.observableTaskAdded.addObserver (new TaskCreatedObserver ());
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
        Manager.host.observableProcAdded.addObserver (new ProcCreatedObserver ());

	// Create infinite loop
	Manager.host.requestCreateAttachedContinuedProc (new String[] {
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
