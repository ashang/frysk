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
import java.util.List;

import org.jdom.Element;

public abstract class LiaisonPoint extends GuiObject implements SaveableXXX {
protected ObservableLinkedList items;
	
	public LiaisonPoint(){
		super();
		this.items = new ObservableLinkedList();
	}
	
	public LiaisonPoint(String name, String toolTip){
		super(name, toolTip);
		this.items = new ObservableLinkedList();
	}
	
	public LiaisonPoint(LiaisonPoint other){
		super(other);
		this.items = new ObservableLinkedList(other.items); // Do copy items
	}
	
	/**
	 * Retrieves a list of applicable items from the apporpriet Manager.
	 * */
	public abstract ObservableLinkedList getApplicableItems();
	
	public void addItem(LiaisonItem item){
		this.items.add(item);
	}
	
	public void removeItem(LiaisonItem item){
		if(!this.items.remove(item)){
			throw new IllegalArgumentException("the passed item ["+ item +"] is not a member of this Liason point");
		}
	}
	
	public ObservableLinkedList getItems(){
		return this.items;
	}
	
	public void save(Element node) {
		super.save(node);
		
		//items
		Element itemsXML = new Element("items");
		
		Iterator iterator = this.getItems().iterator();
		while (iterator.hasNext()) {
			LiaisonItem item = (LiaisonItem) iterator.next();
			if(item.shouldSaveObject()){
				Element itemXML = new Element("item");
				ObjectFactory.theFactory.saveObject(item, itemXML);
				itemsXML.addContent(itemXML);	
			}
		}
		node.addContent(itemsXML);
	}
	
	public void load(Element node) {
		super.load(node);
		
		//items
		Element itemsXML = node.getChild("items");
		List list = (List) itemsXML.getChildren("item");
		Iterator i = list.iterator();
		
		LiaisonItem item;
		while (i.hasNext()){
			item = (LiaisonItem) ObjectFactory.theFactory.loadObject((Element) i.next());
			this.addItem(item);
		}
	}
	
	public String toString(){
		String string = "";
		
		string += "  Name: " + this.getName() + "["+ super.toString() + "]"+"\n";
		Iterator iterator = this.items.iterator();
		while (iterator.hasNext()) {
			LiaisonItem item = (LiaisonItem) iterator.next();
			string += "    " + item + "\n";
		}
		return string;
	}
}
