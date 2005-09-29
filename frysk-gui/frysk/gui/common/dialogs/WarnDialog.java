/*
 * Created on Sep 26, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.common.dialogs;

import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.Label;
import org.gnu.gtk.event.DialogEvent;
import org.gnu.gtk.event.DialogListener;

public class WarnDialog extends Dialog{

	public WarnDialog(String message){
		super();
		this.addButton(GtkStockItem.OK, 1);
		this.getDialogLayout().add(new Label(message));
		this.addListener(new DialogListener(){
			public boolean dialogEvent(DialogEvent arg0) {
				hideAll();
				return false;
			}
		});
		
	}
	
}
