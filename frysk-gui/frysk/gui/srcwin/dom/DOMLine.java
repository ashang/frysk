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
import org.jdom.filter.Filter;

/**
 * DOMLine represents a line of code (not assembly instruction) in a file.
 * 
 * @author ajocksch
 */
public class DOMLine {
	/**
	 * Whether this line is executable or not
	 */
	public static final String EXECUTABLE_ATTR = "executable";

	/**
	 * Whether this line has inlined code or not
	 */
	public static final String HAS_INLINE_ATTR = "has_inline";

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
	 * Creates a new DOMLine using the given data as it's element. data must be
	 * a node with name "line".
	 * 
	 * @param data
	 */
	public DOMLine(Element data) {
		this.myElement = data;
	}

	/**
	 * @return The number of this line
	 */
	public int getLineNum() {
		return Integer.parseInt(this.myElement.getAttributeValue(NUMBER_ATTR));
	}

	/**
	 * @return The length of this line in characters
	 */
	public int getLength() {
		return Integer.parseInt(this.myElement.getAttributeValue(LENGTH_ATTR));
	}

	/**
	 * @return The offset of this line from the start of the file in characters
	 */
	public int getOffset() {
		return Integer.parseInt(this.myElement.getAttributeValue(OFFSET_ATTR));
	}

	/**
	 * sets the offset of the line from the beginning of the file
	 */
	public void setOffset(int offset) {
		this.myElement.setAttribute(OFFSET_ATTR, Integer.toString(offset));
	}

	/**
	 * @return Whether or not this line contains inlined code
	 */
	public boolean hasInlinedCode() {

		// Boolean.getBoolean(this.myElement.getAttributeValue(HAS_INLINE_ATTR));
		// //for some reason the original Boolean.getBoolean did not work
		// as advertised, so went back to the old tried and true
		if (this.myElement.getAttributeValue(HAS_INLINE_ATTR) == "true")
			return true;

		return false;
	}

	/**
	 * @return Whether or not this line is executable
	 */
	public boolean isExecutable() {
		// for some reason the original Boolean.getBoolean did not work
		// as advertised, so went back to the old tried and true
		if (this.myElement.getAttributeValue(EXECUTABLE_ATTR) == "true")
			return true;

		return false;
	}

	/**
	 * sets the executable attribute for this line
	 */
	public void setExecutable(boolean executable) {
		this.myElement.setAttribute(EXECUTABLE_ATTR, Boolean
				.toString(executable));
	}

	/**
	 * Check to see if this line has a breakpoint active
	 * 
	 * @return true if there is a breakpoint set here, false if not
	 */
	public boolean hasBreakPoint() {
		if (this.myElement.getAttributeValue(HAS_BREAK_ATTR) == "true")
			return true;

		return false;
	}

	/**
	 * Set the hasBreak attribute
	 */
	public void setBreakPoint(boolean hasbreak) {
		this.myElement.setAttribute(HAS_BREAK_ATTR, Boolean.toString(hasbreak));
	}

	/**
	 * @return The number of lines of inlined code contained within this line
	 */
	public int getInlinedCodeCount() {
		// TODO: does this need to be an attribute or refer to the earlier
		// nodes?
		return 0;
	}

	/**
	 * get the text associated with this line
	 * 
	 * @return the text of this line
	 */
	public String getText() {
		return this.myElement.getText();
	}

	/**
	 * set the text for this line to the incoming string
	 */
	public void setText(String text) {
		this.myElement.setText(text);
		this.myElement.setAttribute(LENGTH_ATTR, Integer
				.toString(text.length()));
	}

	/**
	 * @return An iterator to all the of tags contained on this line of code
	 */
	public Iterator getTags() {
		return this.myElement.getChildren(DOMTag.TAG_NODE).iterator();
	}

	/**
	 * Tries to find all the tags on this line of a given type
	 * 
	 * @param type
	 *            The type of tag to look for
	 * @return An iterator to all the tags of that type on the line
	 */
	public Iterator getTags(String type) {
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
		Vector v = new Vector();

		while (iter.hasNext())
			v.add(new DOMTag((Element) iter.next()));

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
	public DOMTag getTag(int index) {
		int lineStart = this.getOffset();
		if (index < lineStart)
			index += lineStart;

		Iterator iter = this.myElement.getChildren().iterator();

		while (iter.hasNext()) {
			DOMTag tag = new DOMTag((Element) iter.next());
			if (tag.isInRange(index))
				return tag;
		}

		return null;
	}

	public Iterator getInlines() {
		return this.myElement.getChildren(DOMInlineInstance.LINEINST_NODE).
				iterator();
	}

	/*
	 * public boolean hasBreakpoint(){ return this.myElement.getContent(new
	 * Filter() { private static final long serialVersionUID = 1L; public
	 * boolean matches(Object arg0) { Element elem = (Element) arg0;
	 * if(elem.getAttributeValue(DOMTag.TYPE_ATTR).equals("breakpoint")) return
	 * true; return false; } }).size() != 0; }
	 */

	/**
	 * add a tag element to this line
	 */
	public void addTag(DOMTag tag) {
		Element line_tag = new Element(DOMTag.TAG_NODE);
		line_tag.setAttribute(DOMTag.TYPE_ATTR, tag.getType());
		line_tag.setAttribute(DOMTag.START_ATTR, ""+tag.getStart());
		line_tag.setAttribute(DOMTag.END_ATTR, ""+tag.getEnd());
		this.myElement.addContent(line_tag);
	}

	/**
	 * add an inline instance to this line
	 * 
	 * @param instance
	 *            is the name of the instance to add
	 * @param start_inline
	 *            is the starting character of this instance
	 * @param end_line
	 *            is the ending character of this instance
	 */
	public void addInlineInst(String instance, int start_inline, int end_inline) {
		Element inlineLineInstElement = new Element(
				DOMInlineInstance.LINEINST_NODE);
		inlineLineInstElement.setAttribute(DOMInlineInstance.LINEINST_ATTR,
				instance);
		inlineLineInstElement.setAttribute(OFFSET_ATTR, Integer
				.toString(start_inline));
		inlineLineInstElement.setAttribute(LENGTH_ATTR, Integer
				.toString(end_inline));
		this.myElement.addContent(inlineLineInstElement);
	}

	/**
	 * get the DOMInlineInstance associated with this instance
	 * 
	 * @param inst
	 *            is the name of the instance to retrieve
	 * @return the DOMInlineInstance of this instance
	 */
	public DOMInlineInstance getInlineInst(String inst_name) {

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

	/**
	 * get the JDOM Element associated with this instance
	 * 
	 * @param inst
	 *            is the name of the instance to retrieve
	 * @return the JDOM Element of this instance
	 */
	public Element getInlineElement(String inst_name) {

		Iterator iter = this.myElement.getChildren().iterator();
		while (iter.hasNext()) {
			Element inst = (Element) iter.next();
			String name = inst
					.getAttributeValue(DOMInlineInstance.LINEINST_ATTR);
			if (name == inst_name) {
				return inst;
			}
		}
		return null;
	}
	/**
	 * get the inline instance of this line
	 * 
	 * @return the JDOM Element of the inline instance
	 */
	/*
	 * public DOMInlineInstance getInstElement() { Element inst =
	 * this.myElement.getChild(DOMInlineInstance.LINEINST_NODE);
	 * DOMInlineInstance val = new DOMInlineInstance((Element) inst); }
	 */

	/**
	 * get the JDOM Element of this line
	 * 
	 * @return Element associated with this line
	 */
	protected Element getElement() {
		return this.myElement;
	}
}
