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

package frysk.gui.monitor.eventviewer;

import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.glib.CustomEvents;
import org.gnu.gtk.Adjustment;
import org.gnu.gtk.VBox;

import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.GuiTask;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.ProcObserver.ProcTasks;

public class ProcBox extends VBox
{
  GuiProc guiProc;
 
  private boolean mainGuiTaskAdded;
  private Adjustment hAdjustment;
  
  TimeLineSelectionManager manager;
  
  ProcBox (GuiProc guiProc, Adjustment adjustment, TimeLineSelectionManager manager)
  {
    super(false,0);
  
    this.manager = manager;
    this.hAdjustment = adjustment;
    
    this.mainGuiTaskAdded = false;

    this.setProc(guiProc);
  }

  private void setProc(GuiProc guiProc){
    this.guiProc = guiProc;

    ProcTimeLine procTimeLine = new ProcTimeLine(guiProc, manager);
    procTimeLine.setHAdjustment(hAdjustment);
    this.packStart(procTimeLine, true, true, 0);
    
    new ProcTasksObserver(guiProc.getProc(), new ProcTasks(){

      public void taskAdded (final Task task)
      {
        CustomEvents.addEvent(new Runnable()
        {
          final Task realTask = task;
          public void run ()
          {
            addGuiTask(GuiTask.GuiTaskFactory.getGuiTask(realTask));
          }
        });
      }

      public void taskRemoved (final Task task)
      {
        CustomEvents.addEvent(new Runnable()
        {
          final Task realTask = task;
          public void run ()
          {
            removeGuiTask(GuiTask.GuiTaskFactory.getGuiTask(realTask));
          }
        });
      }

      public void existingTask (Task task)
      {
        taskAdded(task);
      }

      public void addFailed (Object observable, Throwable w)
      {
        System.err.print("Could not add " + this + " to " + observable);
        w.printStackTrace();
      }

      public void addedTo (Object observable){}
      public void deletedFrom (Object observable){}  
    });  
  }
  
  protected void removeGuiTask (GuiTask guiTask)
  {
    
//    Iterator iter = children.iterator();
//    while (iter.hasNext())
//      {
//        TaskTimeLine child = (TaskTimeLine) iter.next();
//        if(child.getGuiTask() == guiTask){
//          this.children.remove(child);
//          this.histogram.removeTimeLine(child);
//          break;
//        }
//      }
  }

  LinkedList guiTasksBuffer = new LinkedList();
  protected void addGuiTask (GuiTask guiTask)
  {
    
    
    // if the main task has not been added and this is not it.
    if(!this.mainGuiTaskAdded && guiTask.getTask().getTid() != guiTask.getTask().getProc().getPid()){
      this.guiTasksBuffer.add(guiTask);
      return;
    }
    
    TaskTimeLine taskTimeLine = new TaskTimeLine(guiTask, manager);
    taskTimeLine.setHAdjustment(hAdjustment);
    
    this.packStart(taskTimeLine,true,true,0);
    
    // if this was the main task that was just added
    if(guiTask.getTask().getTid() == guiTask.getTask().getProc().getPid()){
      this.mainGuiTaskAdded = true;
      Iterator iterator = guiTasksBuffer.iterator();
      while(iterator.hasNext()){
        guiTask = (GuiTask) iterator.next();
        addGuiTask(guiTask);
      }
      guiTasksBuffer.clear();
      guiTasksBuffer = null;
    }
    
    this.showAll();
  }
  
}
