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
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.dom.DOMLine;

public class CurrentStackView extends TreeView implements TreeSelectionListener {

	public interface StackViewListener{
		void currentStackChanged(StackLevel newLevel);
	}
	
	private DataColumn[] stackColumns;
	
	private StackLevel currentLevel;
	
	private Vector observers;
	
	public CurrentStackView(StackLevel topLevel) {
		super();

		this.observers = new Vector();
		
		stackColumns = new DataColumn[] { new DataColumnString(),
				new DataColumnObject() };
		ListStore listModel = new ListStore(stackColumns);

		TreeIter iter = null;
		TreeIter last = null;

		while (topLevel != null) {
			iter = listModel.appendRow();

			CurrentLineSection current = topLevel.getCurrentLine();
			boolean hasInlinedCode = false;
				
			// Go through each segment of the current line, but once we've found
			// one stop checking
			while(current != null && !hasInlinedCode){
				// Go through each line of the segment
				for(int i = current.getStartLine(); i < current.getEndLine(); i++){
					// Check for inlined code
					DOMLine line = topLevel.getData().getLine(i);
					if(line != null && line.hasInlinedCode()){
						hasInlinedCode = true;
						break;
					}
				}
				
				current = current.getNextSection();
			}
				
			// If we've found inlined code, update the display
			if(hasInlinedCode)
				listModel.setValue(iter, (DataColumnString) stackColumns[0], topLevel
						.getData().getFileName()
						+ "  (i)");
			else
				listModel.setValue(iter, (DataColumnString) stackColumns[0], topLevel
						.getData().getFileName());
				
			listModel.setValue(iter, (DataColumnObject) stackColumns[1], topLevel);

			// Save the last node so we can select it
			if (topLevel.getNextScope() == null){
				currentLevel = topLevel;
				last = iter;
			}

			topLevel = topLevel.getNextScope();
		}
		this.setModel(listModel);

		TreeViewColumn column = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				stackColumns[0]);
		this.appendColumn(column);
		
		this.getSelection().setMode(SelectionMode.SINGLE);
		this.getSelection().select(last);
		
		this.getSelection().addListener(this);
	}

	/**
	 * 
	 * @return The currently selected stack level
	 */
	public StackLevel getCurrentLevel() {
		return currentLevel;
	}

	public void addListener(StackViewListener listener){
		this.observers.add(listener);
	}
	
	private void notifyObservers(StackLevel newStack){
		Iterator iter = this.observers.iterator();
		
		while(iter.hasNext()){
			((StackViewListener) iter.next()).currentStackChanged(newStack);
		}
	}
	
	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		TreeModel model = this.getModel();

		TreePath[] paths = this.getSelection().getSelectedRows();
		if(paths.length == 0)
			return;
		
		StackLevel selected = (StackLevel) model.getValue(model.getIter(paths[0]),
				(DataColumnObject) stackColumns[1]);
		
		this.notifyObservers(selected);
	}
	
}
