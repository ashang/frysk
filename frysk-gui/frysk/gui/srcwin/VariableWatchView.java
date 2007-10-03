// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

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
		
		this.setName("varWatchView");
		this.getAccessible().setName("varWatchView_variableWatchList");
		this.getAccessible().setDescription("A list of all the variables that are being watched");
		
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
