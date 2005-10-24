/*
 * Created on Oct 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Window;

public class ProgramAddWindow extends Window {
	
	public ProgramAddWindow(LibGlade glade){
		super(((Window)glade.getWidget("programAddWindow")).getHandle());
	}
}
