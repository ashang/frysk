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
//import java.util.Vector;
import java.math.BigInteger;
import org.jdom.Document;
import org.jdom.Element;

/**
 * DOMFrysk serves as an access point to the document object model for the frysk
 * source window. The Frysk DOM(Document Object Model) is used to model the
 * underlying source code being debugged so that the GUI can accurately display
 * information about it in the source window. The DOM is a dynamic model that
 * can change depending on user actions. A tree-based API for processing XML
 * documents, JDOM, was chosen to implement the Frysk DOM for it was found to be
 * far easier and more intuitive to use than other open source XML manipulation
 * APIs. Since both the Frysk GUI and JDOM are written in Java, using it just
 * made sense from that standpoint too. For more information regarding JDOM, see
 * http://www.jdom.org. The DOM is under constant construction during this phase
 * of development. As more features are added to Frysk, more items will be added
 * to the DOM. This is just a snapshot of the DOM as it exists now (03/24/06),
 * although we will endeavor to keep it as current as possible. Class Structure
 * The following diagram is the current "class" structure of the Frysk DOM. As
 * can be seen from the tree below, the top class is the executable image itself
 * and below it are the subclasses hanging off of it.
 * 
 *  DOMFrysk - the overall DOM
 *  DOMImage - the source images associated with this DOM 
 *  DOMFunction - all functions defined for this source image 
 *  DOMSource - a particular source image
 *  DOMLine - contains the info associated with each line within the source
 *  DOMInlineInstance - contains info about inline functions on this line 
 *  
 * Each one of these classes/subclasses has a set of methods to create/modify/access
 * the information in the DOM. Here is a more detailed version of the above
 * diagram with the methods associated with each class. In the following
 * class/method listing, the backend code would mainly be using the setXxxx
 * methods and the GUI part would be using the getXxxx methods. DOM Structure
 * The following is a mock-up of what a DOM looks like with all of its
 * nodes/elements. 
 * 
 * DOM  |--PC - current program counter 
 *      |--PID - Process ID of debugged process 
 *          |-- attr: value - the process ID number 
 *      |--Image - element: image 
 *          |--attr: name - name associated with this image 
 *          |--attr: filename - name of the executable 
 *          |--attr: CCPATH - path to the executable 
 *        |--Source - element: source 
 *          |--attr: filename - name of the source file 
 *          |--attr: filepath - path to the source file 
 *          |--attr: parsed - boolean to indicate if the GUI has parsed this source 
 *            |--Line - element: line 
 *              |--attr: number - line number to be added 
 *              |--attr: pc - address where this line's executable code begins
 *              |--attr: offset - offset in characters from the beginning of the file
 *              |--attr: length - number of characters in this line 
 *              |--attr: executable - boolean indicating if this line is executable 
 *              |--attr: has_break - boolean indicating if this line is a breakpoint 
 *                |--Tag - element: tag 
 *                  |--attr: type - type of tag this is(variable, keyword, etc.) 
 *                  |--attr: start - starting character from beginning of the file for tag 
 *                  |--attr: length - no. of chars the tag will encompass 
 *                  |--attr: token - 
 *        |--Inline - element: function 
 *          |--attr: function_name - name of the inline function 
 *          |--attr: source - source where this function came from 
 *          |--attr: start - starting char this inline function begins from the beginning of the file 
 *          |--attr: end - ending char this inline function ends at 
 *          |--attr: line_start - line no. where this function begins in the source file 
 *          |--attr: line_end - line no. where this function ends in the source file 
 *          
 * Debugging Scenario
 * When a processe is debugged, no matter how activated, here are the steps that need
 * to happen: Frysk backend identifies the PID or the executable that is to be
 * debugged Frysk backend grabs the source code and creates a DOM adds the image
 * adds a source file adds all functions found adds each line of the source file
 * sends DOM to GUI sends pointer to source code to GUI GUI parses source and
 * marks up the DOM with code highlighting information GUI brings up the source
 * window GUI and backend communicate so GUI can update affected windows
 * 
 * @author ajocksch
 */

public class DOMFrysk
{

  /**
   * The pid of the process this DOM represents
   */
  private static final String PID_ATTR = "pid";

  private static final Element pidValue = new Element(PID_ATTR);

  private static final String PC_ATTR = "PC";

  private static final String value = "value";

  private final Element pcName = new Element(PC_ATTR);

  private Document data;

  /**
   * Creates a new DOMFrysk using the DOM contained in data
   * 
   * @param name TODO
   */
  public DOMFrysk (String name)
  {
    this(new Document(new Element(name)));
  }

