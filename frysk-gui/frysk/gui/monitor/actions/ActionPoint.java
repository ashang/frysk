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

package frysk.gui.monitor.actions;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObjectFactory;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.SaveableXXX;

/**
 * In a similar manner to @link frysk.gui.monitor.filters.FilterPoint
 * ActionPoints provide a flexible interface to add actions to Observers.
 * */
public abstract class ActionPoint extends GuiObject implements SaveableXXX {
	protected ObservableLinkedList actions;
	
	public ActionPoint() {
		super();
		this.actions = new ObservableLinkedList();
	}

	public ActionPoint(String name, String toolTip){
		super(name, toolTip);
		this.actions = new ObservableLinkedList();
	}
	
	public ActionPoint(ActionPoint other){
		super(other);
		this.actions = new ObservableLinkedList(); // Dont copy Actions
	}
	
	/**
	 * Retrieves a list of applicable actions from the ActionManager.
	 * */
	public abstract ObservableLinkedList getApplicableActions();
	
	public void removeAction(Action action){
		if(!this.actions.remove(action)){
			throw new IllegalArgumentException("the passed action ["+ action +"] is not a member of this action point"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public ObservableLinkedList getActions(){
		return this.actions;
	}

	public void addAction(Action action) {
		this.actions.add(action);		
	}
	
	public void save(Element node) {
		super.save(node);
		
		//actions
		Element actionsXML = new Element("actions"); //$NON-NLS-1$
		
		Iterator iterator = this.getActions().iterator();
		while (iterator.hasNext()) {
			Action action = (Action) iterator.next();
			if(action.shouldSaveObject()){
				Element actionXML = new Element("action"); //$NON-NLS-1$
				ObjectFactory.theFactory.saveObject(action, actionXML);
				actionsXML.addContent(actionXML);
			}
		}
		node.addContent(actionsXML);
	}
	
	public void load(Element node) {
		super.load(node);
		
		//actions
		Element actionsXML = node.getChild("actions"); //$NON-NLS-1$
		List list = (List) actionsXML.getChildren("action"); //$NON-NLS-1$
		Iterator i = list.iterator();
		
		Action action;
		while (i.hasNext()){
			action = (Action) ObjectFactory.theFactory.loadObject((Element) i.next());
			this.addAction(action);
		}
	}

	public String toString(){
		String string = "";
		
		string += "  Name: " + this.getName() + "["+ super.toString() + "]"+"\n";
		Iterator iterator = this.actions.iterator();
		while (iterator.hasNext()) {
			Action action = (Action) iterator.next();
			string += "    " + action + "\n";
		}
		return string;
	}
}
