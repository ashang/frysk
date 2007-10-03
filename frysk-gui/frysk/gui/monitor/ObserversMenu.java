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
package frysk.gui.monitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;


/**
 * This is the root menu that appears upon a right click
 * on the watch window. Only one of this object is created
 * and can be be accessed through accessor method for extension
 * or addition to WatchWindows.
 * */
public class ObserversMenu extends Menu{
	
	/** the TaskData of the currently selected task */
	private GuiTask currentTask;
	
	/** the ProcData of the currently selected process */
	private GuiProc currentProc;
	
	private HashMap map;
	
	 public ObserversMenu(ObservableLinkedList actions){
		super();
	
		this.map = new HashMap();
		ListIterator iter = actions.listIterator();
        while(iter.hasNext()){
            final ObserverRoot observer = (ObserverRoot) iter.next();
            this.addGuiObject(observer);
        }
        
		actions.itemAdded.addObserver(new Observer() {
			public void update(Observable observable, Object object) {
				ObserverRoot observer = (ObserverRoot)object;
				addGuiObject(observer);
			}
		});
		
		actions.itemRemoved.addObserver(new Observer() {
			public void update(Observable observable, Object object) {
				ObserverRoot observer = (ObserverRoot)object;
				removeGuiObject(observer);
			}
		});

		this.showAll();
	}
	
	private void addGuiObject(final GuiObject observer) {
	    MenuItem item = new MenuItem(observer.getName(), false);
		ToolTips tip = new ToolTips();
		tip.setTip(item, observer.getToolTip(), "");
		
		item.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent arg0) {
							
				if(currentTask != null)
					currentTask.add((TaskObserverRoot)observer);
			
				if(currentProc != null)
					currentProc.add((TaskObserverRoot)observer);
			}
		});

		this.add(item);
		this.map.put(observer.getName(), item);
        this.showAll();
	}

	private void removeGuiObject(final GuiObject observer) {
        MenuItem item = (MenuItem) this.map.get(observer.getName());
		this.remove(item);
		this.map.remove(observer.getName());
        this.showAll();
	}

	/**
	 * Show the pop-up menu. selected operation is to be 
	 * applied to process with id pid
	 * */
	public void popup(GuiProc selected){
		this.popup();
		this.currentProc = selected;
		this.currentTask = null;
	}
	
	public void popup(GuiTask selected){
		this.popup();
		this.currentTask = selected;
		this.currentProc = null;
	}
	
	public void setCurrentProc(GuiProc current){

		// Assign selected proc
		this.currentProc = current;
		

		// reset menu
		resetMenuSensitivity();
		
		// De-sensitize already added observers from session, or 
		// from previous user selections.
		ObservableLinkedList foo = this.currentProc.getObservers();
		Iterator i = foo.iterator();
		while (i.hasNext())	{
			GuiObject guiProc = ((GuiObject)i.next());
			MenuItem item = (MenuItem) this.map.get(guiProc.getName());
			item.setSensitive(false);
		}
	}
	
	public void setCurrentTask(GuiTask current){
		
		// Assign selected task
		this.currentTask = current;
		
		// Temporary menu item pointer.
		MenuItem item = null;
		
		// reset menu
		resetMenuSensitivity();
		
		// De-sensitize already added observers tasks observers from session, or 
		// from previous user selections.
		ObservableLinkedList foo = this.currentTask.getObservers();
		
		Iterator i = foo.iterator();
		while (i.hasNext())	{
			GuiObject guiTask = ((GuiObject)i.next());			
			item = (MenuItem) this.map.get(guiTask.getName());
			item.setSensitive(false);
		}
		
		
	}
	
	private void resetMenuSensitivity() {
		// Reset menu selection
		Iterator z = this.map.values().iterator();
		while (z.hasNext())
			((MenuItem)z.next()).setSensitive(true);
	}
}
