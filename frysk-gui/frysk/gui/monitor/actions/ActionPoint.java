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

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.LiaisonPoint;
import frysk.gui.monitor.ObservableLinkedList;

/**
 * In a similar manner to @link frysk.gui.monitor.filters.FilterPoint
 * ActionPoints provide a flexible interface to add actions to Observers.
 * */
public abstract class ActionPoint extends LiaisonPoint {
	
	public ActionPoint() {
		super();
	}

	public ActionPoint(String name, String toolTip){
		super(name, toolTip);
	}
	
	public ActionPoint(ActionPoint other){
		super(other);
	}
	
	/**
	 * Retrieves a list of applicable actions from the ActionManager.
	 * */
	public abstract ObservableLinkedList getApplicableActions();
	
	public void removeAction(Action action){
		super.removeItem(action);
	}
	
	public ObservableLinkedList getActions(){
		return super.getItems();
	}

	public void addAction(Action action) {
		super.addItem(action);		
	}

	public abstract GuiObject getCopy();
}
