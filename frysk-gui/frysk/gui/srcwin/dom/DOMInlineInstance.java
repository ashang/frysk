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

//import java.util.Vector;

import org.jdom.Element;

/**
 * DOMInlineInstance represents the instance of a piece of inlined code. It contains the 
 * information specific to this instance as well as a reference to the declaration to speed up
 * parsing time and for reference.
 * @author ajocksch
 */
public class DOMInlineInstance {
	private int start;
	private int end;
	
	public static final String LINEINST_NODE = "inline";
	
	public static final String LINEINST_ATTR = "instance";
	
	private Element myElement;
	
//	public DOMInlineInstance(int start, int end){
//		this.start = start;
//		this.end = end;
//	}
	
	/**
	 * Creates a new DOMLine using the given data as it's element. data must be a node with
	 * name "inline".
	 * @param data
	 */
	public DOMInlineInstance(Element data){
		this.myElement = data;
	}
	
	public Element getInlineInstance() {
		return this.myElement;
	}
	
	/**
	 * set the starting character of the inlined code
	 */
	public void setStart(int start) {
			this.myElement.setAttribute(DOMFunction.START_ATTR, 
					Integer.toString(start));
			this.start = start;
	}
	/**
	 * @return The start of the inlined instance as a character offset from the start of the file
	 */
	public int getStart(){
		return this.start;
	}
	
	/**
	 * set the ending character of the inlined code
	 */
	public void setEnd(int end) {
			this.myElement.setAttribute(DOMFunction.END_ATTR, 
					Integer.toString(end));
			this.end = end;
	}
	/**
	 * @return The end of the instance as a character offset from the start of the file
	 */
	public int getEnd(){
		return this.end;
	}
	
	/** 
	 * @return The original declaration of this inlined code
	 */
	public DOMFunction getDeclaration(){
		String funcName = this.myElement.getAttributeValue(LINEINST_ATTR);
		
		Element parent = this.myElement.getParentElement();
		while(parent != null && !parent.getName().equals(DOMFrysk.IMAGE_ATTR))
			parent = parent.getParentElement();
		
		if(parent != null)
			return new DOMImage(parent).getFunction(funcName);
		
		return null;
	}
}
