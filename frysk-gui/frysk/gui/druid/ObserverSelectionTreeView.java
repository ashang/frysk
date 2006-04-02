package frysk.gui.druid;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

public class ObserverSelectionTreeView extends TreeView {
	
	public ObserverDataModel psDataModel = null; 
	
	public ObserverSelectionTreeView(Handle handle, ObserverDataModel model){
		super(handle);
		this.mountDataModel(model);
		this.psDataModel = model;
		this.getSelection().setMode(SelectionMode.MULTIPLE);
	}	
	
	
	
	private void mountDataModel(ObserverDataModel dataModel){
		//this.setModel(dataModel.getModel());
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , dataModel.getNameDC());
		this.appendColumn(nameCol);
			
		this.setModel(dataModel.getModel());
	}
	

		
}
