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
import frysk.event.RequestStopEvent;

abstract public class ProcBlockObserver
    implements ProcObserver.ProcTasks
{
  private ProcBlockTaskObserver taskObserver = new ProcBlockTaskObserver();

  private class ProcBlockTaskObserver
      implements TaskObserver.Instruction, TaskObserver.Terminating
  {
    public Action updateExecuted (Task task)
    {
      existingTask(task);
      return Action.BLOCK;
    }

    public Action updateTerminating (Task task, boolean signal, int value)
    {
      taskRemoved(task);
      return Action.CONTINUE;
    }

    public void addFailed (Object observable, Throwable w)
    {
      w.printStackTrace();
      taskRemoved((Task) observable);
      logger.log(Level.SEVERE, "{0} could not be added to {1}",
                 new Object[] { this, observable });
    }

    public void addedTo (Object observable)
    {
      // TODO Auto-generated method stub

    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub

    }

  }

  protected static final Logger logger = Logger.getLogger("frysk");

  protected final Proc proc;

  private Task mainTask;

  private int numTasks;

  private LinkedList tasks;

  public ProcBlockObserver (Proc theProc)
  {
    logger.log(Level.FINE, "{0} new\n", this);
    proc = theProc;
    requestAdd();
  }

  public void requestAdd ()
  {
    /*
     * The rest of the construction must be done synchronous to the EventLoop,
     * schedule it.
     */
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {

        if (proc == null)
          {
            System.out.println("Couldn't get the proc");
            System.exit(1);
          }

        /* XXX: deprecated hack. */
        proc.sendRefresh();

        mainTask = Manager.host.get(new TaskId(proc.getPid()));
        if (mainTask == null)
          {
            logger.log(Level.FINE, "Could not get main thread of "
                                   + "this process\n {0}", proc);
            addFailed(
                      proc,
                      new RuntimeException(
                                           "Process lost: could not "
                                               + "get the main thread of this process.\n"
                                               + proc));
            return;
          }

        boolean isOwned = (proc.getUID() == Manager.host.getSelf().getUID() || proc.getGID() == Manager.host.getSelf().getGID());

        if (! isOwned)
          {
            System.err.println("Process " + proc
                               + " is not owned by user/group.");
            System.exit(1);
          }

        numTasks = proc.getTasks().size();
        Iterator i = proc.getTasks().iterator();

        while (i.hasNext())
          {
            requestAddObservers((Task) i.next());
          }

      }
    });
  }

  public void addFailed (Object observable, Throwable w)
  {
    w.printStackTrace();
    proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));

    try
      {
        // Wait for eventLoop to finish.
        Manager.eventLoop.join();
      }
    catch (InterruptedException e)
      {
        e.printStackTrace();
      }
    System.exit(1);

  }

  public void requestAddObservers (Task task)
  {
    task.requestAddInstructionObserver(taskObserver);
    task.requestAddTerminatingObserver(taskObserver);
  }

  public void taskAdded (Task task)
  {

  }

  public void taskRemoved (Task task)
  {

  }

  public void addedTo (Object observable)
  {

  }

  public void blockTask (LinkedList tasks)
  {
    // //System.out.println("in blockTask - setting numTasks to " +
    // tasks.size());
    this.tasks = tasks;
    numTasks = tasks.size();
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {
        Iterator i = ProcBlockObserver.this.tasks.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            // System.out.println("blockTask -> " + t);
            t.requestAddInstructionObserver(taskObserver);
          }
      }
    });
  }

  public void requestUnblock(Task task)
  {
    task.requestUnblock(taskObserver);
  }
  
  public void requestDeleteInstructionObserver(Task task)
  {
    task.requestDeleteInstructionObserver(taskObserver);
  }
  
  public void requestDeleteTerminatingObserver(Task task)
  {
    task.requestDeleteTerminatingObserver(taskObserver);
  }
  
  public int getNumTasks ()
  {
    return this.numTasks;
  }

}
