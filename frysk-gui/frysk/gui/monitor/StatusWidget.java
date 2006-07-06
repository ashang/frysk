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
/*
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */


package frysk.gui.monitor;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import org.gnu.gdk.Color;
import org.gnu.gtk.Frame;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.VBox;
import org.gnu.gtk.VSeparator;

import com.redhat.ftk.EventViewer;

import frysk.gui.monitor.actions.GenericAction;
import frysk.gui.monitor.observers.ObserverRoot;

public class StatusWidget
    extends VBox
{
	
	private Color backgroundColor = Color.WHITE;
	private Color[] traceColors = {Color.BLACK,};
	private Color[] markerColors = {Color.BLACK, Color.BLUE};
	private Random randomColorGenerator;
	
	private Color getColor (Color[] colors) {
		int index = randomColorGenerator.nextInt(colors.length);
		return colors[index];
	}

  Label nameLabel;

  //private GuiData data;

  private Frame frame;

  private EventViewer viewer;

  private int trace0;

  public Observable notifyUser;

  // private int e2;

  public int newTrace(String desc, String info)
  {
    int trace1 = this.viewer.addTrace(desc, info);
    this.viewer.setTraceColor(trace1, getColor(traceColors));
    return trace1;
  }

  public StatusWidget(GuiProc guiProc, String procname)
  {
    super(false, 0);
    
    randomColorGenerator = new Random();
	  
    // FontDescription font = new FontDescription();
    this.notifyUser = new Observable();
//    this.data = guiProc;
    
    HBox mainVbox = new HBox(false, 0);

    // ========================================
    frame = new Frame(""); //$NON-NLS-1$
    frame.setBorderWidth(10);
    frame.add(mainVbox);
    this.add(frame);
    // ========================================

    // ========================================
    // initLogTextView();
    // ScrolledWindow logScrolledWindow = new ScrolledWindow();
    // logScrolledWindow.addWithViewport(logTextView);
    // logScrolledWindow.setShadowType(ShadowType.IN);
    // logScrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
    // mainVbox.packStart(logScrolledWindow, true, true, 0);
    // ========================================

    // ======================================== 
    this.viewer = new EventViewer();
    this.viewer.resize (1, 1);
    this.viewer.setBackgroundColor(backgroundColor);
    this.viewer.setTimebase(10.0);
    
    //XXX: Change "Additional  information to something more meaningfull.
    trace0 = this.viewer.addTrace(procname, "Additional information.");
    this.viewer.setTraceColor(trace0, getColor(traceColors));

    initLogTextView(guiProc);

    mainVbox.setBorderWidth(5);
    mainVbox.packStart(viewer, true, true, 0);
    // ========================================

    this.initThreads(guiProc);

    // ========================================
    VSeparator seperator = new VSeparator();
    mainVbox.packStart(seperator, false, true, 5);
    // ========================================

    // ========================================
    // HBox hbox = new HBox(false, 0);
    // hbox.setBorderWidth(5);
    // VBox vbox = new VBox(false, 0);
    // hbox.packStart(new Label("Attached Observers: "), false, false, 0);
    // //$NON-NLS-1$
    // hbox.packStart(new Label(""), true, false, 0); //$NON-NLS-1$
    // vbox.packStart(hbox, false, false, 0);
    // 
    // ScrolledWindow scrolledWindow = new ScrolledWindow();
    // scrolledWindow.addWithViewport(initAttacheObserversTreeView());
    // scrolledWindow.setShadowType(ShadowType.IN);
    // scrolledWindow.setPolicy(PolicyType.AUTOMATIC, PolicyType.AUTOMATIC);
    // vbox.packStart(scrolledWindow, true, true, 0);
    // mainVbox.packStart(vbox, false, true, 0);
    // ========================================

    this.showAll();
  }

  private void initThreads(GuiProc guiProc)
  {
    ObservableLinkedList list = guiProc.getTasks();
    Iterator iterator = list.iterator();
    while (iterator.hasNext())
      {
        GuiTask guiTask = (GuiTask) iterator.next();
        addTask(guiTask);
      }

    list.itemAdded.addObserver(new Observer()
    {
      public void update(Observable arg0, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        addTask(guiTask);
      }
    });

    list.itemRemoved.addObserver(new Observer()
    {
      public void update(Observable arg0, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        removeTask(guiTask);
      }
    });
  }

  private void addTask(GuiTask guiTask)
  {
    // GuiProc pdata = GuiProcFactory.getGuiProc(data.getTask().getProc());
    // StatusWidget sw = (StatusWidget) pdata.getWidget();
	//XXX: Change "other usefull..." to something more meaningfull.
    int trace = this.newTrace(guiTask.getTask().getName(),
                              "Other useful per-trace information.");
    guiTask.setWidget(this, trace);
    this.initLogTextView(guiTask);
  }

  private void removeTask(GuiTask guiTask)
  {
    // XXX: no api in EventViewer for this
  }

  // private TreeView initAttacheObserversTreeView(){
  // final ListView listView = new ListView();
  // listView.watchLinkedList(data.getObservers());
  //		
  // final Menu menu = new Menu();
  // MenuItem item = new MenuItem("Remove", false); //$NON-NLS-1$
  // item.addListener(new MenuItemListener() {
  // public void menuItemEvent(MenuItemEvent event) {
  // data.remove((ObserverRoot) listView.getSelectedObject());
  // }
  // });
  // menu.add(item);
  // menu.showAll();
  //		
  // listView.addListener(new MouseListener(){
  //
  // public boolean mouseEvent(MouseEvent event) {
  // if(event.getType() == MouseEvent.Type.BUTTON_PRESS
  // & event.getButtonPressed() == MouseEvent.BUTTON3){
  // if((listView.getSelection().getSelectedRows()).length > 0){
  // menu.popup();
  // }
  // return true;
  // }
  // return false;
  // }
  // });
  //		
  // return listView;
  // }

  // private void initLogTextView(){
  // this.logTextView = new TextView();
  // ObservableLinkedList observers = this.data.getObservers();
  // ListIterator iter = observers.listIterator();
  // while(iter.hasNext()){
  // final ObserverRoot observer = (ObserverRoot) iter.next();
  // observer.genericActionPoint.addAction(new GenericAction("",""){
  // public void execute(ObserverRoot observer) {
  // System.out.println("Event: " + observer.getName() + "\n");
  // logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");
  // // area.appendEvent (e2);
  // }
  //
  // public Action getCopy() {
  // return null;
  // }
  //				
  // });
  // }
  //		
  // this.data.getObservers().itemAdded.addObserver(new Observer(){
  //
  // public void update(Observable arg0, Object obj) {
  // final ObserverRoot observer = (ObserverRoot)obj;
  // logTextView.getBuffer().insertText("Event: " + observer.getName() + "
  // added\n");
  // observer.genericActionPoint.addAction(new GenericAction("Logging
  // Action",""){
  // public void execute(ObserverRoot observer) {
  // logTextView.getBuffer().insertText("Event: " + observer.getName() + "\n");
  // System.out.println("Event: " + observer.getName() + "\n");
  // // area.appendEvent (e2);
  // }
  //
  // public Action getCopy() {
  // return null;
  // }
  //					
  // });
  // }
  // });
  //		
  // this.data.getObservers().itemRemoved.addObserver(new Observer(){
  //
  // public void update(Observable arg0, Object obj) {
  // ObserverRoot observer = (ObserverRoot)obj;
  // logTextView.getBuffer().insertText("Event: " + observer.getName() + "
  // removed\n");
  // }
  //			
  // });
  //		
  // }
  //	

  private void initLogTextView(final GuiData guiData)
  {
    ObservableLinkedList observers = guiData.getObservers();
    ListIterator iter = observers.listIterator();
    while (iter.hasNext())
      {
//        LogAction logAction = new LogAction();
        final ObserverRoot observer = (ObserverRoot) iter.next();

//        if (guiData instanceof GuiProc)
//          {
//            logAction.setArgument("PID " + ((GuiProc) guiData).getProc().getPid()
//                                  + " triggered " + observer.getName());
//          }
//        else
//          {
//            logAction.setArgument("TID " + ((GuiTask) guiData).getTask().getTid()
//                                  + " triggered " + observer.getName());
//          }
//
        observer.genericActionPoint.addAction(new TimelineAction(observer, guiData));
//        observer.genericActionPoint.addAction(logAction);
      }

    guiData.getObservers().itemAdded.addObserver(new Observer()
    {
      GuiData realGuiData = guiData;
      public void update(Observable arg0, Object obj)
      {
//        LogAction logAction = new LogAction();

        final ObserverRoot observer = (ObserverRoot) obj;

//        if (realGuiData instanceof GuiProc)
//          {
//            logAction.setArgument("PID " + ((GuiProc) realGuiData).getProc().getPid()
//                                  + " triggered " + observer.getName());
//          }
//        else
//          {
//            logAction.setArgument("TID " + ((GuiTask) realGuiData).getTask().getTid()
//                                  + " triggered " + observer.getName());
//          }
//
        observer.genericActionPoint.addAction(new TimelineAction(observer, realGuiData));
//        observer.genericActionPoint.addAction(logAction);
      }
    });
  }

  public void setName(String name)
  {
    this.frame.setLabel(name);
  }

  public int getTrace0()
  {
    return trace0;
  }

  private static int count = 0;

  class TimelineAction
      extends GenericAction
  {

    int markerId;

    private ObserverRoot observer;

    private GuiData guiData;

    public TimelineAction(ObserverRoot observer, GuiData guiData)
    {
      super("TimeLine Action", ""); //$NON-NLS-1$ //$NON-NLS-2$
      this.observer = observer;
      this.dontSaveObject();
      this.guiData = guiData;
      this.createEvent();
    }

    private void createEvent()
    {
      count++;
      if (count % 3 == 0)
        {
          /* red + green = yellow */
          // this.eventId = area.createEvent(observer.getName(), 65535, 65535,
          // 0);
          this.markerId = viewer.addMarker(0, observer.getName(),
                                           observer.getToolTip());
          viewer.setMarkerColor(this.markerId, getColor(markerColors));
        }

      if (count % 3 == 1)
        {
          /* red + green = yellow */
          // this.eventId = area.createEvent(observer.getName(), 65535, 0,
          // 65535);
          this.markerId = viewer.addMarker(1, observer.getName(),
                                           observer.getToolTip());
          viewer.setMarkerColor(this.markerId, getColor(markerColors));
        }
      if (count % 3 == 2)
        {
          /* red + green = yellow */
          // this.eventId = area.createEvent(observer.getName(), 0, 65535,
          // 65535);
          this.markerId = viewer.addMarker(2, observer.getName(),
                                           observer.getToolTip());
          viewer.setMarkerColor(this.markerId, getColor(markerColors));
        }
    }

    public void execute(ObserverRoot observer)
    {
      viewer.appendEvent(guiData.getTrace(), markerId,
                         "Other useful per-event information.");
    }

  }
}