  public DOMFrysk (Document doc)
  {
    this.data = doc;
    this.data.getRootElement().setText("Frysk JDOM");
    this.data.getRootElement().addContent(pcName);
  }

  /**
   * adds an image element to the DOM
   * 
   * @param image_name = the name of the image to be added
   * @param CCPATH = the CCPATH associated with this image
   * @param source_path = the path to the source of this image
   * @return true if able to add the image, false if not
   */
  public boolean addImage (String image_name, String CCPATH, String source_path)
  {
    return this.addImage(new DOMImage(image_name, source_path, CCPATH, this.data.getRootElement()));
  }

  /**
   * Adds the given image to the DOM
   * 
   * @param image The DOMImage to add
   * @return true if able to add the image, false if not
   */
  public boolean addImage (DOMImage image)
  {
    // Make sure this image name is not already there before adding
    if (checkImageDup(image.getName()))
      return false;

    this.data.getRootElement().addContent(image.getElement());
    return true;
  }

  /**
   * Add the PID to the DOM
   * 
   * @param an int containing the PID
   * @return true if able to add PID, false if not
   */

  public boolean addPID (int pid)
  {
    // Make sure there is not a PID already there before adding
    if (this.data.getRootElement().getChild(PID_ATTR) != null)
      return false;
    this.data.getRootElement().addContent(pidValue);
    this.data.getRootElement().getChild(PID_ATTR).setAttribute(
                                                               "value",
                                                               Integer.toString(pid));
    return true;
  }

  /**
   * checkImageDup - check to see if there is a duplicate image name
   * 
   * @param image is name of the image to check for
   * @return true if there already an image of the same name, false if not
   */

  private boolean checkImageDup (String image)
  {
    Iterator i = this.data.getRootElement().getChildren().iterator();
    while (i.hasNext())
      {
        Element elem = (Element) i.next();
        if (elem.getQualifiedName().equals(DOMImage.IMAGE_NODE))
          {
            if (elem.getAttributeValue(DOMSource.FILENAME_ATTR).equals(image))
              return true;
          }
      }
    return false;
  }

  /**
   * Retrieves all the images contained in the DOM as an iterator
   * 
   * @return
   */
  // This function is commented out right now because it is not being used.
  // Nothing is broken per se, once this functionality is needed it will be
  // uncommented.
  /*
   * public Iterator getImages() { Iterator i =
   * this.data.getRootElement().getChildren().iterator(); Vector v = new
   * Vector(); while (i.hasNext()) { Element elem = (Element) i.next();
   * v.add(new DOMImage(elem)); } return v.iterator(); }
   */

  /**
   * Attempts to fetch an image of the given name from the DOM. If no image is
   * found returns null
   * 
   * @param name The name of the image to look for
   * @return The DOMImage corresponding to the element, or null if no such
   *         element exists
   */
  public DOMImage getImage (String name)
  {
    Iterator i = this.data.getRootElement().getChildren().iterator();

    while (i.hasNext())
      {
        Element elem = (Element) i.next();
        if (elem.getQualifiedName().equals(DOMImage.IMAGE_NODE))
          {
            if (elem.getAttributeValue(DOMSource.FILENAME_ATTR).equals(name))
              return new DOMImage(elem);
          }
      }

    return null;
  }

  /**
   * get the PID associated with this DOMFrysk
   * 
   * @return The PID of the process that this DOM represents
   */
  public int getPID ()
  {
    return Integer.parseInt(this.data.getRootElement().getChild(PID_ATTR).getAttribute(
                                                                                       value).getValue());
  }

  /**
   * get the root element of the DOM
   * 
   * @return The root element of the DOM
   */
  protected Element getElement ()
  {
    return this.data.getRootElement();
  }

  /**
   * get the PC(program counter)
   * 
   * @return BigInteger program counter
   */
  public BigInteger getPC ()
  {
    BigInteger bInt = new BigInteger(
                                     this.data.getRootElement().getChild(
                                                                         PC_ATTR).getAttribute(
                                                                                               value).getValue());
    return bInt;
  }

  /**
   * Set the PC counter value in the DOM
   * @param pc is what the PC should be set to 
   */
  public void setPC (BigInteger pc)
  {
    this.data.getRootElement().getChild(PC_ATTR).setAttribute(value,
                                                              pc.toString());
  }

  /**
   * returns the DOMFrysk document
   * @return  the DOMFrysk document
   */
  public Document getDOMFrysk ()
  {
    return this.data;
  }

}
