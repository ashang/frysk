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

import org.jdom.Element;

/**
 * DOMTag represents a tagged area (i.e. function declaration,
 * varaible use, comment, etc) in a source code file
 */
public class DOMTag {
	/**
	 * The end of the tag
	 */
	public static final String LENGTH_ATTR = "length";
	/**
	 * The start of the tag
	 */
	public static final String START_ATTR = "start";
	/**
	 * The type of the tag
	 */
	public static final String TYPE_ATTR = "type";
	/**
	 * The actual token of this tag
	 */
	public static final String TOKEN_ATTR = "token";
	/**
	 * The name of the DOM Element
	 */
	
	public static final String TAG_NODE = "tag";
	private Element myElement;
	
	/**
	 * Creates a new DOMTag
	 * @param type
	 * 		The type of the tag
	 * @param token
	 * 		The token
	 * @param start
	 * 		The start of the tag on the given line
	 */
	public DOMTag(String type, String token, int start){
		Element tag = new Element(TAG_NODE);
		tag.setAttribute(TYPE_ATTR, type);
		tag.setAttribute(START_ATTR, ""+start);
		tag.setAttribute(LENGTH_ATTR, ""+token.length());
		tag.setAttribute(TOKEN_ATTR, token);
		this.myElement = tag;
	}
	
	/**
	 * Creates a new DOMTag using the given data as it's Element. Data must be of name "tag"
	 * 
	 * @param data is the JDOM element
	 * 
	 * @return returns an instance of this DOMTag
	 */
	public DOMTag(Element data){
		this.myElement = data;
	}
	
	/**
	 * change the type attribue of this tag
	 * 
	 * @param type is the tag type to set 
	 */
	public void setType(String type) {
		this.myElement.setAttribute(TYPE_ATTR, type);
	}
	
	/**
	 * get the type attribute for this tag instance
	 * 
	 * @return The type of the tag
	 */
	public String getType(){
		return this.myElement.getAttributeValue(TYPE_ATTR);
	}
	
	/**
	 * set the starting character offset of this tag
	 * 
	 * @param start is the character offset for this tag
	 */
	public void setStart(int start) {
		this.myElement.setAttribute(START_ATTR, ""+start);
	}
	
	/**
	 * get the character offset from the start of the file
	 * 
	 * @return The starting offset of the tag from the start of the file
	 */
	public int getStart(){
		return Integer.parseInt(this.myElement.getAttributeValue(START_ATTR));
	}
	
	/**
	 * set the length of this tag on this line
	 * 
	 * @param the ending character of this tag
	 */
	public void setLength(int end) {
		this.myElement.setAttribute(LENGTH_ATTR, ""+end);
	}
	/**
	 * gets the length of the tag
	 * 
	 * @return The length of the tag
	 */
	public int getLength(){
		return Integer.parseInt(this.myElement.getAttributeValue(LENGTH_ATTR));
	}
	
	/**
	 * Tests to see if this tag encompasses the given index
	 * @param test The index, relative to the start of the file, to test
	 * @return true if the tag covers the index, false otherwise
	 */
	public boolean isInRange(int test){
		if(test >= this.getStart() && test - this.getStart() <= this.getLength())
			return true;
		
		return false;
	}
	
	/**
	 * get the token attribute for this tag
	 * 
	 * @return The token for this tag
	 */
	public String getToken(){
		return this.myElement.getAttributeValue(TOKEN_ATTR);
	}
	
	/**
	 * 
	 * @return the JDOM element for this tag
	 */
	
	protected Element getElement(){
		return this.myElement;
	}
}
