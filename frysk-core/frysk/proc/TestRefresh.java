// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Observer;
import java.util.Observable;
import frysk.testbed.TaskCounter;
import frysk.testbed.PidCounter;
import frysk.sys.Pid;
import frysk.testbed.TestLib;
import frysk.testbed.Offspring;
import frysk.testbed.SlaveOffspring;

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
	Offspring child;
	ChildTracker (Offspring child)
	{
	    this.child = child;
	    added = new PidCounter (child.getPid (),
				    host.observableProcAddedXXX);
	    removed = new PidCounter (child.getPid (),
				      host.observableProcRemovedXXX);
	}
	Proc proc;
	void verifyAdd (String reason)
	{
	    proc = host.getProc (new ProcId (child.getPid ()));
	    assertNotNull (reason + ", child in process pool;",
			   proc);
	    assertEquals (reason + ", child discovered once",
			  1, added.count);
	    assertEquals (reason + ", child removed nunce",
			  0, removed.count);
	    assertEquals (reason + ", child state",
			  "detached", proc.getState ().toString ());
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
			  "destroyed", proc.getState ().toString ());
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
	    host.requestRefreshXXX();
	    Manager.eventLoop.runPending ();
	    tracker.verifyAdd ("iteration " + i);
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
	host.requestRefreshXXX();
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
	// corresponding observable events occurred.
	host.requestRefreshXXX();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("first add");

	// Delete the process.
	child.reap ();

	// Check that a further refresh removes the process, generates
	// a removed event, and puts the proc into the removed
	// state.
	host.requestRefreshXXX();
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
	SlaveOffspring child = new AckDaemonProcess()
	    .assertSendAddClonesWaitForAcks(nrTasks - 1);

	// Create a task counter, to count the number discovered and
	// removed tasks.
	TaskCounter taskCount = new TaskCounter ();

	// Track the child process.
	ChildTracker tracker = new ChildTracker (child);

	// Do a host refresh so that the child process can be found.
	// At this stage, since the process's tasks are only located
	// on an explicit refresh, the process should have no tasks,
	// and no task events should have been seen.
	host.requestRefreshXXX();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("refresh without tasks");

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
 	    assertEquals ("task " + task + " state", "detached",
			  task.getState ().toString ());
 	}

	// Tell the child to drop two tasks.  Check that the refresh
	// looses two tasks, and that the lost tasks have been
	// transitioned to the dead state.
	for (int i = 0; i < nrKills; i++)
	    child.assertSendDelCloneWaitForAcks ();
	tracker.proc.requestRefresh ();
	Manager.eventLoop.runPending ();
	assertEquals ("proc's task count after kills",
		      nrKills, tracker.proc.taskPool.size ());
 	assertEquals ("tasks added count after kills (no change)",
 		      nrTasks, taskCount.added.size ());
 	for (Iterator i = tracker.proc.taskPool.values ().iterator ();
	     i.hasNext ();) {
 	    Task task = (Task) i.next ();
	    assertEquals ("task " + task + " state", "detached",
			  task.getState ().toString ());
 	}
	assertEquals ("tasks removed by refresh after kills",
		      nrKills, taskCount.removed.size ());
	for (Iterator i = taskCount.removed.iterator (); i.hasNext (); ) {
	    Task task = (Task) i.next ();
	    assertEquals ("removed task state", "destroyed",
			  task.getState ().toString ());
	}

	// Finally, tell the child to add a task back.  Check that the
	// counts are again updated.
	child.assertSendAddCloneWaitForAcks ();
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
     * Check that a parent child relationship is correct.
     */
    public void testParentChild ()
    {
	// Create a sub process, refresh things so that it is known.
	ChildTracker tracker = new ChildTracker (new DetachedAckProcess ());
	host.requestRefreshXXX();
	Manager.eventLoop.runPending ();
	tracker.verifyAdd ("find child");
	
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
	SlaveOffspring zombie = new AckDaemonProcess ();
	zombie.assertSendAddForkWaitForAcks ();
	
	// Do a refresh, find the zombie maker, check it has one child
	// process, save it.
	Proc zombieParent = zombie.assertFindProcAndTasks();
	
	//XXX: Hack to get zombieParent's children.
	Manager.host.requestRefreshXXX();
	Manager.eventLoop.runPending();
    
	assertEquals ("zombie maker has one child",
		      1, zombieParent.getChildren ().size ());
	Proc zombieChild = (Proc) zombieParent.getChildren ().getFirst ();
	assertSame ("zombie and zombie child's parent",
		    zombieChild.getParent (), zombieParent);
	Proc procOne = host.getProc (new ProcId (1));

	// Blow away the parent, this turns the child into a daemon,
	// do a refresh and check that the child's parent changed to
	// process one.
	zombie.assertSendFryParentWaitForAcks ();
	host.requestRefreshXXX();
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
     * Check that when a process exits, its child is removed (and
     * moved to pid 1).
     *
     * Can only test this with one child - in the case of multiple
     * children they all try to tell this process that their parent
     * exted using the same signal, and since the signal isn't
     * counting, some get lost, and some get missed.
     */
    public void testExitLoosesChild ()
    {
	SlaveOffspring daemon = new AckDaemonProcess ();
	daemon.assertSendAddForkWaitForAcks ();

	// Find the process and it's children.
	Proc daemonProc = daemon.assertFindProcAndTasks();

	// Monitor proc deleted events looking for the daemon being
	// removed.
	class DaemonCheck
	    implements Observer
	{
	    Proc procOne = host.getProc (new ProcId (1));
	    Proc proc;
	    boolean deleted;
	    Collection children;
	    DaemonCheck (Proc proc)
	    {
		this.proc = proc;
		this.children = proc.getChildren ();
	    }
	    public void update (Observable o, Object obj)
	    {
		Proc d = (Proc) obj;
		if (d != proc)
		    return;
		assertFalse ("deleted", deleted);
		assertEquals ("proc has no children", 0,
			      proc.getChildren ().size ());
		for (Iterator i = children.iterator (); i.hasNext (); ) {
		    Proc child = (Proc) i.next ();
		    assertSame ("child has 1 as parent", procOne,
				child.getParent ());
		}
		deleted = true;
	    }
	}
	DaemonCheck daemonCheck = new DaemonCheck (daemonProc);
	host.observableProcRemovedXXX.addObserver (daemonCheck);

	// Blow away the daemon, force an update.
	daemon.assertSendFryParentWaitForAcks ();
	host.requestRefreshXXX();
	Manager.eventLoop.runPending ();
	assertTrue ("daemonCheck.deleted", daemonCheck.deleted);
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
	SlaveOffspring zombie = new AckDaemonProcess ();
	zombie.assertSendAddForkWaitForAcks ();
	
	// Do a refresh (that includes updating the task list), find
	// the zombie maker, check that it's child has one task and no
	// processes.
	Proc zombieParent = zombie.assertFindProcAndTasks();
    
	//XXX: Hack to get zombieParent's children.
	Manager.host.requestRefreshXXX();
	Manager.eventLoop.runPending();
    
	assertEquals ("zombie maker child count", 1,
		      zombieParent.getChildren ().size ());
	Proc zombieChild = (Proc) zombieParent.getChildren ().getFirst ();
	assertEquals ("zombie child process count",
		      0, zombieChild.getChildren ().size ());

	// Turn the zombie-child into a true zombie, check things are
	// updated.
	zombie.assertSendZombieForkWaitForAcks ();
	host.requestRefreshXXX ();
	Manager.eventLoop.runPending ();
 	assertEquals ("zombie maker child count",
 		      1, zombieParent.getChildren ().size ());
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
	SlaveOffspring child = new AckDaemonProcess ();
	Proc proc = child.assertFindProcAndTasks();
	
	child.assertSendExecWaitForAcks ();

	host.requestRefreshXXX();
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
	SlaveOffspring child = new AckDaemonProcess()
	    .assertSendAddClonesWaitForAcks(1);
	Proc proc = child.assertFindProcAndTasks();

	Manager.host.requestRefreshXXX ();
	Task taskBefore = child.findTaskUsingRefresh (false);
	assertSame ("task before unattached multiple clone exec", proc,
		    taskBefore.getProc()); // parent/child relationship
	assertTrue ("task before unattached multiple clone exec",
		    proc.getPid () != taskBefore.getTid ()); // not main task

 	child.assertSendExecCloneWaitForAcks ();

	Manager.host.requestRefreshXXX ();
 	Manager.eventLoop.runPending ();
	
	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + taskBefore.getTid (),
		      proc.getCmdLine ()[0]);
	
	Task task = child.findTaskUsingRefresh (false);
	// Exec wipes out non-main tasks so now get back nul.
	assertNull ("task after unattached multiple clone exec", task);
    }
}
