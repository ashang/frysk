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
// 
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

package frysk.gui.srcwin.tags;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.SaveableXXX;

/**
 * A Tagset contains a collection of tags that are applicable to a
 * process.
 * 
 * TODO: Tags not implemented yet
 */
public class Tagset  extends GuiObject implements SaveableXXX{

	private String name;
	private String desc;
	private String command;
	private String version;
	
	private LinkedList tags;
	
	/**
	 * Creates a new tagset
	 * @param name The name of the tagset
	 * @param desc A brief description of the tagset
	 */
	public Tagset(String name, String desc, String command, String version){
		super(name,desc);
		this.name = name;
		this.desc = desc;
		this.command = command;
		this.version = version;
		this.tags = new LinkedList();
		doSaveObject();
	}
	
	public Tagset(){
		super();
		this.tags = new LinkedList();
	}
	
	/**
	 * Saves the tagset to a given element.
	 * @param Element XML node from manager
	 */
	public void save(Element node) {
		super.save(node);
		// Tag Sets
		node.setAttribute("command", this.command);
		node.setAttribute("version", this.version);
		Element tagsXML = new Element("tags");
		Iterator iterator = this.getTags();
		while (iterator.hasNext()) {
			Tag tag = (Tag) iterator.next();
			Element tagXML = new Element("tag");
			tag.save(tagXML);
			tagsXML.addContent(tagXML);
		}
		node.addContent(tagsXML);
	}
	
	/**
	 * Loads the tagset from a given element.
	 * @param Element XML node from manager
	 */
	public void load(Element node) {			
		super.load(node);
		
		//actions
		this.command = node.getAttribute("command").getValue();
		this.version = node.getAttribute("version").getValue();
		this.name = super.getName();
		this.desc = super.getToolTip();
	
		Element tagsXML = node.getChild("tags");
		List list = (List) (tagsXML.getChildren("tag"));
		Iterator iterator = list.iterator();
		while (iterator.hasNext()){
			Tag add = new Tag();
			add.load((Element)iterator.next());
			this.addTag(add);
		}

	}

	/**
	 * 
	 * @return The description of this tagset
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * 
	 * @return The name of this tagset
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return The command that this tagset associates with
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * 
	 * @return The version of the command that this tagset is designed to be
	 * used with
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Adds the given tag to this tagset. If this tag is already in this Tagset an
	 * IllegalArgumentException is raised.
	 * @param newTag The tag to add
	 */
	public void addTag(Tag newTag){
		if(tags.contains(newTag))
			throw new IllegalArgumentException("Attempting to add a tag to a tagset it already belongs to");
		
		this.tags.add(newTag);
		doSaveObject();
	}
	
	/**
	 * 
	 * @return An iterator to all the tags in this tagset
	 */
	public Iterator getTags(){
		return this.tags.iterator();
	}
	
	/**
	 * Check for the presence of the provided tag in the tagset
	 * @param tag The tag to look for 
	 * @return true iff the tag was found
	 */
	public boolean containsTag(Tag tag){
		return this.tags.contains(tag);
	}
	
	/**
	 * Two tagsets are equal if they are the same size and contain the same
	 * tags. Order is irrelevant
	 */
	public boolean equals(Object obj){
		if(!(obj instanceof Tagset))
			return false;
		
		Tagset set2 = (Tagset) obj;
		
		// If they don't have the same size, they're not equal
		if(set2.tags.size() != this.tags.size())
			return false;
		
		// check the name first of all: that's more telling than the contents
		if(!this.name.equals(set2.name) || !this.version.equals(set2.version) ||
				!this.desc.equals(set2.desc))
			return false;
		
		
		boolean same = true;

		// Tags are unique within a set, if they are the same size only need to
		// make sure that every tag contained within set A appears in set B
		Iterator iter1 = this.tags.iterator();
		
		while(iter1.hasNext())
			same = same && set2.containsTag((Tag) iter1.next());
		
		
		return same;
	}
}
