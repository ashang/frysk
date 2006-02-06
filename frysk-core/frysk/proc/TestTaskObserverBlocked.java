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
 * Check the behavior of an observer that blocks a Task's progress.
 * In particular, the case of fork and clone where both the parent and
 * the child are blocked.
 */

public class TestTaskObserverBlocked
    extends TestLib
{

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
	host.requestCreateAttachedProcXXX
	    (new String[] {
		getExecPrefix () + "funit-exit",
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

    /**
     * Attach to the main task, watch for clone events.  Add the
     * observer to the clone.
     */
    abstract class SpawnObserver
	extends TaskObserverBase
	implements TaskObserver.Attached
    {
	/**
	 * Possible states of the spawn observer.
	 */
	private class State
	{
	    String name;
	    State (String name)
	    {
		this.name = name;
	    }
	    public String toString ()
	    {
		return name;
	    }
	}
	private State UNATTACHED = new State ("UNATTACHED");
	private State OBSERVER_ADDED_TO_PARENT = new State ("OBSERVER_ADDED_TO_PARENT");
	private State PARENT_SPAWNED = new State ("PARENT_SPAWNED");
	private State OBSERVER_ADDED_TO_CHILD = new State ("OBSERVER_ADDED_TO_CHILD");
	private State CHILD_ATTACHED = new State ("CHILD_ATTACHED");
	/**
	 * State of the Spawned observer, tracks the sequencing that
	 * occures.
	 */
	public State state = UNATTACHED;

	/**
	 * This observer has been added to Object.
	 */
	public void addedTo (final Object o)
	{
	    if (state == UNATTACHED)
		state = OBSERVER_ADDED_TO_PARENT;
	    else if (state == PARENT_SPAWNED)
		state = OBSERVER_ADDED_TO_CHILD;
	    else
		fail ("in wrong state " + state);
	    super.addedTo (o);
	    Manager.eventLoop.requestStop ();
	}
	private Task parent;
	private Task child;
	protected Action spawned (Task task, Task spawn)
	{
	    assertSame ("state", OBSERVER_ADDED_TO_PARENT, state);
	    state = PARENT_SPAWNED;
	    Manager.eventLoop.requestStop ();
	    parent = task;
	    child = spawn;
	    return Action.BLOCK;
	}
	/**
	 * Officially attached to Task.
	 */
	public Action updateAttached (Task task)
	{
	    assertSame ("state", OBSERVER_ADDED_TO_CHILD, state);
	    state = CHILD_ATTACHED;
	    Manager.eventLoop.requestStop ();
	    return Action.BLOCK;
	}
	/**
	 * Create a new daemon process, attach to it's spawn observer
	 * (forked or clone), and then wait for it to perform the
	 * spawn.  The spawn observer will leave both the parent and
	 * child blocked.
	 */
	public void assertRunToSpawn ()
	{
	    AckProcess proc = new AckDaemonProcess ();
	    Task main = proc.findTaskUsingRefresh (true);
	    requestAddSpawnObserver (main);
	    
	    assertRunUntilStop ("adding clone observer");
	    assertSame ("observer state", OBSERVER_ADDED_TO_PARENT, state);
	    
	    requestSpawn (proc);
	    assertRunUntilStop ("run to spawn");
	    assertSame ("observer state", PARENT_SPAWNED, state);
	}

	/**
	 * Unblock the child, then confirm that it is running (running
	 * child will signal this process).
	 */
	public void assertChildUnblocked ()
	{
	    child.requestAddAttachedObserver (this);
	    assertRunUntilStop ("add observer to child");
	    assertSame ("observer state", OBSERVER_ADDED_TO_CHILD, state);
	    
	    child.requestUnblock (this);
	    assertRunUntilStop ("allow child to attach");
	    assertSame ("observer state", CHILD_ATTACHED, state);
	    
	    AckHandler childAck = new AckHandler (AckProcess.childAck);
	    child.requestUnblock (this);
	    childAck.await ();
	}
	/**
	 * Unblock the parent, then confirm that it is running
	 * (running parent will signal this process).
	 */
	public void assertParentUnblocked ()
	{
	    AckHandler parentAck = new AckHandler (AckProcess.parentAck);
	    parent.requestUnblock (this);
	    parentAck.await ();
	}
	abstract void requestSpawn (AckProcess proc);
	abstract void requestAddSpawnObserver (Task task);
    }
    /**
     * Implementation of SpawnObserver that monitors a clone.
     */
    class CloneObserver
	extends SpawnObserver
	implements TaskObserver.Cloned
    {
	void requestSpawn (AckProcess child)
	{
	    child.signal (AckProcess.addCloneSig);
	}
	void requestAddSpawnObserver (Task task)
	{
	    task.requestAddClonedObserver (this);
	}
	/**
	 * The parent Task cloned.
	 */
	public Action updateCloned (Task task, Task clone)
	{
	    return spawned (task, clone);
	}
    }
    /**
     * Check that a clone observer can block both the parent and
     * child, and that the child can be allowed to run before the
     * parent.
     */
    public void testBlockedCloneUnblockChildFirst ()
    {
	CloneObserver clone = new CloneObserver ();
	clone.assertRunToSpawn ();
	clone.assertChildUnblocked ();
	clone.assertParentUnblocked ();
    }
    /*
     * Check that a clone observer can block both the parent and
     * child, and that the parent can be allowed to run before the
     * child.
     */
    public void testBlockedCloneUnblockParentFirst ()
    {
	CloneObserver clone = new CloneObserver ();
	clone.assertRunToSpawn ();
	clone.assertParentUnblocked ();
	clone.assertChildUnblocked ();
    }
    /**
     * Implementation of SpawnObserver that monitors a fork.
     */
    class ForkObserver
	extends SpawnObserver
	implements TaskObserver.Forked
    {
	void requestSpawn (AckProcess child)
	{
	    child.signal (AckProcess.addForkSig);
	}
	void requestAddSpawnObserver (Task task)
	{
	    task.requestAddForkedObserver (this);
	}
	/**
	 * The parent Task forked.
	 */
	public Action updateForked (Task task, Task fork)
	{
	    return spawned (task, fork);
	}
    }
    /**
     * Check that a fork observer can block both the parent and
     * child, and that the child can be allowed to run before the
     * parent.
     */
    public void testBlockedForkUnblockChildFirst ()
    {
	ForkObserver fork = new ForkObserver ();
	fork.assertRunToSpawn ();
	fork.assertChildUnblocked ();
	fork.assertParentUnblocked ();
    }
    /*
     * Check that a fork observer can block both the parent and
     * child, and that the parent can be allowed to run before the
     * child.
     */
    public void testBlockedForkUnblockParentFirst ()
    {
	ForkObserver fork = new ForkObserver ();
	fork.assertRunToSpawn ();
	fork.assertParentUnblocked ();
	fork.assertChildUnblocked ();
    }


    /**
     * Test that Task.requestAddObserver () can be used to hold
     * multiple tasks at the spawn point.
     *
     * This creates a program that, in turn, creates lots and lots of
     * tasks.  It then checks that the number of task create and
     * delete events matches the expected.
     */
    abstract class BlockingFibonacci
	extends AutoAddTaskObserverBase
    {
	static final int fibCount = 10;
	TaskSet parentTasks = new TaskSet ();
	TaskSet childTasks = new TaskSet ();
	abstract String fibonacciProgram ();
	BlockingFibonacci ()
	{
	    // An object that, when the child process exits, both sets
	    // a flag to record that event, and requests that the
	    // event loop stop.
	    StopEventLoopWhenChildProcRemoved childRemoved
		= new StopEventLoopWhenChildProcRemoved ();

	    // Compute the expected number of tasks (this includes the
	    // main task).
	    Fibonacci fib = new Fibonacci (fibCount);
	    
	    host.requestCreateAttachedProc
		(null, "/dev/null", null, new String[] {
		    fibonacciProgram (),
		    Integer.toString (fibCount)
		});
	    
	    // Repeatedly run the event loop until the child exits
	    // (every time there is a spawn the event loop will stop).
	    int spawnCount = 0;
	    int loopCount = 0;
	    while (loopCount <= fib.callCount && !childRemoved.p) {
		loopCount++;
		assertRunUntilStop ("run \"fibonacci\" until stop, number "
				    + spawnCount + " of " + fib.callCount);
		spawnCount += parentTasks.size ();
		parentTasks.unblock (this);
		parentTasks.clear ();
		childTasks.unblock (this);
		childTasks.clear ();
	    }
	    
	    // The first task, included in fib.callCount isn't
	    // included in the spawn count.
	    assertEquals ("number of times spawnObserver added",
			  fib.callCount, addedCount);
	    assertEquals ("number of times spawnObserver deleted",
			  0, deletedCount);
	    assertEquals ("Number of spawns", fib.callCount - 1, spawnCount);
	    assertTrue ("child exited", childRemoved.p);
	    assertTrue ("at least two iterations of the spawn loop",
			loopCount > 2);
	}
    }
    /**
     * Check that a program rapidly cloning can be stopped and started
     * at the cline points.
     */
    public void testBlockingFibonacciClone ()
    {
	class CloneFibonacci
	    extends BlockingFibonacci
	    implements TaskObserver.Cloned
	{
	    public Action updateCloned (Task task, Task clone)
	    {
		parentTasks.add (task);
		childTasks.add (clone);
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	    void updateTaskAdded (Task task)
	    {
		task.requestAddClonedObserver (this);
	    }
	    String fibonacciProgram ()
	    {
		return getExecPrefix () + "funit-fib-clone";
	    }
	}
    }
    /**
     * Check that a program rapidly cloning can be stopped and started
     * at the cline points.
     */
    public void testBlockingFibonacciFork ()
    {
	class ForkFibonacci
	    extends BlockingFibonacci
	    implements TaskObserver.Forked
	{
	    public Action updateForked (Task task, Task fork)
	    {
		parentTasks.add (task);
		childTasks.add (fork);
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	    void updateTaskAdded (Task task)
	    {
		task.requestAddForkedObserver (this);
	    }
	    String fibonacciProgram ()
	    {
		return getExecPrefix () + "funit-fib-fork";
	    }
	}
    }

    /**
     * Check that excessive un-blocks do not panic the state machine.
     */
    public void testUnblockRunning ()
    {
	Child child = new AckDaemonProcess ();
	Task task = child.findTaskUsingRefresh (true);

	class UnblockRunning
	    extends TaskObserverBase
	    implements TaskObserver.Attached
	{
	    public Action updateAttached (Task task)
	    {
		Manager.eventLoop.requestStop ();
		return Action.BLOCK;
	    }
	    public void deletedFrom (Object o)
	    {
		Manager.eventLoop.requestStop ();
	    }
	}
	UnblockRunning unblockRunning = new UnblockRunning ();

	task.requestAddAttachedObserver (unblockRunning);
	assertRunUntilStop ("attach then block");

	// Queue up three actions, the middle unblock is stray.
	task.requestUnblock (unblockRunning);
	task.requestUnblock (unblockRunning);
	task.requestDeleteAttachedObserver (unblockRunning);
	assertRunUntilStop ("unblock then detach");
    }
}
