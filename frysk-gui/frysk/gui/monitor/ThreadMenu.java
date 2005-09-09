/*
 * Created on 7-Sep-05
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

public class ThreadMenu extends Menu {
	private static ThreadMenu menu = new ThreadMenu();
	
	/** the TaskData of the currently selected task */
	private TaskData current;
	
	ThreadMenu(){
		super();
		
		LinkedList list = ActionPool.theActionPool.threadActions;
		ListIterator iter = list.listIterator();
		
		while(iter.hasNext()){
			final Action action = (Action) iter.next();
			
			MenuItem item = new MenuItem(action.getName(), false);
			ToolTips tip = new ToolTips();
			tip.setTip(item, action.getToolTip(), "");
			
			item.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent event) {
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
					menu.setCurrentTask(current);
				}
				return false;
			}
		});
		
		this.add(item);
		this.showAll();
	}
	
	/**
	 * Singilton pattern. Get the menu from here
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
		System.out.println("-- PID: " + current.getTask().getPid());
	}
}
