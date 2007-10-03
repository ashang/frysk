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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frysk.gui.FryskGui;


public class ProgramData {
	
	private boolean enabled;
	private String name;
	private String executable;
	private ArrayList processList;
	private ArrayList observerList;
	
	private Logger errorLog = Logger.getLogger(FryskGui.ERROR_LOG_ID);
	
	/**
	 * @param name. Name of this monitor.
	 * @param enabled. Is this monitor enabled?
	 * @param executable. String Name of the program Executable
	 * @param processList. ArrayList names of the watched process names
	 * @param observerList. ArrayList names of observers to apply.
	 */
	public ProgramData(String name, boolean enabled, String executable, ArrayList processList, ArrayList observerList)
	{
		this.name = name;
		this.enabled = enabled;
		this.executable = executable;
		this.processList = processList;
		this.observerList = observerList;
	}

	public ArrayList getProcessList() {
		return processList;
	}

	public void setProcessList(ArrayList processList) {
		this.processList = processList;
	}

	public ArrayList getObserverList() {
		return observerList;
	}

	public void setObserverList(ArrayList observerList) {
		this.observerList = observerList;
	}

	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void save(String filename)
	{   

		Element root = new Element("ProgramEvent");
		Document doc =  new Document(root);

		root.setAttribute("name", this.name);
		root.setAttribute("enabled", ""+this.enabled);
		root.setAttribute("executable",this.executable);
		
		Element processList = new Element("processes");
		
		Iterator i = this.processList.iterator();
		while (i.hasNext())
			processList.addContent(new Element("process").setText(((String)i.next())));
		
		Element observerList = new Element("observers");
		
		i = this.observerList.iterator();
		
		while (i.hasNext())
			observerList.addContent(new Element("observer").setText(((String)i.next())));
			
		
		root.addContent(processList);
		root.addContent(observerList);
		
		XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());

		try {
			output.output(doc,new FileWriter(filename));
		} catch (IOException e) {
			errorLog.log(Level.SEVERE,"Errors writing execution event monitor " + filename,e);
		}
	}
	
	public void save() {
		save(this.name);
	}
	
	public void load(String filename) {
		
	    SAXBuilder builder = new SAXBuilder(true);
	    Document doc = null;
	    
	    try {
			doc = builder.build(filename);
		} catch (JDOMException e) {
			errorLog.log(Level.SEVERE,"JDOM Exception while building " + filename,e);
		} catch (IOException e) {
			errorLog.log(Level.SEVERE,"IO Exception while building " + filename,e);
		}
		
		Element root = doc.getRootElement();
		
		this.name = root.getAttribute("name").getValue();
		
		if (root.getAttribute("enabled").getValue().equals("true"))
			this.enabled = true;
		else
			this.enabled = false;
		
		this.executable = root.getAttribute("executable").getValue();
		
		Element observers = root.getChild("observers");
		ArrayList XMLobserverList  = (ArrayList) observers.getChildren("observer");		
		Iterator i = XMLobserverList.iterator();
		
		observerList.clear();
		while (i.hasNext())
			observerList.add(((Element)i).getText());
		
		Element processes = root.getChild("processes");
		ArrayList XMLprocessList  = (ArrayList) processes.getChildren("process");		
		i = XMLprocessList.iterator();
		
		processList.clear();
		while (i.hasNext())
			processList.add(((Element)i).getText());
	}
}
