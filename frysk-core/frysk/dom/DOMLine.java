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
import java.util.LinkedList;

import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * DOMLine represents a line of code (not assembly instruction) in a file.
 */
public class DOMLine 
{
	/**
	 * Whether this line is executable or not
	 */
	public static final String EXECUTABLE_ATTR = "executable";

	/**
	 * Whether this line has inlined code or not
	 */
	public static final String HAS_BREAK_ATTR = "has_break";

	/**
	 * The offset in characters from the start of the file
	 */
	public static final String OFFSET_ATTR = "offset";

	/**
	 * The length of the line including the \n
	 */
	public static final String LENGTH_ATTR = "length";

	/**
	 * The name of the Element node
	 */
	public static final String LINE_NODE = "line";

	/**
	 * The number of this line
	 */
	public static final String NUMBER_ATTR = "number";

	private Element myElement;

	/**
	 * Creates a new DOMLine
	 * @param lineNo
	 * 		The line number of this line
	 * @param lineText
	 * 		The text on this line
	 * @param offset
	 * 		The offset in characters from the start of the file
	 * @param executable
	 * 		Whether this line is executable or not
	 * @param hasBreakpoint
	 * 		Whether this line has any breakpoints on it or not
	 * @param address
	 * 		The program counter value.
	 */
	public DOMLine (int lineNo, String lineText, int offset, 
			boolean executable, boolean hasBreakpoint, long address)
    {
		this.myElement = new Element(DOMLine.LINE_NODE);
		myElement.setText(lineText);
		myElement.setAttribute(DOMLine.NUMBER_ATTR, Integer.toString(lineNo));
		myElement.setAttribute(DOMSource.ADDR_ATTR, ""+address);
		myElement.setAttribute(DOMLine.OFFSET_ATTR, Integer.toString(offset));
		myElement.setAttribute(DOMLine.LENGTH_ATTR, Integer.toString(lineText.length()));
		myElement.setAttribute(DOMLine.EXECUTABLE_ATTR, ""+executable);
		myElement.setAttribute(DOMLine.HAS_BREAK_ATTR, ""+hasBreakpoint);
	}
	
	/**
	 * Creates a new DOMLine using the given data as it's element. data must be
	 * a node with name "line".
	 * 
	 * @param data is a JDOM element named "line"
	 */
	public DOMLine (Element data) 
    {
		this.myElement = data;
	}

	/**
	 * gets the lines niumber within the source file of this line
	 * 
	 * @return The number of this line
	 */
	public int getLineNum () 
    {
		return Integer.parseInt(this.myElement.getAttributeValue(NUMBER_ATTR));
	}

	/**
	 * gets the length of this line in characters
	 * 
	 * @return The length of this line in characters
	 */
	public int getLength () 
    {
		return Integer.parseInt(this.myElement.getAttributeValue(LENGTH_ATTR));
	}

	/**
	 * gets the offset in characters of this line from the start of the source file
	 * 
	 * @return The offset of this line from the start of the file in characters
	 */
	public int getOffset () 
    {
		return Integer.parseInt(this.myElement.getAttributeValue(OFFSET_ATTR));
	}

	/**
	 * sets the offset of the line from the beginning of the file
	 * 
	 * @param character offset from the beginning of the file
	 */
	public void setOffset (int offset) 
    {
		this.myElement.setAttribute(OFFSET_ATTR, ""+offset);
	}
	
	/**
	 * return a boolean indicating whether or not this line contains an inline function
	 * 
	 * @return Whether or not this line contains inlined code
	 */
	public boolean hasInlinedCode () 
    {
		return !this.myElement.getChildren(DOMInlineInstance.INLINE_NODE).isEmpty();
	}

	/**
	 * return a boolean indicating whether or not this line is executable
	 * 
	 * @return whether or not this line is executable
	 */
	public boolean isExecutable () 
    {
		
		if (this.myElement.getAttributeValue(EXECUTABLE_ATTR).equals("true"))
			return true;

		return false;
	}

	/**
	 * sets the executable attribute for this line
	 * 
	 * @param executable is the boolean value to set the executable attribute to
	 */
	public void setExecutable (boolean executable)
    {
		this.myElement.setAttribute(EXECUTABLE_ATTR, 
				""+executable);
	}

	/**
	 * Check to see if this line has a breakpoint active
	 * 
	 * @return true if there is a breakpoint set here, false if not
	 */
	public boolean hasBreakPoint () 
    {
		if (this.myElement.getAttributeValue(HAS_BREAK_ATTR).equals("true"))
			return true;

		return false;
	}

	/**
	 * Set the hasBreak attribute(indicates that this line has a breakpoint set)
	 * 
	 * @param hasbreak is the boolean value to set the hasbreak value to 
	 */
	public void setBreakPoint (boolean hasbreak)
    {
		this.myElement.setAttribute(HAS_BREAK_ATTR, ""+hasbreak);
	}

	/**
	 * get the text associated with this line
	 * 
	 * @return the text of this line
	 */
	public String getText () 
    {
		return this.myElement.getText();
	}

