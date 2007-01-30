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
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.Label;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.VBox;

import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.ObservableLinkedList;

public class ProcBox
    extends Box
{
  GuiProc guiProc;
  GuiTask mainGuiTask;
  private boolean mainGutTaskAdded;
  VBox timeLinesVBox;
  VBox labelsVBox;
  
  ProcBox (GuiProc guiProc, SizeGroup labelsSizeGroup)
  {
    super();
    this.setBorderWidth(6);
    this.mainGutTaskAdded = false;

    this.getAccessible().setName(guiProc.getExecutableName()+"ProcBox");
    
    this.timeLinesVBox = new VBox(false,0);
    this.timeLinesVBox.getAccessible().setName(guiProc.getExecutableName()+"TimeLinesVBox");
    this.labelsVBox = new VBox(false,0);
    this.labelsVBox.getAccessible().setName(guiProc.getExecutableName()+"LablesVBox");
    
    SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.VERTICAL);
    sizeGroup.addWidget(labelsVBox);
    sizeGroup.addWidget(timeLinesVBox);
    
    labelsSizeGroup.addWidget(labelsVBox);
    
    VBox spacerVbox = new VBox(false,0);
    spacerVbox.packStart(labelsVBox, false, false, 0);
    spacerVbox.packStart(new Label(""), true, true, 0);
    this.packStart(spacerVbox, false, true, 0);
    
    //this.packStart(labelsVBox, false, true, 0);
    ScrolledWindow scrolledWindow = new ScrolledWindow();
    scrolledWindow.addWithViewport(timeLinesVBox);
    scrolledWindow.setPolicy(PolicyType.ALWAYS, PolicyType.NEVER);
    
    this.packStart(scrolledWindow, true, true, 0);
    this.setProc(guiProc);
  }

  private void setProc(GuiProc guiProc){
    ObservableLinkedList tasks = guiProc.getTasks();
    this.guiProc = guiProc;
    
    ProcTimeLine procTimeLine = new ProcTimeLine(guiProc);
    
    Label label = new Label(procTimeLine.getLabel());
    SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.VERTICAL);
    sizeGroup.addWidget(procTimeLine);
    sizeGroup.addWidget(label);
    
    this.timeLinesVBox.packStart(procTimeLine, true, true, 0);
    this.labelsVBox.packStart(label, true, true, 0);
    
    tasks.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        addGuiTask(guiTask);
      }
    });
    
    tasks.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        removeGuiTask(guiTask);
      }
    });
    
    Iterator iterator = tasks.iterator();
    while (iterator.hasNext())
      {
        GuiTask task = (GuiTask) iterator.next();
        addGuiTask(task);
      }
    
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

  protected void addGuiTask (GuiTask guiTask)
  {
    // if a proc has only one task there is no need to have
    // a time line for the proc and one for the main task
    // because they will look exactly the same.
    if(this.mainGuiTask == null){
      this.mainGuiTask = guiTask;
      return;
    }
    
    if(!this.mainGutTaskAdded && guiTask != this.mainGuiTask){
      this.addGuiTask(this.mainGuiTask);
      this.mainGutTaskAdded = true;
    }
    
    TaskTimeLine taskTimeLine = new TaskTimeLine(guiTask);
    //this.packEnd(taskTimeLine, true, true, 0);
    
    Label label = new Label(taskTimeLine.getLabel());
    label.getAccessible().setName(label.getName());
    this.timeLinesVBox.packStart(taskTimeLine, true, true, 0);
    this.labelsVBox.packStart(new Label(taskTimeLine.getLabel()), true, true, 0);
    SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.VERTICAL);
    sizeGroup.addWidget(taskTimeLine);
    sizeGroup.addWidget(label);
    
    //this.histogram.addTimeLine(taskTimeLine);
    
//    int index = this.children.indexOf(taskTimeLine);
//    taskTimeLine.setSize(getX()+ TIMELINE_LEFT_MARGIN, getY() + (TIMELINE_SPACING*++index), getWidth() - TIMELINE_RIGHT_MARGIN - TIMELINE_LEFT_MARGIN, getHeight());
//    
//    if(taskTimeLine.getY()+TIMELINE_SPACING >= this.getHeight()){
//      this.setSize(this.getX(), this.getY(), this.getWidth(), this.getHeight()+2*TIMELINE_SPACING);
//    }
    
    this.showAll();
  }

  
//  public void setSize(int x, int y, int w, int h){
//    int index = 0;
//    super.setSize(x, y, w, h);
//    
//    Iterator iter = children.iterator();
//    
//    // ProcTimeLine
//    EventViewerWidget child = (EventViewerWidget) iter.next();
//    child.setSize(getX() + TIMELINE_LEFT_MARGIN, this.getY() + (TIMELINE_SPACING*++index), getWidth() - TIMELINE_RIGHT_MARGIN - TIMELINE_LEFT_MARGIN, getHeight());
//    
//    while (iter.hasNext())
//      {
//        child = (EventViewerWidget) iter.next();
//        child.setSize(getX() + TIMELINE_LEFT_MARGIN, this.getY() + (TIMELINE_SPACING*++index), getWidth() - TIMELINE_RIGHT_MARGIN - TIMELINE_LEFT_MARGIN, getHeight());
//      }
//    
//    if(child.getY()+HISTOGRAM_HEIGHT >= this.getY()+this.getHeight()){
//      this.setSize(this.getX(), this.getY(), this.getWidth(), this.getHeight()+TIMELINE_SPACING+HISTOGRAM_HEIGHT);
//    }
//    
//    this.histogram.setSize(this.getX(), this.getY()+this.getHeight() - HISTOGRAM_HEIGHT, this.getWidth(), HISTOGRAM_HEIGHT);
//  }
  
}
