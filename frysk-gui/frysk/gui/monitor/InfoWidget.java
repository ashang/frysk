/*
 * Created on Sep 19, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;

import org.gnu.gdk.Window;
import org.gnu.gtk.Notebook;

public class InfoWidget extends Notebook {
	private StatusWidget statusWidget;
	public Observable notifyUser;
	
	public InfoWidget(ProcData data){
		
		//Window myWindow = this.getWindow();
		
		this.notifyUser = new Observable();
		//========================================
		NotifyingLabel statusWidgetLabel = new NotifyingLabel("Status");
		this.statusWidget = new StatusWidget(data);
		this.statusWidget.notifyUser.addObserver(statusWidgetLabel);
		this.appendPage(statusWidget, statusWidgetLabel);		
		//========================================
				
		this.showAll();
	}
	
}
