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
import java.util.LinkedList;

import lib.dw.Dwfl;
import lib.dw.DwflLine;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;

/**
 * Model for state transitions in the SourceWindow and HPD classes. Currently
 * possible states include: running, stopped, instruction step, step in,
 * step over, step out. Performs necessary operations for each of these states 
 * including Task management, adding/removing observers, and source work.
 * 
 * @author mcvet
 */
public class StateModel
{

  /* The InstructionObserver handling the process being examined. */
  private ProcBlockObserver pbo;

  /* Keeps track of the Dwfl objects for teach Task; not necessary to
   * re-generate these each time a step is done. */
  private HashMap dwflMap;

  /* Keeps track of the last executed line in source for each Task */
  private HashMap lineMap;

  /* Set of Tasks currently running, or unblocked. */
  private HashSet runningTasks;

  /* Keep track of how many times the task has been unblocked for a particular
   * line. If it gets too high, assume that the line won't change (while(1))
   * and finish. */
  private int lineLoopCount = 0;

  private int taskStepCount = 0;

  private int numSteppingThreads = 0;

  protected static final int STOPPED = 0;
  protected static final int RUNNING = 1;
  protected static final int INSTRUCTION_STEP = 2;
  protected static final int STEP_IN = 3;
  protected static final int STEP_OVER = 4;
  protected static final int STEP_OUT = 5;

  /**
   * Constructor - sets the InstructionObserver for this model and initializes
   * the Maps and Set.
   * 
   * @param pbo The ProcBlockObserver this StateModel delegates to.
   */
  public StateModel (ProcBlockObserver pbo)
  {
    this.pbo = pbo;
    this.dwflMap = new HashMap();
    this.lineMap = new HashMap();
    this.runningTasks = new HashSet();
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
  public void setUpStep (LinkedList tasks)
  {
    this.numSteppingThreads = tasks.size();
    Iterator i = tasks.iterator();
    
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (this.dwflMap.get(t) == null)
          {
            Dwfl d = new Dwfl(t.getTid());
            DwflLine line = null;
            try
              {
                line = d.getSourceLine(t.getIsa().pc(t));
              }
            catch (TaskException te)
              {
                continue;
              }
            catch (NullPointerException npe)
            {
              //System.out.println("ZOMG! Null.");
              //System.out.println(npe.getMessage());
              continue;
            }

            this.dwflMap.put(t, d);
            this.lineMap.put(t, new Integer(line.getLineNum()));
          }
        this.pbo.requestUnblock(t);
      }
  }

  /**
   * Simply perform a single instruction step. Unblocks each Task which will
   * return to the ProcBlockObserver callback in the calling class.
   * 
   * @param tasks   The list of Tasks to step one instruction
   */
  public void stepInstruction (LinkedList tasks)
  {
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        this.pbo.requestUnblock(t);
      }
  }

  /**
   * Performs a step-in. Continues to unblock the Task instruction by
   * instruction until its Dwfl object tell us its line number has changed.
   * 
   * @param task    The task to step in
   */
  public void stepIn (Task task)
  {
    DwflLine line = null;
    try
      {
        line = ((Dwfl) this.dwflMap.get(task)).getSourceLine(task.getIsa().pc(
                                                                              task));
        //System.out.println()
      }
    catch (TaskException te)
      {
         //System.out.println("task execption");
        return;
      }
    catch (NullPointerException npe)
      {
         //System.out.println("NPE");
        return;
      }

    if (line == null)
      {
        return;
      }

    // System.out.println("Nothing is null");
    int lineNum = line.getLineNum();
    int prev = ((Integer) this.lineMap.get(task)).intValue();

    if (lineNum != prev)
      {
        this.lineMap.put(task, new Integer(lineNum));
        --taskStepCount;
      }
    else
      {
        this.lineLoopCount++;
        if ((this.lineLoopCount / this.numSteppingThreads) > 8)
          {
            this.lineMap.put(task, new Integer(lineNum));
            --taskStepCount;
            return;
          }

        this.pbo.requestUnblock(task);
      }
  }

  public void stepOver (Task task)
  {

  }

  public void stepOut (Task task)
  {

  }

  /**
   * All stepping has completed, clean up.
   */
  public void stepCompleted ()
  {
    this.lineLoopCount = 0;
    this.taskStepCount = 0;
    this.numSteppingThreads = 0;
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
    // System.out.println("In statemodel run " + proc.getTasks().size());
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (! this.runningTasks.contains(t))
          {
            this.runningTasks.add(t);
            // System.out.println("deleting from " + t);
            this.pbo.requestDeleteInstructionObserver(t);
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
        this.pbo.requestAdd();
      }
    else
      {
        if (unblockTasks.size() == 0)
          this.pbo.requestAdd();
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
            this.pbo.blockTask(blockTasks);
          }
      }
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
   * 
   * @return 0  There are no running Tasks
   * @return 1  There are Tasks running
   */
  public synchronized int executeTasks (LinkedList tasks)
  {

     //System.out.println("In executeThreads with thread size " + tasks.size()
     //+ " and runningtasks size "
     //+ this.runningTasks.size());

    /* No incoming Tasks and no Tasks already running */
    if (tasks.size() == 0 && this.runningTasks.size() == 0)
      return STOPPED;

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
        this.pbo.blockTask(l);
        return STOPPED;
      }

    /* There are incoming Tasks to be run, and no Tasks already running */
    if (this.runningTasks.size() == 0)
      {
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("(0) Running " + t);
            this.runningTasks.add(t);

            this.pbo.requestDeleteInstructionObserver(t);
          }
        return RUNNING;
      }
    else
      /* There are incoming Tasks to be run, and some Tasks are already running.
       * If they are not already running, unblock the incoming Tasks, and block
       * any running Task not in the incoming list. */
      {
        HashSet temp = new HashSet();
        // this.runningThreads.clear();
        Iterator i = tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
             //System.out.println("Iterating running thread" + t);
            /* If this thread has not already been unblocked, do it */
            if (! this.runningTasks.remove(t))
              {
                // System.out.println("unBlocking " + t);
                this.pbo.requestDeleteInstructionObserver(t);
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
            this.pbo.blockTask(l);
          }

        this.runningTasks = temp;
        // System.out.println("rt temp" + this.runningThreads.size() + " "
        // + temp.size());
      }
    return RUNNING;
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
   * Sets the number of stepping Tasks
   * 
   * @param count   The number of stepping Tasks
   */
  public void setTaskStepCount (int count)
  {
    this.taskStepCount = count;
  }

  /**
   * Decrements the number of stepping Tasks
   */
  public void decTaskStepCount ()
  {
    this.taskStepCount--;
  }

}
