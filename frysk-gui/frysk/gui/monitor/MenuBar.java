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

/**
 * provides a handle for the windows menu bar.
 * Extending the meunu bar and attaching listiners to 
 * MenuItems will be done through here.
 * */
public class MenuBar extends org.gnu.gtk.MenuBar {
	
	public MenuBar(LibGlade glade){
		super((glade.getWidget("menuBar")).getHandle());
		MenuItem item = (MenuItem) glade.getWidget("prefrencesMenuItem");
		item.addListener(new MenuItemListener(){
			public void menuItemEvent(MenuItemEvent arg0) {
				WindowManager.theManager.prefsWindow.showAll();
			}
		});
	}
	
}
