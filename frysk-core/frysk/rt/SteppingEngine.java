// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.dw.DwflLine;

import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rt.states.*;

public class SteppingEngine
{
  protected static Logger logger = Logger.getLogger ("frysk");

  /* Set of Tasks currently running, or unblocked. */
  private static HashSet runningTasks;

  /* Tasks that have hit a breakpoint */
  private static HashMap breakpointMap;
  
  private static HashMap contextMap;
  
  private static Map taskStateMap;
  
  private static  LinkedList[] tasks;
  
  private static Proc stateProc;
  
  private static int taskStepCount = 0;
  
  private static int current = 0;
  
  private static SteppingObserver steppingObserver;
  
  private static ThreadLifeObservable threadLifeObservable;
  
  static
  {
    runningTasks = new HashSet();
    breakpointMap = new HashMap();
    contextMap = new HashMap();
    taskStateMap = Collections.synchronizedMap(new HashMap());
    steppingObserver = new SteppingObserver();
  }
  
  public static void setProc (Proc proc)
  {
    stateProc = proc;
    tasks = new LinkedList[1];
    tasks[0] = proc.getTasks();
//    numRunningTasks += tasks[0].size();
    contextMap.put(((Task) tasks[0].getFirst()).getProc(), new Integer(tasks[0].size()));
    current = 0;
    
    threadLifeObservable = new ThreadLifeObservable();
    
    Iterator iter = tasks[0].iterator();
    while(iter.hasNext())
      {
	Task t = (Task) iter.next();
	t.requestAddTerminatingObserver(threadLifeObservable);
	t.requestAddClonedObserver(threadLifeObservable);
    	taskStateMap.put(t, new TaskStepEngine(t));
      }
    
    requestAdd(tasks[0]);
  }
  
  public static void setProcs (Proc[]  procs)
  {
    stateProc = procs[0];
    tasks = new LinkedList[procs.length];
    current = procs.length - 1;
    
    threadLifeObservable = new ThreadLifeObservable();
    
    for (int i = procs.length - 1; i >= 0; i--)
      {
        tasks[i] = procs[i].getTasks();
        contextMap.put(((Task) tasks[i].getFirst()).getProc(), new Integer(tasks[i].size()));
        
        Iterator iter = tasks[i].iterator();
        while(iter.hasNext())
          {
            	Task t = (Task) iter.next();
        	t.requestAddTerminatingObserver(threadLifeObservable);
        	t.requestAddClonedObserver(threadLifeObservable);
        	taskStateMap.put(t, new TaskStepEngine(t));
          }
        
        requestAdd(tasks[i]);
      }
  }
  
  public static boolean addProc (Proc proc)
  {
    LinkedList[] list = new LinkedList[tasks.length + 1];
    System.arraycopy(tasks, 0, list, 0, tasks.length);

    current = list.length - 1;
    list[current] = proc.getTasks();
    contextMap.put(((Task) list[current].getFirst()).getProc(),
			new Integer(list[current].size()));
    tasks = list;
    
    Iterator iter = tasks[current].iterator();
    while(iter.hasNext())
      {
        Task t = (Task) iter.next();
    	t.requestAddTerminatingObserver(threadLifeObservable);
    	t.requestAddClonedObserver(threadLifeObservable);
    	taskStateMap.put(t, new TaskStepEngine(t));
      }

    requestAdd(tasks[current]);
    return true;
  }
  
  public static void notifyStopped ()
  {
    steppingObserver.notifyStopped();
  }
  
  /**
   * Remove the incoming Observer object from this Observable's list of 
   * Observers to notify. If, after removing it, the list is empty, unblock
   * the process and return 1. Otherwise return 0.
   * @param p The proc to delete the observer from
   * 
   * @return 0 This Observable's Observer list is not empty
   * @return 1 This Observable's Observer list is empty
   */
  public static int removeObserver (Observer o, Proc p)
  {
    steppingObserver.deleteObserver(o);
    if (p.observationsSize() == p.getTasks().size())
      {
        continueExecution(p.getTasks());
        return 1;
      }
    else
      return 0;
  }
  