	/**
	 * set the text for this line to the incoming string
	 * 
	 * @param text is the text of the source line
	 */
	public void setText (String text) 
    {
		this.myElement.setText(text);
		this.myElement.setAttribute(LENGTH_ATTR, ""+text.length());
	}

	/**
	 * get an iterator to all of the tags contained on this line of code
	 * 
	 * @return An iterator to all the of tags contained on this line of code
	 */
	public Iterator getTags ()
    {
		return this.myElement.getChildren(DOMTag.TAG_NODE).iterator();
	}

	/**
	 * adds a new tag to a source line
	 * 
	 * @param type - the type of tag(keyword, variable, function)
	 * @param token - token associated with this tag
	 * @param start - the starting character within the line
	 */
	public void addTag (String type, String token, int start)
    {
      // Don't add blank tokens
      if (token.equals("")) 
        return;
      // Sometimes the CDT Parser returns a token with a space on the end, nip it
      if (token.endsWith(" "))
      {
        token = token.substring(0, token.length() - 1);
      }
		// Check for duplicate tags
		Iterator elements = this.myElement.getChildren().iterator();
		while(elements.hasNext()){
			Element element = (Element) elements.next();
			
			int elStart = Integer.parseInt(element.getAttributeValue(DOMTag.START_ATTR));
			String elType = element.getAttributeValue(DOMTag.TYPE_ATTR);
			
			// only one function body per line
			if(type.equals(DOMTagTypes.FUNCTION_BODY) && elType.equals(DOMTagTypes.FUNCTION_BODY))
				return;
			
			// we're more particular with the other tags, only one of each tag
			// at a given point on the line.
            if(start == elStart && type.equals(elType))
				return;
		}

		this.addTag(new DOMTag(type, token, start));
	}

	/**
	 * Tries to find all the tags on this line of a given type
	 * 
	 * @param type
	 *            The type of tag to look for
	 * @return An iterator to all the tags of that type on the line
	 */
	public Iterator getTags (String type) 
    {
		final String theType = type;

		Iterator iter = this.myElement.getContent(new Filter() {
			static final long serialVersionUID = 1L;

			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;

				if (elem.getName().equals(DOMTag.TAG_NODE)
						&& elem.getAttributeValue(DOMTag.TYPE_ATTR).equals(
								theType))
					return true;
				return false;
			}
		}).iterator();
		LinkedList v = new LinkedList();

		while (iter.hasNext()){
			Element e = (Element) iter.next();
			v.add(new DOMTag(e));
		}

		return v.iterator();
	}

	/**
	 * Returns the tag at the given index of the line. Index can either be from
	 * the start of the file or from the start of the line
	 * 
	 * @param index
	 *            Offset to look for a tag at
	 * @return The tag (if any) at that position
	 */
	public DOMTag getTag (int index) 
    {
		Iterator iter = this.myElement.getChildren(DOMTag.TAG_NODE).iterator();
		
		while (iter.hasNext()) {
			Element elem = (Element) iter.next();
			DOMTag tag = new DOMTag(elem);
			
			if (tag.isInRange(index))
				return tag;
		}

		return null;
	}
	
	/**
	 * gets all of the inline instances attached with is line
	 * 
	 * @return an iterator pointing to all inline instances on this line
	 */

	public Iterator getInlines ()
    {
		return this.myElement.getChildren(DOMInlineInstance.LINEINST_NODE).
				iterator();
	}

	/**
	 * add a tag element to this line
	 * 
	 * @param tag is the element to add to this line
	 */
	public void addTag (DOMTag tag) 
    {
		this.myElement.addContent(tag.getElement());
	}

	/**
	 * add an inline instance to this line
	 * 
	 * @param instance
	 *            is the name of the instance to add
	 * @param start_inline
	 *            is the starting character of this instance in this line
	 * @param end_line
	 *            is the ending character of this instance in this line
	 */
	public void addInlineInst (String instance, int start_inline, int length, int PCLine) 
    {
		this.myElement.addContent(new DOMInlineInstance(instance, start_inline, length, PCLine).getElement());
	}

	/**
	 * get the DOMInlineInstance associated with this instance
	 * 
	 * @param inst is the name of the instance to retrieve
	 * 
	 * @return the DOMInlineInstance of this instance
	 */
	public DOMInlineInstance getInlineInst (String inst_name) 
    {

		Iterator iter = this.myElement.getChildren().iterator();
		while (iter.hasNext()) {
			Element inst = (Element) iter.next();
			String name = inst
					.getAttributeValue(DOMInlineInstance.LINEINST_ATTR);
			if (name == inst_name) {
				DOMInlineInstance val = new DOMInlineInstance((Element) inst);
				return val;
			}
		}
		return null;
	}

	public long getAddress ()
    {
		return Long.parseLong(this.myElement.getAttributeValue(DOMSource.ADDR_ATTR));
	}
	
	/**
	 * get the JDOM Element of this line
	 * 
	 * @return Element associated with this line
	 */
	protected Element getElement ()
    {
		return this.myElement;
	}
}
