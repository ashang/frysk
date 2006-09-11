// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import org.gnu.gdk.Color;
import org.gnu.gtk.Label;
import org.gnu.gtk.VBox;

import com.redhat.ftk.EventViewer;

import frysk.gui.monitor.actions.TaskAction;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskCloneObserver;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskForkedObserver;
import frysk.gui.monitor.observers.TaskSyscallObserver;
import frysk.gui.monitor.observers.TaskTerminatingObserver;
import frysk.proc.Task;

public class StatusWidget extends VBox {

	private Color backgroundColor = new Color(50000, 50000, 50000);

	private int[] legendAdded = {0,0,0,0,0}; 
	private Color[] markerColors = { Color.RED, Color.GREEN, Color.BLUE,
			Color.ORANGE };

	Label nameLabel;

	private EventViewer viewer;

	private int initialTrace;

	public Observable notifyUser;

	private HashMap procMap;

	ObservableLinkedList threadList;
	
	public int newTrace(String desc, String info) {
		int newTrace = this.viewer.addTrace(desc, info);
		return newTrace;
	}
	
	private void buildEventViewer()	{
		this.viewer = new EventViewer();
		this.viewer.setBorderWidth(5);
		this.viewer.resize(1, 1);
		this.viewer.setBackgroundColor(backgroundColor);
		this.viewer.setTimebase(10.0);
	}
	
	public EventViewer getViewer() {
		return viewer;
	}

	public StatusWidget(GuiProc guiProc, String procname) {
		super(false, 0);

		this.notifyUser = new Observable();
		this.procMap = new HashMap();

		buildEventViewer();
		
		// XXX: Change "Additional information to something more meaningfull.
		this.initialTrace = this.viewer.addTrace(procname, "Additional information.");
		this.procMap.put(new Integer(initialTrace), guiProc);
		initEventViewer(guiProc);
		
		this.add(this.viewer);
		this.initThreads(guiProc);
		this.showAll();
	}

	private void initThreads(GuiProc guiProc) {
		threadList = guiProc.getTasks();
		Iterator iterator = threadList.iterator();
		while (iterator.hasNext()) {
			GuiTask guiTask = (GuiTask) iterator.next();
			addTask(guiTask);
		}

		threadList.itemAdded.addObserver(new Observer() {
			public void update(Observable arg0, Object object) {
				GuiTask guiTask = (GuiTask) object;
				addTask(guiTask);
			}
		});

		threadList.itemRemoved.addObserver(new Observer() {
			public void update(Observable arg0, Object object) {
				GuiTask guiTask = (GuiTask) object;
				removeTask(guiTask);
			}
		});
	}

	private void addTask(GuiTask guiTask) {

		// XXX: Change "other usefull..." to something more meaningfull.
		int trace = this.newTrace(guiTask.getTask().getName(),
				"Other useful per-trace information.");
		guiTask.setWidget(this, trace);

		this.procMap.put(new Integer(trace), guiTask);
		this.initEventViewer(guiTask);
	}

	private void removeTask(GuiTask guiTask) {
		int trace = guiTask.getTrace();
		viewer.deleteTrace(trace);
	}
	
	private void addObserverActionPoint(ObserverRoot observer, GuiData guiData)	{
		if (observer instanceof TaskSyscallObserver) {
			((TaskSyscallObserver)observer).enteringTaskActionPoint.addAction(new TimelineAction(
				observer, guiData));
		}
		
		if (observer instanceof TaskCloneObserver) {
			((TaskCloneObserver)observer).cloningTaskActionPoint.addAction(new TimelineAction(
					observer, guiData));
		}
		
		if (observer instanceof TaskExecObserver) {
			((TaskExecObserver)observer).taskActionPoint.addAction(new TimelineAction(
					observer, guiData));
		}
		
		if (observer instanceof TaskForkedObserver) {
			((TaskForkedObserver)observer).forkingTaskActionPoint.addAction(new TimelineAction(
					observer, guiData));
		}
		
		if (observer instanceof TaskTerminatingObserver) {
			((TaskTerminatingObserver)observer).taskActionPoint.addAction(new TimelineAction(
					observer, guiData));
		}

	}

	private void initEventViewer(final GuiData guiData) {
		ObservableLinkedList observers;
		if (guiData instanceof GuiTask)
			observers = ((GuiTask)guiData).getParent().getObservers();
		else
			observers = null;
			//observers = guiData.getObservers();
			// uncomment above line to add observers to proc view in timeline
		
		
		if (observers != null) {

			ListIterator iter = observers.listIterator();
			while (iter.hasNext()) {
				final ObserverRoot observer = (ObserverRoot) iter.next();
				addObserverActionPoint(observer,guiData);

				//observer.genericActionPoint.addAction(new TimelineAction(
				//	observer, guiData));
		
			}
		}
		guiData.getObservers().itemAdded.addObserver(new Observer() {
			GuiData realGuiData = guiData;
			public void update(Observable arg0, Object obj) {
				final ObserverRoot observer = (ObserverRoot) obj;
				addObserverActionPoint(observer,realGuiData);
//				observer.genericActionPoint.addAction(new TimelineAction(
//						observer, realGuiData));
			}
		});
	}

	public void setName(String name) {

	}

	public int getTrace0() {
		return initialTrace;
	}

  private static int count = 0;

  class TimelineAction
      extends TaskAction
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
      
      if  (legendAdded[observerglyph] == 0) {
	
	      viewer.addMarker(observerglyph, observer.getName(),
	                                       observer.getToolTip());
	      viewer.setMarkerColor(this.markerId, markerColors[observercolor]);
	      
      }
      
      legendAdded[observerglyph]++;

    }

  
	public void execute(Task task) {
		      
	      if (guiData instanceof GuiTask)
	    	  if (task.getTid() == ((GuiTask)guiData).getTask().getTid())
	    		  viewer.appendEvent(guiData.getTrace(), markerId,
	                         "Other useful per-event information.");
	}

	public String getArgument() {
		return null;
	}

	public ObservableLinkedList getArgumentCompletionList() {
		return null;
	}

	public GuiObject getCopy() {
		return null;
	}

	public boolean setArgument(String argument) {
		return false;
	}
  }
}