  public static void addObserver (Observer o)
  {
    steppingObserver.addObserver(o);
  }
  
  public static void setThreadObserver (Observer o)
  {
    threadLifeObservable.addObserver(o);
  }
  
  /*****************************************************************************
   * STEP HANDLING METHODS
   ****************************************************************************/
  
  public static boolean stepInstruction (Task task)
  {
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    if (!tse.isStopped())
      return false;
    
    tse.setState(new InstructionStepState(tse, task));
    
    ++taskStepCount;
   steppingObserver.notifyNotBlocked();
    task.requestUnblock(steppingObserver);
    return true;
  }
  
  public static boolean stepInstruction (LinkedList tasks)
  {
	if (isProcRunning(tasks))
	  return false;
    
    taskStepCount = tasks.size();
   steppingObserver.notifyNotBlocked();
    Iterator iter = tasks.iterator();
    while (iter.hasNext())
      {
        Task t = (Task) iter.next();
        TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(t);
        tse.setState(new InstructionStepState(tse, t));
        t.requestUnblock(steppingObserver);
      }
    
    return true;
  }
  
  /**
   * Set up line stepping by setting the appropriate state and then 
   * setting up the data structures for the step.
   * 
   * @param task   The Task to be stepped.
   */
  public static boolean setUpLineStep (Task task)
  {
    if (isTaskRunning(task))
      return false;
	
    ++taskStepCount;
    contextMap.put (task.getProc(), new Integer(1));
    
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);

    if (tse.getLine() == 0)
      {
	DwflLine line = tse.getDwflLine();
	
        if (line == null)
          {
            tse.setState(new InstructionStepState(tse, task));
            task.requestUnblock(steppingObserver);
            return true;
          }
	else
	  {
	    tse.setLine(line.getLineNum());
	  }
      }

    tse.setState(new LineStepState(tse, task));
    task.requestUnblock(steppingObserver);
    return true;
  }
  
  public static boolean setUpLineStep (LinkedList tasks)
  {
	if (isProcRunning(tasks))
	  return false;
	
	setUp(tasks, true);
	return false;
  }
  
  /**
   * Sets up stepping information - which tasks are stepping, how many there, 
   * and then initialize the dwflMap and lineMap with their information. Then
   * unblock each Task to begin the stepping.
   * 
   * @param tasks   The list of Tasks to step
   */
  public static void setUp (LinkedList tasks, boolean isLine)
  {
    if (tasks.size() == 0)
      return;
    
    Iterator i = tasks.iterator();
    int zeroCount = 0;
    TaskStepEngine tse;
    Task t = null;
    
    steppingObserver.notifyNotBlocked();
    while (i.hasNext())
      {
        t = (Task) i.next();
        tse = (TaskStepEngine) taskStateMap.get(t);
       //System.out.println("SetupStep.iterate " + t);
//        if (this.lineMap.get(t) == null)
        if (tse.getLine() == 0)
          {
            DwflLine line = tse.getDwflLine();
            
            if (line == null)
              {
               //System.out.println("Coulnd't get DwflLine, assigning 0");
                ++zeroCount;
                tse.setState(new InstructionStepState(tse, t));
                continue;
              }
            else
              tse.setLine(line.getLineNum());
          }
        
        tse.setState(new LineStepState(tse, t));
      }
    
    contextMap.put(t.getProc(), new Integer(tasks.size()));
    
//    /* None of these tasks have any debug information, so a 
//     * "line-step" is meaningless - perform an instruction step instead. */
//    if (zeroCount == tasks.size())
//      {
//      //  System.out.println("setUp --> No frames with debuginfo!");
//        this.dwflMap.clear();
//        this.lineMap.clear();
//        state = STEP_INSTRUCTION;
//      }
    
    i = tasks.iterator();
    while (i.hasNext())
      {
    	t = (Task) i.next();
    	t.requestUnblock(steppingObserver);
      }
  }
  
  public static void setUpStepAdvance (Task task, StackFrame frame)
  {
    
  }
  
  public static void setUpStepNextInstruction (Task task, StackFrame lastFrame)
  {
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    tse.setState(new NextInstructionStepTestState(task));
    frameIdentifier = lastFrame.getFrameIdentifier();

    ++taskStepCount;
    steppingObserver.notifyNotBlocked();
    task.requestUnblock(steppingObserver);
  }
  
  public static void setUpStepNextInstruction (LinkedList tasks, StackFrame lastFrame)
  {
//    Iterator i = tasks.iterator();
//   steppingObserver.notifyNotBlocked();
//    while (i.hasNext())
//      {
//        Task t = (Task) i.next();
//        t.requestUnblock(this);
//      }
  }
  
  public static void stepNextInstruction (Task task)
  {
    StackFrame newFrame = null;
    newFrame = StackFactory.createStackFrame(task, 2);
   
    /* The two frames are the same; treat this step-over as an instruction step. */
    if (newFrame.getFrameIdentifier().equals(frameIdentifier))
      {
        steppingObserver.notifyTask(task);
        return;
      }
    else
      {
        
        /* There is a different innermost frame on the stack - run until
         * it exits - success!. */
        StackFrame frame = newFrame.getOuter();
        TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
        tse.setState(new NextInstructionStepTestState(task));
        breakpoint = new SteppingBreakpoint(frame.getAddress());
        task.requestAddCodeObserver(breakpoint, frame.getAddress());
      }
  }

  static SteppingBreakpoint breakpoint;
  static FrameIdentifier frameIdentifier;
