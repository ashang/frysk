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

/**
 * State machine for thread and process stepping. Provides static methods for
 * manipulating groups of threads by running, blocking, and various types of 
 * stepping.
 */
public class SteppingEngine
{
  protected static Logger logger = Logger.getLogger ("frysk");

  /* Set of Tasks currently running, or unblocked. */
  private static HashSet runningTasks;

  /* Tasks that have hit a breakpoint */
  private static HashMap breakpointMap;
  
  /* Maps a Proc to an Integer, where the Integer represents the number of
   * Tasks associated with the current context of the process - the number
   * of stepping or running threads of that process. */
  private static Map contextMap;

  /* Maps Tasks to TaskStepEngines, which contain information about the 
   * Task State, line number, and other useful things. */
  private static Map taskStateMap;

  /* Observer used to block Tasks, as well as providing the mechanism to 
   * step them, by an instruction each time. */
  private static SteppingObserver steppingObserver;

  /* Ensures that newly spawned Tasks are maintained by the SteppingEngine
   * class, and also makes sure that exiting Tasks are taken care of and cleaned
   * up after. */
  private static ThreadLifeObservable threadLifeObservable;
  
  /* List of Tasks which require attachment from the SteppingObserver */
  private static LinkedList threadsList;

  static
  {
    runningTasks = new HashSet();
    breakpointMap = new HashMap();
    contextMap = Collections.synchronizedMap(new HashMap());
    taskStateMap = Collections.synchronizedMap(new HashMap());
    steppingObserver = new SteppingObserver();
  }
  
  /**
   * Sets the initial process for this SteppingEngine. Maps the initial keys in
   * contextMap and taskStateMap, and adds the necessary observers to each of 
   * the Tasks.
   * 
   * @param proc The Proc to be managed by SteppingEngine
   */
  public static void setProc (Proc proc)
  {
    Task t = null;
    LinkedList tasksList = proc.getTasks();
    
    threadLifeObservable = new ThreadLifeObservable();
    
    Iterator iter = tasksList.iterator();
    while(iter.hasNext())
      {
	t = (Task) iter.next();
	t.requestAddTerminatingObserver(threadLifeObservable);
	t.requestAddClonedObserver(threadLifeObservable);
    	taskStateMap.put(t, new TaskStepEngine(t));
      }
    
    contextMap.put(t.getProc(), new Integer(tasksList.size()));    
    requestAdd();
  }
  
  /**
   * Sets the initial processes for this SteppingEngine. Maps the initial keys 
   * in contextMap and taskStateMap for each Task from each Proc, and adds the 
   * necessary observers to each of the Tasks.
   * 
   * @param procs The Procs to be managed by SteppingEngine
   */
  public static void setProcs (Proc[] procs)
  {
    Task t = null;
    LinkedList tasksList;

    threadLifeObservable = new ThreadLifeObservable();

    for (int i = procs.length - 1; i >= 0; i--)
      {
	tasksList = procs[i].getTasks();
	threadsList.addAll(tasksList);

	Iterator iter = tasksList.iterator();
	while (iter.hasNext())
	  {
	    t = (Task) iter.next();
	    t.requestAddTerminatingObserver(threadLifeObservable);
	    t.requestAddClonedObserver(threadLifeObservable);
	    taskStateMap.put(t, new TaskStepEngine(t));
	  }

	contextMap.put(t.getProc(), new Integer(tasksList.size()));
      }
    
	requestAdd();
  }
  
  /**
   * Once SteppingEngine is already managing one or more Tasks, appends
   * the given process to the data structures in SteppingEngine.
   * 
   * @param proc The Proc to be added to SteppingEngine
   */
  public static boolean addProc (Proc proc)
  {
    Task t = null;
    
    LinkedList tasksList = proc.getTasks();
    threadsList.addAll(tasksList);

    Iterator iter = tasksList.iterator();
    while (iter.hasNext())
      {
	t = (Task) iter.next();
	t.requestAddTerminatingObserver(threadLifeObservable);
	t.requestAddClonedObserver(threadLifeObservable);
	taskStateMap.put(t, new TaskStepEngine(t));
      }
    
    contextMap.put(t.getProc(), new Integer(tasksList.size()));
    requestAdd();
    return true;
  }
  
  
  /***********************************************************************
   * STEP HANDLING METHODS
   **********************************************************************/
  
