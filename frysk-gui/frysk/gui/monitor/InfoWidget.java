/*
 * Created on Sep 19, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;

import org.gnu.gtk.Notebook;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;

public class InfoWidget extends Notebook {

	public Observable notifyUser;

	public VBox procStatusVbox;
	public VBox taskStatusVbox;
	
	public InfoWidget(){
		
		//Window myWindow = this.getWindow();
		
		this.setBorderWidth(4);
		this.notifyUser = new Observable();
		
		this.procStatusVbox = new VBox(false, 0);
		this.taskStatusVbox = new VBox(false, 0);
		
		//========================================
		VBox statusVbox = new VBox(true, 0);
		NotifyingLabel statusWidgetLabel = new NotifyingLabel("Status");
		statusVbox.packStart(procStatusVbox);
		statusVbox.packStart(taskStatusVbox);
		this.appendPage(statusVbox, statusWidgetLabel);		
		//========================================
	
		this.showAll();
	}
	
	public void setSelectedProc(ProcData data){
		Widget[] widgets = this.procStatusVbox.getChildren();
		if(widgets.length > 0){
			this.procStatusVbox.remove(widgets[0]);
		}
		this.procStatusVbox.add(data.getWidget());
	}
	
	public void setSelectedTask(TaskData data){
		Widget[] widgets = this.taskStatusVbox.getChildren();
		if(widgets.length > 0){
			this.taskStatusVbox.remove(widgets[0]);
		}
		this.taskStatusVbox.add(data.getWidget());
	}
	
}
