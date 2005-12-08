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
 * @author ajocksch
 *
 */
public class DOMFunction {
	
	public static final String FUNCTION_NODE = "function";
	
	public static final String END_ATTR = "end";
	public static final String START_ATTR = "start";
	public static final String FUNCTION_NAME_ATTR = "function_name";
	public static final String SOURCE_NAME_ATTR = "source";
	public static final String LINE_START_ATTR = "line_start";
	public static final String LINE_END_ATTR = "line_end";
	
	public static DOMFunction createDOMFunction(String name, String source,
			int lineStart, int lineEnd, int start, int end){
		Element func = new Element(FUNCTION_NODE);
		func.setAttribute(FUNCTION_NAME_ATTR, name);
		func.setAttribute(SOURCE_NAME_ATTR, source);
		func.setAttribute(START_ATTR, ""+start);
		func.setAttribute(END_ATTR, ""+end);
		func.setAttribute(LINE_START_ATTR, ""+lineStart);
		func.setAttribute(LINE_END_ATTR, ""+lineEnd);
		
		return new DOMFunction(func);
	}
	
	public static DOMFunction createDOMFunction(DOMImage parent, 
			String name, String source, 
			int lineStart, int lineEnd, int start, int end){
		DOMFunction func = DOMFunction.createDOMFunction(name, source, lineStart,
				lineEnd, start, end); 
		parent.getElement().addContent(0, 
				func.getElement()); // We want functions, then lines		
		
		return func;
	}
	
	private Element myElement;
	
	public DOMFunction(Element data){
		this.myElement = data;
	}
	
	/**
	 * @return The name of the inlined code
	 */
	public String getName(){
		return this.myElement.getAttributeValue(FUNCTION_NAME_ATTR);
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
	
	public String getSourceName(){
		return this.myElement.getAttributeValue(SOURCE_NAME_ATTR);
	}
	
	public int getStartingLine(){
		return Integer.parseInt(this.myElement.getAttributeValue(LINE_START_ATTR));
	}
	
	public int getEndingLine(){
		return Integer.parseInt(this.myElement.getAttributeValue(LINE_END_ATTR));
	}
	
	public String[] getLines(){
		int start = Integer.parseInt(this.myElement.getAttributeValue(LINE_START_ATTR));
		int end = Integer.parseInt(this.myElement.getAttributeValue(LINE_END_ATTR));
		
		String[] lines = new String[end - start ];
		
		// Parent of this should be a  DOMImage
		Element elem = this.getElement().getParentElement();

		DOMSource source = new DOMImage(elem).getSource(this.myElement.getAttributeValue(SOURCE_NAME_ATTR));
		
		for(int i = start; i< end; i++){
			String text = source.getLine(i).getText();
			System.out.print("Line "+ i +": "+text);
			if(text.equals(""))
				lines[i-start] = "\n";
			else
				lines[i-start] = text;
		}
		
		return lines;
	}
	
	/**
	 * returns the JDOM Element associated with this Function
	 * @return JDOM Element
	 */
	protected Element getElement(){
		return this.myElement;
	}
}