  /**
   * Perform an instruction step on the given Task.
   * 
   * @param task The Task to be stepped a single instruction.
   * @return false The Task is not currently blocked
   * @return true The step request was successful
   */
  public static boolean stepInstruction (Task task)
  {
    /* Check to make sure this thread is not already involved with another 
     * operation before asking it to step an instruction. */
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    if (! tse.isStopped())
      return false;

    steppingObserver.notifyNotBlocked();
    tse.setState(new InstructionStepState(tse, task));
    contextMap.put (task.getProc(), new Integer(1));
    
    task.requestUnblock(steppingObserver);
    return true;
  }
  
  /**
   * Perform an instruction step on a list of Tasks.
   * 
   * @param tasks The Tasks to be stepped a single instruction.
   * @return false Not all tasks are currently blocked.
   * @return true The step requests were successful
   */
  public static boolean stepInstruction (LinkedList tasks)
  {
    /* Check to make sure these threads are not already involved with another 
     * operation before asking them to step an instruction. */
    if (isProcRunning(tasks))
      return false;

    steppingObserver.notifyNotBlocked();
    
    Task t = (Task) tasks.getFirst();
    contextMap.put (t.getProc(), new Integer(tasks.size()));
    
    Iterator iter = tasks.iterator();
    while (iter.hasNext())
      {
		t = (Task) iter.next();
		TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(t);
		tse.setState(new InstructionStepState(tse, t));
		t.requestUnblock(steppingObserver);
      }

    return true;
  }
  
/**
 * Sets up SteppingEngine to perform a line step on the given Task.
 * 
 * @param task The Task to be line-stepped
 * @return false The Task is not currently blocked
 * @return true The step request was successful
 */
  public static boolean setUpLineStep (Task task)
  {
    /* Check to make sure these threads are not already involved with another 
     * operation before asking them to step an instruction. */
    if (isTaskRunning(task))
      return false;

    contextMap.put(task.getProc(), new Integer(1));
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);

    /* Check to see if either the TaskStepEngine for this Task has not had its
     * line number set yet, or its previous state was in non-debuginfo code. */
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
  
  /**
   * Perform a line-step on a list of Tasks
   * 
   * @param tasks The Tasks to line step
   * @return false Not all tasks are currently blocked.
   * @return true The step requests were successful
   */
  public static boolean setUpLineStep (LinkedList tasks)
  {
    /* Check to make sure these threads are not already involved with another 
     * operation before asking them to step an instruction. */
    if (isProcRunning(tasks))
      return false;

    /* Do all the work here */
    setUp(tasks, true);
    
    return true;
  }
  
/**
 * Sets up stepping information for a list of Tasks. Checks line numbers and 
 * whether the Tasks have any debuginfo at this point in time, before proceeding.
 * 
 * @param tasks The Tasks to begin stepping
 * @param isLine Whether this set up is for a line step or otherwise.
 */
  public static void setUp (LinkedList tasks, boolean isLine)
  {
    /* Make sure there's actually something to step before going any further. */
    if (tasks.size() == 0)
      return;
    
    TaskStepEngine tse;
    steppingObserver.notifyNotBlocked();
    
    Task t = null;
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        t = (Task) i.next();
        tse = (TaskStepEngine) taskStateMap.get(t);
        
        if (tse.getLine() == 0)
          {
            DwflLine line = tse.getDwflLine();
        
            /* Check to see if any line debugging information can be resolved
             * from the Task at this point. If not, there's no point in doing
             * 'line stepping' since there are no 'lines' to step. */
            if (line == null)
              {
                tse.setState(new InstructionStepState(tse, t));
                continue;
              }
            else
              tse.setLine(line.getLineNum());
          }
        
        tse.setState(new LineStepState(tse, t));
      }
    
