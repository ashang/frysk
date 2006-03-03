package frysk.gui.common.prefs;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Alignment;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

public class PreferenceWindow extends Window implements TreeSelectionListener{

	private LibGlade glade;
	private TreeView prefView;
	
	private DataColumn[] cols = {new DataColumnString(), new DataColumnObject()};
	
	public PreferenceWindow(LibGlade glade){
		super(((Window) glade.getWidget("prefWin")).getHandle());
		this.glade = glade;
		this.prefView = (TreeView) this.glade.getWidget("preferenceTree");
		
		this.setupPreferenceTree();
	}
	
	private void setupPreferenceTree(){
		TreeStore model = new TreeStore(cols);
		Iterator groups = PreferenceManager.getPreferenceGroups();
		
		model.setSortColumn(cols[0], SortType.ASCENDING);
		
		while(groups.hasNext()){
			PreferenceGroup group = (PreferenceGroup) groups.next();
			
			TreeIter groupIter = model.appendRow(null);
			model.setValue(groupIter, (DataColumnString) cols[0], group.getName());
			model.setValue(groupIter, (DataColumnObject) cols[1], group);

		}
		
		this.prefView.setModel(model);
		
		TreeViewColumn column = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				cols[0]);
		this.prefView.appendColumn(column);
		
		this.prefView.getSelection().unselectAll();
		this.prefView.getSelection().setMode(SelectionMode.SINGLE);
		this.prefView.getSelection().addListener(this);
	}

	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		TreePath[] paths = this.prefView.getSelection().getSelectedRows();
		
		if(paths.length == 0 || paths[0].getDepth() < 1)
			return;
		
		TreeModel model = this.prefView.getModel();
		
		TreeIter selectedRow = model.getIter(paths[0]);
		PreferenceGroup group = (PreferenceGroup) model.getValue(selectedRow, (DataColumnObject) cols[1]);
		
		VBox box = (VBox) this.glade.getWidget("preferenceArea");
		Widget[] children = box.getChildren();
		for(int i = 0; i < children.length; i++)
			box.remove(children[i]);
		
		Iterator prefs = group.getPreferences();
		while(prefs.hasNext()){
			FryskPreference pref = (FryskPreference) prefs.next();
			PreferenceEditor editor = new PreferenceEditor(pref);
			Alignment align = new Alignment(0,0,1.0,0.0);
			align.add(editor);
			box.packStart(align, true, true, 12);
		}
		
		this.showAll();
	}
	
}
