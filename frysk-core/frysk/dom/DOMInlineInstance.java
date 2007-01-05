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
 * DOMInlineInstance represents the instance of a piece of inlined code. It
 * contains the information specific to this instance as well as a reference to
 * the declaration to speed up parsing time and for reference.
 */
public class DOMInlineInstance
{
  private int start;

  private int end;

  public static final String LINEINST_NODE = "inline";

  public static final String LINEINST_ATTR = "instance";

  public static final String PCLINE_ATTR = "PC_line";

  public static final String OFFSET_ATTR = "offset";

  public static final String LENGTH_ATTR = "length";
  
  public static final String LINE_NUM = "line";

  private Element myElement;
  
  private DOMSource parent;

  /**
   * name of the inline element
   */
  public static final String INLINE_NODE = "inline";

  /**
   * Creates a new DOMInlineInstance
   * 
   * @param name The name of the inline instance
   * @param offset The offset of the inline instance from the start of the line
   * @param length The length of the inline instance
   * @param PCLine The program counter line number within the inline block.
   * @param line    The line in the source jumping to this inline instance
   */
  public DOMInlineInstance (String name, int offset, int length, int PCLine, int line)
  {
    this.myElement = new Element(LINEINST_NODE);
    myElement.setAttribute(DOMInlineInstance.LINEINST_ATTR, name);
    myElement.setAttribute(OFFSET_ATTR, Integer.toString(offset));
    myElement.setAttribute(LENGTH_ATTR, Integer.toString(length));
    myElement.setAttribute(DOMInlineInstance.PCLINE_ATTR,
                           String.valueOf(PCLine));
    myElement.setAttribute(LINE_NUM, Integer.toString(line));
  }

  /**
   * Creates a new DOMLine using the given data as it's element. data must be a
   * node with name "inline".
   * 
   * @param data
   */
  public DOMInlineInstance (Element data)
  {
    this.myElement = data;
  }
  
  public void setParent (DOMSource parent)
  {
    this.parent = parent;
  }

  public Element getElement ()
  {
    return this.myElement;
  }

  /**
   * set the starting character of the inlined code
   * 
   * @param set the starting character offset of the inlined code from the start
   *          of the file
   */
  public void setStart (int start)
  {
    this.myElement.setAttribute(DOMFunction.START_ATTR, Integer.toString(start));
    this.start = start;
  }

  /**
   * get the starting character offset from the beginning of the file
   * 
   * @return The start of the inlined instance as a character offset from the
   *         start of the file
   */
  public int getStart ()
  {
    return this.start;
  }

  /**
   * set the ending character of the inlined code from the beginning of the file
   * 
   * @param int is the character offset from the beginning of the file
   */
  public void setEnd (int end)
  {
    this.myElement.setAttribute(DOMFunction.END_ATTR, Integer.toString(end));
    this.end = end;
  }

  /**
   * get the end of the instance as a character offset from the start of the
   * file
   * 
   * @return The end of the instance as a character offset from the start of the
   *         file
   */
  public int getEnd ()
  {
    return this.end;
  }

  /**
   * provides the original declaration of this inlined code
   * 
   * @return The original declaration of this inlined code
   */
  public DOMFunction getDeclaration ()
  {
    return parent.getFunction(this.myElement.getAttributeValue(LINEINST_ATTR));
  }

  /**
   * gets the program counter line number within the inline block
   * 
   * @return the program counter line number within the inline block
   */

  public int getPCLine ()
  {
    return Integer.parseInt(this.myElement.getAttributeValue(PCLINE_ATTR));
  }
  
  public int getLine ()
  {
    return Integer.parseInt(this.myElement.getAttributeValue(LINE_NUM));
  }

  /**
   * adds an inline instance to a source element
   * 
   * @param instance is the name of the instance
   * @param start_inline is the starting line number of the inline instance
   * @param length is the number of lines in the inline instance
   * @param PCLine is the program counter of this inline instance
   */

  public void addInlineInst (String instance, int start_inline, int length,
                             int PCLine)
  {
    Element inlineLineInstElement = new Element(DOMInlineInstance.LINEINST_NODE);
    inlineLineInstElement.setAttribute(LINEINST_ATTR, instance);
    inlineLineInstElement.setAttribute(DOMLine.OFFSET_ATTR,
                                       Integer.toString(start_inline));
    inlineLineInstElement.setAttribute(DOMLine.LENGTH_ATTR,
                                       Integer.toString(length));
    inlineLineInstElement.setAttribute(PCLINE_ATTR, String.valueOf(PCLine));
    this.myElement.addContent(inlineLineInstElement);
  }

  /**
   * checks to see if this inline instance contains an inline instance
   * 
   * @return True iff this inline instance has an inline instance nested within
   *         it
   */
  public boolean hasInlineInstance ()
  {
    return ! this.myElement.getChildren(INLINE_NODE).isEmpty();
  }

  /**
   * checks to see if this inline instance is contained within another inline
   * instance
   * 
   * @return True iff this inline instance is nested within another inlined
   *         instance
   */
  public boolean hasParentInlineInstance ()
  {
    Element parent = this.myElement.getParentElement();
    return (parent != null && parent.getName().equals(
                                                      DOMInlineInstance.INLINE_NODE));
  }

  /**
   * Returns the inline instance nested within this one if one exists
   * 
   * @return The nested inline instance if it exists, null otherwise
   */
  public DOMInlineInstance getInlineInstance ()
  {
    Element child = this.myElement.getChild(INLINE_NODE);
    if (child != null)
      return new DOMInlineInstance(child);

    return null;
  }

  /**
   * Returns the inline instance that this method is nested within, if one
   * exists
   * 
   * @return the parent instance if it exists, null otherwise.
   */
  public DOMInlineInstance getPreviousInstance ()
  {
    Element parent = this.myElement.getParentElement();
    if (parent != null
        && parent.getName().equals(DOMInlineInstance.INLINE_NODE))
      return new DOMInlineInstance(parent);

    return null;
  }
}
