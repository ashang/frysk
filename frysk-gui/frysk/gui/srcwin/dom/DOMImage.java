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

package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;

/**
 * DOMImage represents an image within the source window document object model
 * 
 * @author ajocksch
 */
public class DOMImage {
	/**
	 * CCPATH for the image
	 */
	public static final String CCPATH_ATTR = "CCPATH";
	/**
	 * name of the inline element
	 */
		
	public static final String INLINE_NODE = "inline";
	private Element myElement;
	
	/**
	 * Creates a new DOMImage from the given Element. Data must be of name "image".
	 * @param data An Element of name "image"
	 */
	public DOMImage(Element data){
		this.myElement = data;
	}
	
	/**
	 * adds a source element under this image
	 * @param source_name
	 * @param path
	 * @return
	 */
	public void addSource(String source_name, String path) {
		
		Element sourceNameElement = new Element(DOMSource.SOURCE_NODE);
		sourceNameElement.setAttribute(DOMSource.FILENAME_ATTR, source_name);
		sourceNameElement.setAttribute(DOMSource.FILEPATH_ATTR, path);
		this.myElement.addContent(sourceNameElement);
	}
	
	/**
	 * adds an inline function to an image
	 * @param inline_name is the name of the inline function
	 * @param lines is an array of Strings containing the lines in the function
	 * @param start_offset is the starting character offset from the beginning
	 * 					of the file of the first character of the function
	 * @param end_offset is the ending character offset from the beginning
	 * 					of the file of the last character of the function
	 */
	public void addFunction(String inline_name, String source, 
			int startLine, int endLine,
			int start_offset, int end_offset) {
		DOMFunction.createDOMFunction(this, inline_name, source, startLine, endLine,
				start_offset, end_offset);
	}
	
	/**
	 * @return The name of the image
	 */
	public String getName(){
		return this.myElement.getAttributeValue(DOMSource.FILENAME_ATTR);
	}
	
	/**
	 * Sets the CCPATH of the current image
	 * @param image_name
	 */
	public void setCCPath(String image_name) {
		this.myElement.setAttribute(CCPATH_ATTR, image_name);
		return;
	}
	/**
	 * 
	 * @param name what the name of the image will be
	 */
	public void setName(String name) {
		
	}
	/**
	 * @return The CCPATH of the image
	 */
	public String getCCPath(){
		return this.myElement.getAttributeValue(CCPATH_ATTR);
	}
	
	/**
	 * @return an iterator to all the source files contained in this image.
	 */
	public Iterator getSources(){
		return this.myElement.getChildren(DOMSource.SOURCE_NODE).iterator();
	}
	
	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is
	 * found returns null
	 * 
	 * @param name
	 *            The name of the image to look for
	 * @return The DOMSource corresponding to the element, or null if no such
	 *         element exists
	 */
	public DOMSource getSource(String name) {
		Iterator i = this.myElement.getChildren().iterator();

		while (i.hasNext()) {
			Element elem = (Element) i.next();
			if (elem.getQualifiedName().equals(DOMSource.SOURCE_NODE)) {
				if (elem.getAttributeValue(DOMSource.FILENAME_ATTR)
						.equals(name))
					return new DOMSource(elem);
			}
		}
		return null;
	}
	
	/**
	 * attempts to fetch an inlined function DOM element
	 * @param name of the inlined function to return
	 * @return the DOMImage corresponding to the element, or null if no such
	 * 	element exists
	 */
	public DOMFunction getFunction(String name) {
		Iterator iter = this.myElement.getChildren(INLINE_NODE).iterator();
		while (iter.hasNext()) {
			Element node = (Element) iter.next();
			if (node.getAttributeValue(DOMFunction.INLINENAME_ATTR) == name)
				return new DOMFunction (node);
		}
		return null;
	}
	
	/**
	 * fetches all of the inlined functions for this DOMImage 
	 * @return an iterator of all of the inlined functions for this DOMImage
	 */

	public Iterator getInlinedFunctions(){
		Iterator iter = this.myElement.getChildren(INLINE_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add((Element) iter.next());
		
		return v.iterator();
	}
	
	/**
	 * This function should only be used internally within the frysk source dom
	 * @return The JDom element at the core of this node
	 */
	protected Element getElement() {
		return this.myElement;
	}
}
