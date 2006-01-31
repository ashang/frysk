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
	    added = new PidCounter (child.getPid (),
				    host.observableProcAddedXXX);
	    removed = new PidCounter (child.getPid (),
				      host.observableProcRemovedXXX);
	}
	Proc proc;
	void verifyAdd (String reason, int tasks)
	{
	    proc = host.getProc (new ProcId (child.getPid ()));
	    assertNotNull (reason + ", child in process pool;",
			   proc);
	    assertEquals (reason + ", child's task count",
			  tasks, proc.taskPool.size ());
	    assertEquals (reason + ", child discovered once",
			  1, added.count);
	    assertEquals (reason + ", child removed nunce",
			  0, removed.count);
	    assertEquals (reason + ", child state",
			  "unattached", proc.getStateString ());
	}
	void verifyRemove (String reason)
	{
	    assertNull (reason + ", child removed from process pool",
			host.getProc (new ProcId (child.getPid ())));
	    assertEquals (reason + ", child discovered once",
			  1, added.count);
	    assertEquals (reason + ", child removed once",
			  1, removed.count);
	    assertEquals (reason + ", child state",
			  "destroyed", proc.getStateString ());
	}
    }

    /**
     * Test that host refresh does not re-add an existing process.
     */
    public void testRepeatedHostRefresh ()
    {
	// Create a daemon process, set things up to watch and verify
	// the child.
	ChildTracker tracker = new ChildTracker (new AckDaemonProcess ());

	// Do several refreshes, check that the child is only added
	// once, and never removed.
	for (int i = 0; i < 2; i++) {
	    host.requestRefresh ();
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
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	
	// Check that it isn't empty.
	assertTrue ("host.procPool non-empty",
		    host.procPool.size () > 0);

	// Create a suspended sub-process, and wait for it to start.
	DetachedAckProcess child = new DetachedAckProcess ();

	// Set up a tracker to watch for this specific child being
	// added and removed.
	ChildTracker tracker = new ChildTracker (child);

	// Do a refresh, check that the process was added and
	// corresponding observable events occured.
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("first add", 0);

	// Delete the process.
	child.reap ();

	// Check that a further refresh removes the process, generates
	// a removed event, and puts the proc into the removed
	// state.
	host.requestRefresh ();
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
	int nrTasks = 4;
	int nrKills = 2;

	// Create a suspended sub-process that contains three cloned
	// tasks, and wait for it to start.
	AckProcess child = new AckDaemonProcess (nrTasks - 1);

	// Create a task counter, to count the number discovered and
	// removed tasks.
	TaskCounter taskCount = new TaskCounter ();

	// Track the child process.
	ChildTracker tracker = new ChildTracker (child);

	// Do a host refresh so that the child process can be found.
	// At this stage, since the process's tasks are only located
	// on an explicit refresh, the process should have no tasks,
	// and no task events should have been seen.
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("refresh without tasks", 0);

	// Ask the proc to refresh its task list, check that 2(clone)
	// + 1(main) tasks are found, and that all are in the
	// unattached state.
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("proc's task count",
		      nrTasks, tracker.proc.taskPool.size ());
 	assertEquals ("tasks addeded by refresh",
 		      nrTasks, taskCount.added.size ());
	assertEquals ("tasks removed by refresh",
		      0, taskCount.removed.size ());
 	for (Iterator i = tracker.proc.taskPool.values ().iterator ();
	     i.hasNext ();) {
 	    Task task = (Task) i.next ();
 	    assertEquals ("task " + task + " state", "unattached",
			  task.getStateString ());
 	}

	// Tell the child to drop two tasks.  Check that the refresh
	// looses two tasks, and that the lost tasks have been
	// transitioned to the dead state.
	for (int i = 0; i < nrKills; i++)
	    child.delClone ();
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("proc's task count after kills",
		      nrKills, tracker.proc.taskPool.size ());
 	assertEquals ("tasks added count after kills (no change)",
 		      nrTasks, taskCount.added.size ());
 	for (Iterator i = tracker.proc.taskPool.values ().iterator ();
	     i.hasNext ();) {
 	    Task task = (Task) i.next ();
	    assertEquals ("task " + task + " state", "unattached",
			  task.getStateString ());
 	}
	assertEquals ("tasks removed by refresh after kills",
		      nrKills, taskCount.removed.size ());
	for (Iterator i = taskCount.removed.iterator (); i.hasNext (); ) {
	    Task task = (Task) i.next ();
	    assertEquals ("removed task state", "destroyed",
			  task.getStateString ());
	}

	// Finally, tell the child to add a task back.  Check that the
	// counts are again updated.
	child.addClone ();
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("proc's task count after add",
		      nrTasks - nrKills + 1,
		      tracker.proc.taskPool.size ());
 	assertEquals ("task's added count (increases by 1)",
		      nrTasks + 1, taskCount.added.size ());
	assertEquals ("task's deleted (no change)",
		      nrKills, taskCount.removed.size ());
    }

    /**
     * Check that, when requested, both the process and task tables
     * are refreshed.
     */
    public void testRefreshAll ()
    {
	// Get an initial PS reading.
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	
	// Create a suspended sub-process with two threads (in
	// addition to main).
	Child child = new AckDaemonProcess (2);

	// Track what this gets up to.
	ChildTracker tracker = new ChildTracker (child);

	// Do a refresh, check that the process was added.
	host.requestRefresh (true);
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("all refreshed", 3);
    }

    /**
     * Check that a parent child relationship is correct.
     */
    public void testParentChild ()
    {
	// Create a sub process, refresh things so that it is known.
	ChildTracker tracker = new ChildTracker (new DetachedAckProcess ());
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("find child", 0);
	
	// Find this process.
	Proc me = host.getProc (new ProcId (Pid.get ()));
	assertNotNull ("this process", me);

	assertSame ("this process and child's parent",
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
	AckProcess zombie = new AckDaemonProcess ();
	zombie.addFork ();
	
	// Do a refresh, find the zombie maker, check it has one child
	// process, save it.
	Proc zombieParent = zombie.findProcUsingRefresh ();
	assertEquals ("zombie maker has one child",
		      1, zombieParent.getChildren ().size ());
	Proc zombieChild = (Proc) zombieParent.getChildren ().getFirst ();
	assertSame ("zombie and zombie child's parent",
		    zombieChild.getParent (), zombieParent);
	Proc procOne = host.getProc (new ProcId (1));

	// Blow away the parent, this turns the child into a daemon,
	// do a refresh and check that the child's parent changed to
	// process one.
	zombie.fryParent ();
	host.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertNotSame ("child's parent and zombie maker",
		       zombieChild.getParent (), zombieParent);
	assertSame ("child's parent and process 1",
		    zombieChild.getParent (), procOne);
	assertTrue ("process 1 includes child",
		    procOne.getChildren ().contains (zombieChild));
	assertEquals ("count of children of dead zombie parent",
		      0, zombieParent.getChildren ().size ());
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
	AckProcess zombie = new AckDaemonProcess ();
	zombie.addFork ();
	
	// Do a refresh (that includes updating the task list), find
	// the zombie maker, check that it's child has one task and no
	// processes.
	Proc zombieParent = zombie.findProcUsingRefresh (true);
	assertEquals ("zombie maker child count", 1,
		      zombieParent.getChildren ().size ());
	Proc zombieChild = (Proc) zombieParent.getChildren ().getFirst ();
	assertEquals ("zombie child task count", 1,
		      zombieChild.taskPool.size ());
	assertEquals ("zombie child process count",
		      0, zombieChild.getChildren ().size ());

	// Turn the zombie-child into a true zombie, check things are
	// updated.
	zombie.zombieFork ();
	host.requestRefresh (true);
	Manager.eventLoop.runPending ();
 	assertEquals ("zombie maker child count",
 		      1, zombieParent.getChildren ().size ());
	assertEquals ("Zombie child task count",
		      0, zombieChild.taskPool.size ());
	assertEquals ("Zombie child process count",
		      0, zombieChild.getChildren ().size ());
	assertSame ("zombie and zombie's child's parent",
		    zombieParent, zombieChild.getParent ());
    }

    /**
     * A single threaded program performs an exec, check that it is correctly
     * tracked. 
     */
    public void testUnattachedSingleExec ()
    {
	AckProcess child = new AckDaemonProcess ();
	Proc proc = child.findProcUsingRefresh ();
	
	child.exec ();

	host.requestRefresh ();
	Manager.eventLoop.runPending ();

	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + proc.getPid (),
		      proc.getCmdLine ()[0]);
	
	assertEquals ("pid after exec", child.getPid (), proc.getPid ());
    }

    /**
     * A multiple threaded program performs an exec, check that it is correctly
     * tracked. 
     */
    public void testUnattachedMultipleExec ()
    {
	AckProcess child = new AckDaemonProcess (1);
	Proc proc = child.findProcUsingRefresh (true);

	Manager.host.requestRefresh (true);
	Task taskBefore = child.findTaskUsingRefresh (false);
	assertSame ("task before unattached multiple clone exec", proc,
		    taskBefore.getProc()); // parent/child relationship
	assertTrue ("task before unattached multiple clone exec",
		    proc.getPid () != taskBefore.getTid ()); // not main task

 	child.execClone ();

	Manager.host.requestRefresh (true);
 	Manager.eventLoop.runPending ();
	
	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + taskBefore.getTid (),
		      proc.getCmdLine ()[0]);
	
	Task task = child.findTaskUsingRefresh (false);
	// Exec wipes out non-main tasks so now get back nul.
	assertNull ("task after unattached multiple clone exec", task);
    }
}
