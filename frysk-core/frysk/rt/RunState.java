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

package frysk.rt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
//import java.util.List;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import java.util.logging.Level;
import java.util.logging.Logger;


import lib.dw.Dwfl;
import lib.dw.DwflLine;
//import frysk.cli.hpd.SymTab;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Model for state transitions in the SourceWindow and HPD
 * classes. Currently possible states include: running, stopped,
 * instruction step, step in, step over, step out. Performs necessary
 * operations for each of these states including Task management,
 * adding/removing observers, and source work.
 */
public class RunState extends Observable implements TaskObserver.Instruction
{
  protected static Logger logger = Logger.getLogger ("frysk");
  
  /* Keeps track of the Dwfl objects for teach Task; not necessary to
   * re-generate these each time a step is done. */
  private HashMap dwflMap;

  /* Keeps track of the last executed line in source for each Task */
  private HashMap lineMap;
  
  /* Keep track of how many times the task has been unblocked for a particular
   * line. If it gets too high, assume that the line won't change (while(1))
   * and finish. */
  private HashMap lineCountMap;

  /* Set of Tasks currently running, or unblocked. */
  private HashSet runningTasks;

  /* Tasks that have hit a breakpoint */
  private HashMap breakpointMap;
  
  private int taskStepCount = 0;
  
  private int numRunningTasks = 0;
  
  private int state = 0;

  public static final int STOPPED = 0;
  public static final int RUNNING = 1;
  public static final int INSTRUCTION_STEP = 2;
  public static final int STEP_IN = 3;
  public static final int STEP_OVER = 4;
  public static final int STEP_OUT = 5;
  public static final int STEP_OVER_LINE_STEP = 6;
  
  private Proc stateProc;
  
  private LinkedList tasks;
  
  /**
   * Constructor - sets the InstructionObserver for this model and initializes
   * the Maps and Set.
   */
  public RunState ()
  {
    this.dwflMap = new HashMap();
    this.lineMap = new HashMap();
    this.lineCountMap = new HashMap();
    this.runningTasks = new HashSet();
    this.breakpointMap = new HashMap();
    
  }

  /**
   * Returns the Dwfl for this Task, and inserts it into the Dwfl map.
   * 
   * @param task The Task we want the Dwfl object from
   * @return d The Dwfl for the incoming Task
   */
  Dwfl getDwfl (Task task)
  {
    Dwfl d = (Dwfl) this.dwflMap.get(task);
    if (d == null)
      {
        d = new Dwfl(task.getTid());
        this.dwflMap.put(task, d);
      }
    return d;
  }
  
  /*****************************************************************************
   * STEP HANDLING METHODS
   ****************************************************************************/

  
  
  /**
   * Sets up stepping information - which tasks are stepping, how many there, 
   * and then initialize the dwflMap and lineMap with their information. Then
   * unblock each Task to begin the stepping.
   * 
   * @param tasks   The list of Tasks to step
   */
  public void setUp(LinkedList tasks)
  {
    this.taskStepCount = tasks.size();
    Iterator i = tasks.iterator();
    int zeroCount = 0;
    
    notifyNotBlocked();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
       //System.out.println("SetupStep.iterate " + t);
        if (lineMap.get(t) == null)
          {
            Dwfl d = getDwfl(t);
            DwflLine line = null;
            line = d.getSourceLine(t.getIsa().pc(t));
            
            if (line == null)
              {
               //System.out.println("Coulnd't get DwflLine, assigning 0");
                zeroCount++;
                this.lineMap.put(t, new Integer(0));
                this.lineCountMap.put(t, new Integer(0));
                continue;
              }

            this.lineMap.put(t, new Integer(line.getLineNum()));
          }
        this.lineCountMap.put(t, new Integer(0));
      }
    
    /* None of these tasks have any debug information, so a 
     * "line-step" is meaningless - perform an instruction step instead. */
    if (zeroCount == tasks.size())
      {
        this.dwflMap.clear();
        this.lineMap.clear();
        this.lineCountMap.clear();
        this.state = INSTRUCTION_STEP;
      }
    
