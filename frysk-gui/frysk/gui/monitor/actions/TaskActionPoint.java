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


package frysk.gui.monitor.actions;

import java.util.Iterator;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.eventviewer.Event;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.proc.Task;

public class TaskActionPoint
    extends ActionPoint
{

  private ObservableLinkedList applicableActions;

  public TaskActionPoint ()
  {
    super();

    this.applicableActions = new ObservableLinkedList();

    this.initApplicableActions();
  }

  public TaskActionPoint (String name, String toolTip)
  {
    super(name, toolTip);

    this.applicableActions = new ObservableLinkedList();

    this.initApplicableActions();
  }

  public TaskActionPoint (TaskActionPoint other)
  {
    super(other);

    this.applicableActions = new ObservableLinkedList(other.applicableActions, true);
  }

  public ObservableLinkedList getApplicableActions ()
  {
    return ActionManager.theManager.getTaskActions();
  }

  private void initApplicableActions ()
  {
    this.applicableActions.add(new ShowSourceWin());
    this.applicableActions.add(new AddTaskObserverAction());
    this.applicableActions.add(new PrintTask());
    this.applicableActions.add(new PrintTaskBacktrace());
    this.applicableActions.add(new ShowRegWin());
    this.applicableActions.add(new ShowMemWin());
    this.applicableActions.add(new RunExternal());
    this.applicableActions.add(new CaptureStackFrameAction());
  }

  /**
   * Run all the actions that belong to this
   * 
   * @link ActionPoint.
   * @param task the task to perform the actions on.
   */
  public void runActions (Task task, TaskObserverRoot observer, Event event)
  {
    Iterator iter = this.items.iterator();
    while (iter.hasNext())
      {
        TaskAction action = (TaskAction) iter.next();
        action.execute(task, observer, event);
      }
  }

  public ObservableLinkedList getApplicableItems ()
  {
    return this.applicableActions;
  }

  public GuiObject getCopy ()
  {
    return new TaskActionPoint(this);
  }

}
