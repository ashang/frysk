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
	private static ObserversMenu menu = new ObserversMenu();
	
	/** the TaskData of the currently selected task */
	private TaskData currentTask;
	
	/** the ProcData of the currently selected process */
	private ProcData currentProc;
	
	public ObserversMenu(){
		super();
		
		LinkedList list = ActionPool.theActionPool.processObservers;
		ListIterator iter = list.listIterator();
		
		while(iter.hasNext()){
			final Action action = (Action) iter.next();
			//System.out.println(action.getName());
			
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
	 * Singilton pattern. Get the menu from here
	 * and add it to any watch window or add more
	 * items to it.
	 * */
	public static ObserversMenu getMenu(){
		return menu;
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
