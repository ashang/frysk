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
            proc.observableTaskAdded.addObserver (new TaskCreatedObserver ());
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
        Manager.host.observableProcAdded.addObserver (new ProcCreatedObserver ());

	// Create threaded infinite loop
	int pid = XXX.infCloneLoop ();
	Manager.host.requestAttachProc (new ProcId (pid));

        // Register child to be removed at end of test
        registerChild (pid);

        // Once a proc destroyed has been seen stop the event loop.
	Child child = new PidChild (pid);
        child.stopEventLoopOnDestroy ();

	assertRunUntilStop ("XXX: run until?");

	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
