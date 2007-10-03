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
 * Created on 7-Sep-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.ListIterator;

import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.gui.monitor.actions.ActionManager;
import frysk.gui.monitor.actions.TaskAction;
import frysk.gui.monitor.observers.ObserverManager;

public class ThreadMenu extends Menu {
	private static ThreadMenu menu = new ThreadMenu();
	
	/** the TaskData of the currently selected task */
	private TaskData current;
	
	ThreadMenu(){
		super();
		
		ObservableLinkedList list = ActionManager.theManager.getTaskActions();
		ListIterator iter = list.listIterator();
		
		while(iter.hasNext()){
			final TaskAction action = (TaskAction) iter.next();
			
			MenuItem item = new MenuItem(action.getName(), false);
			ToolTips tip = new ToolTips();
			tip.setTip(item, action.getToolTip(), ""); //$NON-NLS-1$
			
			item.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent event) {
					action.execute(current.getTask());
				}
			});
			this.add(item);
		}
		
		final ObserversMenu menu = new ObserversMenu(ObserverManager.theManager.getTaskObservers());
		MenuItem item = new MenuItem("Add observer ", false); //$NON-NLS-1$
		item.setSubmenu(menu);

		item.addListener(new MouseListener(){
			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.ENTER){
					menu.setCurrentTask(current);
				}
				return false;
			}
		});
		
		this.add(item);
		this.showAll();
	}
	
	/**
	 * Singelton pattern. Get the menu from here
	 * and add it to any watch window or add more
	 * items to it.
	 * */
	public static ThreadMenu getMenu(){
		return menu;
	}
	
	/**
	 * Show the popup menu. selected operation is to be 
	 * applied to process with id pid
	 * */
	public void popup(TaskData selected){
		this.popup();
		this.current = selected;
		System.out.println("-- PID: " + current.getTask().getTid()); //$NON-NLS-1$
	}
}
