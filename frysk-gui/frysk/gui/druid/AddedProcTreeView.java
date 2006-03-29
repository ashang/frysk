package frysk.gui.druid;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import frysk.gui.monitor.ProcWiseDataModel;

public class AddedProcTreeView extends TreeView {
	
	public ProcWiseDataModel psDataModel = null; 
	private TreeModelFilter procFilter;
	
	public AddedProcTreeView(Handle handle, ProcWiseDataModel model){
		super(handle);
		this.mountDataModel(model);
		this.psDataModel = model;
	}
	
	
	
	
	private void mountDataModel(ProcWiseDataModel dataModel){
		//this.setModel(dataModel.getModel());
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , dataModel.getNameDC());
		this.appendColumn(nameCol);
		
		this.procFilter = new TreeModelFilter(dataModel.getModel());
		
		procFilter.setVisibleMethod(new TreeModelFilterVisibleMethod(){

			public boolean filter(TreeModel model, TreeIter iter) {

				if(model.getValue(iter, psDataModel.getSelectedDC()) == true){
					return true;
				}else{
					return false;
				}
			}
			
		});

		
		this.setModel(procFilter);
	}
	

		
}
