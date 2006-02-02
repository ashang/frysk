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
import java.util.Vector;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

import frysk.gui.common.Messages;
import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.filters.FilterPoint;
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
	private Vector listObservers;

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
		this.listObservers = new Vector();
		
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
		Iterator iterator = list.iterator();
		
		Observer itemAddedObserver = new Observer() {
			public void update(Observable observable, Object object) {
				addObserverRoot((ObserverRoot) object);
			}
		}; 
		list.itemAdded.addObserver(itemAddedObserver);
		
		Observer itemRemovedObserver = new Observer() {
			public void update(Observable arg0, Object object) {
				removeObserverRoot((ObserverRoot) object);
			}
		};
		list.itemRemoved.addObserver(itemRemovedObserver);
		
		this.listObservers.add(new Object[]{list, itemAddedObserver, itemRemovedObserver});
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			addObserverRoot((ObserverRoot) object);
		}
	}


	private void removeObserverRoot(ObserverRoot observer) {
		System.out.println("DetailedObserverTreeView.removeObserverRoot() removing " + observer.getName() + " base " + observer.getBaseName() );
		Iterator iterator = observer.getFilterPoints().iterator();
		while (iterator.hasNext()) {
			FilterPoint filterPoint = (FilterPoint) iterator.next();
			System.out.println("DetailedObserverTreeView.removeObserverRoot() FilterPoint " + filterPoint.getName());
			this.removeList(filterPoint.getFilters());
		}
		removeList(observer.getFilterPoints());
		
		iterator = observer.getActionPoints().iterator();	
		while (iterator.hasNext()) {
			ActionPoint actionPoint = (ActionPoint) iterator.next();
			System.out.println("DetailedObserverTreeView.removeObserverRoot() ActionPoint " + actionPoint.getName());
			this.removeList(actionPoint.getActions());
		}
		removeList(observer.getActionPoints());

		TreePath path = ((TreeIter)this.map.get(observer)).getPath();
		path.down();
		TreeIter iter = treeStore.getIter(path);
		while(iter != null){
			GuiObject object = (GuiObject)treeStore.getValue(iter, objectDC);
			this.remove(object);
			iter = treeStore.getIter(path);
		}
		
		this.remove(observer);
	}
	
	private void addObserverRoot(ObserverRoot observer){

		this.add(observer, (GuiObject)null);
		GuiObject label = new GuiObject(Messages.getString("DetailedObserverTreeView.1"),""); //$NON-NLS-1$ //$NON-NLS-2$

		this.add(label, observer);
		addList(label, observer.getFilterPoints());
		
		Iterator iterator = observer.getFilterPoints().iterator();
		while (iterator.hasNext()) {
			FilterPoint filterPoint = (FilterPoint) iterator.next();
			this.addList(filterPoint, filterPoint.getFilters());
		}

		
		label = new GuiObject(Messages.getString("DetailedObserverTreeView.3"),""); //$NON-NLS-1$ //$NON-NLS-2$
		this.add(label, observer);
		addList(label, observer.getActionPoints());
		iterator = observer.getActionPoints().iterator();	
		while (iterator.hasNext()) {
			ActionPoint actionPoint = (ActionPoint) iterator.next();
			this.addList(actionPoint, actionPoint.getActions());
		}
	}
	
	private void addList(final GuiObject parent, final ObservableLinkedList list){
		//System.out.println("DetailedObserverTreeView.addList() " + list.hashCode() + " size: " + list.size());
		Iterator iterator = list.iterator();
		
		 Observer itemAddedObserver = new Observer() {
			 public void update(Observable observable, Object object) {
				 GuiObject guiObject = (GuiObject) object;
				 int index = list.indexOf(guiObject);
				 add(guiObject,parent, index);
			 }
		 };
		 list.itemAdded.addObserver(itemAddedObserver);
		 
		 Observer itemRemovedObserver = new Observer() {
			 public void update(Observable arg0, Object object) {
				 remove((GuiObject) object);
			 }
		 };
		 list.itemRemoved.addObserver(itemRemovedObserver);
		 
		 this.listObservers.add(new Object[]{list,itemAddedObserver, itemRemovedObserver});
		 while (iterator.hasNext()) {
			 GuiObject object = (GuiObject) iterator.next();
			 this.add(object, parent);
		 }
	}
	
	private void removeList(final ObservableLinkedList list){
		System.out.println("DetailedObserverTreeView.removeList() " + list.hashCode() + " size: " + list.size());
		Iterator iterator = list.iterator();
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			this.remove(object);
		}
	
		this.removeListObservers(list);
	}
	
	private void add(GuiObject object, GuiObject parent) {
		TreeIter parentIter = (TreeIter) this.map.get(parent);
		TreeIter iter = this.treeStore.appendRow(parentIter);
		this.add(object, iter);
	}

	protected void remove(GuiObject object) {
		TreeIter iter = (TreeIter) this.map.get(object);
		System.out.println("DetailedObserverTreeView.remove() removing " + object.getName() + " iter: " + iter );
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
		this.treeStore.setValue(iter, nameDC, guiObject.getName());
		this.treeStore.setValue(iter, objectDC, guiObject);
		this.map.put(guiObject, iter);
		guiObject.addObserver(this);
		System.out.println("DetailedObserverTreeView.add() adding " + guiObject.getName() +" "+ iter );
	}
	
	public ObserverRoot getSelectedObserver(){
		if(this.getSelection().getSelectedRows().length == 0){
			return null;
		}
		
		TreeIter iter = this.treeStore.getIter(this.getSelection().getSelectedRows()[0]);
		GuiObject selected = (GuiObject)this.treeStore.getValue(iter, objectDC);
		
		while(selected != null && !(selected instanceof ObserverRoot)){

			TreePath treePath = iter.getPath();
			treePath.up();
			iter = treeStore.getIter(treePath);

			selected = (GuiObject)this.treeStore.getValue(iter, objectDC);
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
	
	private void removeListObservers(ObservableLinkedList list){
		for (int i = 0; i < listObservers.size(); i++) {
			ObservableLinkedList storeList = (ObservableLinkedList) (((Object[])listObservers.elementAt(i))[0]);
			if(storeList == list){
				list.itemAdded.deleteObserver((Observer) ((((Object[])listObservers.elementAt(i))[1])));
				list.itemRemoved.deleteObserver((Observer) ((((Object[])listObservers.elementAt(i))[2])));
				listObservers.remove(i);
				return;
			}
		}
	}
}
