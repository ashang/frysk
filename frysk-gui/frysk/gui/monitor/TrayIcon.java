// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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
import java.util.List;
import java.util.prefs.Preferences;

import org.gnu.gtk.EventBox;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Menu;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.frysk.EggTrayIcon;

import frysk.gui.common.IconManager;


/**
 * TrayIcon is intended as a wrapper class for EggTrayIcon, and provides some useful 
 * functionalities such as facilitating popup menus and opening/minimizing windows
 */

public class TrayIcon implements Saveable{
	public static int NO_BUTTON = 0;
	public static int BUTTON_1 = 1;
	public static int BUTTON_2 = 2;
	public static int BUTTON_3 = 3;
	
	private EggTrayIcon tray;
	private LinkedList popupWindows;
	private Menu popupMenu;
	
	private EventBox trayItem;
	
	private String tooltip;
	private ToolTips tips;
	
	private boolean active;
	
	private int windowButton;
	private int menuButton;

	/*
	 * Constructors
	 */
	
	/**
     * Creates a new TrayIcon
     * @param tooltip the tooltip to display when you hover over the icon
     * @param active whether the icon should be animated or not 
	 */
	public TrayIcon(String tooltip, boolean active){
        this.tips = new ToolTips();
        
        this.tooltip = tooltip;
		tray = new EggTrayIcon(null);
		this.clearPopups();
		if(!active)
			this.setContents(new Image(new GtkStockItem("frysk-tray-24"), IconSize.BUTTON)); //$NON-NLS-1$
		else
			this.setContents(new Image(IconManager.anim));
		this.active = active;
		windowButton = 0;
		menuButton = 0;
		tray.showAll();
		this.setListener();
	}
	
	/*
	 * Public Methods
	 */
	
	/**
	 * Adds a popup menu to be shown when the user clicks the given mouse button
	 * @param popup The menu to be shown
	 */
	public void setPopupMenu(Menu popup){
		popupMenu = popup;
	}
	
	/**
	 * Adds a new window to be triggered when the appropriate mouse button
	 * is pressed
	 * @param popup The window to be added
	 */
	public void addPopupWindow(final Window popup){	
		popup.addListener(new LifeCycleListener() {
			
				public boolean lifeCycleQuery(LifeCycleEvent arg0) {
					if(arg0.isOfType(LifeCycleEvent.Type.DELETE) ||
					   arg0.isOfType(LifeCycleEvent.Type.DESTROY)){
						popup.hideAll();
						return true;
					}
					
					return false;
				}
			
				public void lifeCycleEvent(LifeCycleEvent arg0) {}
			});
		
		popupWindows.add(popup);
	}
	
	/**
	 * Sets the windows to be displayed to be exactly the contents of popups. Every
	 * element in popups must be of type org.gnu.gtk.Window
	 * @param popups The popups to be displayed
	 */
	public void setPopupWindows(List popups){
		popupWindows = new LinkedList(popups);
	}
	
	/**
	 * Sets the button that can be used to open the popup menu. If the button is already in use
	 * the previous item is cleared to NO_BUTTON
	 * @param button The button to trigger the menu
	 * @throws IllegalArgumentException If button is not one of BUTTON_1, BUTTON_2, BUTTON_3, or NO_BUTTON
	 */
	public void setMenuButton(int button) throws IllegalArgumentException{
		if(button < 0 || button > BUTTON_3)
			throw new IllegalArgumentException("Button must be one of BUTTON_1, BUTTON_2, BUTTON_3, or NO_BUTTON"); //$NON-NLS-1$
		
		if(button == windowButton)
			windowButton = NO_BUTTON;
		
		menuButton = button;
	}

	/**
	 * Sets the button that can be used to open the windows associated with the icon. If the 
	 * give button is already in use, the previous action is cleared to NO_BUTTON
	 * @param button The button to trigger showing the window
	 * @throws IllegalArgumentException If button is not one of BUTTON_1, BUTTON_2, BUTTON_3, or NO_BUTTON
	 */
	public void setWindowButton(int button) throws IllegalArgumentException{
		if(button < 0 || button > BUTTON_3)
			throw new IllegalArgumentException("Button must be one of BUTTON_1, BUTTON_2, BUTTON_3, or NO_BUTTON"); //$NON-NLS-1$
		
		if(button == menuButton)
			menuButton = NO_BUTTON;
		
		windowButton = button;
	}
	
	/**
	 * Returns the button that is currently set to display the popup window(s)
	 * @return The assigned button, either NO_BUTTON, BUTTON_1, BUTTON_2, or BUTTON_3
	 */
	public int getWindowButton(){
		return windowButton;
	}
	
	/**
	 * Returns the button that is currently set to display the popup menu
	 * @return The assigned button, either NO_BUTTON, BUTTON_1, BUTTON_2, or BUTTON_3
	 */
	public int getMenuButton(){
		return menuButton;
	}
	
	/**
	 * Clears the popup windows and the popup menu
	 */
	public void clearPopups(){
		popupWindows = new LinkedList();
		popupMenu = null;
	}
	
	/*
	 * Private Methods
	 */
	
	/*
	 * Sets the button in the system tray with the given text and Icon
	 */
	private void setContents(Image icon){
		// First clear out anything that was in the system tray, if anything
		if(trayItem != null){
			Widget[] inTray = trayItem.getChildren();
			
			for(int i = 0; i < inTray.length; i++)
				trayItem.remove(inTray[i]);
		}
		else{
			trayItem = new EventBox();
		}
		
		trayItem.add(icon);
        tips.setTip(trayItem, this.tooltip, ""); //$NON-NLS-1$
		
		if(trayItem.getParent() == null)
			tray.add(trayItem);
		
		tray.showAll();
	}
	
	/*
	 * Sets up the listeners so that the appropriate buttons open the menus and
	 * windows
	 */
	private void setListener(){
		trayItem.addListener(new MouseListener() {
		
			public boolean mouseEvent(MouseEvent arg0) {
				
				if(arg0.getButtonPressed() == menuButton && popupMenu != null){
					popupMenu.popup();
					popupMenu.showAll();
				}
				
				if(arg0.getButtonPressed() == windowButton && popupWindows.size() != 0){
					for(int i = 0; i < popupWindows.size(); i++){
						((Window)popupWindows.get(i)).showAll();
						((Window)popupWindows.get(i)).deiconify();
						((Window)popupWindows.get(i)).present();
					}
                    setActive(false, tooltip);
                }
                return false;
			}	
		
		});
	}

	public void save(Preferences prefs) {
	    throw new RuntimeException ("Auto-generated method stub");
	}

	public void load(Preferences prefs) {
	    throw new RuntimeException ("Auto-generated method stub");
	}

	public boolean isActive() {
		return active;
	}

    public void setActive(boolean active, String tooltip) {
      //this.tips = new ToolTips();
      tips.setTip(tray, tooltip, "");
      this.active = active;
        
		if(!this.active)
			this.setContents(new Image(new GtkStockItem("frysk-tray-24"), IconSize.BUTTON)); //$NON-NLS-1$
		else
			this.setContents(new Image(IconManager.anim));
	}
}
