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


package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Added to observe Exec events.
 */
public class TaskExecObserver
    extends TaskObserverRoot
    implements TaskObserver.Execed
{

  public TaskFilterPoint taskFilterPoint;

  public TaskActionPoint taskActionPoint;

  public TaskExecObserver ()
  {
    super("Exec Observer", "Fires every time this task executes an exec call");

    this.taskFilterPoint = new TaskFilterPoint("execing thread",
                                               "The thread that is calling exec");
    this.addFilterPoint(taskFilterPoint);

    this.taskActionPoint = new TaskActionPoint(taskFilterPoint.getName(),
                                               taskFilterPoint.getToolTip());
    this.addActionPoint(taskActionPoint);
  }

  public TaskExecObserver (TaskExecObserver other)
  {
    super(other);

    this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
    this.addFilterPoint(taskFilterPoint);

    this.taskActionPoint = new TaskActionPoint(other.taskActionPoint);
    this.addActionPoint(taskActionPoint);
  }

  public Action updateExeced (Task task)
  {
    // System.out.println("TaskExecObserver.updateExeced() " +
    // task.getProc().getCommand());
    final Task myTask = task;
    org.gnu.glib.CustomEvents.addEvent(new Runnable()
    {
      public void run ()
      {
        bottomHalf(myTask);
      }
    });
    return Action.BLOCK;
  }

  private void bottomHalf (Task task)
  {
    this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid()
                 + " TID: " + task.getTid() + " Event: called exec "
                 + " Host: " + Manager.host.getName());
    if (this.runFilters(task))
      {
        this.runActions(task);
      }

    Action action = this.whatActionShouldBeReturned();
    if (action == Action.CONTINUE)
      {
        task.requestUnblock(this);
      }
  }

  private void runActions (Task task)
  {
    super.runActions();
    this.taskActionPoint.runActions(task);
  }

  private boolean runFilters (Task task)
  {
    return this.filter(task);
  }

  public void apply (Task task)
  {
    task.requestAddExecedObserver(this);
  }

  public GuiObject getCopy ()
  {
    return new TaskExecObserver(this);
  }

  private boolean filter (Task task)
  {
    return this.taskFilterPoint.filter(task);
  }

}
