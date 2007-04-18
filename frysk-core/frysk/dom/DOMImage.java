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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import frysk.dom.cparser.CDTParser;
import frysk.proc.Proc;
import frysk.rt.Line;

import org.jdom.Element;

/**
 * DOMImage represents an image within the source window document
 * object model
 */
public class DOMImage
{
	/**
	 * CCPATH for the image
	 */
	public static final String CCPATH_ATTR = "CCPATH";
	private Element myElement;
	public static final String IMAGE_NODE = "image";
	public static final String NAME_ATTR = "filename";
	public static final String PATH_ATTR = "filepath";
    public DOMFrysk dom;
    public DOMImage image;
    
    /* Keep a cache of DOMSources contained within this image. Prevents
     * excessive new JDOM Object creation and iteration. This is important
     * because DOMSources have a lot of child elements.
     * 
     * This map is cleared each time that the JDOM tree potentially has
     * changed, being after a step, or a continuation of the process. See
     * more about this in getSource() below. */
    private HashMap sourceMap = new HashMap();
	
	/**
	 * Creates a new DOMImage from the given Element. Data must be of name "image".
	 * @param data A JDOM Element of name "image"
	 */
	public DOMImage (Element data)
    {
		this.myElement = data;
	}
	
	/**
	 * Creates a new DOMImage
	 * @param name The name of the image
	 * @param path the path to the image
	 * @param ccpath The CCPATH for the image
	 */
	public DOMImage (String name, String path, String ccpath, Element rootElement)
    {
		myElement = new Element(DOMImage.IMAGE_NODE);
		myElement.setAttribute(NAME_ATTR, name);
		myElement.setAttribute(PATH_ATTR, path);
		myElement.setAttribute(CCPATH_ATTR, ccpath);
	}
	
	/**
	 * adds a source element under this image
	 * @param source_name
	 * @param path
	 * @return
	 */
	public void addSource (String source_name, String path, String[] incpaths)
    {
		this.addSource(new DOMSource(source_name, path, incpaths));
	}
    
    /**
     * adds a source element under this DOMImage
     * @param proc - the proc image for this source
     * @param frame - the frame for this source
     * 
     */
	
    public DOMSource addSource (Proc proc, Line line, DOMFrysk dom) throws IOException
    {
//    Get the list of include file paths associated with this image
      ArrayList arrayincpaths = DOMCommon.getIncludePaths(proc.getExe());
      String includepaths[] = (String[]) arrayincpaths.toArray(new String[0]);
      File file = line.getFile();
      String sourcefile = file.getPath();

      String filename = file.getName ();
      String path = file.getParent ();
      
      DOMSource source = new DOMSource(filename, path, includepaths);

      // Read the file lines from disk
      // XXX: Remote file access?
      BufferedReader reader = new BufferedReader(
                                                 new FileReader(
                                                                new File(
                                                                         sourcefile)));
      int offset = 0;
      int lineNum = 0;

      while (reader.ready())
        {
          String text = reader.readLine();
          // XXX: detect executable lines?
          DOMLine l = new DOMLine(lineNum++, text + "\n", offset, false,
                                  false, Long.parseLong("deadbeef", 16));
          source.addLine(l);

          offset += text.length() + 1;
        }
        // Parse the file and populate the DOM if the Frame says this is the
        // current source file
        StaticParser parser = new CDTParser();
        parser.parse(dom, source, this.image);
        source.setParsed(true);
        
        addSource(source);
        return source;
      
    }
	/**
	 * Adds the given DOMSource as a source in this image
	 * @param source The source to add
	 */
	public void addSource (DOMSource source)
    {
		this.myElement.addContent(source.getElement());
	}
	
	/**
	 * gets the name of the image
	 * @return The name of the image
	 */
	public String getName ()
    {
		return this.myElement.getAttributeValue(NAME_ATTR);
	}
	
	/**
	 * Sets the CCPATH of the current image
	 * @param image_name is the image for which this CCPATH is intended
	 */
	public void setCCPath (String image_name)
    {
		this.myElement.setAttribute(CCPATH_ATTR, image_name);
		return;
	}
	/**
	 * sets the name of the image
	 * @param name what the name of the image will be
	 */
	public void setName (String name) 
    {
		
	}
	/**
	 * returns the name of the image
	 * @return The CCPATH of the image
	 */
	public String getCCPath ()
    {
		return this.myElement.getAttributeValue(CCPATH_ATTR);
	}
	
	/**
	 * gets an iterator pointing to all of the sources belonging to this image
	 * @return an iterator to all the source files contained in this image.
	 */
	public Iterator getSources ()
    {
		return this.myElement.getChildren(DOMSource.SOURCE_NODE).iterator();
	}
	
	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is
	 * found returns null. The first time a particular source name is searched
     * for, the child source elements of this DOMImage are searched until
     * the requested source Element is found. At that point, that newly created
     * Object is hashed into this DOMImage's sourceMap to be cached for 
     * future requests.
	 * 
	 * @param name is the name of the image to look for
	 * @return The DOMSource corresponding to the element, or null if no such
	 *         element exists
	 */
	public DOMSource getSource (String name)
    {
      DOMSource source = (DOMSource) this.sourceMap.get(name);
      
      /* We found the requested DOMSource in the HashMap cache. */
      if (source != null)
        {
          return source;
        }
      else
      {
        /* This DOMSource hasn't be cached yet, so iterate through 
         * children elements to find a potential match. */
        Iterator i = this.myElement.getChildren().iterator();
        while (i.hasNext())
          {
            Element elem = (Element) i.next();
            if (elem.getQualifiedName().equals(DOMSource.SOURCE_NODE))
              {
                if (elem.getAttributeValue(DOMSource.FILENAME_ATTR).equals(name))
                  {
                    source = new DOMSource(elem);
                    this.sourceMap.put(name, source);
                    return source;
                  }
              }
          }
        return null;
      }
	}

	/**
	 * This function should only be used internally within the frysk source dom
	 * @return The JDom element at the core of this node
	 */
	protected Element getElement () 
    {
		return this.myElement;
	}
    
    /**
     * Clears this DOMImage's HashMap cache after DOM work has been
     * completed after a particular segment of operation. The longevity of each
     * source file can't be predicted, so take the high road and free the 
     * space each time.
     */
    public void clearSourceMap ()
    {
      this.sourceMap.clear();
    }
}
