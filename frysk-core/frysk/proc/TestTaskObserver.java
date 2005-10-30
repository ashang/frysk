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
 * Generic observer tests - that the framework functions ok.
 */

public class TestTaskObserver
    extends TestLib
{
    /**
     * Test that Task observers can be added when the task is in the
     * running state.
     */
    public void testAddObserverToRunning ()
    {
	new StopEventLoopWhenChildProcRemoved ();
	class AddToAttached
	    extends AutoAddTaskObserverBase
	    implements TaskObserver.Attached, TaskObserver.Terminated
	{
	    void updateTaskAdded (Task task)
	    {
		task.requestAddAttachedObserver (this);
	    }
	    int attachedCount;
	    public Action updateAttached (Task task)
	    {
		task.requestAddTerminatedObserver (this);
		attachedCount++;
		return Action.CONTINUE;
	    }
	    int terminatedCount;
	    public Action updateTerminated (Task task, boolean signal,
					    int value)
	    {
		terminatedCount++;
		assertEquals ("signal", false, signal);
		assertEquals ("value", 0, value);
		return Action.CONTINUE;
	    }
	}
	AddToAttached addToAttached = new AddToAttached ();

	Manager.host.requestCreateAttachedContinuedProc
	    (new String[] {
		"./prog/terminated/exit",
		"0"
	    });
	assertRunUntilStop ("run \"exit\" to exit");

	assertEquals ("number of times attached", 1,
		      addToAttached.attachedCount);
	assertEquals ("number of times terminated", 1,
		      addToAttached.terminatedCount);
    }

    /**
     * Check that a blocker appears in the blocker list returned by
     * getBlockers.
     */
    public void testGetBlockers ()
    {
	// Block any task that reports a TaskAttached event and then
	// shut down the event loop.  Accumulate all blocked tasks in
	// the TaskObserverBase's task set.
	class BlockAttached
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
	BlockAttached blockAttached = new BlockAttached ();

	// Run a program, any program so that blockedAttached has
	// something to block.
	Manager.host.requestCreateAttachedContinuedProc
	    (new String[] {
		"./prog/terminated/exit",
		"0"
	    });
	assertRunUntilStop ("run \"exit\" to exit");

	// That one task was blocked.
	Task[] tasks = blockAttached.attachedTasks.toArray ();
	assertEquals ("blocked task count", 1, tasks.length);
	
	// That the Task's blocker set only contains this task.
	for (int i = 0; i < tasks.length; i++) {
	    Task task = tasks[i];
	    TaskObserver[] blockers = task.getBlockers ();
	    assertEquals ("blockers length", 1, blockers.length);
	    assertSame ("blocker and blockAttached", blockAttached,
			blockers[0]);
	}
    }
}
