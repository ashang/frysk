/**
 * 
 */
package frysk.gui.srcwin.prefs;

import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

/**
 * @author ajocksch
 * 
 */
public class PreferenceList extends TreeView implements TreeSelectionListener {

	private DataColumn[] cols = { new DataColumnString(),
			new DataColumnObject() };

	private PreferenceViewer parent;

	public PreferenceList(PreferenceViewer parent, String[] names) {
		this.setHeadersVisible(false);
		this.getSelection().setMode(SelectionMode.SINGLE);

		this.parent = parent;

		ListStore model = new ListStore(cols);
		for (int i = 0; i < names.length; i++) {
			TreeIter iter = model.appendRow();
			FryskPreference synPref = PreferenceManager.getPreference(names[i]);
			model.setValue(iter, (DataColumnString) cols[0], synPref.getName());
			model.setValue(iter, (DataColumnObject) cols[1], synPref);
		}

		this.setModel(model);

		TreeViewColumn col = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				cols[0]);
		this.appendColumn(col);

		this.getSelection().addListener(this);
	}

	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		TreePath path = this.getSelection().getSelectedRows()[0];

		TreeIter iter = this.getModel().getIter(path);

		ColorPreference pref = (ColorPreference) this.getModel().getValue(
				iter, (DataColumnObject) this.cols[1]);

		this.parent.showPreferenceEditor(pref);
	}

	public void saveAll() {
		TreeIter iter = this.getModel().getFirstIter();
		int index = 1;
		
		while (iter != null) {
			FryskPreference pref = (FryskPreference) this.getModel()
					.getValue(iter, (DataColumnObject) this.cols[1]);
			pref.saveValues();
			iter = this.getModel().getIter(new TreePath(""+(++index)));
		}
	}
}
