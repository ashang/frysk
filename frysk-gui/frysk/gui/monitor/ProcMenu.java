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
 * Created on 22-Jun-05
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
import frysk.gui.monitor.actions.ProcAction;
import frysk.gui.monitor.PIDColumnDialog;
import frysk.gui.monitor.observers.ObserverManager;


/**
 * This is the root menu that appears upon a right click
 * on the process view. Only one of this object is created
 * and can be be accessed through accessor method for extention
 * or addition to WatchWindows.
 * */
public class ProcMenu extends Menu
{
	
	/** the ProcData of the currently selected process */
	private GuiProc current;
    
    private PIDColumnDialog pidColumnDialog;
	
	ProcMenu(PIDColumnDialog pcd)
    {
		super();
		
		ObservableLinkedList list = ActionManager.theManager.getProcActions();
		ListIterator iter = list.listIterator();
        
        this.pidColumnDialog = pcd;
        MenuItem item = new MenuItem("Edit Columns...", false);
        ToolTips tip = new ToolTips();
        tip.setTip(item, "Edit Columns...", "");

        item.addListener(new MenuItemListener()
        {
          public void menuItemEvent (MenuItemEvent arg0)
          {
            pidColumnDialog.showAll();
          }
        });
        
        this.add(item);
		
		while(iter.hasNext()){
			final ProcAction action = (ProcAction) iter.next();
			//System.out.println(action.getName());
			
			item = new MenuItem(action.getName(), false);
			tip = new ToolTips();
			tip.setTip(item, action.getToolTip(), ""); //$NON-NLS-1$
			
			item.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent arg0) {
					action.execute(current.getProc());
				}
			});
			this.add(item);
		}
		
		final ObserversMenu menu = new ObserversMenu(ObserverManager.theManager.getTaskObservers());
		item = new MenuItem("Add observer ", false); //$NON-NLS-1$
		item.setSubmenu(menu);

		item.addListener(new MouseListener(){
			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.ENTER){
					menu.setCurrentProc(current);
				}
				return false;
			}
		});
		
		this.add(item);

		this.current = null;
		
		this.showAll();
	}
	
	/**
	 * Show the popup menu. selected operation is to be 
	 * applied to process with id pid
	 * */
	public void popup(GuiProc selected){
		this.popup();
		this.current = selected;
		//System.out.println("-- PID: " + current.getProc().getPid()); //$NON-NLS-1$
	}
	
}
