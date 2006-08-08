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
 * Created on Oct 6, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.gui.Gui;

/**
 * provides a handle for the windows menu bar.
 * Extending the meunu bar and attaching listiners to 
 * MenuItems will be done through here.
 * */
public class MenuBar extends org.gnu.gtk.MenuBar {
	
	public MenuBar(LibGlade glade){
		super((glade.getWidget("menuBar")).getHandle());
//		MenuItem item = (MenuItem) glade.getWidget("prefrencesMenuItem");
//		item.addListener(new MenuItemListener(){
//			public void menuItemEvent(MenuItemEvent arg0) {
//				WindowManager.theManager.prefsWindow.showAll();
//				WindowManager.theManager.prefsWindow.present();
//			}
//		});

		MenuItem item;
		
		item = (MenuItem) glade.getWidget("quitMenuItem");
		item.addListener(new MenuItemListener(){
			public void menuItemEvent(MenuItemEvent arg0) {
				Gui.quitFrysk();
			}
		});
		
		item = (MenuItem) glade.getWidget("customObserversMenuItem");
		item.addListener(new MenuItemListener(){
			public void menuItemEvent(MenuItemEvent arg0) {
				WindowManager.theManager.observersDialog.showAll();
				WindowManager.theManager.observersDialog.present();
			}
		});
		
		item = (MenuItem) glade.getWidget("closeMenuItem");
		item.addListener(new MenuItemListener(){
			public void menuItemEvent(MenuItemEvent arg0) {
				WindowManager.theManager.mainWindow.hideAll();
				WindowManager.theManager.logWindow.hideAll();
				WindowManager.theManager.prefsWindow.hideAll();
			}
		});
		
		item = (MenuItem) glade.getWidget("aboutFrysk");
		item.addListener(new MenuItemListener(){
			public void menuItemEvent(MenuItemEvent arg0) {
				WindowManager.theManager.aboutWindow.showAll();
			}
		});

        item = (MenuItem) glade.getWidget("programObserverMenuItem");
        item.addListener(new MenuItemListener(){
            public void menuItemEvent(MenuItemEvent event) {
                WindowManager.theManager.programObserverDialog.showAll();
            }
        });

	}
	
}
