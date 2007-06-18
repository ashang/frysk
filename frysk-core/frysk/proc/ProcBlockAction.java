// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.event.Event;
import frysk.proc.dead.LinuxHost;

/**
 * This class blocks all of the threads in a process and performs a given action
 * defined by the method existingTask(Task task) on each task.
 * Extensions of this class must implement existingTask(), and 
 * allExistingTasksCompleted() which is called when existingTask() has been 
 * called on all tasks.
 * 
 */
public class ProcBlockAction
{
  private ProcObserver.ProcAction action;
  private ProcBlockTaskObserver taskObserver = new ProcBlockTaskObserver();

  private boolean isMainTaskAdded = false;

  private class ProcBlockTaskObserver
      implements TaskObserver.Instruction, TaskObserver.Terminated
  {
    public Action updateExecuted (final Task task)
    {

      if (! isMainTaskAdded)
        {
          isMainTaskAdded = true;

          Iterator i = proc.getTasks().iterator();

          while (i.hasNext())
            {
              Task t = (Task) i.next();
              if (t != task)
                requestAddObservers(t);
            }
        }

      /*
       * Must have existingTask called later so that there are no issues with
       * synchronization with going through the task list. Happens in
       * ProcState.allAttached, ConcurrentModificationException.
       */
      Manager.eventLoop.add(new Event()
      {

        public void execute ()
        {
          action.existingTask(task);
          checkFinish(task);
        }

      });

      return Action.BLOCK;
    }

    public void addFailed (Object observable, Throwable w)
    {
      action.taskAddFailed(observable, w);
      checkFinish((Task) observable);
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
    }

    public Action updateTerminated (Task task, boolean signal, int value)
    {
      action.taskAddFailed(task, new RuntimeException("Task terminated"));
      checkFinish(task);
      return Action.BLOCK;
    }

  }

  protected static final Logger logger = Logger.getLogger("frysk");

  protected Proc proc;

  private LinkedList tasks = new LinkedList();

  private LinkedList taskList;

  /**
   * Creates a ProcBlockAction which will attach to the given process stopping
   * all of its tasks, performing the requested action on each task, and then
   * removing itself.
   * 
   * @param theProc a non-null Process.
   */
  public ProcBlockAction (Proc theProc, ProcObserver.ProcAction action)
  {
    logger.log(Level.FINE, "{0} new\n", this);
    proc = theProc;
    this.action = action;

    taskList = proc.getTasks();
    requestAdd();
  }
  
  public ProcBlockAction (ProcId procId)
  {
    logger.log(Level.FINE, "{0} new\n", this);
    
    Manager.host.requestFindProc(procId, new Host.FindProc() {

      public void procFound (ProcId procId)
      {
        proc = Manager.host.getProc(procId);
        taskList = proc.getTasks();
        requestAdd();
      }

      public void procNotFound (ProcId procId, Exception e)
      {
        throw new RuntimeException("Proc not found " + procId.intValue());
      }
      
    });
    
  }
  
  public ProcBlockAction (File coreFile)
  {
    LinuxHost core = new LinuxHost(Manager.eventLoop, coreFile);

    core.requestRefreshXXX();
    Manager.eventLoop.runPending();
    Iterator iterator = core.getProcIterator();

    if (iterator.hasNext())
      proc = (Proc) iterator.next();
    else
      {
        proc = null;
        throw new RuntimeException("No proc in this corefile");
      }
    if (iterator.hasNext())
      throw new RuntimeException("Too many procs on this corefile");

    taskList = proc.getTasks();
    
    iterator = taskList.iterator();
    
    while (iterator.hasNext())
      {
        Task task = (Task) iterator.next();
        action.existingTask(task);
      }
    
    action.allExistingTasksCompleted();
  }

  private void requestAdd ()
  {
    /*
     * The rest of the construction must be done synchronous to the EventLoop,
     * schedule it.
     */
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {
        Task mainTask = proc.getMainTask();

        if (mainTask == null)
          {
            logger.log(Level.FINE, "Could not get main thread of "
                                   + "this process\n {0}", proc);
            action.addFailed(proc, new RuntimeException("Process lost: could not get "
                                                 + "the main thread of this "
                                                 + "process.\n" + proc));
            return;
          }
        requestAddObservers(mainTask);
      }
    });
  }

  

  private void requestAddObservers (Task task)
  {
    tasks.add(task);
    task.requestAddInstructionObserver(taskObserver);
    task.requestAddTerminatedObserver(taskObserver);
  }


  private void requestDeleteObservers (Task task)
  {
    task.requestDeleteInstructionObserver(taskObserver);
    task.requestDeleteTerminatedObserver(taskObserver);
  }

  private void requestDelete ()
  {
    Iterator iter = tasks.iterator();

    while (iter.hasNext())
      {
        Task task = (Task) iter.next();
        requestDeleteObservers(task);
        iter.remove();
      }
  }  

  private boolean finished = false;

  private void checkFinish (Task task)
  {

    if (task != null)
      taskList.remove(task);

    logger.log(Level.FINEST, "{0} this taskList, {1} proc.taskList\n",
               new Object[] { taskList, proc.getTasks() });

    if (taskList.isEmpty())
      {
        if (! finished)
          {
            finished = true;

            action.allExistingTasksCompleted();

            requestDelete();
          }
      }

  }

}
