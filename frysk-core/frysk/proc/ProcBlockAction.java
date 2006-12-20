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
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.event.Event;

abstract public class ProcBlockAction
    implements ProcObserver
{
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
          existingTask(task);
          checkFinish(task);
        }

      });

      return Action.BLOCK;
    }

    public void addFailed (Object observable, Throwable w)
    {
      taskAddFailed(observable, w);
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
      taskAddFailed(task, new RuntimeException("Task terminated"));
      checkFinish(task);
      return Action.BLOCK;
    }

  }

  protected static final Logger logger = Logger.getLogger("frysk");

  protected final Proc proc;

  private LinkedList tasks = new LinkedList();

  private LinkedList taskList;

  /**
   * Creates a ProcBlockAction which will attach to the given process stopping
   * all of its tasks, performing the requested action on each task, and then
   * removing itself.
   * 
   * @param theProc a non-null Process.
   */
  public ProcBlockAction (Proc theProc)
  {
    logger.log(Level.FINE, "{0} new\n", this);
    proc = theProc;

    taskList = proc.getTasks();
    requestAdd();
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
            addFailed(proc, new RuntimeException("Process lost: could not get "
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

  public void addedTo (Object observable)
  {
  }   

  public void deletedFrom (Object observable)
  {
  }

  public void taskAddFailed (Object observable, Throwable w)
  {
  }

  public LinkedList getTasks ()
  {
    return tasks;
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

  public abstract void allExistingTasksCompleted ();

  private boolean finished = false;

  private void checkFinish (Task task)
  {

    if (null != task)
      taskList.remove(task);

    logger.log(Level.FINEST, "{0} this taskList, {1} proc.taskList\n",
               new Object[] { taskList, proc.getTasks() });

    // Check for destroyed tasks.
    Iterator iter = taskList.iterator();
    while (iter.hasNext())
      {
        Task t = (Task) iter.next();
        if (t.isDestroyed())
          iter.remove();
      }

    if (taskList.isEmpty())
      {
        if (! finished)
          {
            finished = true;

            allExistingTasksCompleted();

            requestDelete();
          }
      }

  }

}
