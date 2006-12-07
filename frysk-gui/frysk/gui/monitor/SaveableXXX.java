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
 * An iterface for objects that can be save/loaded from XML.
 * 
 * Implementors of this object need to have a no argument constuctor
 * to be used when it is being reconstructed from disk. The heavy
 * lifting that would normally be in the constructor should then me
 * moved to load().
 */
public interface SaveableXXX {
	
	
	/**
	 * Save object to the given node
	 * Saving Conventions:
	 * If this object has a property such as a
	 * String, char, int, etc, that is saved by
	 * using node.setAttribute("someProperty", someProperty);
	 * If this object contains composing @link frysk.gui.monitor.SaveableXXX
	 * then a node is created for that and that object is told to save itself
	 * to that node. Example:
	 *      
	 *      Element filtersNode = new Element("filtersList");
	 *      this.filtersList.save(filtersNode);
	 *      node.addContent(filtersNode);
	 *      
	 * Be careful some times one wants to save the objects name rather
	 * than the object itself, and then use the name to later recreate
	 * that object via a managers. Like observers for instance. Perhaps
	 * objects such as these should be smart enough to use a manager to
	 * save/load themselfs, that way an api client doesnt have to worry
	 * ... one day :).
	 * @param node
	 */
	void save(Element node);
	
	/**
	 * To load a stored property one does this:
	 * 		String someString = node.getAttributeValue("someString");
	 * To load @link frysk.gui.monitor.SaveableXXX one gets the appropriet
	 * node and tells that object to load itself from that node.
	 * Example:
	 *      
	 *      Element filtersNode = node.getChild("filtersList");
	 *		this.filtersList.load(filtersNode);
	 *      
	 * @param node
	 */
	void load(Element node);
	
	/**
	 * queried to see if this object should be saved
	 * or not
	 * @return wether this object should be saved or not
	 */
	boolean shouldSaveObject();
	
	/**
	 * Object will be savable if save is called
	 */
	void doSaveObject();

	/**
	 * object will not be saved if save is called
	 */
	void dontSaveObject();
}