    contextMap.put(t.getProc(), new Integer(tasks.size()));
    
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
	if (threadsList.size() > 0)
	  {
	    Iterator iter = stopTasks.iterator();
	    while (iter.hasNext())
	      {
			Task t = (Task) iter.next();
			if (!threadsList.contains(t))
			  threadsList.addLast(t);
	      }
	  }
	else
	  threadsList.addAll(stopTasks);
	
	TaskStepEngine tse;
	Iterator i = stopTasks.iterator();
	while (i.hasNext())
	  {
	    Task t = (Task) i.next();
	    tse = (TaskStepEngine) taskStateMap.get(t);
	    tse.setState(new StoppedState(t));
	  }
	requestAdd();
      }
    else
      {
	synchronized (threadsList)
	  {
	    Iterator i = runningTasks.iterator();
	    while (i.hasNext())
	      {
		Task t = (Task) i.next();
		if (! keepRunning.contains(t))
		  {
		    threadsList.add(t);
		    i.remove();
		  }
	      }
	    requestAdd();
	  }
      }
    runningTasks.clear();
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
    /* No incoming Tasks and no Tasks already running */
    if (tasks.size() == 0 && runningTasks.size() == 0)
      {
        return;
      }

    /* No incoming Tasks, but some Tasks are running. Block them. */
    else if (tasks.size() == 0 && runningTasks.size() != 0)
      {
        Iterator i = runningTasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            threadsList.addLast(t);
            i.remove();
          }
        requestAdd();
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
        int numRunning = 0;
        steppingObserver.notifyNotBlocked();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            
            /* If this thread has not already been unblocked, do it */
            if (! runningTasks.remove(t))
              {
            	++numRunning;
                tse = (TaskStepEngine) taskStateMap.get(t);
                tse.setState(new RunningState(t));
                t.requestDeleteInstructionObserver(steppingObserver);
              }
            else
              {
              /* Put all threads back into a master list */
            	temp.add(t);
              }
          }
        
        /* Now catch the threads which have a block request */
        if (runningTasks.size() != 0)
          {
            i = runningTasks.iterator();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
                --numRunning;
                threadsList.addLast(t);
              }
            requestAdd();
          }

        contextMap.put(((Task) tasks.getFirst()).getProc(), new Integer(numRunning));
        runningTasks = temp;
      }
    return;
  }
  
  /**
   * Iterates through the given list of Tasks and returns true if any have 
   * a non-stopped state. Tasks are assumed be Tasks from the same Proc.
   * 
   * @param tasks The Tasks to check states for.
   * 
   * @return true If any of the Tasks are not stopped
   * @retruen false If all the Tasks are stopped
   */
  public static boolean isProcRunning (LinkedList tasks)
  {
    TaskStepEngine tse = null;
    Iterator iter = tasks.iterator();
    while (iter.hasNext())
      {
		Task t = (Task) iter.next();
		tse = (TaskStepEngine) taskStateMap.get(t);
		if (tse != null && ! tse.isStopped())
		  return true;
      }

    return false;
  }
  
  /**
   * Checks to see if the state of the given task is not a stopped state.
   * 
   * @param task The Task to check the state of
   * @return true If the Task is not stopped
   * @return false If the Task is stopped
   */
  public static boolean isTaskRunning (Task task)
  {
    TaskStepEngine tse = (TaskStepEngine) taskStateMap.get(task);
    
    if (tse == null)
      return false;
    
    return !tse.isStopped();
  }
  
  /**
   * Clears information out of SteppingEngine data structures which are mapped
   * to the given Task.
   * 
   * @param task The Task to clear information for
   */
  public static void cleanTask (Task task)
  {
    taskStateMap.remove(task);
    runningTasks.remove(task);
  }
  
  /**
   * Removes all information from all SteppingEngine data structures.
   */
  public static void clear ()
  {
    taskStateMap.clear();
    breakpointMap.clear();
    contextMap.clear();
    runningTasks.clear();
    threadLifeObservable.deleteObservers();
    threadLifeObservable = new ThreadLifeObservable();
    breakpoint = null;
    steppingObserver.deleteObservers();
    steppingObserver = new SteppingObserver();
  }

  /*****************************************************************************
   * GETTERS AND SETTERS
   ****************************************************************************/
  
  /**
   * Set the state of a particular Task.
   * 
   * @param task The Task to set the state of
   * @param state The State to set it to
   */
  public static void setTaskState (Task task, State state)
  {
    ((TaskStepEngine) taskStateMap.get(task)).setState(state);
  }
  
  /**
   * Get the state of a particular Task.
   * 
   * @param task The Task to return the State of
   * @return state The State of the given Task
   */
  public static State getTaskState (Task task)
  {
    return ((TaskStepEngine) taskStateMap.get(task)).getState();
  }
  
  /**
   * Gets the SteppingObserver currently being used by the SteppingEngine.
   * 
   * @return steppingObserver The SteppingObserver currently in use
   */
  public static SteppingObserver getSteppingObserver ()
  {
    return steppingObserver;
  }
  
  /**
   * Sets a SteppingBreakpoint on the given Task at the given address.
   * 
   * @param task The Task to breakpoint
   * @param address The address to set the breakpoint at
   */
  public static void setBreakpoint (Task task, long address)
  {
    breakpoint = new SteppingBreakpoint (address);
    task.requestAddCodeObserver(breakpoint, address);
  }
  
  /**
   * Removes the set SteppingBreakpoint from the given Task.
   * 
   * @param task The Task to remove the SteppingBreakpoint from
   */
  public static void removeBreakpoint (Task task)
  {
    task.requestDeleteCodeObserver(breakpoint, addy);
  }
  
  /**
   * Returns the Breakpoint set on the given Task
   * 
   * @param task The Task whose breakpoint is requested
   * @return bp The Breakpoint set at the given Task
   */
  public static Breakpoint getTaskBreakpoint (Task task)
  {
    return (Breakpoint) breakpointMap.get(task);
  }
  
  /**
   * Adds the given Breakpoint on the given Task.
   * 
   * @param task The Task to have the Breakpoint added to
   * @param bp The Breakpoint to add to the given Task
   */
  public static void addBreakpoint (Task task, Breakpoint bp)
  {
    task.requestAddCodeObserver(bp, bp.getAddress());
  }
  
  /**
   * Deletes the given Breakpoint from the given Task
   * 
   * @param task The Task to delete the Breakpoint from
   * @param bp The Breakpoint to delete from the given Task
   */
  public static void deleteBreakpoint (Task task, Breakpoint bp)
  {
    task.requestDeleteCodeObserver(bp, bp.getAddress());
  }
  
  /**
   * Set the current state of the given tasks as running. Used when the running
   * of these Tasks was out of the scope of control for SteppingEngine.
   * 
   * @param tasks The Tasks to be set as running.
   */
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
  
  /**
   * Adds the given Observer to SteppingObserver's Observer list.
   * 
   * @param o The Observer to be added.
   */
  public static void addObserver (Observer o)
  {
    steppingObserver.addObserver(o);
  }
  
  /**
   * Supplies an Observer to be added to the ThreadLifeObserver's Observer list.
   * 
   * @param o The Observer to be added.
   */
  public static void setThreadObserver (Observer o)
  {
    threadLifeObservable.addObserver(o);
  }
  
  /***********************************************************************
   * TASKOBSERVER.INSTRUCTION OBSERVER CLASS
   **********************************************************************/
  
  /**
   * Used by other objects to let the steppingObserver know that the work 
   * blocking the tasks is complete, from their perspective.
   */
  public static void notifyStopped ()
  {
    steppingObserver.notifyStopped();
  }
  
  /**
   * Remove the incoming Observer object from the SteppingObserver's list of 
   * Observers to notify. If, after removing it, the list is empty, unblock
   * the process and return 1. Otherwise return 0. Unblocks the requested 
   * process after deleting it from the Observer list.
   * 
   * @param o The Observer to delete
   * @param p The Proc to delete the Observer from
   * 
   * @return 0 SteppingObserver's Observer list is not empty
   * @return 1 SteppingObserver's Observer list is empty
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
      
//      System.err.println("SE.SO.updateEx: " + task + threadsList.size());
      /* Check to see if acting upon this event produces a stopped state
       * change. If so, decrement the number of Tasks active in the Task's 
       * process context. If there are no Tasks left, then notify the this 
       * Object's observers that all work has been completed. */
      if (((TaskStepEngine) taskStateMap.get(task)).handleUpdate())
	{
	  Proc proc = task.getProc();
	  int i = ((Integer) contextMap.get(proc)).intValue();
	  
	  if (--i <= 0)
	    {
	      if (threadsList.size() > 0)
		{
		  contextMap.put(proc, new Integer(threadsList.size() + i));
		  requestAdd();
		  return Action.BLOCK;
		}
	      
	      this.setChanged();
	      this.notifyObservers(task);
	    }
	  
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
  
  
  public static void requestAdd ()
  {
    /*
         * The rest of the construction must be done synchronous to the
         * EventLoop, schedule it.
         */
    Manager.eventLoop.add(new Event()
    {
      public synchronized void execute ()
      {
	LinkedList list = new LinkedList();
	Task t;
	
	while (threadsList.size() > 0)
	  {
	    t = (Task) threadsList.removeFirst();
	    
	    if (t == null)
	      continue;
	    
	    list.add(t);
	    Proc proc = t.getProc();

	    if (! (proc.getUID() == Manager.host.getSelf().getUID()
		|| proc.getGID() == Manager.host.getSelf().getGID()))
	      {
		System.err.println("Process " + proc
				   + " is not owned by user/group.");
		continue;
	      }
	  }
	
	Iterator i = list.iterator();
	while (i.hasNext())
	  {
	    t = (Task) i.next();
	    if (!t.isDestroyed())
	      t.requestAddInstructionObserver(steppingObserver);
	  }
      }
    });
  }
  
  /***********************************************************************
         * TASKOBSERVER.CLONED/TERMINATED OBSERVER CLASS
         **********************************************************************/
  
  protected static class ThreadLifeObservable 
  extends Observable
  implements TaskObserver.Cloned, TaskObserver.Terminating
  {
    
    public ThreadLifeObservable ()
    {
      threadsList = new LinkedList ();
    }
	
    public Action updateClonedParent (Task parent, Task offspring)
    {
      return Action.CONTINUE;
    }

    public Action updateClonedOffspring (Task parent, Task offspring)
    {
      Integer i = (Integer) contextMap.get(parent.getProc());
      contextMap.put(parent.getProc(), new Integer(i.intValue() + 1));
      taskStateMap.put(offspring, new TaskStepEngine(offspring));
      threadsList.addLast(offspring);
      offspring.requestAddClonedObserver(this);
      offspring.requestAddTerminatingObserver(this);
      return Action.CONTINUE;
    }
    
    public Action updateTerminating (Task task, boolean signal, int value)
    {
//      System.err.println("threadlife.updateTerminating " + task + " " + value);
      runningTasks.remove(task);
      
      Integer context = (Integer) contextMap.get(task.getProc());
      contextMap.put (task.getProc(), new Integer(context.intValue() - 1));
      
      taskStateMap.remove(task);
      threadsList.remove(task);
      cleanTask(task);
      this.setChanged();
	  
      if (taskStateMap.size() == 0)
	this.notifyObservers(null);
      else
	this.notifyObservers(task);
      
      return Action.CONTINUE;
    }

    public void addedTo (Object observable)
    {
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
          logger.logp(Level.FINEST, "SteppingEngine.SteppingBreakpoint", "updateHit",
                      message, logArgs);
        }
    }
    
    public Action updateHit (Task task, long address)
    {
//      System.err.println("SteppingBreakpoint.updateHIt " + task);
      logHit(task, address, "task {0} at 0x{1}\n");
      if (address != this.address)
        {
          logger.logp(Level.WARNING, "SteppingEngine.SteppingBreakpoint", "updateHit",
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
