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
import java.util.Observable;
import java.util.Observer;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;

/**
 * 
 * @author swagiaal, pmuldoon
 *
 * A container that refers to an executable
 * there could be zero or many instances of these
 * executable. This keeps track of those too. 
 */

public class DebugProcess extends GuiObject {
 
	String executablePath;
	ObservableLinkedList procs;
	
	ObservableLinkedList observers;
	ObservableLinkedList tagsets;
	
	ObservableLinkedList allProcsList;
	
	public DebugProcess(){
		super();
		
		this.procs = new ObservableLinkedList();
		
		this.observers = new ObservableLinkedList();
		this.tagsets = new ObservableLinkedList();
		
		allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;
	}
	
	public DebugProcess(String name, String executablePath){
		super(name, name);
		
		this.executablePath = executablePath;
		
		this.procs = new ObservableLinkedList();
		
		this.observers = new ObservableLinkedList();
		this.tagsets = new ObservableLinkedList();

		allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;
	}
	
	public DebugProcess(DebugProcess other) {
		super(other);
		
		this.executablePath = other.executablePath;
		
		this.procs = new ObservableLinkedList();
		
		this.observers = new ObservableLinkedList(other.observers);
		this.tagsets = new ObservableLinkedList(other.tagsets);

		allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;
	}
	
	public void populateProcs() {
		
		
		allProcsList.itemAdded.addObserver(new Observer() {
			public void update(Observable observable, Object arg) {
				GuiProc guiProc = (GuiProc) arg;
				if((guiProc.getFullExecutablePath()).equals(executablePath)){
					addProc(guiProc);
				}
			}
		});
		
		allProcsList.itemRemoved.addObserver(new Observer() {
			public void update(Observable observable, Object arg) {
				GuiProc guiProc = (GuiProc) arg;
				if(guiProc.getFullExecutablePath().equals(executablePath)){
					removeProc(guiProc);
				}
			}
		});
	
		Iterator iterator = allProcsList.iterator();
		while (iterator.hasNext()) {
			GuiProc guiProc = (GuiProc) iterator.next();
			if((guiProc.getFullExecutablePath()).equals(this.executablePath)){
				this.addProc(guiProc);
			}
		}
	}


	public void addProc(GuiProc guiProc){
		Iterator iterator = this.observers.iterator();
		while (iterator.hasNext()) {
			TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
			guiProc.add(observer);
		}
		
		this.procs.add(guiProc);
	}
	
	public void removeProc(GuiProc guiProc){
	
		Iterator iterator = this.observers.iterator();
		while (iterator.hasNext()) {
			TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
			guiProc.add(observer);
		}
		
		this.procs.remove(guiProc);
	}

	public ObservableLinkedList getProcs(){
		return this.procs;
	}
	
	/**
	 * Adds an obsever to the list of observers
	 * to be added to composing procs
	 * @param observer
	 */
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