    i = tasks.iterator();
    while (i.hasNext())
      ((Task) i.next()).requestUnblock(this);
  }
  
  /**
   * Set up line stepping by setting the appropriate state and then 
   * setting up the data structures for the step.
   * 
   * @param tasks   The Tasks to be stepped.
   */
  public void setUpLineStep (LinkedList tasks)
  {
    this.state = STEP_IN;
    setUp(tasks);
  }
  
  /**
   * Simply perform a single instruction step. Unblocks each Task which will
   * return to the ProcBlockObserver callback in the calling class.
   * 
   * @param tasks   The list of Tasks to step one instruction
   */
  public void stepInstruction (LinkedList tasks)
  {
    //System.out.println("Instruction step");
    this.state = INSTRUCTION_STEP;
    this.taskStepCount = tasks.size();
    Iterator i = tasks.iterator();
    notifyNotBlocked();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        //System.out.println("Requesitng unblock for " + t);
        t.requestUnblock(this);
      }
  }

  /**
   * Performs a step-in. Continues to unblock the Task instruction by
   * instruction until its Dwfl object tell us its line number has changed.
   * 
   * @param task    The task to step in
   */
  public synchronized void stepIn (Task task)
  {
    DwflLine line = null;
    line = getDwfl(task).getSourceLine(task.getIsa().pc(task));

    int lineNum;
    int prev;
    
    if (line == null) /* We're in no-debuginfo land */
      {
        lineNum = 0;
        prev = ((Integer) this.lineMap.get(task)).intValue();
      }
    else
      {
        lineNum = line.getLineNum();
        prev = ((Integer) this.lineMap.get(task)).intValue();
      }
    
    if (lineNum != prev)
      {
        this.lineMap.put(task, new Integer(lineNum));
        if (this.state == STEP_OVER_LINE_STEP && stepOutCount > 0)
          {
            task.requestUnblock (this);
            stepOutCount = 0;
            return;
          }
        --taskStepCount;
        return;
      }
    else
      {
        int count = ((Integer) this.lineCountMap.get(task)).intValue();
        count++;
        if (count > 10)
          {
           //System.out.println("single line broke");
            --taskStepCount;
            return;
          }
        else
          this.lineCountMap.put(task, new Integer(count));

        task.requestUnblock(this);
      }
  }
  
  Breakpoint breakpoint;
  StackFrame lastFrame;
  int stepOutCount = 0;
  
  /**
   * Sets up for step-over.
   * 
   * XXX: Not finished yet. Needs to work with multiple threads.
   * 
   * @param tasks   The list of Tasks to be stepped-over
   * @param lastFrame
   */
  public void setUpStepOver (LinkedList tasks, StackFrame lastFrame)
  {
    this.state = STEP_OVER_LINE_STEP;
    stepOutCount = 1;
    this.lastFrame = lastFrame;
    this.taskStepCount =  1;

    setUp(tasks);
  }
  
  /**
   * Checks to see if a step actually results in a new frame, and then sets
   * the breakpoint and lets the thread run.
   * 
   * XXX: Needs to work with multiple threads
   * 
   * @param task    The Task to execute step-over for.
   */
  public void stepOver (Task task)
  {
   
    StackFrame newFrame = null;
    newFrame = StackFactory.createStackFrame(task, 2);
   
    System.out.println("RunState -> stepOver " + newFrame.getCFA() + " " + lastFrame.getCFA());
    System.out.println("Comparing frame names " + newFrame.getMethodName() + " " + lastFrame.getMethodName());
    
    /* The two frames are the same; treat this step-over as a line step. */
    if (newFrame.getCFA() == this.lastFrame.getCFA())
      {
        this.setChanged();
        this.notifyObservers(task);
        return;
      }
    else
      {
        /* There is a different innermost frame on the stack - run until
         * it exits - success!. */
        long pc = task.getIsa().pc(task);
        System.out.println("RunState.stepOver Sucess " + lastFrame + " " + newFrame + " " + pc);
        StackFrame frame = newFrame.getOuter();
        this.state = STEP_OVER;
        this.breakpoint = new Breakpoint(frame.getAddress());
        task.requestAddCodeObserver(breakpoint, frame.getAddress());
      }
  }
  
  /**
   * Sets the stage for stepping out of a frame. Runs until a breakpoint on the
   * return address is hit. 
   * 
   * XXX: Needs to work properly with multiple threads.
   * 
   * @param tasks   The Tasks to step out.
   * @param lastFrame
   */
  public void setUpStepOut (LinkedList tasks, StackFrame lastFrame)
  {
    this.state = STEP_OUT;
    this.breakpoint = new Breakpoint(lastFrame.getOuter().getAddress());
    this.lastFrame = lastFrame;
    this.taskStepCount =  tasks.size();
    
    Task t = lastFrame.getTask();
    t.requestAddCodeObserver(breakpoint, lastFrame.getOuter().getAddress());
  }
  
  /**
   * Cleans up after a step-out operation, deletes the breakpoint.
   * 
   * @param task    The task finished stepping out.
   */
  public void stepOut (Task task)
  {
    task.requestDeleteCodeObserver(breakpoint, addy);
    cleanUpBreakPoint(task);
  }
  
  public void cleanUpBreakPoint (Task task)
  {
    this.setChanged();
    this.notifyObservers(task);
  }
  
  /**
   * Decrements the number of stepping Tasks
   */
  public void decTaskStepCount ()
  {
    this.taskStepCount--;
  }

  /**
   * All stepping has completed, clean up.
   */
  public void stepCompleted ()
  {
    this.state = STOPPED;
    this.lineCountMap.clear();
    this.taskStepCount = 0;
  }

  /*****************************************************************************
   * STOP/CONTINUE HANDLING METHODS
   ****************************************************************************/

  /**
   * Deletes the blocking observer from each of the incoming tasks,
   * effectively 'running', or continuing, the process.
   * 
   * @param tasks   The list of Tasks to be run
   */
  public void run (LinkedList tasks)
  {
    this.state = RUNNING;
    this.numRunningTasks = tasks.size();
    notifyNotBlocked();
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (! this.runningTasks.contains(t))
          {
            this.runningTasks.add(t);
            t.requestDeleteInstructionObserver(this);
	    // This seems to be required to unblock the process when
	    // there are code observers installed. Filed as bz4051.
	    t.requestUnblock(this);
	    Breakpoint bpt = (Breakpoint)breakpointMap.get(t);
	    if (bpt != null) 
	      {
		breakpointMap.remove(t);
		t.requestUnblock(bpt);
	      }
          }
      }
  }

  /**
   * Re-blocks all running Tasks except for those in the incoming LinkedList.
   * If the list is null or has size zero, than by default all Tasks are 
   * blocked by the ProcBlockObserver. Otherwise, the list is compared to the
   * set of running tasks and those Tasks not in the list are stopped.
   * 
   * @param unblockTasks    The list of Tasks to not block
   */
  public void stop (LinkedList unblockTasks)
  {
    if (unblockTasks == null)
      {
        requestAdd();
      }
    else
      {
        if (unblockTasks.size() == 0)
          requestAdd();
        else
          {
            Iterator i = this.runningTasks.iterator();
            LinkedList blockTasks = new LinkedList();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
                if (! unblockTasks.contains(t))
                  {
                    blockTasks.add(t);
                    i.remove();
                  }
              }
            blockTask(blockTasks);
          }
      }
    this.state = STOPPED;
    this.runningTasks.clear();
  }

  
  /**
   * Method to handle am incoming list of tasks to be run, with four cases.
   * 
   * Case 1:    List of tasks is empty, and there are no running tasks.
   * Case 2:    List of tasks is empty, and there are running tasks.
   * Case 3:    List of tasks is not empty, and there are no running tasks.
   * Case 4:    List of tasks is not empty, and there are running tasks.
   * 
   * It is assumed that any running Tasks not in the incoming list are to be
   * blocked, and all tasks in that list are to be unblocked or run. If any of
   * those Tasks are already running, leave them be. Return an int representing
   * which state the calling class should be in after this method is executed.
   * 
   * @param tasks   The list of tasks to be run
   */
  public synchronized void executeTasks (LinkedList tasks)
  {

     //System.out.println("In executeThreads with thread size " + tasks.size()
     //+ " and runningtasks size "
     //+ this.runningTasks.size());

    /* No incoming Tasks and no Tasks already running */
    if (tasks.size() == 0 && this.runningTasks.size() == 0)
      {
        this.state = STOPPED;
        return;
      }

    /* No incoming Tasks, but some Tasks are running. Block them. */
    else if (tasks.size() == 0 && this.runningTasks.size() != 0)
      {
        LinkedList l = new LinkedList();
        Iterator i = this.runningTasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            l.add(t);
            i.remove();
            // System.out.println("Blocking " + t);
          }
        blockTask(l);
        return;
      }

    /* There are incoming Tasks to be run, and no Tasks already running */
    if (this.runningTasks.size() == 0)
      {
        this.state = RUNNING;
        notifyNotBlocked();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("(0) Running " + t);
            this.runningTasks.add(t);
            t.requestDeleteInstructionObserver(this);
          }
        return;
      }
    else
      /* There are incoming Tasks to be run, and some Tasks are already running.
       * If they are not already running, unblock the incoming Tasks, and block
       * any running Task not in the incoming list. */
      {
        this.state = RUNNING;
        HashSet temp = new HashSet();
        // this.runningThreads.clear();
        notifyNotBlocked();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("Iterating running thread" + t);
            /* If this thread has not already been unblocked, do it */
            if (! this.runningTasks.remove(t))
              {
                // System.out.println("unBlocking " + t);
                t.requestDeleteInstructionObserver(this);
              }
            else
              // System.out.println("Already Running");
              /* Put all threads back into a master list */
              temp.add(t);
          }

        /* Now catch the threads which have a block request */
        if (this.runningTasks.size() != 0)
          {
            // System.out.println("temp size not zero");
            LinkedList l = new LinkedList();
            i = this.runningTasks.iterator();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
                l.add(t);
                // System.out.println("Blocking from runningTasks " + t);
              }
            blockTask(l);
          }

        this.runningTasks = temp;
        // System.out.println("rt temp" + this.runningThreads.size() + " "
        // + temp.size());
      }
    return;
  }
  
  /**
   * All Tasks have been reblocked after running.
   */
  public void runCompleted ()
  {
    this.state = STOPPED;
  }
  
  /**
   * Decrement the number of running Tasks.
   */
  public void decNumRunningTasks ()
  {
    this.numRunningTasks--;
  }

  /*****************************************************************************
   * GETTERS AND SETTERS
   ****************************************************************************/

  /**
   * Returns the number of stepping Tasks.
   * 
   * @return taskStepCount  The number of stepping Tasks
   */
  public int getTaskStepCount ()
  {
    return this.taskStepCount;
  }

  /**
   * Sets the number of stepping Tasks.
   * 
   * @param count   The number of stepping Tasks
   */
  public void setTaskStepCount (int count)
  {
    this.taskStepCount = count;
  }

  /**
   * Get the number of running Tasks.
   * 
   * @return numRunningTasks The number of running Tasks
   */
  public int getNumRunningTasks ()
  {
    return this.numRunningTasks;
  }
  
  /**
   * Set the number of running Tasks.
   * 
   * @param num The number of running Tasks
   */
  public void setNumRunningTasks (int num)
  {
    this.numRunningTasks = num;
  }
  
  /**
   * Get the number of Observers watching this Observable.
   * 
   * @return this.countObservers() The number of Observers in this Observable's
   * Observer list
   */
  public int getNumObservers ()
  {
    return this.countObservers();
  }
  
  /**
   * Get the current state of this RunState.
   * 
   * @return state The current state of this RunState
   */
  public int getState ()
  {
    return this.state;
  }
  
  /**
   * Set the current state of this RunState.
   * 
   * @param s The new state for this RunState
   */
  public void setState (int s)
  {
    this.state = s;
  }
  
  /**
   * Return the Proc this RunState is controlling.
   * 
   * @return stateProc The Proc this RunState is controlling
   */
  public Proc getProc ()
  {
    return this.stateProc;
  }
  
  /**
   * Set the process for this RunState. Blocks the Process.
   * 
   * @param proc The Proc for this RunState to control
   */
  public void setProc (Proc proc)
  {
    this.stateProc = proc;
    this.tasks = proc.getTasks();
    requestAdd(this.tasks);
  }
  
  /**
   * Get the number of Tasks this RunState is concerned with.
   * 
   * @return tasks.size() The number of Tasks this RunState is concerned with
   */
  public int getNumTasks ()
  {
    return tasks.size();
  }
  
  /*****************************************************************************
   * OBSERVER LIST MANIPULATION METHODS
   ****************************************************************************/
  
  /**
   * Remove the incoming Observer object from this Observable's list of 
   * Observers to notify. If, after removing it, the list is empty, unblock
   * the process and return 1. Otherwise return 0.
   * 
   * @return 0 This Observable's Observer list is not empty
   * @return 1 This Observable's Observer list is empty
   */
  public int removeObserver (Observer o)
  {
    this.deleteObserver(o);
    if (countObservers() == 0)
      {
        run(this.stateProc.getTasks());
        return 1;
      }
    else
      return 0;
  }

  /**
   * Notify certain classes observing this object's Proc that it will 
   * immanently no longer be blocked.
   */
  public void notifyNotBlocked ()
  {
    //System.out.println("notifyNotBlocked");
    this.setChanged();
    this.notifyObservers(null);
  }
  
  /**
   * Nofity certain classes observing this object's Proc that it has finished
   * becoming re-blocked.
   */
  public void notifyStopped ()
  {
    //System.out.println("notifyStopped");
    this.setChanged();
    this.notifyObservers(null);
  }
  
  
  /*****************************************************************************
   * TASKOBSERVER.INSTRUCTION CALLBACKS
   ****************************************************************************/
  
  /**
   * Callback for TaskObserver.Instruction. Each time a Task is blocked, either
   * after this Observer was added to it for the first time, or it was
   * unblocked to allow execution of a single instruction, it passes through
   * here. 
   * Depending on what the state of this object is at the time and how
   * many Tasks are left to be blocked, we'll either do nothing and return 
   * a blocking Action, or notify our Observers that this task came through,
   * and they can perform the necessary operations.
   * 
   * @param task The Task which was just blocked
   * @return Action.BLOCK Continue blocking this incoming Task
   */
  public Action updateExecuted (Task task)
  {
   //System.out.println("UpdateExecuted " + task + " " + taskStepCount);
    if (state >= INSTRUCTION_STEP && state <= STEP_OVER_LINE_STEP)
      {
        switch (RunState.this.state)
          {
          case INSTRUCTION_STEP:
            this.taskStepCount--;
            break;
          case STEP_IN:
            stepIn(task);
            break;
          case STEP_OVER:
            long pc = task.getIsa().pc(task);
            System.out.println("RunState.update -> STEP OVER " + task + " " + pc);
            task.requestDeleteCodeObserver(breakpoint, addy);
            taskStepCount = 0;
            break;
          case STEP_OUT:
            stepOut(task);
            break;
          case STEP_OVER_LINE_STEP:
            System.out.println("RunState.update -> STEPOVERLINESTEP " + task);
            stepIn(task);
            break;
          }

        /* No more Tasks have to be blocked */
        if (taskStepCount == 0)
          {
            if (RunState.this.state == STEP_OVER_LINE_STEP)
              {
                stepOver(task);
                return Action.BLOCK;
              }
//            SymTab.flushExperSymTabStackFrame();
           //System.out.println("UpdateExecuted - No more tasks");
            RunState.this.setChanged();
            RunState.this.notifyObservers(task);
          }
      }
    else
      {
        this.numRunningTasks--;
        
        /* No more Tasks have to be blocked, or this RunState is already blocked
         * and this is the first time this method has been called. */
        if (numRunningTasks == 0)
          {
//            SymTab.flushExperSymTabStackFrame();
            RunState.this.setChanged();
            RunState.this.notifyObservers(task);
          }
      }

    return Action.BLOCK;
  }

  /**
   * This Observer has been added to the Object.
   */
  public void addedTo (Object o)
  {

  }

  /**
   * This Observer has been deleted from the Object.
   */
  public void deletedFrom (Object o)
  {

  }

  /**
   * The add to the Object failed
   */
  public void addFailed (Object o, Throwable w)
  {
    w.printStackTrace();
    stateProc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
    System.exit(1);
  }

  /**
   * Request the adding of this observer to all of this Object's Tasks.
   */
  public void requestAdd ()
  {
    requestAdd(this.stateProc.getTasks());
  }

  /**
   * Add this Instruction Observer to each of the incoming Tasks.
   * 
   * @param tasks The tasks to be added to
   */
  public void requestAdd (LinkedList tasks)
  { 
    this.tasks = tasks;
    this.numRunningTasks = tasks.size();
    
    /*
     * The rest of the construction must be done synchronous to the EventLoop,
     * schedule it. */
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {

        if (RunState.this.tasks == null)
          {
            System.out.println("Couldn't get the tasks");
            System.exit(1);
          }

        /* XXX: deprecated hack. */
        // proc.sendRefresh();
        if (stateProc.getMainTask() == null)
          {
            // logger.log(Level.FINE, "Could not get main thread of "
            // + "this process\n {0}", proc);
            addFailed(
                      stateProc,
                      new RuntimeException(
                                           "Process lost: could not "
                                               + "get the main thread of this process.\n"
                                               + stateProc));
            return;
          }

        if (!(stateProc.getUID() == Manager.host.getSelf().getUID()
            || stateProc.getGID() == Manager.host.getSelf().getGID()))
          {
            System.err.println("Process " + stateProc + " is not owned by user/group.");
            System.exit(1);
          }

        Iterator i = RunState.this.tasks.iterator();
        while (i.hasNext())
          requestAddObservers((Task) i.next());

      }
    });
  }

  /**
   * Adds the necessary Observers to the incoming Task.
   * 
   * @param task The Task to have Observers added to
   */
  public void requestAddObservers (Task task)
  {
    //System.out.println("Adding instruction observer to " + task);
    task.requestAddInstructionObserver(this);
  }

  /**
   * Re-block only a certain set of Tasks.
   * 
   * @param tasks The Tasks to be reblocked
   */
  public void blockTask (LinkedList tasks)
  {
    this.tasks = tasks;
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {
        Iterator i = RunState.this.tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            t.requestAddInstructionObserver(RunState.this);
            requestAddObservers(t);
          }
      }
    });
  }
  
  /*****************************************************************************
   * TASKOBSERVER.CODE BREAKPOINT CLASS
   ****************************************************************************/
  
  private Object monitor;
  private long addy;
  
  protected class Breakpoint implements TaskObserver.Code
  {
    protected long address;

    protected int triggered;

    protected boolean added;

    protected boolean removed;

    Breakpoint (long address)
    {
      System.out.println("Setting address to 0x" + Long.toHexString(address));
      this.address = address;
      if (monitor == null)
        monitor = new Object();
    }

    protected void logHit(Task task, long address, String message)
    {
      if (logger.isLoggable(Level.FINEST))
	{
	  Object[] logArgs = {task,
			      Long.toHexString(address),
			      Long.toHexString(task.getIsa().pc(task)),
			      Long.toHexString(this.address)};
	  logger.logp(Level.FINEST, "RunState.Breakpoint", "updateHit",
		      message, logArgs);
	}
    }
    
    public Action updateHit (Task task, long address)
    {
      logHit(task, address, "task {0} at 0x{1}\n");
      if (address != this.address)
        {
	  logger.logp(Level.WARNING, "RunState.Breakpoint", "updateHit",
		      "Hit wrong address!");
          return Action.CONTINUE;
        }
      else
        {
          addy = address;
	  logHit(task, address, "adding instructionobserver {0} 0x{2}");
          task.requestAddInstructionObserver(RunState.this);
        }
      triggered++;
      return Action.BLOCK;
    }

    int getTriggered ()
    {
      return triggered;
    }

    public void addFailed (Object observable, Throwable w)
    {
      w.printStackTrace();
    }

    public void addedTo (Object observable)
    {
      synchronized (monitor)
        {
          added = true;
          removed = false;
          monitor.notifyAll();
        }
      ((Task) observable).requestDeleteInstructionObserver(RunState.this);
    }

    public boolean isAdded ()
    {
      return added;
    }

    public void deletedFrom (Object observable)
    {
      synchronized (monitor)
        {
          removed = true;
          added = false;
          monitor.notifyAll();
        }
    }

    public boolean isRemoved ()
    {
      return removed;
    }

    public long getAddress()
    {
      return address;
    }
    
  }

  public class SourceBreakpoint extends Breakpoint
  {
    SourceBreakpoint(long address, LineBreakpoint lineBreakpoint) 
    {
      super(address);
      this.lineBreakpoint = lineBreakpoint;
    }
    
    private LineBreakpoint lineBreakpoint;

    public LineBreakpoint getLineBreakpoint()
    {
      return lineBreakpoint;
    }
    
    public Action updateHit(Task task, long address)
    {
      logger.entering("RunState.SourceBreakpoint", "updateHit");
      state = STOPPED;
      if (runningTasks.contains(task))
	{
	  runningTasks.remove(task);
	  numRunningTasks--;
	}
      else
	logger.logp(Level.WARNING, "RunState.SourceBreakpoint", "updateHit",
		    "task {0} not in runningTasks", task);
      breakpointMap.put(task, this);
      setChanged();
      notifyObservers(task);
      return super.updateHit(task, address);
    }

    public void addedTo (Object observable)
    {
      synchronized (monitor)
        {
          added = true;
          removed = false;
          monitor.notifyAll();
        }
    }
  }

  public SourceBreakpoint getTaskSourceBreakpoint(Task task)
  {
    return (SourceBreakpoint)breakpointMap.get(task);
  }
  
  public class LineBreakpoint
  {
    private String fileName;
    private int lineNumber;
    private int column;
    private LinkedList dwflAddrs;
    private LinkedList breakpoints;
    
    LineBreakpoint () 
    {
      breakpoints = new LinkedList();
    }

    LineBreakpoint(Task task, String fileName, int lineNumber, int column) 
    {
      this();
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.column = column;
      dwflAddrs = getDwfl(task).getLineAddresses(fileName, lineNumber, column);
    }

    public String getFileName() 
    {
      return fileName;
    }
    
    public int getLineNumber() 
    {
      return lineNumber;
    }
    
    public int getColumn() 
    {
      return column;
    }
    
    public String toString() 
    {
      return "breakpoint file " + getFileName() + " line " + getLineNumber() 
	+ " column " + getColumn();
    }
    
    public void addBreakpoint(Task task)
    {
      DwflLine line = (DwflLine)dwflAddrs.getFirst();
      long address = line.getAddress();
	  
      SourceBreakpoint breakpoint = new SourceBreakpoint(address, this);
      breakpoints.add(breakpoint);
      task.requestAddCodeObserver(breakpoint, address);
    }

    public void deleteBreakpoint(Task task)
    {
      SourceBreakpoint sb = (SourceBreakpoint)breakpoints.getFirst();
      task.requestDeleteCodeObserver(sb, sb.getAddress());
      breakpoints.removeFirst();
    }
  }

  public LineBreakpoint addBreakpoint(Task task, String fileName, int lineNo)
  {
    LineBreakpoint bp = new LineBreakpoint(task, fileName, lineNo, 0);
    bp.addBreakpoint(task);
    return bp;
  }

  public boolean deleteBreakpoint(Task task, LineBreakpoint lbp)
  {
    lbp.deleteBreakpoint(task);
    return true;
  }
  
}
