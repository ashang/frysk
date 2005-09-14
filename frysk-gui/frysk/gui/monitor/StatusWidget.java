/*
 * Created on Sep 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Label;
import org.gnu.gtk.Widget;

public class StatusWidget extends Widget{

	Label nameLabel;
	public StatusWidget(LibGlade glade) throws ClassNotFoundException {
		super(glade.getWidget("statusWidget").getHandle());
		this.nameLabel = (Label)glade.getWidget("nameLabel");
	}
	
	public void setName(String name){
		this.nameLabel.setText(name);
	}
	
}
