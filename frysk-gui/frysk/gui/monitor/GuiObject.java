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

import org.jdom.Element;


/**
 * A GuiObject is one that has a name and a tooltip.
 * */
public class GuiObject extends GuiObservable implements SaveableXXX{
	
	private boolean saveObject;
	
	private String name;
	private String toolTip;
	
	public GuiObject(String name, String toolTip){
		this.name = name;
		this.toolTip = toolTip;
		this.saveObject = true;
	}

	public GuiObject(GuiObject other) {
		this.name = other.name;
		this.toolTip = other.toolTip;
		this.saveObject = other.saveObject;
	}

	public GuiObject() {
		this.setName("NoName");
		this.setToolTip("NoTootip");
	}

	public void setName(String name) {
		this.name = name;
		
		this.notifyObservers();
	}

	public String getName() {
		return name;
	}

	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
		
		this.notifyObservers();
	}

	public String getToolTip() {
		return toolTip;
	}

	public void save(Element node) {
		node.setAttribute("name", this.getName());
		node.setAttribute("tooltip", this.getToolTip());
	}

	public void load(Element node) {
		this.setName(node.getAttribute("name").getValue());
		this.setToolTip(node.getAttribute("tooltip").getValue());
	}
	
	public boolean shouldSaveObject(){
		return this.saveObject;
	}

	public void doSaveObject() {
		this.saveObject = true;
	}

	public void dontSaveObject() {
		this.saveObject = false;
	}
	
}
