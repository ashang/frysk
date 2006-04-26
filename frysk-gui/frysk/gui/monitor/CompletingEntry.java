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
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.EntryCompletion;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;

public class CompletingEntry extends Entry implements Observer {
	
	private EntryCompletion entryCompletion;
	private ListStore listStore; 
	private DataColumnString dataColumnString;
	private HashMap hash;
	
	private ObservableLinkedList watchedList;
	
	private ItemAddedObserver itemAddedObserver;
	private ItemRemovedObserver itemRemovedObserver;
	
	CompletingEntry(Handle handle){
		super(handle);
		init();
	}
	
	CompletingEntry(){
		super();
		init();
	}
	
	private void init(){
		this.hash = new HashMap();
		
		this.itemAddedObserver = new ItemAddedObserver();
		this.itemRemovedObserver = new ItemRemovedObserver();
		
		this.entryCompletion = new EntryCompletion();
		entryCompletion.setInlineCompletion(true);
		//entryCompletion.setPopupCompletion(true);
		
		dataColumnString = new DataColumnString();
		
		listStore = new ListStore(new DataColumn[]{dataColumnString});
		
		entryCompletion.setModel(listStore);
		this.setCompletion(entryCompletion);
		entryCompletion.setTextColumn(dataColumnString.getColumn());
	
	
	}
	
	public void watchList(ObservableLinkedList linkedList){
		if(this.watchedList != null){
			this.unWatchList();
		}
		
		this.watchedList = linkedList;
		Iterator iterator = linkedList.iterator();
		
		watchedList.itemAdded.addObserver(this.itemAddedObserver);
		watchedList.itemRemoved.addObserver(this.itemRemovedObserver);
		
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			this.add(object);
		}
	}

	
	public void unWatchList(){
		if(this.watchedList == null){
			throw new RuntimeException("No list is being watched");
		}
		
		Iterator iterator = this.watchedList.iterator();
		while (iterator.hasNext()) {
			GuiObject element = (GuiObject) iterator.next();
			this.remove(element);
		}
		
		watchedList.itemAdded.deleteObserver(itemAddedObserver);
		watchedList.itemRemoved.deleteObserver(itemAddedObserver);
		
		this.watchedList = null;
	}
	
	private void add(GuiObject object) {
		TreeIter iter = this.listStore.appendRow();
		this.add(object, iter);
	}

	protected void remove(GuiObject object) {
		TreeIter iter = (TreeIter) hash.get(object);
		this.listStore.removeRow(iter);
		this.hash.remove(object);
		object.deleteObserver(this);
	}

	protected void add(GuiObject object, int index) {
		TreeIter iter = this.listStore.insertRow(index);
		this.add(object, iter);
	}

	protected void add(GuiObject object, TreeIter iter){
		this.listStore.setValue(iter, dataColumnString, object.getName());
		this.hash.put(object, iter);
		object.addObserver(this);
	}
	
	public void update(Observable object, Object arg) {
		TreeIter iter = (TreeIter) hash.get(object);
		this.listStore.setValue(iter, dataColumnString, ((GuiObject)object).getName());
	}
	
	private class ItemAddedObserver implements Observer{
		public void update(Observable observable, Object object) {
			GuiObject guiObject = (GuiObject) object;
			int index = watchedList.indexOf(guiObject);
			add(guiObject, index);
		}
	}

	private class ItemRemovedObserver implements Observer{
		public void update(Observable arg0, Object object) {
			remove((GuiObject) object);
		}
	}

}
