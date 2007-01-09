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
  
//  private Histogram histogram;
//  
//  private static final int TIMELINE_LEFT_MARGIN = 5;
//  private static final int TIMELINE_RIGHT_MARGIN = 5;
//  private static final int TIMELINE_SPACING = 16;
//  private static final int HISTOGRAM_HEIGHT = 20;
  
  ProcBox (GuiProc guiProc, SizeGroup labelsSizeGroup)
  {
    super();
    this.setBorderWidth(6);
    this.mainGutTaskAdded = false;
//    this.histogram = new Histogram(this.getName(), this.getToolTip());
//    this.addChild(histogram);
    this.timeLinesVBox = new VBox(false,0);
    this.labelsVBox = new VBox(false,0);
    
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
