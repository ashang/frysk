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
 * */
public class ObservableLinkedList extends LinkedList{

	private static final long serialVersionUID = 1L;
	
	public final GuiObservable itemAdded;
	public final GuiObservable itemRemoved;
	
	
	public ObservableLinkedList(){
		super();
		this.itemAdded = new GuiObservable();
		this.itemRemoved = new GuiObservable();
	}
	
	public ObservableLinkedList(ObservableLinkedList other){
//		super(collection);

		this.itemAdded = new GuiObservable();
		this.itemRemoved = new GuiObservable();
		
		this.copyFromList(other);
		
	}
	
	public void copyFromList(ObservableLinkedList other){
		Iterator iterator = other.iterator();
		while (iterator.hasNext()) {
			Object original = iterator.next() ;
			Object copy;
//			System.out.println("ObservableLinkedList.ObservableLinkedList() original " + original.getClass());
			Class cls;
			try {
				cls = Class.forName(original.getClass().getName());
				java.lang.reflect.Constructor constr = cls.getConstructor(new Class[]{original.getClass()});
				copy =  constr.newInstance(new Object[] {original});
				this.add(copy);
			} catch (Exception e) {
				e.printStackTrace();
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
			return false;
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
}
