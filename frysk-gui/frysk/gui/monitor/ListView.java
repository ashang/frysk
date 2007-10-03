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
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;

import frysk.gui.monitor.GuiObject;

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
	private ObservableLinkedList watchedList;
	
	ListView(){
		super();
		this.init();
	}
	
	ListView(Handle handle){
		super(handle);
		this.init();
	}
	
	private void init(){
		this.setHeadersVisible(false);
		
		this.map = new HashMap();
		
		this.nameDC = new DataColumnString();
		this.objectDC = new DataColumnObject();
		this.listStore = new ListStore(new DataColumn[]{nameDC, objectDC});
		this.setModel(listStore);
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , nameDC);
		this.appendColumn(nameCol);
	}
	
	public GuiObject[] getSelectedObjects(){
		GuiObject[] selecteds = new GuiObject[this.getSelection().getSelectedRows().length]; //selecteds is a word... ask Gollum :)
		
		for (int i = 0; i < selecteds.length; i++) {
			selecteds[i] = (GuiObject) this.listStore.getValue(this.listStore.getIter(this.getSelection().getSelectedRows()[0]), objectDC);
		}
		
		return selecteds;
	}
		
	public GuiObject getSelectedObject(){
		GuiObject selected = null;
		
		if(this.getSelection().getSelectedRows().length > 0){
			selected = (GuiObject) this.listStore.getValue(this.listStore.getIter(this.getSelection().getSelectedRows()[0]), objectDC);
		}
		
		return selected;
	}

	public void add(GuiObject object){
		TreeIter treeIter = listStore.appendRow();
		this.add(object, treeIter);
	}
	
	/**
	 * Add the given object at the given index
	 * @param object object to be added
	 * @param index the position to insert the given object at.
	 * */
	public void add(GuiObject object, int index){
		TreeIter treeIter = listStore.insertRow(index);
		this.add(object, treeIter);
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
	}
	
	public void remove(GuiObject object){
		TreeIter treeIter = (TreeIter) this.map.get(object);
		listStore.removeRow(treeIter);
		this.map.remove(object);
	}
	
	public void clear(){
		this.listStore.clear();
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
		this.watchedList = linkedList;
		Iterator iterator = linkedList.iterator();
		
		linkedList.itemAdded.addObserver(new Observer() {
			public void update(Observable observable, Object object) {
				GuiObject guiObject = (GuiObject) object;
				int index = watchedList.indexOf(guiObject);
				add(guiObject, index);
			}
		});
		
		linkedList.itemRemoved.addObserver(new Observer() {
			public void update(Observable arg0, Object object) {
				remove((GuiObject) object);
			}
		});
		
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
		throw new IllegalArgumentException("the passes text argument ["+ text +"] does not match any of the items in this ComboBox");
	}

}
