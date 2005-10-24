/*
 * Created on Oct 20, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.io.IOException;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.FileChooserAction;
import org.gnu.gtk.FileChooserDialog;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.DialogEvent;
import org.gnu.gtk.event.DialogListener;

/**
 * A widget representing the program view page.
 * */
public class ProgramViewPage extends Widget {
	
	private TreeView programTreeView;
	private FileChooserDialog fileChooserDialog;
	
	private static final int RESPONCE_OK     = 0;
	private static final int RESPONCE_CANCEL = 1;
	
	public ProgramViewPage(LibGlade glade) throws IOException {
		super((glade.getWidget("programVBox")).getHandle());
		
		this.programTreeView = (TreeView) glade.getWidget("programTreeView");
		Button browseButton = (Button) glade.getWidget("programBrowseButton");
		this.fileChooserDialog = new FileChooserDialog("Choose a program on observe", (Window)getToplevel(), FileChooserAction.ACTION_OPEN);
		this.fileChooserDialog.addButton(GtkStockItem.OK, RESPONCE_OK);
		this.fileChooserDialog.addButton(GtkStockItem.CANCEL, RESPONCE_CANCEL);
		
		browseButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					fileChooserDialog.run();
					System.out.println("File name: " + fileChooserDialog.getFilenames());
				}
			}
		});
		
		this.fileChooserDialog.addListener(new DialogListener(){
			public boolean dialogEvent(DialogEvent event) {
			System.out.println("event: " + event + " RESPONCE: " + event.getResponse());
				if(event.getType() == DialogEvent.Type.RESPONSE){
					fileChooserDialog.hide();
				}
				return false;
			}
		});
				
		this.initProgramTreeView();
		
	}

	private void initProgramTreeView() {
					
	}
	
	
}
