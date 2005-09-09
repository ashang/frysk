/*
 * Created on 22-Jun-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.ListIterator;

import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.gui.monitor.ActionPool.Action;


/**
 * This is the root menu that appears upon a right click
 * on the watch window. Only one of this object is created
 * and can be be accessed through accessor method for extention
 * or addition to WatchWindows.
 * */
public class WatchMenu extends Menu{
	private static WatchMenu menu = new WatchMenu();
	
	/** the ProcData of the currently selected process */
	private ProcData current;
	
	WatchMenu(){
		super();
		
		LinkedList list = ActionPool.theActionPool.processActions;
		ListIterator iter = list.listIterator();
		
		while(iter.hasNext()){
			final Action action = (Action) iter.next();
			//System.out.println(action.getName());
			
			MenuItem item = new MenuItem(action.getName(), false);
			ToolTips tip = new ToolTips();
			tip.setTip(item, action.getToolTip(), "");
			
			item.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent arg0) {
					action.execute(current);
				}
			});
			this.add(item);
		}
		
		final ObserversMenu menu = new ObserversMenu();
		MenuItem item = new MenuItem("Add observer ", false);
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
	 * Singilton pattern. Get the menu from here
	 * and add it to any watch window or add more
	 * items to it.
	 * */
	public static WatchMenu getMenu(){
		return menu;
	}
	
	/**
	 * Show the popup menu. selected operation is to be 
	 * applied to process with id pid
	 * */
	public void popup(ProcData selected){
		this.popup();
		this.current = selected;
		System.out.println("-- PID: " + current.getProc().getPid());
	}
	
}
