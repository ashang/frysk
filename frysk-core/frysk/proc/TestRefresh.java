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

import java.util.Iterator;
import frysk.sys.Pid;

/**
 * Check the host's behavior.
 */

public class TestRefresh
    extends TestLib
{
    /**
     * Class to track various refresh operations and verify the
     * results are as expected.
     */
    class ChildTracker
    {
	PidCounter added;
	PidCounter removed;
	Child child;
	ChildTracker (Child child)
	{
	    this.child = child;
	    added = new PidCounter (child.pid, Manager.host.procAdded);
	    removed = new PidCounter (child.pid, Manager.host.procRemoved);
	}
	Proc proc;
	void verifyAdd (String reason, int tasks)
	{
	    proc = Manager.host.getProc (new ProcId (child.pid));
	    assertNotNull (reason + ", child in process pool;",
			   proc);
	    assertEquals (reason + ", child's task count",
			  tasks, proc.taskPool.size ());
	    assertEquals (reason + ", child discovered once",
			  1, added.count);
	    assertEquals (reason + ", child removed nunce",
			  0, removed.count);
	    assertEquals (reason + ", child unattached",
			  ProcState.unattached, proc.state);
	}
	void verifyRemove (String reason)
	{
	    assertNull (reason + ", child removed from process pool",
			Manager.host.getProc (new ProcId (child.pid)));
	    assertEquals (reason + ", child discovered once",
			  1, added.count);
	    assertEquals (reason + ", child removed once",
			  1, removed.count);
	    assertEquals (reason + ", child in destroyed state",
			  ProcState.destroyed, proc.state);
	}
    }

    /**
     * Test that host refresh does not re-add an existing process.
     */
    public void testRepeatedHostRefresh ()
    {
	// Create a daemon process, set things up to watch and verify
	// the child.
	ChildTracker tracker = new ChildTracker (new DaemonChild ());

	// Do several refreshes, check that the child is only added
	// once, and never removed.
	for (int i = 0; i < 2; i++) {
	    Manager.host.requestRefresh ();
	    Manager.eventLoop.runPending ();
	    tracker.verifyAdd ("iteration " + i, 0);
	}
    }

    /**
     * Test the Host's refresh mechanism.
     *
     * This runs the Host's refresh() method checking that it
     * correctly reports a process addition and then removal.
     */
    public void testHostRefresh ()
    {
	// Get an initial PS reading.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	
	// Check that it isn't empty.
	assertTrue ("Host.procPool non-empty",
		    Manager.host.procPool.size () > 0);

	// Create a suspended sub-process, and wait for it to start.
	DetachedChild child = new DetachedChild ();

	// Set up a tracker to watch for this specific child being
	// added and removed.
	ChildTracker tracker = new ChildTracker (child);

	// Do a refresh, check that the process was added and
	// corresponding observable events occured.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("first add", 0);

	// Delete the process.
	child.reap ();

	// Check that a further refresh removes the process, generates
	// a removed event, and puts the proc into the removed
	// state.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyRemove ("first removed");
    }

    /**
     * Test Proc refresh.
     *
     * Check that a refresh of a specific Proc gets the correct task
     * list.
     */
    public void testProcRefresh ()
    {
	// Create a suspended sub-process that contains three cloned
	// tasks, and wait for it to start.
	DaemonChild child = new DaemonChild (3);

	// Create a task counter, to count the number discovered and
	// removed tasks.
	TaskCounter taskCount = new TaskCounter ();

	// Track the child process.
	ChildTracker tracker = new ChildTracker (child);

	// Do a host refresh so that the child process can be found.
	// At this stage, since the process's tasks are only located
	// on an explicit refresh, the process should have no tasks,
	// and no task events should have been seen.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("refresh without tasks", 0);

	// Ask the proc to refresh its task list, check that 2(clone)
	// + 1(main) tasks are found, and that all are in the
	// unattached state.
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("Proc has correct number of tasks",
		      3 + 1, tracker.proc.taskPool.size ());
 	assertEquals ("After refresh, all tasks are discovered",
 		      4, taskCount.numberAdded ());
	assertEquals ("After refresh, no tasks have been removed",
		      0, taskCount.numberRemoved ());
 	for (Iterator i = tracker.proc.taskPool.values ().iterator ();
	     i.hasNext ();) {
 	    Task task = (Task) i.next ();
 	    assertEquals ("Task " + task + " in the unattached state",
 			  TaskState.unattached, task.state);
 	}

	// Tell the child to drop two tasks.  Check that the refresh
	// looses two tasks, and that the lost tasks have been
	// transitioned to the dead state.
	child.delTask ();
	child.delTask ();
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("Proc has been reduced by two tasks",
		      2, tracker.proc.taskPool.size ());
 	assertEquals ("After kill, no further tasks discovered",
 		      4, taskCount.numberAdded ());
 	for (Iterator i = tracker.proc.taskPool.values ().iterator ();
	     i.hasNext ();) {
 	    Task task = (Task) i.next ();
	    assertEquals ("Task " + task + " in the unattached state",
			  TaskState.unattached, task.state);
 	}
	assertEquals ("After kill, one task was removed",
		      2, taskCount.numberRemoved ());
	for (Iterator i = taskCount.removed.iterator (); i.hasNext (); ) {
	    Task task = (Task) i.next ();
	    assertEquals ("Removed task " + task + " state",
			  TaskState.destroyed, task.state);
	}

	// Finally, tell the child to add a task back.  Check that the
	// counts are again updated.
	child.addTask ();
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("Proc gained one task",
		      3, tracker.proc.taskPool.size ());
 	assertEquals ("One task discovered",
		      5, taskCount.numberAdded ());
	assertEquals ("No additional tasks removed",
		      2, taskCount.numberRemoved ());
    }

    /**
     * Check that, when requested, both the process and task tables
     * are refreshed.
     */
    public void testRefreshAll ()
    {
	// Get an initial PS reading.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	
	// Create a suspended sub-process with two threads (in
	// addition to main).
	Child child = new DaemonChild (2);

	// Track what this gets up to.
	ChildTracker tracker = new ChildTracker (child);

	// Do a refresh, check that the process was added.
	Manager.host.requestRefresh (true);
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("all refreshed", 3);
    }

    /**
     * Check that a parent child relationship is correct.
     */
    public void testParentChild ()
    {
	// Create a sub process, refresh things so that it is known.
	ChildTracker tracker = new ChildTracker (new DetachedChild ());
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("Find child", 0);
	
	// Find this process.
	Proc me = Manager.host.getProc (new ProcId (Pid.get ()));
	assertNotNull ("This process found", me);

	assertEquals ("This process is the parent",
		      me, tracker.proc.getParent ());
	// Don't check for tracker.child being the only process; other
	// tests may have corrupted things leaving this process with
	// other children.
    }

    /**
     * Check that a process that becomes a daemon gets its parent id
     * changed to 1.
     */
    public void testRefreshDaemon ()
    {
	// Create the zombie maker, and then get it to create one
	// child.
	ZombieChild zombie = new ZombieChild ();
	zombie.addChild ();
	
	// Do a refresh, find the zombie maker, check it has one child
	// process, save it.
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	Proc zombieParent = zombie.findProc ();
	assertEquals ("Zombie maker has one child",
		      1, zombieParent.children.size ());
	Proc zombieChild = (Proc) zombieParent.children.iterator().next();
	assertSame ("Is child of zombie",
		      zombieChild.getParent (), zombieParent);
	Proc procOne = Manager.host.getProc (new ProcId (1));

	// Blow away the parent, this turns the child into a daemon,
	// do a refresh and check that the child's parent changed to
	// process one.
	zombie.fryParent ();
	Manager.host.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertNotSame ("Child's parent and zombie maker",
		       zombieChild.getParent (), zombieParent);
	assertSame ("Child's parent and process 1",
		    zombieChild.getParent (), procOne);
	assertTrue ("Process 1 includes child",
		    procOne.children.contains (zombieChild));
	assertEquals ("Count of children of dead zombie parent",
		      0, zombieParent.children.size ());
	// XXX: What about notification.
    }

    /**
     * Check that a refresh involving a zombie is ok.
     *
     * In /proc, a zombie has no child tasks.  Within PS, a zombie
     * appears as "defunct".
     */
    public void testRefreshZombie ()
    {
	// Create the zombie maker, and then get it to create one
	// child.
	ZombieChild zombie = new ZombieChild ();
	zombie.addChild ();
	
	// Do a refresh, find the zombie maker, check that it's child
	// has one task and no processes.
	Manager.host.requestRefresh (true);
	Manager.eventLoop.runPending ();
	Proc zombieParent = zombie.findProc ();
	assertEquals ("Zombie maker has one child",
		      1, zombieParent.children.size ());
	Proc zombieChild = (Proc) zombieParent.children.iterator().next();
	assertEquals ("Zombie child has one task",
		      1, zombieChild.taskPool.size ());
	assertEquals ("Zombie child has no processes",
		      0, zombieChild.children.size ());

	// Turn the zombie-child into a true zombie, check things are
	// updated.
	zombie.fryChild ();
	Manager.host.requestRefresh (true);
	Manager.eventLoop.runPending ();
 	assertEquals ("Zombie maker has one child",
 		      1, zombieParent.children.size ());
	assertEquals ("Zombie child has zero tasks",
		      0, zombieChild.taskPool.size ());
	assertEquals ("Zombie child has no processes",
		      0, zombieChild.children.size ());
	assertSame ("Zombie parent of child",
		    zombieParent, zombieChild.getParent ());
    }
}
