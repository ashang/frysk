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

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
// import java.util.Random;

// import org.gnu.atk.AtkObject;
import org.gnu.gdk.Color;
// import org.gnu.gtk.Frame;
import org.gnu.gtk.Label;
// import org.gnu.gtk.ShadowType;
import org.gnu.gtk.VBox;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import com.redhat.ftk.EventViewer;

import frysk.gui.monitor.actions.GenericAction;
import frysk.gui.monitor.observers.ObserverRoot;

public class StatusWidget
    extends VBox
{

  private Color backgroundColor = new Color(50000, 50000, 50000);

  private Color[] markerColors = { Color.RED, Color.GREEN, Color.BLUE,
                                  Color.ORANGE };

  Label nameLabel;

  // private GuiData data;

  // private Frame frame;

  private EventViewer viewer;

  private int trace0;

  public Observable notifyUser;

  private HashMap procMap;

  // private ProcMenu procMenu;

  // private int e2;

  public int newTrace (String desc, String info)
  {
    int trace1 = this.viewer.addTrace(desc, info);
    // this.viewer.setTraceColor(trace1, getColor(traceColors));
    return trace1;
  }

  public StatusWidget (GuiProc guiProc, String procname)
  {
    super(false, 0);

    // FontDescription font = new FontDescription();
    this.notifyUser = new Observable();
    // this.data = guiProc;

    this.procMap = new HashMap();
    // ========================================
    // frame = new Frame(""); //$NON-NLS-1$
    // frame.setBorderWidth(0);
    // frame.setShadow(ShadowType.NONE);
    // AtkObject atk = frame.getAccessible();
    // atk.setName("Status Frame");
    // atk.setDescription("Frame to hold the status widget.");
    // frame.add(mainVbox);
    // this.add(frame);
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
    this.viewer.setBorderWidth(5);
    this.viewer.resize(1, 1);
    this.viewer.setBackgroundColor(backgroundColor);
    this.viewer.setTimebase(10.0);

    // XXX: Change "Additional information to something more meaningfull.
    trace0 = this.viewer.addTrace(procname, "Additional information.");

    this.procMap.put(new Integer(trace0), guiProc);

    initLogTextView(guiProc);
    this.add(viewer);
    // mainVbox.packStart(viewer, true, true, 0);
    // ========================================

    this.initThreads(guiProc);

    // ========================================
    // VSeparator seperator = new VSeparator();
    // mainVbox.packStart(seperator, false, true, 5);
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

    this.viewer.addListener(new MouseListener()
    {

      public boolean mouseEvent (MouseEvent event)
      {

        if (event.getType() == MouseEvent.Type.BUTTON_PRESS
            & event.getButtonPressed() == MouseEvent.BUTTON3)
          {
            System.out.println("Button Press, Third Button");
            long traces[] = viewer.getSelectedTraces();

            if (traces != null)
              {
                if (1 == traces.length)
                  {
                    GuiData data = (GuiData) procMap.get(new Long(traces[0]));
                    if (data.getClass().equals(GuiTask.class))
                      {
                        ThreadMenu.getMenu().popup((GuiTask) data);
                      }
                    else if (data.getClass().equals(GuiProc.class))
                      {
                        // procMenu.popup((GuiProc) data);
                        // FIXME: Get a process menu.
                        System.out.println("This is a GuiProc, can't get the menu yet.");
                      }
                    else
                      {
                        System.out.println("Sorry this isn't a task or a proc, its a: "
                                           + data.getClass());
                      }
                  }
                else if (1 < traces.length)
                  {
                    System.out.println("Multiple traces selected, "
                                       + "TODO: multiple trace functionality, "
                                       + "also drag and drop.");
                  }
              }

            // System.out.println("click : " + data); //$NON-NLS-1$
            return true;
          }
        return false;
      }
    });

    this.showAll();
  }

  private void initThreads (GuiProc guiProc)
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
      public void update (Observable arg0, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        addTask(guiTask);
      }
    });

    list.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable arg0, Object object)
      {
        GuiTask guiTask = (GuiTask) object;
        removeTask(guiTask);
      }
    });
  }

  private void addTask (GuiTask guiTask)
  {
    // GuiProc pdata = GuiProcFactory.getGuiProc(data.getTask().getProc());
    // StatusWidget sw = (StatusWidget) pdata.getWidget();
    // XXX: Change "other usefull..." to something more meaningfull.
    int trace = this.newTrace(guiTask.getTask().getName(),
                              "Other useful per-trace information.");
    guiTask.setWidget(this, trace);

    this.procMap.put(new Integer(trace), guiTask);

    this.initLogTextView(guiTask);
  }

  private void removeTask (GuiTask guiTask)
  {
    int trace = guiTask.getTrace();
    viewer.deleteTrace(trace);
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

  private void initLogTextView (final GuiData guiData)
  {
    ObservableLinkedList observers = guiData.getObservers();
    ListIterator iter = observers.listIterator();
    while (iter.hasNext())
      {
        // LogAction logAction = new LogAction();
        final ObserverRoot observer = (ObserverRoot) iter.next();

        // if (guiData instanceof GuiProc)
        // {
        // logAction.setArgument("PID " + ((GuiProc) guiData).getProc().getPid()
        // + " triggered " + observer.getName());
        // }
        // else
        // {
        // logAction.setArgument("TID " + ((GuiTask) guiData).getTask().getTid()
        // + " triggered " + observer.getName());
        // }
        //
        observer.genericActionPoint.addAction(new TimelineAction(observer,
                                                                 guiData));
        // observer.genericActionPoint.addAction(logAction);
      }

    guiData.getObservers().itemAdded.addObserver(new Observer()
    {
      GuiData realGuiData = guiData;

      public void update (Observable arg0, Object obj)
      {
        // LogAction logAction = new LogAction();

        final ObserverRoot observer = (ObserverRoot) obj;

        // if (realGuiData instanceof GuiProc)
        // {
        // logAction.setArgument("PID " + ((GuiProc)
        // realGuiData).getProc().getPid()
        // + " triggered " + observer.getName());
        // }
        // else
        // {
        // logAction.setArgument("TID " + ((GuiTask)
        // realGuiData).getTask().getTid()
        // + " triggered " + observer.getName());
        // }
        //
        observer.genericActionPoint.addAction(new TimelineAction(observer,
                                                                 realGuiData));
        // observer.genericActionPoint.addAction(logAction);
      }
    });
  }

  public void setName (String name)
  {
    // this.frame.setLabel(name);
    System.out.println("Why do you want to change my name");
  }

  public int getTrace0 ()
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

    public TimelineAction (ObserverRoot observer, GuiData guiData)
    {
      super("TimeLine Action", ""); //$NON-NLS-1$ //$NON-NLS-2$
      this.observer = observer;
      this.dontSaveObject();
      this.guiData = guiData;
      this.createEvent();
    }

    private void createEvent ()
    {
      count++;

      int observerglyph = 0, observercolor = 0;
      if (observer.getBaseName().equals("Fork Observer"))
        {
          observerglyph = 0;
          observercolor = 0;
        }
      else if (observer.getBaseName().equals("Exec Observer"))
        {
          observerglyph = 1;
          observercolor = 1;
        }
      else if (observer.getBaseName().equals("Terminating Observer"))
        {
          observerglyph = 2;
          observercolor = 2;
        }
      else if (observer.getBaseName().equals("Clone Observer"))
        {
          observerglyph = 3;
          observercolor = 0;
        }
      else if (observer.getBaseName().equals("Syscall Observer"))
        {
          observerglyph = 4;
          observercolor = 1;
        }
      else if (observer.getBaseName().equals("Exit Notification Observer"))
        {
          observerglyph = 5;
          observercolor = 2;
        }
      else
        {
          System.out.println("Couldn't understand observer base name: "
                             + observer.getBaseName());

        }

      this.markerId = viewer.addMarker(observerglyph, observer.getName(),
                                       observer.getToolTip());
      viewer.setMarkerColor(this.markerId, markerColors[observercolor]);

    }

    public void execute (ObserverRoot observer)
    {

      // XXX: Change other usefull...
      viewer.appendEvent(guiData.getTrace(), markerId,
                         "Other useful per-event information.");
    }

  }
}
