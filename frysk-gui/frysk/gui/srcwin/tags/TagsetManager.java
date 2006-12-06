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

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;

import frysk.Config;
import frysk.gui.monitor.ObjectFactory;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.UniqueHashMap;

/**
 * The TagsetManager keeps track of the tagsets available for frysk,
 * as well as maintaining the association between tagsets and what
 * executable command they are applicable to.
 */
public class TagsetManager {
	
	// singleton
	public static TagsetManager manager;
	
	private final String TAGSETS_DIR = Config.FRYSK_DIR + "Tagsets" + "/";
	static{
		// initialize our singleton
		manager = new TagsetManager();
	}
	
	private ObservableLinkedList tagsets;
	private UniqueHashMap nameHash;
	
	/**
	 * Create a new TagsetManager
	 *
	 */
	public TagsetManager(){
		tagsets = new ObservableLinkedList();
		nameHash = new UniqueHashMap();
		
		ObjectFactory.theFactory.makeDir(TAGSETS_DIR);
		this.load();
	}
	
	/**
	 * Saves the registered tag sets in the
	 * manager to disk
	 **/
	public void save(){
		Iterator iterator = this.getTagsets();
		while (iterator.hasNext()) {
			Tagset tagSet = (Tagset) iterator.next();
			if(tagSet.shouldSaveObject()){
				Element node = new Element("Tagset");
				ObjectFactory.theFactory.saveObject(tagSet, node);
				ObjectFactory.theFactory.exportNode( TAGSETS_DIR + tagSet.getName(), node);
			}
		}
	}
	
	/**
	 * Loads tag sets from disk and registers loaded
	 * tag sets with the manager.
	 **/
	public void load(){
		Element node = new Element("Tagset");
		File tagSetsDir = new File(this.TAGSETS_DIR);
		
		String[] array = tagSetsDir.list();
		Tagset loadedTagset = null;
		for (int i = 0; i < array.length; i++) {
			if(array[i].startsWith(".")){
				continue;
			}
			node = ObjectFactory.theFactory.importNode(TAGSETS_DIR+array[i]);
			loadedTagset = (Tagset)ObjectFactory.theFactory.loadObject(node);
			this.addTagset(loadedTagset);
		}
		
	}

	/**
	 * Adds the tagset to the manager. If the tagset already exists in the
	 * manager an IllegalArgumentException is thrown. 
	 * @param toAdd The tagset to store.
	 */
	public void addTagset(Tagset toAdd){
		if(tagsets.contains(toAdd)){
			throw new IllegalArgumentException("Tagset " + toAdd.getName() + " was already in manager");
		}
		this.nameHash.add(toAdd);
		this.tagsets.add(toAdd);
	}

	/**
	 * Removes the tagset from the manager. 
	 * @param tagSet The tagset to remove.
	 */
	public void removeTagset(Tagset tagSet){
		ObjectFactory.theFactory.deleteNode( TAGSETS_DIR + tagSet.getName());
		this.tagsets.remove(tagSet);
		this.nameHash.remove(tagSet);
	}
	
	/**
	 * Returns all of the tagsets applicable to the provided command
	 * @param command The command to get the tagsets for
	 * @return The tagsets applicable to command.
	 */
	public Iterator getTagsets(String command){
		Iterator iter = tagsets.iterator();
		
		Vector matches = new Vector();
		
		while(iter.hasNext()){
			Tagset set = (Tagset) iter.next();
			if(set.getCommand().equals(command))
				matches.add(set);
		}
		
		return matches.iterator();
	}
	
	/**
	 * 
	 * @return An iterator to all tagsets in the manager
	 */
	public Iterator getTagsets(){
		return tagsets.iterator();
	}
	
	/**
	 * 
	 * @return ObserverableLinkedList with all tagsets in the manager
	 */
 	public ObservableLinkedList getListTagsets()
 	{
 		return tagsets;
 	}
 	
	/**
	 * Returns the tagset that matches the name
	 * @param name The name of the tagset
	 * @return The tagset applicable to name.
	 */
	public Tagset getTagsetByName(String name) {
		return (Tagset) this.nameHash.get(name);
	}
	
	/**
	 * Checks for the presence of the given tagset in the TagsetManager
	 * @param set The Tagset to look for
	 * @return True iff the tagset is registered with the manager
	 */
	public boolean containsTagset(Tagset set){
		return this.tagsets.contains(set);
	}
}
