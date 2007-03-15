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
 * Created on Sep 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */


package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.eventviewer.Event;
import frysk.gui.monitor.eventviewer.EventManager;
import frysk.gui.monitor.filters.IntFilterPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.sys.Sig;

public class TaskTerminatingObserver
    extends TaskObserverRoot
    implements TaskObserver.Terminating
{

  public TaskFilterPoint taskFilterPoint;

  public IntFilterPoint intFilterPoint;

  public TaskActionPoint taskActionPoint;

  public TaskTerminatingObserver ()
  {
    super("Terminating Observer", "Fires when this process is exiting");

    this.taskFilterPoint = new TaskFilterPoint("terminating task",
                                               "The task that is terminating");
    this.intFilterPoint = new IntFilterPoint("exit value",
                                             "the exit value of the task");

    this.addFilterPoint(taskFilterPoint);
    this.addFilterPoint(intFilterPoint);

    this.taskActionPoint = new TaskActionPoint("terminating task",
                                               "The task that is terminating");

    this.addActionPoint(taskActionPoint);
  }

  public TaskTerminatingObserver (TaskTerminatingObserver other)
  {
    super(other);

    this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
    this.intFilterPoint = new IntFilterPoint(other.intFilterPoint);

    this.addFilterPoint(taskFilterPoint);
    this.addFilterPoint(intFilterPoint);

    this.taskActionPoint = new TaskActionPoint(other.taskActionPoint);

    this.addActionPoint(taskActionPoint);

  }

  public Action updateTerminating (Task task, boolean signal, int value)
  {
    final Task myTask = task;
    final boolean mySignal = signal;
    final int myValue = value;

    org.gnu.glib.CustomEvents.addEvent(new Runnable()
    {
      public void run ()
      {
        bottomHalf(myTask, mySignal, myValue);
      }
    });
    return Action.BLOCK;
  }

  protected void bottomHalf (Task task, boolean signal, int value)
  {
    this.setInfo("PID: " + task.getProc().getPid() + " TID: " + task.getTid()
                 + " Event: " + this.getName() + " Host: "
                 + Manager.host.getName());
    if (this.runFilters(task, signal, value))
      {
        this.runActions(task, signal , value);
      }

    Action action = this.whatActionShouldBeReturned();
    if (action == Action.BLOCK)
      {
        // 
      }
    else
      {
        task.requestUnblock(this);
      }
  }

  private void runActions (Task task, boolean signal, int value)
  {
    // TODO implement action points to take care of signal and value
    String name = "terminating";
    String tooltip = "task terminating";
    Event event = new Event(name, tooltip, GuiTask.GuiTaskFactory.getGuiTask(task), this);
    
    super.runActions();
    this.taskActionPoint.runActions(task, this, event);
    
    if(signal){
      name += " sig " + Sig.toPrintString(value);
      tooltip += " with signal " + Sig.toPrintString(value);
    }
    EventManager.theManager.addEvent(event);
  }

  private boolean runFilters (Task task, boolean signal, int value)
  {
    if (! this.taskFilterPoint.filter(task))
      return false;
    //To do add boolean filterPoint
    if (! this.intFilterPoint.filter(value))
      return false;
    return true;
  }

  public void apply (Task task)
  {
    task.requestAddTerminatingObserver(this);
  }

  public GuiObject getCopy ()
  {
    return new TaskTerminatingObserver(this);
  }

  public void unapply (Task task)
  {
    task.requestDeleteTerminatingObserver(this);
  }

}
