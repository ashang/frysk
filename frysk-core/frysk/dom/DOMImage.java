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

package frysk.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;

/**
 * DOMImage represents an image within the source window document
 * object model
 */
public class DOMImage {
	/**
	 * CCPATH for the image
	 */
	public static final String CCPATH_ATTR = "CCPATH";
	private Element myElement;
	public static final String IMAGE_NODE = "image";
	public static final String NAME_ATTR = "filename";
	public static final String PATH_ATTR = "filepath";
    public DOMFrysk dom;
	
	/**
	 * Creates a new DOMImage from the given Element. Data must be of name "image".
	 * @param data A JDOM Element of name "image"
	 */
	public DOMImage(Element data){
		this.myElement = data;
	}
	
	/**
	 * Creates a new DOMImage
	 * @param name The name of the image
	 * @param path the path to the image
	 * @param ccpath The CCPATH for the image
	 */
	public DOMImage(String name, String path, String ccpath, Element rootElement){
		myElement = new Element(DOMImage.IMAGE_NODE);
		myElement.setAttribute(NAME_ATTR, name);
		myElement.setAttribute(PATH_ATTR, path);
		myElement.setAttribute(CCPATH_ATTR, ccpath);
	}
	
	/**
	 * adds a source element under this image
	 * @param source_name
	 * @param path
	 * @return
	 */
	public void addSource(String source_name, String path, String[] incpaths) {
		this.addSource(new DOMSource(source_name, path, incpaths));
	}
	
	/**
	 * Adds the given DOMSource as a source in this image
	 * @param source The source to add
	 */
	public void addSource(DOMSource source){
		this.myElement.addContent(source.getElement());
	}
	
	/**
	 * adds an inline function to an image
	 * @param inline_name is the name of the inline function
	 * @param source is the name of the source this function came from
	 * @param startLine is the starting line number of this function in the source
	 * @param endLine is the ending line number of this function in the source
	 * @param start_offset is the starting character offset from the beginning
	 * 					of the file of the first character of the function
	 * @param end_offset is the ending character offset from the beginning
	 * 					of the file of the last character of the function
	 */
	public void addFunction(String inline_name, String source, 
			int startLine, int endLine,
			int start_offset, int end_offset, String function_call) {
		DOMFunction.createDOMFunction(this, inline_name, source, startLine, endLine,
				start_offset, end_offset, function_call);
	}
	
	/**
	 * gets the name of the image
	 * @return The name of the image
	 */
	public String getName(){
		return this.myElement.getAttributeValue(NAME_ATTR);
	}
	
	/**
	 * Sets the CCPATH of the current image
	 * @param image_name is the image for which this CCPATH is intended
	 */
	public void setCCPath(String image_name) {
		this.myElement.setAttribute(CCPATH_ATTR, image_name);
		return;
	}
	/**
	 * sets the name of the image
	 * @param name what the name of the image will be
	 */
	public void setName(String name) {
		
	}
	/**
	 * returns the name of the image
	 * @return The CCPATH of the image
	 */
	public String getCCPath(){
		return this.myElement.getAttributeValue(CCPATH_ATTR);
	}
	
	/**
	 * gets an iterator pointing to all of the sources belonging to this image
	 * @return an iterator to all the source files contained in this image.
	 */
	public Iterator getSources(){
		return this.myElement.getChildren(DOMSource.SOURCE_NODE).iterator();
	}
	
	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is
	 * found returns null
	 * 
	 * @param name is the name of the image to look for
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
		Iterator iter = this.myElement.getChildren(DOMFunction.FUNCTION_NODE).iterator();
		while (iter.hasNext()) {
			Element node = (Element) iter.next();
			if (node.getAttributeValue(DOMFunction.FUNCTION_NAME_ATTR) == name)
				return new DOMFunction (node);
		}
		return null;
	}
	
	/**
	 * 
	 * @return An iterator to all functions in this image
	 */
	public Iterator getFunctions(){
		Vector v = new Vector();
		Iterator iter = this.myElement.getChildren(DOMFunction.FUNCTION_NODE).iterator();
		while (iter.hasNext()) 
			v.add(new DOMFunction((Element) iter.next()));
		
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