//  FrameIdentifier outerFrameIdentifier;
  
  /**
   * Sets up for step-over.
   * 
   * XXX: Not finished yet. Needs to work with multiple threads.
   * 
   * @param tasks   The list of Tasks to be stepped-over
   * @param lastFrame
   */
  public static void setUpStepOver (Task task, StackFrame lastFrame)
  {
    frameIdentifier = lastFrame.getFrameIdentifier();
    ++taskStepCount;
    
    steppingObserver.notifyNotBlocked();
//    stateMap.put(task, new StepOverTestState(task));
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    tse.setState(new StepOverTestState(task));
    task.requestUnblock(steppingObserver);
    //XXX: Fixme
  }
  
  public static void setUpStepOver (LinkedList tasks, StackFrame lastFrame)
  {
//    this.frameIdentifier = lastFrame.getFrameIdentifier();
////    this.outerFrameIdentifier = lastFrame.getOuter().getFrameIdentifier();
//    taskStepCount =  tasks.size();
//
//    setUp(tasks, STEP_OVER_TEST);
  }
//  
  /**
   * Checks to see if a step actually results in a new frame, and then sets
   * the breakpoint and lets the thread run.
   * 
   * XXX: Needs to work with multiple threads
   * 
   * @param task    The Task to execute step-over for.
   */
  public static void stepOver (Task task)
  {
    StackFrame newFrame = null;
    newFrame = StackFactory.createStackFrame(task, 2);
   
    /* The two frames are the same; treat this step-over as a line step. */
    if (newFrame.getFrameIdentifier().equals(frameIdentifier))
      {
	steppingObserver.notifyTask(task);
        return;
      }
    else
      {
        
        /* There is a different innermost frame on the stack - run until
         * it exits - success!. */
        StackFrame frame = newFrame.getOuter();
        TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
        tse.setState(new StepOverState(task));
        breakpoint = new SteppingBreakpoint(frame.getAddress());
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
  
  public static void setUpStepOut (Task task, StackFrame lastFrame)
  {
    long address = lastFrame.getOuter().getAddress();
    ++taskStepCount;
    steppingObserver.notifyNotBlocked();
    
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    tse.setState(new StepOutState(task));
    breakpoint = new SteppingBreakpoint(address);
    task.requestAddCodeObserver(breakpoint, address);
  }
  
  public static void setUpStepOut (LinkedList tasks, StackFrame lastFrame)
  {
//    taskStepCount =  tasks.size();
//    this.frameIdentifier = lastFrame.getFrameIdentifier();
////    this.outerFrameIdentifier = lastFrame.getOuter().getFrameIdentifier();
//    
//    Iterator i = tasks.iterator();
//    while (i.hasNext())
//      {
//        Task t = (Task) i.next();
//        stateMap.put(t, new Integer(STEP_OUT_ASM_STEP));
//        t.requestUnblock(steppingObserver);
//      }
  }
  
  /**
   * Cleans up after a step-out operation, deletes the breakpoint.
   * 
   * @param task The task finished stepping out.
   */
  public static void stepOut (Task task)
  {
    StackFrame newFrame = null;
    newFrame = StackFactory.createStackFrame(task, 3);
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    tse.setState(new StepOutState(task));

    FrameIdentifier fi = newFrame.getFrameIdentifier();
    
    if (fi.equals(frameIdentifier))
      {
        breakpoint = new SteppingBreakpoint(newFrame.getOuter().getAddress());
        task.requestAddCodeObserver(breakpoint,
                                    newFrame.getOuter().getAddress());
      }
    else
      {
        if (fi.outerTo(frameIdentifier))
          {
//            this.breakpoint = new SteppingBreakpoint(newFrame.getOuter().getAddress());
//            task.requestAddCodeObserver(
//                                        this.breakpoint,
//                                        newFrame.getOuter().getOuter().getAddress());
            steppingObserver.notifyTask(task);
            return;
          }
        else if (fi.innerTo(frameIdentifier))
          {
            breakpoint = new SteppingBreakpoint(
                                             newFrame.getOuter().getOuter().getAddress());
            task.requestAddCodeObserver(breakpoint,
                                        newFrame.getOuter().getOuter().getAddress());
          }
      }
  }
  
  public static void cleanUpBreakPoint (Task task)
  {
    breakpoint = null;
    addy = 0;
  }

  
  /*****************************************************************************
   * STOP/CONTINUE HANDLING METHODS
   ****************************************************************************/

  
  /**
   * When implemented, re-runs the process from the beginning of the
   * debugging session if proper breakpoints are in place; otherwise just 
   * re-executes the process from its start. 
   */
  public void run (LinkedList tasks)
  {
    // for now point to continueExecution.
    
    continueExecution(tasks);
  }
  
  /**
   * Deletes the blocking observer from each of the incoming tasks,
   * effectively 'running', or continuing, the process.
   * 
   * @param tasks   The list of Tasks to be run
   */
  public static void continueExecution (LinkedList list)
  {
    TaskStepEngine tse = null;
    current = 0;
    tasks[0] = list;
    contextMap.put(((Task) list.getFirst()).getProc(), new Integer(list.size()));
    
    steppingObserver.notifyNotBlocked();
    Iterator i = list.iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (! runningTasks.contains(t))
          {
            runningTasks.add(t);
            tse = (TaskStepEngine) taskStateMap.get(t);
            if (tse != null)
              tse.setState(new RunningState(t));
            t.requestDeleteInstructionObserver(steppingObserver);
            SteppingBreakpoint bpt = (SteppingBreakpoint) breakpointMap.get(t);
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
   * @param keepRunning    The list of Tasks to not block
   */
  public static void stop (LinkedList keepRunning, LinkedList stopTasks)
  {
    if (keepRunning == null || keepRunning.size() == 0)
      {
	current = 0;
	tasks[0] = stopTasks;
	TaskStepEngine tse;
	Iterator i = stopTasks.iterator();
	while (i.hasNext())
	  {
	    Task t = (Task) i.next();
	    tse = (TaskStepEngine) taskStateMap.get(t);
	    tse.setState(new StoppedState(t));
	  }
	requestAdd(stopTasks);
      }
    else
      {
	synchronized (tasks)
	  {
	    Iterator i = runningTasks.iterator();
	    LinkedList blockTasks = new LinkedList();
	    while (i.hasNext())
	      {
		Task t = (Task) i.next();
		if (! keepRunning.contains(t))
		  {
		    blockTasks.add(t);
		    i.remove();
		  }
	      }
	    blockTask(blockTasks);
	  }
      }
    runningTasks.clear();
  }
  
  /**
   * Re-block only a certain set of Tasks.
   * 
   * @param tasks The Tasks to be reblocked
   */
  public static void blockTask (LinkedList list)
  {
    tasks[++current] = list;
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {
        Iterator i = tasks[current].iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            t.requestAddInstructionObserver(steppingObserver);
          }
      }
    });
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
  public static synchronized void executeTasks (LinkedList tasks)
  {

     //System.out.println("In executeThreads with thread size " + tasks.size()
     //+ " and runningtasks size "
     //+ runningTasks.size());

    /* No incoming Tasks and no Tasks already running */
    if (tasks.size() == 0 && runningTasks.size() == 0)
      {
//        this.state = STOPPED;
        return;
      }

    /* No incoming Tasks, but some Tasks are running. Block them. */
    else if (tasks.size() == 0 && runningTasks.size() != 0)
      {
        LinkedList l = new LinkedList();
        Iterator i = runningTasks.iterator();
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

    TaskStepEngine tse = null;
    /* There are incoming Tasks to be run, and no Tasks already running */
    if (runningTasks.size() == 0)
      {
        steppingObserver.notifyNotBlocked();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("(0) Running " + t);
            runningTasks.add(t);
            tse = (TaskStepEngine) taskStateMap.get(t);
            tse.setState(new RunningState(t));
            t.requestDeleteInstructionObserver(steppingObserver);
          }
        return;
      }
    else
      /* There are incoming Tasks to be run, and some Tasks are already running.
       * If they are not already running, unblock the incoming Tasks, and block
       * any running Task not in the incoming list. */
      {
        HashSet temp = new HashSet();
        // this.runningThreads.clear();
        int numRunning = 0;
        steppingObserver.notifyNotBlocked();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("Iterating running thread" + t);
            /* If this thread has not already been unblocked, do it */
            if (! runningTasks.remove(t))
              {
                // System.out.println("unBlocking " + t);
//            	++this.numRunningTasks;
            	++numRunning;
                tse = (TaskStepEngine) taskStateMap.get(t);
                tse.setState(new RunningState(t));
                t.requestDeleteInstructionObserver(steppingObserver);
              }
            else
              {
              // System.out.println("Already Running");
              /* Put all threads back into a master list */
            	temp.add(t);
              }
          }
        
//        contextMap.put(((Task) temp.))

        /* Now catch the threads which have a block request */
        if (runningTasks.size() != 0)
          {
            // System.out.println("temp size not zero");
            LinkedList l = new LinkedList();
            i = runningTasks.iterator();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
//                --this.numRunningTasks;
                --numRunning;
                l.add(t);
                // System.out.println("Blocking from runningTasks " + t);
              }
            blockTask(l);
          }

        contextMap.put(((Task) tasks.getFirst()).getProc(), new Integer(numRunning));
        runningTasks = temp;
        // System.out.println("rt temp" + this.runningThreads.size() + " "
        // + temp.size());
      }
    return;
  }
  
  public static void setRunning (LinkedList tasks)
  {
    TaskStepEngine tse = null;
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
	Task t = (Task) i.next();
	tse = (TaskStepEngine) taskStateMap.get(t);
	tse.setState(new RunningState(t));
      }
  }
  
  public static boolean isProcRunning (LinkedList tasks)
  {
    TaskStepEngine tse = null;
    Iterator iter = tasks.iterator();
    while (iter.hasNext())
      {
	Task t = (Task) iter.next();
	tse = (TaskStepEngine) taskStateMap.get(t);
	if (! tse.isStopped())
	  return true;
      }

    return false;
  }
  
  public static boolean isTaskRunning (Task task)
  {
    return !((TaskStepEngine) taskStateMap.get(task)).isStopped();
  }
  
  public static void cleanTask (Task task)
  {
    taskStateMap.remove(task);
    contextMap.remove(task.getProc());
    runningTasks.remove(task);
  }
  
  public static void clear ()
  {
    taskStateMap.clear();
    breakpointMap.clear();
    contextMap.clear();
    runningTasks.clear();
    tasks = null;
    threadLifeObservable = null;
    breakpoint = null;
    steppingObserver.deleteObservers();
    steppingObserver = new SteppingObserver();
  }

  /*****************************************************************************
   * GETTERS AND SETTERS
   ****************************************************************************/
  
  public static void setTaskState (Task task, State state)
  {
    ((TaskStepEngine) taskStateMap.get(task)).setState(state);
  }
  
  public static SteppingObserver getSteppingObserver ()
  {
    return steppingObserver;
  }
  
  public static void setBreakpoint (Task task, long address)
  {
    breakpoint = new SteppingBreakpoint (address);
    task.requestAddCodeObserver(breakpoint, address);
  }
  
  public static void removeBreakpoint (Task task)
  {
    task.requestDeleteCodeObserver(breakpoint, addy);
  }
  
  public static Breakpoint getTaskBreakpoint (Task task)
  {
    return (SteppingBreakpoint) breakpointMap.get(task);
  }
  
  public static void addBreakpoint (Task task, Breakpoint bp)
  {
    task.requestAddCodeObserver(bp, bp.getAddress());
  }
  
  public static void deleteBreakpoint (Task task, Breakpoint bp)
  {
    task.requestDeleteCodeObserver(bp, bp.getAddress());
  }
  
  public static State getTaskState (Task task)
  {
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    if (tse != null)
      return tse.getState();
    else
      return null;
  }
  
  public static void requestAdd (LinkedList t)
  { 
//    tasks[this.current] = tasks;
    
    /*
     * The rest of the construction must be done synchronous to the EventLoop,
     * schedule it. */
    Manager.eventLoop.add(new Event()
    {
      public synchronized void execute ()
      {

        if (tasks[current] == null)
          {
            System.out.println("Couldn't get the tasks");
            System.exit(1);
          }
        
        stateProc = (Proc) ((Task) (tasks[current].getFirst())).getProc();

        /* XXX: deprecated hack. */
        // proc.sendRefresh();
        if (stateProc.getMainTask() == null)
          {
            // logger.log(Level.FINE, "Could not get main thread of "
            // + "this process\n {0}", proc);
            steppingObserver.addFailed(
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
          	//System.exit(1);
          	return;
          }

        Iterator i = stateProc.getTasks().iterator();
        while (i.hasNext())
          {
        	Task t = (Task) i.next();
        	t.requestAddInstructionObserver(steppingObserver);
          }
        
        if (current > 0)
          --current;
      }
    });
  }
  
  	/***********************************************************************
         * TASKOBSERVER.INSTRUCTION OBSERVER CLASS
         **********************************************************************/
  
  protected static class SteppingObserver
  extends Observable
  implements TaskObserver.Instruction
  {
    
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
    public synchronized Action updateExecuted (Task task)
    {
      if (((TaskStepEngine) taskStateMap.get(task)).handleUpdate())
	{
	  Proc proc = task.getProc();
	  int i = ((Integer) contextMap.get(proc)).intValue();
	  
	  if (--i <= 0)
	    {
	      this.setChanged();
	      this.notifyObservers(task);
	    }
	  else
	    contextMap.put(proc, new Integer(i));
	}
      
      return Action.BLOCK;
    }
    
    public void addedTo (Object o)
    {
    }
    
    public void deletedFrom (Object o)
    {

    }
    
    /**
     * The add to the Object failed
     */
    public void addFailed (Object o, Throwable w)
    {
      w.printStackTrace();
      ((Task) o).getProc().requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      //System.exit(1);
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
    
    public void notifyTask (Task task)
    {
      this.setChanged();
      this.notifyObservers(task);
    }
  }
  
  /*****************************************************************************
   * TASKOBSERVER.CLONED/TERMINATED OBSERVER CLASS
   ****************************************************************************/
  
  protected static class ThreadLifeObservable 
  extends Observable
  implements TaskObserver.Cloned, TaskObserver.Terminating
  {
	
    public Action updateClonedParent (Task parent, Task offspring)
    {
      return Action.CONTINUE;
    }

    public Action updateClonedOffspring (Task parent, Task offspring)
    {
      taskStateMap.put(offspring, new TaskStepEngine(offspring));
      offspring.requestAddInstructionObserver(steppingObserver);
      offspring.requestAddClonedObserver(this);
      offspring.requestAddTerminatingObserver(this);
      return Action.CONTINUE;
    }
    
    public Action updateTerminating (Task task, boolean signal, int value)
    {
      System.err.println("threadlife: Terminating: " + task + " " + value);
      int pid = task.getProc().getPid();

      runningTasks.remove(task);
      
      Integer context = (Integer) contextMap.get(task.getProc());
      contextMap.put (task.getProc(), new Integer(context.intValue() - 1));
      
      taskStateMap.remove(task);
      
      int i;
      for (i = 0; i < tasks.length; i++)
	{
	  if (((Task) (tasks[i].getFirst())).getProc().getPid() == pid)
	    {
//	      System.err.println("Removing task " + task);
	      tasks[i].remove(task);
	      
	      if (tasks[i].size() > 0)
		{
//		  System.err.println("Decrementing numrunningtasks");
		      this.setChanged();
		      this.notifyObservers(task);
		}
	      else
		{
		  tasks[i] = null;
		  if (tasks.length > 1)
		    {
//		      System.err.println("stateProcs.length > 1");
		      current = 0;
		      this.setChanged();
		      this.notifyObservers(task);
//		      int j = 0;
//		      while (j < tasks.length)
//			{
//			  if (j != i)
//			    {
//			      current = j;
//			      // Task t =
//                                // stateProcs[j].getMainTask();
//			      Task t = (Task) (tasks[j].getFirst());
//			      current = j;
//			      System.err.println("setting task to " + t);
//			      contextMap.remove(task.getProc());
//			      this.setChanged();
//			      this.notifyObservers(task);
////			      setChanged();
////			      notifyObservers(t);
//			      break;
//			    }
//			  ++j;
//			}
		    }
		  else
		    {
		      this.setChanged();
		      this.notifyObservers(null);
		      System.err.println("Processes exited " + this.countObservers());
		      return Action.CONTINUE;
		    }
		}
	      break;
	    }
	}
      
	  LinkedList[] newTasks = new LinkedList[tasks.length - 1];
	  int j = 0;
	  for (int k = 0; k < tasks.length; k++)
	    {
	      if (k != i)
		{
		  newTasks[j] = tasks[k];
		  ++j;
		}
	    }
	  
	  tasks = newTasks;
	                                         

//      return Action.CONTINUE;
      return Action.BLOCK;
    }

    public void addedTo (Object observable)
    {
//      System.err.println("threadlife addedTo: " + (Task) observable);
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }

    public void deletedFrom (Object observable)
    {
    }
  }
  
  /*****************************************************************************
   * TASKOBSERVER.CODE BREAKPOINT CLASS
   ****************************************************************************/
  
  static long addy;
  
  protected static class SteppingBreakpoint
  extends Breakpoint
  implements TaskObserver.Code
  {
    protected long address;

    protected int triggered;

    protected boolean added;

    protected boolean removed;
    
    public SteppingBreakpoint (long address)
    {
//      System.out.println("Setting address to 0x" + Long.toHexString(address));
      this.address = address;
      if (monitor == null)
        monitor = new Object();
    }

    protected void logHit (Task task, long address, String message)
    {
      if (logger.isLoggable(Level.FINEST))
        {
          Object[] logArgs = { task, Long.toHexString(address),
                              Long.toHexString(task.getIsa().pc(task)),
                              Long.toHexString(this.address) };
          logger.logp(Level.FINEST, "RunState.Breakpoint", "updateHit",
                      message, logArgs);
        }
    }
    
    public Action updateHit (Task task, long address)
    {
//      System.err.println("SteppingBreakpoint.updateHIt " + task);
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
          task.requestAddInstructionObserver(steppingObserver);
        }

      ++triggered;
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
//      System.err.println("BreakPoint.addedTo");
      ((Task) observable).requestDeleteInstructionObserver(steppingObserver);
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
  
}
