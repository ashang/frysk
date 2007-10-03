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

import java.util.LinkedList;
import java.util.ListIterator;

import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.gui.monitor.ActionPool.Action;


/**
 * This is the root menu that appears upon a right click
 * on the watch window. Only one of this object is created
 * and can be be accessed through accessor method for extention
 * or addition to WatchWindows.
 * */
public class ObserversMenu extends Menu{
	
	/** the TaskData of the currently selected task */
	private TaskData currentTask;
	
	/** the ProcData of the currently selected process */
	private ProcData currentProc;
	
	public ObserversMenu(LinkedList actions){
		super();
		
		//LinkedList list = ActionPool.theActionPool.processObservers;
		ListIterator iter = actions.listIterator();
		
		while(iter.hasNext()){
			final Action action = (Action) iter.next();
		//	System.out.println(action.getName());
			
			MenuItem item = new MenuItem(action.getName(), false);
			ToolTips tip = new ToolTips();
			tip.setTip(item, action.getToolTip(), "");
			
			item.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent arg0) {
					if(currentTask != null){ action.execute(currentTask); }
					if(currentProc != null){ action.execute(currentProc); }
				}
			});
			this.add(item);
		}

		this.showAll();
	}
	
	/**
	 * Show the popup menu. selected operation is to be 
	 * applied to process with id pid
	 * */
	public void popup(ProcData selected){
		this.popup();
		this.currentProc = selected;
	}
	
	public void popup(TaskData selected){
		this.popup();
		this.currentTask = selected;
	}
	
	public void setCurrentProc(ProcData current){
		System.out.println("setCurrentProc");
		this.currentProc = current;
	}
	
	public void setCurrentTask(TaskData current){
		System.out.println("setCurrentProc");
		this.currentTask = current;
	}
}
