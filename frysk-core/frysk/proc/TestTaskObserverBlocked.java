// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.util.logging.Level;

/**
 * Check the behavior of an observer that blocks a Task's progress. In
 * particular, the case of fork and clone where both the parent and the child
 * are blocked.
 */

public class TestTaskObserverBlocked
    extends TestLib
{

  /**
   * Check that a blocker appears in the blocker list returned by getBlockers.
   */
  public void testGetBlockers ()
  {
    // Block any task that reports a TaskAttached event and then
    // shut down the event loop. Accumulate all blocked tasks in
    // the TaskObserverBase's task set.
    class BlockAttached
        extends TaskObserverBase
        implements TaskObserver.Attached
    {
      TaskSet attachedTasks = new TaskSet();

      public Action updateAttached (Task task)
      {
        attachedTasks.add(task);
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }
    }
    BlockAttached blockAttached = new BlockAttached();

    // Run a program, any program so that blockedAttached has
    // something to block.
    AckProcess child = new DetachedAckProcess();
    Task mainTask = child.findTaskUsingRefresh(true);
    mainTask.requestAddAttachedObserver(blockAttached);
    assertRunUntilStop("run \"exit\" to exit");

    // That one task was blocked.
    Task[] tasks = blockAttached.attachedTasks.toArray();
    assertEquals("blocked task count", 1, tasks.length);

    // That the Task's blocker set only contains this task.
    for (int i = 0; i < tasks.length; i++)
      {
        Task task = tasks[i];
        TaskObserver[] blockers = task.getBlockers();
        assertEquals("blockers length", 1, blockers.length);
        assertSame("blocker and blockAttached", blockAttached, blockers[0]);
      }
  }

  /**
   * Attach to the main task, watch for clone events. Add the observer to the
   * clone.
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

    private State UNATTACHED = new State("UNATTACHED");

    private State OBSERVER_ADDED_TO_PARENT = new State(
                                                       "OBSERVER_ADDED_TO_PARENT");

    private State SPAWN_PARENT = new State("SPAWN_PARENT");

    private State SPAWN_OFFSPRING = new State("SPAWN_OFFSPRING");

    private State OBSERVER_ADDED_TO_CHILD = new State("OBSERVER_ADDED_TO_CHILD");

    private State CHILD_ATTACHED = new State("CHILD_ATTACHED");

    /**
     * State of the Spawned observer, tracks the sequencing that occurs.
     */
    private State currentState = UNATTACHED;

    void assertInState (State state)
    {
      assertSame("state", state, currentState);
    }

    void nextState (State state)
    {
      currentState = state;
      logger.log(Level.FINE, "{0} nextState {1}\n",
                 new Object[] { this, state });
    }

    /**
     * This observer has been added to Object.
     */
    public void addedTo (final Object o)
    {
      if (currentState == UNATTACHED)
        nextState(OBSERVER_ADDED_TO_PARENT);
      else if (currentState == SPAWN_OFFSPRING)
        nextState(OBSERVER_ADDED_TO_CHILD);
      else
        fail("in wrong state <" + currentState + "> when adding to " + o);
      super.addedTo(o);
      Manager.eventLoop.requestStop();
    }

    private Task parent;

    private Task offspring;

    protected Action spawnedParent (Task parent, Task offspring)
    {
      logger.log(Level.FINE, "{0} spawnedParent\n", this);
      assertInState(OBSERVER_ADDED_TO_PARENT);
      nextState(SPAWN_PARENT);
      this.parent = parent;
      // Can't stop the event loop at this point as, more often
      // than not, it has a back-to-back offspring event.
      return Action.BLOCK;
    }

    protected Action spawnedOffspring (Task parent, Task offspring)
    {
      logger.log(Level.FINE, "{0} spawnedOffspring\n", this);
      assertInState(SPAWN_PARENT);
      nextState(SPAWN_OFFSPRING);
      this.offspring = offspring;
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    /**
     * Officially attached to Task.
     */
    public Action updateAttached (Task task)
    {
      assertInState(OBSERVER_ADDED_TO_CHILD);
      nextState(CHILD_ATTACHED);
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    /**
     * Create a new daemon process, attach to it's spawn observer (forked or
     * clone), and then wait for it to perform the spawn. The spawn observer
     * will leave both the parent and child blocked.
     */
    public void assertRunToSpawn ()
    {
      logger.log(Level.FINE, "{0} assertRunToSpawn\n", this);
      AckProcess proc = new AckDaemonProcess();
      Task main = proc.findTaskUsingRefresh(true);

      requestAddSpawnObserver(main);
      assertRunUntilStop("adding spawn observer");
      assertInState(OBSERVER_ADDED_TO_PARENT);

      requestSpawn(proc);
      assertRunUntilStop("run to spawn");
      assertInState(SPAWN_OFFSPRING);
    }

    /**
     * Unblock the child, then confirm that it is running (running child will
     * signal this process).
     */
    public void assertUnblockOffspring ()
    {
      logger.log(Level.FINE, "{0} assertUnblockOffspring\n", this);

      offspring.requestAddAttachedObserver(this);
      assertRunUntilStop("add observer to child");
      assertInState(OBSERVER_ADDED_TO_CHILD);

      // Remove this from the blockers list, is preventing the
      // spawned offspring from running. The child will then
      // notify any attached observers.
      offspring.requestUnblock(this);
      assertRunUntilStop("allow child to attach");
      assertInState(CHILD_ATTACHED);

      // Remove this from the blockers list as an attached
      // observer.
      AckHandler ack = new AckHandler(childAck, "childAck");
      offspring.requestUnblock(this);
      ack.await();
    }

    /**
     * Unblock the parent, then confirm that it is running (running parent will
     * signal this process).
     */
    public void assertUnblockParent ()
    {
      logger.log(Level.FINE, "{0} assertUnblockParent\n", this);
      AckHandler ack = new AckHandler(parentAck, "parentAck");
      parent.requestUnblock(this);
      ack.await();
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
      child.signal(addCloneSig);
    }

    void requestAddSpawnObserver (Task task)
    {
      task.requestAddClonedObserver(this);
    }

    /**
     * The parent Task cloned.
     */
    public Action updateClonedParent (Task parent, Task offspring)
    {
      return spawnedParent(parent, offspring);
    }

    /**
     * The parent Task cloned.
     */
    public Action updateClonedOffspring (Task parent, Task offspring)
    {
      return spawnedOffspring(parent, offspring);
    }
  }

  /**
   * Check that a clone observer can block both the parent and child, and that
   * the child can be allowed to run before the parent.
   */
  public void testBlockedCloneUnblockChildFirst ()
  {
    CloneObserver clone = new CloneObserver();
    clone.assertRunToSpawn();
    clone.assertUnblockOffspring();
    clone.assertUnblockParent();
  }

  /*
   * Check that a clone observer can block both the parent and child, and that
   * the parent can be allowed to run before the child.
   */
  public void testBlockedCloneUnblockParentFirst ()
  {
    CloneObserver clone = new CloneObserver();
    clone.assertRunToSpawn();
    clone.assertUnblockParent();
    clone.assertUnblockOffspring();
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
      child.signal(addForkSig);
    }

    void requestAddSpawnObserver (Task task)
    {
      task.requestAddForkedObserver(this);
    }

    /**
     * The parent Task forked.
     */
    public Action updateForkedParent (Task parent, Task offspring)
    {
      return spawnedParent(parent, offspring);
    }

    public Action updateForkedOffspring (Task parent, Task offspring)
    {
      return spawnedOffspring(parent, offspring);
    }
  }

  /**
   * Check that a fork observer can block both the parent and child, and that
   * the child can be allowed to run before the parent.
   */
  public void testBlockedForkUnblockChildFirst ()
  {
    ForkObserver fork = new ForkObserver();
    fork.assertRunToSpawn();
    fork.assertUnblockOffspring();
    fork.assertUnblockParent();
  }

  /**
   * Check that a fork observer can block both the parent and child, and that
   * the parent can be allowed to run before the child.
   */
  public void testBlockedForkUnblockParentFirst ()
  {
    ForkObserver fork = new ForkObserver();
    fork.assertRunToSpawn();
    fork.assertUnblockParent();
    fork.assertUnblockOffspring();
  }

  /**
   * Check that an unblocked offspring, that then exits, can be refreshed. This
   * confirms that an unblocked task transitioned to detached.
   */
  public void testRefreshAfterUnblockedForkExits ()
  {
    AckProcess proc = new AckDaemonProcess();
    Task task = proc.findTaskUsingRefresh(true);
    class ForkUnblock
        extends TaskObserverBase
        implements TaskObserver.Forked
    {
      Task parent;

      Task offspring;

      public void addedTo (Object o)
      {
        Manager.eventLoop.requestStop();
      }

      public Action updateForkedParent (Task parent, Task offspring)
      {
        this.parent = parent;
        this.offspring = offspring;
        return Action.CONTINUE;
      }

      public Action updateForkedOffspring (Task parent, Task offspring)
      {
        offspring.requestUnblock(this);
        return Action.BLOCK;
      }
    }
    ForkUnblock forkUnblock = new ForkUnblock();
    task.requestAddForkedObserver(forkUnblock);
    assertRunUntilStop("adding fork observer");

    // Create a child process, will transition through to
    // detached.
    proc.assertSendAddForkWaitForAcks();

    // Now make the child exit. Frysk's core can't see this since
    // it isn't attached to the process.
    proc.assertSendDelForkWaitForAcks();

    logger.log(Level.FINE, "{0} parent\n", forkUnblock.parent);
    logger.log(Level.FINE, "{0} offspring\n", forkUnblock.offspring);

    // Finally force a refresh.
    host.requestRefreshXXX(true);
    Manager.eventLoop.runPending();
  }

  /**
   * Check that new observers being added hot on the heals of an unblock get
   * processed correctly - this forces the state machine that was detaching to
   * start attaching again, but very late.
   */
  public void testAddObserverAfterUnblock ()
  {

    // if (brokenXXX (2937))
    // return;

    AckProcess proc = new AckDaemonProcess();
    Task task = proc.findTaskUsingRefresh(true);
    class UnblockAdd
        extends TaskObserverBase
        implements TaskObserver.Forked
    {
      public void addedTo (Object o)
      {
        Manager.eventLoop.requestStop();
      }

      public Action updateForkedParent (Task parent, Task offspring)
      {
        return Action.CONTINUE;
      }

      public Action updateForkedOffspring (Task parent, Task offspring)
      {
        offspring.requestUnblock(this);
        offspring.requestAddForkedObserver(this);
        return Action.BLOCK;
      }
    }
    UnblockAdd observer = new UnblockAdd();
    task.requestAddForkedObserver(observer);
    assertRunUntilStop("adding fork observer");

    // Create a child process, will transition through to
    // detached.
    proc.assertSendAddForkWaitForAcks();
  }

  /**
   * Test that Task.requestAddObserver () can be used to hold multiple tasks at
   * the spawn point. This creates a program that, in turn, creates lots and
   * lots of tasks. It then checks that the number of task create and delete
   * events matches the expected.
   */
  abstract class BlockingFibonacci
      extends TaskObserverBase
  {
    static final int fibCount = 10;

    TaskSet parentTasks = new TaskSet();

    TaskSet childTasks = new TaskSet();

    /** Program to run. */
    abstract String fibonacciProgram ();

    /** Seed the observer. */
    abstract void addFirstObserver (Task task);

    BlockingFibonacci ()
    {

      // Compute the expected number of tasks (this includes the
      // main task).
      Fibonacci fib = new Fibonacci(fibCount);

      AttachedDaemonProcess child = new AttachedDaemonProcess(
                                                              new String[] {
                                                                            fibonacciProgram(),
                                                                            Integer.toString(fibCount) });
      addFirstObserver(child.mainTask);
      child.resume();

      // An object that, when the child process exits, both sets
      // a flag to record that event, and requests that the
      // event loop stop.
      StopEventLoopWhenProcRemoved childRemoved = new StopEventLoopWhenProcRemoved(
                                                                                   child.mainTask.proc.getPid());

      // Repeatedly run the event loop until the child exits
      // (every time there is a spawn the event loop will stop).
      int spawnCount = 0;
      int loopCount = 0;
      while (loopCount <= fib.callCount && ! childRemoved.p)
        {
          loopCount++;
          assertRunUntilStop("run \"fibonacci\" until stop, number "
                             + spawnCount + " of " + fib.callCount);
          spawnCount += parentTasks.size();
          parentTasks.unblock(this).clear();
          childTasks.unblock(this).clear();
        }

      // The first task, included in fib.callCount isn't
      // included in the spawn count.
      assertEquals("number of times spawnObserver added", fib.callCount,
                   addedCount);
      assertEquals("number of times spawnObserver deleted", 0, deletedCount);
      assertEquals("Number of spawns", fib.callCount - 1, spawnCount);
      assertTrue("child exited", childRemoved.p);
      assertTrue("at least two iterations of the spawn loop", loopCount > 2);
    }
  }

  /**
   * Check that a program rapidly cloning can be stopped and started at the
   * cline points.
   */
  public void testBlockingFibonacciClone ()
  {
    class CloneFibonacci
        extends BlockingFibonacci
        implements TaskObserver.Cloned
    {
      public Action updateClonedParent (Task parent, Task offspring)
      {
        parentTasks.add(parent);
        return Action.BLOCK;
      }

      public Action updateClonedOffspring (Task parent, Task offspring)
      {
        offspring.requestAddClonedObserver(this);
        childTasks.add(offspring);
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }

      void addFirstObserver (Task task)
      {
        task.requestAddClonedObserver(this);
      }

      String fibonacciProgram ()
      {
        return getExecPrefix() + "funit-fib-clone";
      }
    }
    new CloneFibonacci();
  }

  /**
   * Check that a program rapidly cloning can be stopped and started at the
   * cline points.
   */
  public void testBlockingFibonacciFork ()
  {
    class ForkFibonacci
        extends BlockingFibonacci
        implements TaskObserver.Forked
    {
      public Action updateForkedParent (Task parent, Task offspring)
      {
        parentTasks.add(parent);
        return Action.BLOCK;
      }

      public Action updateForkedOffspring (Task parent, Task offspring)
      {
        childTasks.add(offspring);
        offspring.requestAddForkedObserver(this);
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }

      void addFirstObserver (Task task)
      {
        task.requestAddForkedObserver(this);
      }

      String fibonacciProgram ()
      {
        return getExecPrefix() + "funit-fib-fork";
      }
    }
    new ForkFibonacci();
  }

  /**
   * Check that excessive un-blocks do not panic the state machine.
   */
  public void testUnblockRunning ()
  {
    Child child = new AckDaemonProcess();
    Task task = child.findTaskUsingRefresh(true);

    class UnblockRunning
        extends TaskObserverBase
        implements TaskObserver.Attached
    {
      public Action updateAttached (Task task)
      {
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }

      public void deletedFrom (Object o)
      {
        Manager.eventLoop.requestStop();
      }
    }
    UnblockRunning unblockRunning = new UnblockRunning();

    task.requestAddAttachedObserver(unblockRunning);
    assertRunUntilStop("attach then block");

    // Queue up three actions, the middle unblock is stray.
    task.requestUnblock(unblockRunning);
    task.requestUnblock(unblockRunning);
    task.requestDeleteAttachedObserver(unblockRunning);
    assertRunUntilStop("unblock then detach");
  }
}
