package frysk.gui.druid;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;

public class ProcessExitSelectionTreeView extends TreeView {
	
	public ExitProcessGroupsDataModel psDataModel = null; 
	
	public ProcessExitSelectionTreeView(Handle handle, ExitProcessGroupsDataModel model){
		super(handle);
		this.mountDataModel(model);
		this.psDataModel = model;
		this.buildListeners();
		
		this.getSelection().setMode(SelectionMode.MULTIPLE);

	}	
	
	private void buildListeners()
	{
//		this.addListener(new TreeViewListener() {

//			public void treeViewEvent(TreeViewEvent arg0) {
//				if ( arg0.isOfType(TreeViewEvent.Type.ROW_ACTIVATED ))
//				{
//					TreeIter selected = arg0.getTreeIter();
//					boolean valueToggle = psDataModel.getModel().getValue(selected,psDataModel.getSelectedDC());
//					valueToggle = !valueToggle;
//					psDataModel.getModel().setValue(selected,psDataModel.getSelectedDC(),Boolean.valueOf(valueToggle).booleanValue());
//				}
//			}});
	}
	
	 void mountDataModel(ExitProcessGroupsDataModel dataModel){
		//this.setModel(dataModel.getModel());
		
		CellRendererToggle cellRendererToggle = new CellRendererToggle();
		cellRendererToggle.addListener(new CellRendererToggleListener() {

			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {
				TreeIter iter = psDataModel.getModel().getIter(arg0.getPath());
				boolean valueToggle = psDataModel.getModel().getValue(iter,psDataModel.getSelectedDC());
				valueToggle = !valueToggle;
				psDataModel.getModel().setValue(iter,psDataModel.getSelectedDC(),Boolean.valueOf(valueToggle).booleanValue());
			}
				
			});
		TreeViewColumn selectedCol = new TreeViewColumn();
		selectedCol.packStart(cellRendererToggle,false);
		selectedCol.addAttributeMapping( cellRendererToggle,
				 CellRendererToggle.Attribute.ACTIVE, dataModel.getSelectedDC());
		this.appendColumn(selectedCol);
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , dataModel.getNameDC());
		this.appendColumn(nameCol);
			
		this.setModel(dataModel.getModel());
	}


		
}
