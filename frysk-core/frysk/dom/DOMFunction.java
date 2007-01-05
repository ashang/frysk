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
 * DOMFunction represents a function element to the DOM for any
 * functions found within an image.
 */
public class DOMFunction 
{
	
	public static final String FUNCTION_NODE = "function";
	
	public static final String END_ATTR = "end";
	public static final String START_ATTR = "start";
	public static final String FUNCTION_NAME_ATTR = "function_name";
	public static final String SOURCE_NAME_ATTR = "source";
	public static final String LINE_START_ATTR = "line_start";
	public static final String LINE_END_ATTR = "line_end";
    public static final String FUNCTION_CALL = "function_call";
    
    private DOMSource parent;
	
	/**
	 * creates a DOMFunction
	 * 
	 * @param name is the name of this DOMFunction
	 * @param source is the source this function was found in
	 * @param lineStart is the starting line number in the source file of this function
	 * @param lineEnd is the ending line number in the source file of this function
	 * @param start is the starting character of the function from the start of the file
	 * @param end is the ending character of the function from the start of the file
	 * @return the created DOMFunction
	 */
	
	public static DOMFunction createDOMFunction (String name, String source,
			int lineStart, int lineEnd, int start, int end, String func_call)
    {
		Element func = new Element(FUNCTION_NODE);
		func.setAttribute(FUNCTION_NAME_ATTR, name);
		func.setAttribute(SOURCE_NAME_ATTR, source);
		func.setAttribute(START_ATTR, ""+start);
		func.setAttribute(END_ATTR, ""+end);
		func.setAttribute(LINE_START_ATTR, ""+lineStart);
		func.setAttribute(LINE_END_ATTR, ""+lineEnd);
        
        String[] callItems = func_call.split("\\s+");
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < callItems.length; i++)
          buffer.append(callItems[i]);
          
        func.setAttribute(FUNCTION_CALL, buffer.toString());
		
		return new DOMFunction(func);
	}
	
	/**
	 * creates a DOMFunction element to an image in the DOM
	 * 
	 * @param parent is the image element to attach this DOMFunction to
	 * @param name is the name of this DOMFunction
	 * @param source is the source this function was found in
	 * @param lineStart is the starting line number in the source file of this function
	 * @param lineEnd is the ending line number in the source file of this function
	 * @param start is the starting character of the function from the start of the file
	 * @param end is the ending character of the function from the start of the file
	 * @return the created DOMFunction
	 */
	public static DOMFunction createDOMFunction (DOMSource parent, 
			String name, String source, 
			int lineStart, int lineEnd, int start, int end, String func_call)
    {
		DOMFunction func = DOMFunction.createDOMFunction(name, source, lineStart,
				lineEnd, start, end, func_call); 
		parent.getElement().addContent(0, 
				func.getElement()); // We want functions, then lines		
		
		return func;
	}
    
    public void setParent (DOMSource parent)
    {
      this.parent = parent;
    }
	
	private Element myElement;
	
	/**
	 * assign a JDOM element to this function name
	 * 
	 * @param data is a JDOM element
	 */
	public DOMFunction (Element data)
    {
		this.myElement = data;
	}
	
	/**
	 * gets the name of the inlined code
	 * 
	 * @return The name of the inlined code
	 */
	public String getName ()
    {
		return this.myElement.getAttributeValue(FUNCTION_NAME_ATTR);
	}
	
	/**
	 * gets the length in lines of the code block that will be inlined
	 * 
	 * @return The length in lines of the code block that will be inlined
	 */
	public int getLineCount ()
    {
		return this.myElement.getChildren().size();
	}
	
	/**
	 * gets the char offset from the start of the file of the first char of the function
	 * 
	 * @return The start of the inlined code as a char offset from the start of the file
	 */
	public int getStart ()
    {
		return Integer.parseInt(this.myElement.getAttributeValue(START_ATTR));
	}
	
	/**
	 * gets the char offset from the start of the file of the last char of the function
	 * 
	 * @return The end of the inlined code as a char offset from the start of the file
	 */
	public int getEnd ()
    {
		return Integer.parseInt(this.myElement.getAttributeValue(END_ATTR));
	}
	
    /**
     * gets the char offset from the start of the file of the last char of the function
     * 
     * @param The end of the function/inlined code as a char offset from the start of the file
     */
    public void setEnd (int endingchar)
    {
      this.myElement.setAttribute(END_ATTR, ""+endingchar);
    }
	/**
	 * gets the name of the source that this function came from
	 * 
	 * @return the source that this function came from, null if cannot find
	 */
	public DOMSource getSource ()
    {
        return this.parent;
        
//		Element parent = this.myElement.getParentElement();
//		while(parent != null && !parent.getName().equals(DOMImage.IMAGE_NODE))
//			parent = parent.getParentElement();
//		
//		if(parent != null){
//			DOMImage image = new DOMImage(parent);
//			return image.getSource(sourceName);
//		}
//		
//		return null;
	}
	
	/**
	 * gets the starting line number in the source file for this function
	 * 
	 * @return the starting line number in the source file
	 */
	public int getStartingLine ()
    {
		return Integer.parseInt(this.myElement.getAttributeValue(LINE_START_ATTR));
	}
    
    /**
     * sets the ending line number in the source file for this function
     * 
     * @param an integer with the starting line number in the source file to set it to
     */
    public void setEndingLine (int linenum)
    {
      this.myElement.setAttribute(LINE_END_ATTR, ""+linenum);
      return;
    }
	
	/**
	 * gets the ending line number in the source file for this function
	 * 
	 * @return the ending line number in the source file
	 */
	public int getEndingLine ()
    {
		return Integer.parseInt(this.myElement.getAttributeValue(LINE_END_ATTR));
	}
	
	/**
	 * gets the lines associated with this function
	 * 
	 * @return a String array containing the source lines for this function
	 */
	public String[] getLines ()
    {
		int start = Integer.parseInt(this.myElement.getAttributeValue(LINE_START_ATTR));
		int end = Integer.parseInt(this.myElement.getAttributeValue(LINE_END_ATTR));
		
		String[] lines = new String[end - start ];
		
		// Parent of this should be a  DOMImage
		Element elem = this.getElement().getParentElement();

		DOMSource source = new DOMImage(elem).getSource(this.myElement.getAttributeValue(SOURCE_NAME_ATTR));
		
		for (int i = start; i< end; i++)
          {
			String text = source.getLine(i).getText();
			if (text.equals(""))
				lines[i-start] = "\n";
			else
				lines[i-start] = text;
		}
		
		return lines;
	}
    
    public String getFunctionCall ()
    {
      return this.myElement.getAttributeValue(FUNCTION_CALL);
    }
	
	/**
	 * returns the JDOM Element associated with this Function
	 * @return JDOM Element
	 */
	protected Element getElement ()
    {
		return this.myElement;
	}
}

