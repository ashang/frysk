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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 *  Provides a detailed view of the observers currently available
 *  in the system.
 *  TODO: add more details to this widget.
 * */
public class DetailedObserverTreeView extends TreeView implements Observer {

	private ObservableLinkedList observerList;
	
	private TreeStore treeStore;
	private DataColumnString nameDC;
	private DataColumnObject objectDC;
	
	private HashMap map;
	
	public DetailedObserverTreeView() {
		super();
		this.init();
	}

	public DetailedObserverTreeView(Handle handle) {
		super(handle);
		this.init();
	}

	private void init() {	
		this.map = new HashMap();
		this.nameDC = new DataColumnString();
		this.objectDC = new DataColumnObject();
		this.treeStore = new TreeStore(new DataColumn[]{nameDC,objectDC});
		this.setModel(treeStore);
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		this.appendColumn(nameCol);
		
		this.observerList = ObserverManager.theManager.getTaskObservers();
		
		this.addObserverList(observerList);
	}
	
	private void addObserverList(ObservableLinkedList list) {
		this.addList(null, list);
		
		Iterator iterator = list.iterator();
		
		list.itemAdded.addObserver(new Observer() {
			public void update(Observable observable, Object object) {
				addObserverRoot((ObserverRoot) object);
			}
		});
		
		list.itemRemoved.addObserver(new Observer() {
			public void update(Observable arg0, Object object) {
				remove((GuiObject) object);
				//XXX: Not implemented.
				throw new RuntimeException("Not implemented");
			}
		});
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			addObserverRoot((ObserverRoot) object);
		}
	}

	private void addObserverRoot(ObserverRoot observer){
		GuiObject label = new GuiObject("FilterPoints","");
		this.add(label, observer);
		addList(label, observer.getFilterPoints());
		
		label = new GuiObject("ActionPoints","");
		this.add(label, observer);
		addList(label, observer.getActionPoints());
	}
	
	private void addList(final GuiObject parent, final ObservableLinkedList list){
		Iterator iterator = list.iterator();
		
		list.itemAdded.addObserver(new Observer() {
			public void update(Observable observable, Object object) {
				GuiObject guiObject = (GuiObject) object;
				int index = list.indexOf(guiObject);
				add(guiObject,parent, index);
			}
		});
		
		list.itemRemoved.addObserver(new Observer() {
			public void update(Observable arg0, Object object) {
				remove((GuiObject) object);
			}
		});
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			this.add(object, parent);
		}
	}
	
	private void add(GuiObject object, GuiObject parent) {
		TreeIter parentIter = (TreeIter) this.map.get(parent);
		TreeIter iter = this.treeStore.appendRow(parentIter);
		this.add(object, iter);
	}

	protected void remove(GuiObject object) {
		TreeIter iter = (TreeIter) this.map.get(object);
		this.map.remove(object);
		this.treeStore.removeRow(iter);
		object.deleteObserver(this);
	}

	protected void add(GuiObject guiObject, GuiObject parent, int index) {
		TreeIter iter = (TreeIter) this.map.get(parent);
		TreeIter newIter = this.treeStore.insertRow(iter, index);
		this.add(guiObject, newIter);
	}

	public void add(GuiObject guiObject, TreeIter iter){
		//System.out.println("DetailedObserverTreeView.add() guiObject: "+ guiObject+ " iter: "+ iter );
		this.treeStore.setValue(iter, nameDC, guiObject.getName());
		this.map.put(guiObject, iter);
		guiObject.addObserver(this);
	}
	
	public ObserverRoot getSelectedObserver(){
		if(this.getSelection().getSelectedRows().length == 0){
			return null;
		}
		
		TreeIter iter = this.treeStore.getIter(this.getSelection().getSelectedRows()[0]);
		GuiObject selected = (GuiObject)this.treeStore.getValue(iter, objectDC);
		
		if(!(selected instanceof ObserverRoot)){
			selected = (GuiObject)this.treeStore.getValue(iter.getParent(), objectDC);
		}
		
		return (ObserverRoot) selected;
	}
	
	public void setSelected(GuiObject guiObject){
		TreeIter iter = (TreeIter) this.map.get(guiObject);
		this.getSelection().select(iter);
	}

	public void update(Observable observable, Object object) {
		GuiObject guiObject = (GuiObject) observable;
		TreeIter iter = (TreeIter) this.map.get(guiObject);
		this.treeStore.setValue(iter, nameDC, guiObject.getName());
	}
	
}
