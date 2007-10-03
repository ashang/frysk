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
// type filter text
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

package frysk.gui.monitor;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 *  Provides a detailed view of the observers currently available
 *  in the system.
 *  TODO: add more details to this widget.
 * */
public class DetailedObserverTreeView extends TreeView {

	ListStore listStore;
	DataColumnString nameDC;
	DataColumnObject observersDC;
	
	public DetailedObserverTreeView() {
		super();
		this.initTreeView();
	}

	public DetailedObserverTreeView(Handle handle) {
		super(handle);
		this.initTreeView();
	}

	private void initTreeView() {
		
		this.nameDC = new DataColumnString();
		this.observersDC = new DataColumnObject();
		
		DataColumn[] columns = new DataColumn[2];
		columns[0] = nameDC;
		columns[1] = observersDC;
		listStore = new ListStore(columns);
		
		this.setHeadersVisible(false);
		
		this.setModel(listStore);
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn observersCol = new TreeViewColumn();
		observersCol.packStart(cellRendererText, false);
		observersCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		this.appendColumn(observersCol);
		
		this.populateList();
	}
	
	private void populateList(){
		Iterator iter = ObserverManager.theManager.getObservers().iterator();
		while(iter.hasNext()){
			ObserverRoot observer = (ObserverRoot) iter.next();
			TreeIter treeIter = listStore.appendRow();
			listStore.setValue(treeIter, nameDC, observer.getName());
			listStore.setValue(treeIter, observersDC, observer);
		}
		
		// handle add events
		ObserverManager.theManager.addObserver(new Observer(){
			public void update(Observable observable, Object obj) {
				ObserverRoot observer = (ObserverRoot) obj;
				TreeIter iter = listStore.appendRow();
				listStore.setValue(iter, nameDC, observer.getName());
				listStore.setValue(iter, observersDC, observer);
			}
		});
	}
	
	public ObserverRoot getSelectedObserver(){
		ObserverRoot selected = null;
		
		if(this.getSelection().getSelectedRows().length > 0){
			selected = (ObserverRoot) this.listStore.getValue(this.listStore.getIter(this.getSelection().getSelectedRows()[0]), observersDC);
		}
		
		return selected;
	}
	
}
