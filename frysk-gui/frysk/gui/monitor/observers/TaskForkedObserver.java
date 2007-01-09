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
// type filter text
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

/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */


package frysk.gui.monitor.observers;

import java.util.logging.Level;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.eventviewer.Event;
import frysk.gui.monitor.eventviewer.EventManager;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver
    extends TaskObserverRoot
    implements TaskObserver.Forked
{

  // ObservableLinkedList forkedActions;

  public TaskFilterPoint forkingTaskFilterPoint;

  public TaskFilterPoint forkedTaskFilterPoint;

  public TaskActionPoint forkingTaskActionPoint;

  public TaskActionPoint forkedTaskActionPoint;

  public TaskForkedObserver ()
  {
    super("Fork Observer", "Fires when a proc forks");

    this.forkingTaskFilterPoint = new TaskFilterPoint("forking thread",
                                                      "Thread that performed the fork");
    this.forkedTaskFilterPoint = new TaskFilterPoint("forked thread",
                                                     "Main thread of newly forked process");

    this.addFilterPoint(this.forkingTaskFilterPoint);
    this.addFilterPoint(this.forkedTaskFilterPoint);

    this.forkingTaskActionPoint = new TaskActionPoint("forking thread",
                                                      "Thread that performed the fork");
    this.forkedTaskActionPoint = new TaskActionPoint("forked thread",
                                                     "Main thread of newly forked process");

    this.addActionPoint(this.forkingTaskActionPoint);
    this.addActionPoint(this.forkedTaskActionPoint);

  }

  public TaskForkedObserver (TaskForkedObserver other)
  {
    super(other);

    this.forkingTaskFilterPoint = new TaskFilterPoint(
                                                      other.forkingTaskFilterPoint);
    this.forkedTaskFilterPoint = new TaskFilterPoint(
                                                     other.forkedTaskFilterPoint);

    this.addFilterPoint(this.forkingTaskFilterPoint);
    this.addFilterPoint(this.forkedTaskFilterPoint);

    this.forkingTaskActionPoint = new TaskActionPoint(
                                                      other.forkingTaskActionPoint);
    this.forkedTaskActionPoint = new TaskActionPoint(
                                                     other.forkedTaskActionPoint);

    this.addActionPoint(this.forkingTaskActionPoint);
    this.addActionPoint(this.forkedTaskActionPoint);

  }

  public Action updateForkedParent (Task task, Task child)
  {
    return Action.BLOCK;
  }

  public Action updateForkedOffspring (Task task, Task child)
  {
    // WarnDialog dialog = new WarnDialog("Fork ya'll");
    // dialog.showAll();
    // dialog.run();

    WindowManager.logger.log(Level.FINE,
                             "{0} updateForkedOffspring child: {1} \n",
                             new Object[] { this, child });
    final Task myTask = task;
    final Task myChild = child;
    org.gnu.glib.CustomEvents.addEvent(new Runnable()
    {
      public void run ()
      {
        // This does the unblock.
        bottomHalf(myTask, myChild);
      }
    });

    // return this.getReturnAction();
    return Action.BLOCK;
  }

  private void bottomHalf (Task task, Task child)
  {
    WindowManager.logger.log(Level.FINE, "{0} bottomHalf\n", this);
    this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid()
                 + " TID: " + task.getTid() + " Event: forked new child PID: "
                 + child.getProc().getPid() + " Host: "
                 + Manager.host.getName());
    if (this.runFilters(task, child))
      {
        this.runActions(task, child);
      }
    else
      {
        WindowManager.logger.log(Level.FINER,
                                 "{0} bottomHalf run filters returned False\n",
                                 this);
      }

    // child.requestAddForkedObserver(new TaskForkedObserver());

    Action action = this.whatActionShouldBeReturned();
    if (action == Action.CONTINUE)
      {
        task.requestUnblock(this);
        child.requestUnblock(this);
      }
  }

  public void apply (Task task)
  {
    task.requestAddForkedObserver(this);
  }

  public GuiObject getCopy ()
  {
    return new TaskForkedObserver(this);
  }

  private boolean runFilters (Task task, Task child)
  {
    if (! this.forkingTaskFilterPoint.filter(task))
      return false;
    if (! this.forkedTaskFilterPoint.filter(child))
      return false;
    return true;
  }

  private void runActions (Task task, Task child)
  {
    WindowManager.logger.log(Level.FINE, "{0} runActions\n", this);
    super.runActions();
    
    // add events to event manager
    EventManager.theManager.addEvent(new Event("fork", "parent called fork", GuiTask.GuiTaskFactory.getGuiTask(task), this));
    //EventManager.theManager.addEvent(new Event("forked", "new child has been forked", GuiTask.GuiTaskFactory.getGuiTask(child), this));
    
    this.forkingTaskActionPoint.runActions(task);
    this.forkedTaskActionPoint.runActions(child);
  }

  public void unapply (Task task)
  {
    task.requestDeleteForkedObserver(this);
  }

}
