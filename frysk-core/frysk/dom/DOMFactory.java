// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import java.io.IOException;
import java.util.HashMap;

import lib.dw.NoDebugInfoException;
import frysk.proc.Proc;
import frysk.rt.StackFrame;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

public class DOMFactory
{
  private static final boolean DEBUG = false;

  private static HashMap hashmap = new HashMap();

  public static DOMFrysk createDOM (StackFrame frame, Proc proc) throws NoDebugInfoException,
  IOException
  {
    DOMFrysk dom = null;

    String sourcefile = frame.getSourceFile();

    String filename = sourcefile.substring(sourcefile.lastIndexOf("/") + 1);

    if (hashmap.containsKey(proc))
      {
        // retrieve the previously created dom
        dom = (DOMFrysk) hashmap.get(proc);
      }
    else
      {
        // create a new dom and associate it with the given task
        // XXX create a fake name for now, must create unique names later
        dom = new DOMFrysk(proc.getCommand());
        dom.addImage(proc.getMainTask().getName(), sourcefile, sourcefile);
      }

    DOMSource source = dom.getImage(proc.getMainTask().getName()).getSource(
                                                                            filename);

    /*
     * If this source file has not previously been incorporated into the dom, so
     * do now
     */
    if (source == null)
      {
        DOMImage image = dom.getImage(proc.getMainTask().getName());
        image.addSource(proc, frame, dom);
      }
    hashmap.put(proc, dom);
    // if we are debugging the DOM, print it out now
    if (DEBUG)
      {
        printDOM(dom);
      }
    return dom;
  }
  
  /*
   * Print out the DOM @param dom is the DOMFrysk object to print out
   */
  
  public static void printDOM(DOMFrysk dom) {
    Document doc = dom.getDOMFrysk();
    try {
      XMLOutputter serializer = new XMLOutputter();
      serializer.getFormat();
      serializer.output(doc, System.out);
    }
    catch (IOException e) {
      System.err.println(e);
    }
  }
  
  /**
   * getDOM returns a pointer to the desired dom from the hashmap
   * 
   * @param proc is the key used to retrieve the dom from the hashmap
   */
  
  public static DOMFrysk getDOM(Proc proc)
  {
    DOMFrysk dom = (DOMFrysk) hashmap.get(proc);
    return dom;
  }
  /*
   * remove the spaces from the DOM ID 'cause JDOM don't like spaces there
   * 
   * @param name of the task
   */
  
/*  private static String removeSpace(String name) {
    // if there are no spaces, just return the name
    if (name.lastIndexOf(" ") == -1)
      return name;
    
    char[] newname = new char[name.length()];
    char[] namechararray = name.toCharArray();
    int i = 0;
    for(int j=0; j<name.length(); j++) {
      if (Character.isSpaceChar(namechararray[j]))
        continue;
      newname[i] = namechararray[j];
      i++;
    }
    String str = new String(newname);
    if (str.length() <= 8)
      return str;
    return str.substring(0,8);
  }  */

  public static void clearDOMSourceMap (Proc proc)
  {
    DOMFrysk dom = (DOMFrysk) hashmap.get(proc);
    if (dom != null)
      {
        DOMImage image = dom.getImage(proc.getMainTask().getName());
        image.clearSourceMap();
      }
  }
  
}
