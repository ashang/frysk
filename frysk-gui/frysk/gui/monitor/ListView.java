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
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SortType;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.CellRendererTextListener;

/**
 * A widget that combines the TreeView and DataModel and displays
 * a simple linear list. Meant to take away all the complexities of
 * setting up a treeView and a model.
 * Inherits from TreeView so it can be initialized by a handle retrieved
 * from a glade file. 
 * */
public class ListView extends TreeView implements Observer {
	protected HashMap map;
	protected ListStore listStore;
	
	protected DataColumnString nameDC;
	protected DataColumnObject objectDC;
	protected ObservableLinkedList watchedList;
	
	private ItemAddedObserver itemAddedObserver;
	private ItemRemovedObserver itemRemovedObserver;
	
	private CellRendererText cellRendererText;
	private boolean stickySelect = false;
	
	public ListView(){
		super();
		this.init();
	}
	
	public ListView(Handle handle){
		super(handle);
		this.init();
	}
	
	public void setStickySelect(boolean selectMode)
	{
		this.stickySelect = selectMode;
	}

	public void addEditListener(CellRendererTextListener listener)
	{
		if ((cellRendererText != null) && (listener != null) &&
			(listener instanceof CellRendererTextListener))
		{
			cellRendererText.setBooleanProperty("editable",true);
			cellRendererText.addListener((CellRendererTextListener)listener);
		}

	}
	
	protected void initListStore(){
		this.listStore = new ListStore(new DataColumn[]{nameDC, objectDC});
	}
	
	protected void initTreeView() {
		cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		
		this.listStore.setSortColumn(nameDC,SortType.ASCENDING);
		this.setReorderable(true);
		this.appendColumn(nameCol);	
		
	}
	
	private void init(){
		this.setHeadersVisible(false);
	
		this.itemAddedObserver = new ItemAddedObserver();
		this.itemRemovedObserver = new ItemRemovedObserver();
		
		this.map = new HashMap();
		
		this.nameDC = new DataColumnString();
		this.objectDC = new DataColumnObject();
		
		this.initListStore();
		this.setModel(listStore);
		this.initTreeView();
	
	}
	
	public Iterator getSelectedObjects(){

		LinkedList selecteds = new LinkedList();
		TreePath[] selectedPaths = this.getSelection().getSelectedRows();

		/* Check for no selected rows */
		if (selectedPaths.length > 0) {

			for (int i = 0; i < selectedPaths.length; i++) {
				selecteds.add((GuiObject) this.listStore.getValue(this.listStore.getIter(selectedPaths[i]), objectDC));
			}
		return selecteds.iterator();
		} else { return null; }
	}
		
	public GuiObject getSelectedObject(){

		GuiObject selected = null;
		
		/* Check for no selected rows */
		if(this.getSelection().getSelectedRows().length > 0){
			selected = (GuiObject) this.listStore.getValue(this.listStore.getIter(this.getSelection().getSelectedRows()[0]), objectDC);
		}
		return selected;
	}

	public void add(GuiObject object){
		TreeIter treeIter = listStore.appendRow();
		this.add(object, treeIter);
		if (this.stickySelect)
			setSelectedObject(object);
	}
	
	/**
	 * Set the selection to the item that represents
	 * the given object.
	 * @param object the object that is to be displayed as selected.
	 * */
	public void setSelectedObject(GuiObject object){
		TreeIter iter = (TreeIter) this.map.get(object);
		if(iter == null){
			throw new IllegalArgumentException("The object passed ["+ object +"] is not a member of this list"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.getSelection().select(iter);
	}
	
	/**
	 * Add the given object at the given index
	 * @param object object to be added
	 * @param index the position to insert the given object at.
	 * */
	public void add(GuiObject object, int index){
		TreeIter treeIter = listStore.insertRow(index);
		this.add(object, treeIter);
		if (this.stickySelect)
			setSelectedObject(object);
		else if (getSelectedObject() == null)
			setSelectedObject(object);
	}
	
	/**
	 * Add the given object at the given treeIter
	 * @param object object to be added
	 * @param treeIter a @link TreeIter pointing to the
	 * position to insert the given object at.
	 * */
	public void add(GuiObject object, TreeIter treeIter){
		listStore.setValue(treeIter, nameDC, object.getName());
		listStore.setValue(treeIter, objectDC, object);
		this.map.put(object, treeIter);
		object.addObserver(this);	
		if (this.stickySelect)
			setSelectedObject(object);
		else if (getSelectedObject() == null)
				setSelectedObject(object);
	}
	
	public void remove(GuiObject object){
		TreeIter treeIter = (TreeIter) this.map.get(object);
		listStore.removeRow(treeIter);
		this.map.remove(object);
		object.deleteObserver(this);
	}
	
	public void unwatchList(){
		this.clear();
		this.watchedList.itemAdded.deleteObserver(itemAddedObserver);
		this.watchedList.itemRemoved.deleteObserver(itemRemovedObserver);
		this.watchedList = null;
		
	}
	
	public void clear(){
		Set set = this.map.keySet();
		Iterator iterator = set.iterator();
		while (iterator.hasNext()) {
			GuiObject element = (GuiObject) iterator.next();
			element.deleteObserver(this);
		}
		this.listStore.clear();
		this.map.clear();
	}
	
	public void update(Observable guiObject, Object object) {
		TreeIter treeIter = (TreeIter) this.map.get(guiObject);
		listStore.setValue(treeIter, nameDC, ((GuiObject)guiObject).getName());
	}
	
	/**
	 * Tell this ListView to initialize itself with the given list
	 * and watch the given ObservableLinkedList and update itself 
	 * when the list changes. Clients will then not have to worry
	 * about updating the ComboBox.
	 * @param linkedList the list to be watched.
	 * */
	public void watchLinkedList(ObservableLinkedList linkedList){
		if(this.watchedList != null){
			this.unwatchList();
		}
		
		this.watchedList = linkedList;
		Iterator iterator = linkedList.iterator();
		
		linkedList.itemAdded.addObserver(this.itemAddedObserver);
		linkedList.itemRemoved.addObserver(this.itemRemovedObserver);
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			this.add(object);
		}
	}
	
	/**
	 * Set the selection to the first item with the text that matches
	 * the give text. If the given text is not found an exception is
	 * thrown.
	 * @param text the text that is to be matched and the match selected.
	 * */
	public void setSelectedText(String text){
		TreePath treePath = this.listStore.getFirstIter().getPath();
		
		String displayedText;
		TreeIter iter = this.listStore.getIter(treePath);

		while(iter != null){
			displayedText = (String) this.listStore.getValue(iter, nameDC);

			if(text.equals(displayedText)){
				this.getSelection().select(iter);
				return;
			}
			treePath.next();
			iter = this.listStore.getIter(treePath);
		}
		throw new IllegalArgumentException("the passes text argument ["+ text +"] does not match any of the items in this ComboBox"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	class ItemAddedObserver implements Observer{
		public void update(Observable arg0, Object object) {
			GuiObject guiObject = (GuiObject) object;
			int index = watchedList.indexOf(guiObject);
			add(guiObject, index);
		}
	}
	
	class ItemRemovedObserver implements Observer{
		public void update(Observable arg0, Object object) {
			remove((GuiObject) object);
		}
	}
}
