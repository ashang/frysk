package frysk.gui.druid;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

public class ProcessObserverSelectionTreeView extends TreeView {
	
	public ProcessObserverDataModel psDataModel = null; 
	
	public ProcessObserverSelectionTreeView(Handle handle, ProcessObserverDataModel model){
		super(handle);
		this.mountDataModel(model);
		this.psDataModel = model;
		this.getSelection().setMode(SelectionMode.MULTIPLE);
	}	
	
	
	
	private void mountDataModel(ProcessObserverDataModel dataModel){
		//this.setModel(dataModel.getModel());
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , dataModel.getNameDC());
		this.appendColumn(nameCol);
			
		this.setModel(dataModel.getModel());
	}
	

		
}
