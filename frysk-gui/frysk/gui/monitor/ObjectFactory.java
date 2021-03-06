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
// type filter text
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

package frysk.gui.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import frysk.gui.Gui;

/**
 * The factory instantiates an object using the information
 * stored in an XML node.
 * If an object is dynamically instantiated during runtime then
 * the written code does not know its type so the type must be saved
 * and the object dynamically instantiated again at load time. For this
 * purpose the saveObject/loadObject methods of ObjectFactory must
 * be used instead of directly calling the objects save/load.
 * It doesnt harm of course of objects were always saved/loaded using the 
 * factory methods so one might as well do that for safety.
 * */
public class ObjectFactory {
	public static final ObjectFactory theFactory = new ObjectFactory();
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	/**
	 * Dynamically instantiates the object save to the given node,
	 * @param node the node form where to retrieved object information.
	 * @return the instantiated object. NULL if the object cannot be
	 * loaded.
	 */
	public Object getObject(Element node){
	  Object loadedObject = null;
	  String type = node.getAttribute("type").getValue();
//	  System.out.println("\n===========================================");
//	  System.out.println("ObjectFactory.getObject() type: " + type);

	  Class cls;
	  try {
	    cls = Class.forName(type);
//	    System.out.println("ObjectFactory.getObject() cls: " + cls);
	    java.lang.reflect.Constructor constr = cls.getDeclaredConstructor(new Class[]{});
//	    System.out.println("ObjectFactory.getObject() constr: " + constr);
	    loadedObject =  constr.newInstance(new Object[] {});
//	    System.out.println("ObjectFactory.getObject() loadedObject: " + loadedObject);
	  } catch (Exception e) {
//	    System.err.println("Object ["+node.getAttribute("type").getValue() + "] Does not have a no argument constructor");
//	    e.printStackTrace();
	    loadedObject = null;
	  }

//	  System.out.println("ObjectFactory.getObject() " + loadedObject.getClass());
//	  System.out.println("===========================================\n");

	  return loadedObject;
	}
	
	/**
	 * Instantiates the object then calls its load function.
	 * @param node The node from wich to retrieve the information
	 * @return instantiated and loaded object
	 */
	public Object loadObject(Element node){
		Object loadedObject = this.getObject(node);
		if(loadedObject != null){
		  ((SaveableXXX)loadedObject).load(node);
		}
		return loadedObject;
	}
	
	public void saveObject(SaveableXXX saveable, Element node){
		if(saveable.shouldSaveObject()){
			node.setAttribute("type", saveable.getClass().getName());
			saveable.save(node);
		}
	}
	
  public void exportNode (File file, Element node)
  {
    XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());

    try
      {
	output.output(node, new FileWriter(file));
      }
    catch (IOException e)
      {
	e.printStackTrace();
      }
  }
	
  public Element importNode (File file)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    org.w3c.dom.Document doc = null;
    try
      {
	DocumentBuilder builder = factory.newDocumentBuilder();
	doc = (org.w3c.dom.Document) builder.parse(file);

      }
    catch (Exception e)
      {
	errorLog.log(Level.WARNING, "Could not parse node ", e); //$NON-NLS-1$
	return null;
      }

    DOMBuilder doo = new DOMBuilder();
    Document document = doo.build(doc);

    return document.getRootElement();
  }

  public boolean deleteNode (File file)
  {
    return file.delete();
  }
	
}
