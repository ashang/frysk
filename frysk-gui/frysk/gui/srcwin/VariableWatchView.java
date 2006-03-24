package frysk.gui.srcwin;

import java.util.Iterator;
import java.util.Vector;

import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

public class VariableWatchView extends TreeView implements TreeSelectionListener {

	public interface WatchViewListener{
		void variableSelected(Variable var);
	}
	
	private DataColumn[] traceColumns;
	
	private Vector observers;
	
	public VariableWatchView(){
		super();
		
		this.observers = new Vector();
		
		traceColumns = new DataColumn[] {new DataColumnString(), new DataColumnString(), new DataColumnObject()};
		
		ListStore store = new ListStore(traceColumns);
		
		this.setModel(store);
		
		TreeViewColumn column = new TreeViewColumn();
		column.setTitle("Name");
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				traceColumns[0]);
		this.appendColumn(column);
		
		column = new TreeViewColumn();
		column.setTitle("Value");
		renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				traceColumns[1]);
		this.appendColumn(column);
		
		this.getSelection().setMode(SelectionMode.SINGLE);
		
		this.getSelection().addListener(this);
	}

	public void addTrace(Variable var){
		ListStore store = (ListStore) this.getModel();
		
		TreeIter iter = store.appendRow();
		store.setValue(iter, (DataColumnString) this.traceColumns[0], var.getName());
		store.setValue(iter, (DataColumnString) this.traceColumns[1], "0xfeedcalf");
		store.setValue(iter, (DataColumnObject) this.traceColumns[2], var);
		
		this.showAll();
	}
	
	public void addObserver(WatchViewListener listener){
		this.observers.add(listener);
	}
	
	private void notifyListeners(Variable var){
		Iterator iter = this.observers.iterator();
		
		while(iter.hasNext())
			((WatchViewListener) iter.next()).variableSelected(var);
	}
	
	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		ListStore store = (ListStore) this.getModel();
		
		TreeIter selected = store.getIter(this.getSelection().getSelectedRows()[0]);
		
		this.notifyListeners((Variable) store.getValue(selected, (DataColumnObject) this.traceColumns[2]));
	}
	
}
