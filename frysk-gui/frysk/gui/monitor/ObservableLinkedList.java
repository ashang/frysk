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
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;

/**
 * Extends LinkedList but accepts observers that will 
 * be notified when elements are added/removed.
 * Not all functions are overwritten check to see that
 * the function you use is overwritten and the appropriate
 * listeners are being notified.
 * 
 * Overwrites the copy constructor to dynamically call
 * copy constructor of all elements and add them to the
 * new list.
 * 
 * Knows how to save its elements and load them.
 * 
 * should really be called ObservableSavableCopyiableLinkedList
 * */
public class ObservableLinkedList extends LinkedList implements SaveableXXX {

	private static final long serialVersionUID = 1L;
	
	public final GuiObservable itemAdded;
	public final GuiObservable itemRemoved;
	
	
	public ObservableLinkedList(){
		super();
		this.itemAdded = new GuiObservable();
		this.itemRemoved = new GuiObservable();
	}
	
	public ObservableLinkedList(ObservableLinkedList other, boolean copyObjects){
	        super(other);

		this.itemAdded = new GuiObservable();
		this.itemRemoved = new GuiObservable();
		
		if(copyObjects){
		  this.clear();
		  this.copyFromList(other);
		}
		
	}
	
	public void copyFromList(ObservableLinkedList other) {
		Iterator iterator = other.iterator();
		while (iterator.hasNext()) {
			GuiObject original = (GuiObject) iterator.next() ;
			GuiObject copy;
			
			copy =  original.getCopy();
			if(copy == null){
				//should not copy
			}else{
				this.add(copy);
			}
		}
	}
	
	public boolean add(Object o){
		boolean val = super.add(o);
		this.itemAdded.notifyObservers(o);
		return val;
	}
	
	public void add(int index, Object element){
		super.add(index, element);
		this.itemAdded.notifyObservers(element);
	}

	public Object remove(int index){
		Object removed = super.remove(index);
		this.itemRemoved.notifyObservers(removed);
		return removed;
	}
	
	public boolean remove(Object o){
		if(super.remove(o)){
			this.itemRemoved.notifyObservers(o);			
			return true;
		}else{
			throw new IllegalArgumentException("You are trying to remove an object which does not belong to this list." +
					"If this is intentional catch the exception");
		}
	}
	
	public void clear(){
		for (int i = 0; i < this.size(); ) {
			this.remove(i);	
		}
	}
	
	/**
	 * Removed oldObject and add newObject in its spot 
	 * @param oldObject
	 * @param newObject
	 */
	public void swap(Object oldObject, Object newObject){
		int index = this.indexOf(oldObject);
		if(index < 0){
			throw new IllegalArgumentException("The object passed oldObject ["+ oldObject +"] is not an element in this list");
		}
		
		this.remove(index);
		this.add(index, newObject);
	}

	public void save(Element node) {
		Element filtersXML = new Element("elements");

		Iterator iterator = this.iterator();
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			if(object.shouldSaveObject()){
				Element filterXML = new Element("element");
				ObjectFactory.theFactory.saveObject(object, filterXML);
				filtersXML.addContent(filterXML);	
			}
		}
		node.addContent(filtersXML);
	}

	public void load(Element node) {
		Element filtersXML = node.getChild("elements");
		List list = (List) filtersXML.getChildren("element");
		Iterator i = list.iterator();
		
		GuiObject  object;
		while (i.hasNext()){
			object = (GuiObject) ObjectFactory.theFactory.loadObject((Element) i.next());
			this.add(object);
		}
	}

	public boolean shouldSaveObject() {
		return true;
	}

	public void doSaveObject() {}

	public void dontSaveObject() {}
	
}
