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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

import frysk.Config;
import frysk.gui.Gui;
import frysk.gui.common.Messages;


public class ProgramData {
	
	private boolean enabled;
	private String name;
	private String executable;
	private ArrayList processList;
	private ArrayList observerList;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	private static final String EVENT_STORE_LOC = Config.FRYSK_DIR +
	"event_watchers_store" + "/"; //$NON-NLS-1$ //$NON-NLS-2$
	
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
	
	public ProgramData()
	{
		this.name = ""; //$NON-NLS-1$
		this.enabled = false;
		this.executable = ""; //$NON-NLS-1$
		this.processList = new ArrayList();
		this.observerList = new ArrayList();
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

		Element root = new Element("ProgramEvent"); //$NON-NLS-1$
		Document doc =  new Document(root);

		root.setAttribute("name", this.name); //$NON-NLS-1$
		root.setAttribute("enabled", ""+this.enabled); //$NON-NLS-1$ //$NON-NLS-2$
		root.setAttribute("executable",this.executable); //$NON-NLS-1$
		
		Element processList = new Element("processes"); //$NON-NLS-1$
		
		Iterator i = this.processList.iterator();
		while (i.hasNext())
			processList.addContent(new Element("process").setText(((String)i.next()))); //$NON-NLS-1$
		
		Element observerList = new Element("observers"); //$NON-NLS-1$
		
		i = this.observerList.iterator();
		
		while (i.hasNext())
			observerList.addContent(new Element("observer").setText(((String)i.next()))); //$NON-NLS-1$
			
		
		root.addContent(processList);
		root.addContent(observerList);
		
		XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());

		try {
			output.output(doc,new FileWriter(filename));
		} catch (IOException e) {
			errorLog.log(Level.SEVERE,Messages.getString("ProgramData.13") + filename,e); //$NON-NLS-1$
		}
	}
	
	public void save() {
		buildStore();
		save(EVENT_STORE_LOC + this.name + ".xml"); //$NON-NLS-1$
	}
	
	public void load(String filename) {		
		// For some reason SaxBuilder() from JDOM
		// causes linkage errors. Build DOM this way.
        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document doc = null; 
        try {
           DocumentBuilder builder = factory.newDocumentBuilder();
           doc = (org.w3c.dom.Document) builder.parse(new File(filename));
 
        } catch (SAXException sxe) {
			errorLog.log(Level.SEVERE,Messages.getString("ProgramData.15") + filename,sxe); //$NON-NLS-1$
        } catch (ParserConfigurationException pce) {
        	errorLog.log(Level.SEVERE,Messages.getString("ProgramData.16") + filename,pce); //$NON-NLS-1$
        } catch (IOException ioe) {
        	errorLog.log(Level.SEVERE,Messages.getString("ProgramData.17") + filename,ioe); //$NON-NLS-1$
        }
		
        DOMBuilder doo = new DOMBuilder();
		Document document =	doo.build(doc);
        		
		Element root = document.getRootElement();
				
		this.name = root.getAttribute("name").getValue(); //$NON-NLS-1$

		if (root.getAttribute("enabled").getValue().equals("true")) //$NON-NLS-1$ //$NON-NLS-2$
			this.enabled = true;
		else
			this.enabled = false;

		this.executable = root.getAttribute("executable").getValue(); //$NON-NLS-1$

		Element observers = root.getChild("observers"); //$NON-NLS-1$
		List XMLobserverList = (List) observers
				.getChildren("observer"); //$NON-NLS-1$
		Iterator i = XMLobserverList.iterator();

		observerList.clear();
		while (i.hasNext())
			observerList.add(((Element) i.next()).getText());

		Element processes = root.getChild("processes"); //$NON-NLS-1$
		List XMLprocessList = (List) processes.getChildren("process"); //$NON-NLS-1$
		i = XMLprocessList.iterator();

		processList.clear();
		while (i.hasNext())
			processList.add(((Element) i.next()).getText());
	
	}
	
	private void buildStore()
	{
		File store = new File(EVENT_STORE_LOC);
		if (store.exists() == false)
			store.mkdirs();
	}

	public void delete() {
		File delete = new File(EVENT_STORE_LOC + this.name + ".xml"); //$NON-NLS-1$
		if (delete.isFile() && delete.canWrite())
			delete.delete();
	
	}
}
