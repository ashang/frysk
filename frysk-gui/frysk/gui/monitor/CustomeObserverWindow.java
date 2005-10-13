/*
 * Created on Oct 13, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Window;

public class CustomeObserverWindow extends Window {
	
	public CustomeObserverWindow(LibGlade glade){
		super(((Window)glade.getWidget("customeObserverWindow")).getHandle());
	}
}
