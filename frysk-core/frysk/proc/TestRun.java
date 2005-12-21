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

/**
 * Check that a program can be run to completion.
 *
 * A scratch file is created.  The program "rm -f TMPFILE" is then
 * run.  That the tmp file has been removed is then checked.
 */

public class TestRun
    extends TestLib
{
    /**
     * Check that a free running sub-process can be created.
     */
    public void testCreateAttachedContinuedProc ()
    {
	TmpFile tmpFile = new TmpFile ();
	assertNotNull ("temporary file", tmpFile);

	// Add an observer that counts the number of proc create
	// events.
	ProcCounter procCounter = new ProcCounter (true);

	// Once a proc destroyed has been seen stop the event loop.
	new StopEventLoopWhenChildProcRemoved ();

	// Create a program that removes the above tempoary file, when
	// it exits the event loop will be shutdown.
	String[] command = new String[] {"rm", "-f", tmpFile.toString () };
	Manager.host.requestCreateAttachedProc (command);

	// Run the event loop, cap it at 5 seconds.
	assertRunUntilStop ("run \"rm\" to exit");

	assertEquals ("processes added",
		      1, procCounter.added.size ());
	assertEquals ("processes removed",
		      1, procCounter.removed.size ());
	assertFalse ("the file exists", tmpFile.stillExists ());
    }

    /**
     * Check that a stopped (at entry point) sub-process can be
     * created.
     *
     * This gets a little messy, need to get TaskObserver.Attached
     * installed on the just added task.
     */

    public void testCreateAttachedStoppedProc ()
    {
	TmpFile tmpFile = new TmpFile ();
	assertNotNull ("temporary file", tmpFile);

	// Create a program that removes the above temporary file, when
	// it exits the event loop will be shutdown.
	Manager.host.requestCreateAttachedProc (new String[]
	    {
		"rm",
		"-f",
		tmpFile.toString ()
	    });

	// Once a proc destroyed has been seen stop the event loop.
	new StopEventLoopWhenChildProcRemoved ();

	// Observe TaskObserver.Attached events; when any occure
	// indicate that the curresponding task should block, and then
	// request that the event-loop stop.
	class TaskAttachedObserver
	    extends AutoAddTaskObserverBase
	    implements TaskObserver.Attached
	{
	    TaskSet attachedTasks = new TaskSet ();
	    void updateTaskAdded (Task task)
	    {
		task.requestAddAttachedObserver (this);
	    }
	    public Action updateAttached (Task task)
	    {
		attachedTasks.add (task);
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	}
	TaskAttachedObserver taskAttachedObserver
	    = new TaskAttachedObserver ();

	// Run the event loop.  TaskAttachedObserver will halt the
	// process at the entry point.
	assertRunUntilStop ("run \"rm\" to entry");

	// A single task should be blocked at its entry point.
	assertEquals ("attached task count", 1,
		      taskAttachedObserver.attachedTasks.size ());
	assertTrue ("tmp file exists", tmpFile.stillExists ());

	// Unblock the attached task and resume the event loop.  This
	// will allow the "rm" command to run to completion.
	taskAttachedObserver.attachedTasks.unblock (taskAttachedObserver);
	assertRunUntilStop ("run \"rm\" to exit");

	assertFalse ("tmp file exists", tmpFile.stillExists ());
    }
}
