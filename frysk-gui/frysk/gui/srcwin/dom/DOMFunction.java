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
import java.util.List;
//import java.util.Vector;

import org.jdom.Element;

/**
 * @author ajocksch
 *
 */
public class DOMFunction {
	public static final String END_ATTR = "end";
	public static final String START_ATTR = "start";
	public static final String INLINE_NODE = "inline";
	public static final String INLINENAME_ATTR = "inlinename";
	
	public static DOMFunction createDOMFunction(String name, int start, int end){
		Element func = new Element(INLINE_NODE);
		func.setAttribute(INLINENAME_ATTR, name);
		func.setAttribute(START_ATTR, ""+start);
		func.setAttribute(END_ATTR, ""+end);
		
		return new DOMFunction(func);
	}
	
	public static DOMFunction createDOMFunction(DOMImage parent, 
			String name, int start, int end){
		Element func = new Element(INLINE_NODE);
		func.setAttribute(INLINENAME_ATTR, name);
		func.setAttribute(START_ATTR, ""+start);
		func.setAttribute(END_ATTR, ""+end);
		parent.getElement().addContent(0, func); // We want functions, then lines		
		
		return new DOMFunction(func);
	}
	
	private Element myElement;
	
	public DOMFunction(Element data){
		this.myElement = data;
	}
	
	/**
	 * @return The name of the inlined code
	 */
	public String getName(){
		return this.myElement.getAttributeValue(INLINENAME_ATTR);
	}
	
	/**
	 * @return The length in lines of the code block that will be inlined
	 */
	public int getLineCount(){
		return this.myElement.getChildren().size();
	}
	
	/**
	 * @return The start of the inlined code as a char offset from the start of the file
	 */
	public int getStart(){
		return Integer.parseInt(this.myElement.getAttributeValue(START_ATTR));
	}
	
	/**
	 * @return The end of the inlined code as a char offset from the start of the file
	 */
	public int getEnd(){
		return Integer.parseInt(this.myElement.getAttributeValue(END_ATTR));
	}
	
	/**
	 * @return The number of the first line of inlined code
	 */
	public int getStartLine(){
		DOMLine firstChild = new DOMLine((Element) this.myElement.getChildren().get(0));
		return firstChild.getLineNum();
	}
	
	/**
	 * @return The number of the last line of inlined code
	 */
	public int getEndLine(){
		List children = this.myElement.getChildren();
		DOMLine lastChild = new DOMLine((Element) children.get(children.size()-1));
		
		return lastChild.getLineNum();
	}
	
	/**
	 * @return An iterator over the lines of code contained within the inlined block
	 */
//	public Iterator getLines(){
//		return this.myElement.getChildren().iterator();
		/* Iterator iter = this.myElement.getChildren().iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMLine((Element) iter.next()));
		
		return v.iterator(); 
	} */
	
	/**
	 * get the lines associated with the function
	 * @return A String array containing the lines
	 */
	public String[] getLines() {
		Iterator iter = this.myElement.
			getChildren(DOMSource.LINENO_NODE).iterator();
		int ctr = 0;
		int size = 30;
		String[] lines = new String[size];
		
		while(iter.hasNext()) {
			Element line = (Element) iter.next();
			lines[ctr] = line.getText();
			ctr++;
		}
		return lines;
	}
	protected Element getElement(){
		return this.myElement;
	}
}
