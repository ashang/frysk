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

package frysk.gui.sessions;

import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;
import frysk.proc.Proc;

/**
 * 
 * @author swagiaal, pmuldoon
 *
 * A container class representing a process
 * in a debug session
 */

public class DebugProcess extends GuiObject {
 
	String executablePath;
	Proc proc;
	
	ObservableLinkedList observers;
	ObservableLinkedList tagsets;
	
	public DebugProcess(){
		super();
		
		this.observers = new ObservableLinkedList();
		this.tagsets = new ObservableLinkedList();
	}
	
	public DebugProcess(String name, String executablePath){
		super(name, name);
		
		this.executablePath = executablePath;
		
		this.observers = new ObservableLinkedList();
		this.tagsets = new ObservableLinkedList();
	}
	
	public DebugProcess(DebugProcess other) {
		super(other);
		
		this.executablePath = other.executablePath;
		
		this.observers = new ObservableLinkedList(other.observers);
		this.tagsets = new ObservableLinkedList(other.tagsets);
	}
	
	/**
	 * Serves a Proc object when required to
	 * do so. There are several possible sources:
	 * 
	 * o User chooses to debug and executable setProc
	 *   is never called this.proc is null. An instance
	 *   of the process is run using Frysk back-end and
	 *   returned.
	 *   
	 * o User chooses a process from a list when creating
	 *   a Session, setProc is called and that instance is
	 *   returned.
	 *   frysk.gui.srcwin.tags
	 * o User loads a session, is presented with a list of
	 *   possible candidates, they pick one, setProc is called
	 *   and that instance is returned.
	 * 
	 * @return a proc object corresponding to the
	 * executable of this DebugProcess.
	 */
	public Proc getProc(){
		return this.proc;
	}
	
	public void setProc(Proc proc){
		this.proc = proc;
		if(proc  != null){
			Iterator iterator = this.observers.iterator();
			while (iterator.hasNext()) {
				TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
				observer.apply(proc);
			}
		}
	}
	
	public void addObserver(ObserverRoot observer){
		this.observers.add(observer);
	}
	
	public void removeObserver(ObserverRoot observer){
		this.observers.remove(observer);
	}

	public ObservableLinkedList getObservers(){
		return this.observers;
	}

	public void addTagset(Tagset tagset){
		this.tagsets.add(tagset);
	}
	
	public void removeTagset(Tagset tagset){
		this.tagsets.remove(tagset);
	}
	
	public ObservableLinkedList getTagsets(){
		return this.tagsets;
	}
	

	public GuiObject getCopy() {
		return new DebugProcess(this);
	}
	public void save(Element node) {
		super.save(node);
		
		node.setAttribute("executablePath", this.executablePath);
		Element observersXML= new Element("observers");
		
		Iterator iterator = observers.iterator();
		while (iterator.hasNext()) {
			GuiObject object = (GuiObject) iterator.next();
			Element elementXML = new Element("element");
			elementXML.setAttribute("name", object.getName());
			observersXML.addContent(elementXML);	
		}
		
		node.addContent(observersXML);
		
		//save tagsets
		Element tagSetsXML= new Element("tagsets");
		
		Iterator i = tagsets.iterator();
		while (i.hasNext()) {
			GuiObject object = (GuiObject) i.next();
			Element elementXML = new Element("element");
			elementXML.setAttribute("name", object.getName());
			tagSetsXML.addContent(elementXML);	
		}
		
		node.addContent(tagSetsXML);

	}

	public void load(Element node) {
		super.load(node);
		
		this.executablePath = node.getAttribute("executablePath").getValue();
		Element observersXML = node.getChild("observers");
		List list = (List) observersXML.getChildren("element");
		Iterator i = list.iterator();
		
		while (i.hasNext()){
			Element elementXML = (Element) i.next();
			ObserverRoot observer = ObserverManager.theManager.getObserverByName(elementXML.getAttributeValue("name"));
			observers.add(observer);
		}
				
		// load tagsets
		
		Element tagSetsXML = node.getChild("tagsets");
		List tagList = (List) tagSetsXML.getChildren("element");
		Iterator iterator = tagList.iterator();
		
		while (iterator.hasNext()){
			Element elementXML = (Element) iterator.next();
			Tagset tag = TagsetManager.manager.getTagsetByName(elementXML.getAttributeValue("name"));
			if (tag != null)
				tagsets.add(tag);
		}
	}

}
