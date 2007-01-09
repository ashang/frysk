// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import frysk.junit.TestCase;
import frysk.junit.Paths;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.dom.DOMLine;
import frysk.dom.DOMFactory;
import frysk.dom.StaticParser;
import frysk.dom.cparser.CDTParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 * This is a test for the parsing capabilities. Frysk uses a snapshot
 * of the Eclipse CDT parser 2.0 which is used in standalone mode.
 */

public class TestParser
    extends TestCase
{
    /**
     * A scratch file for writing out the dom, if non-null it is
     * deleted during tear-down.  This is more robust than relying on
     * the garbage collector to delete the file during finalization.
     */
    private File tmpFile;

    public void testParser ()
	throws IOException
    {
	// XXX: The current problem is that the relevant paths are
	// pointing into the build tree.
	if (brokenXXX (3841))
	  return;

	String IMAGE_FILENAME = "Task 28428";
	String CC_FILE = "./test";

	String execPath = Paths.getExecPrefix () + "/test_main_looper";
	List sources = DOMFactory.getSrcFiles (execPath);
	assertTrue ("sources.size > 0", sources.size () > 0);
	
	// Get the list of include file paths associated with this
	// image
	String[] includePaths = (String[])
	    DOMFactory.getIncludePaths(execPath).toArray (new String[0]);

	DOMFrysk dom = new DOMFrysk ("TaskTask");
	dom.addImage (IMAGE_FILENAME, CC_FILE, CC_FILE);
	DOMImage image = dom.getImage (IMAGE_FILENAME);
	for (Iterator i = sources.iterator (); i.hasNext (); ) {
	    String filename = (String) i.next ();
	    File file = new File (filename);
	    // XXX: Use file and its parser here
	    String basename = filename.substring(filename.lastIndexOf("/") + 1);
	    String path = filename.substring(0, filename.lastIndexOf("/"));
	    DOMSource source = new DOMSource (basename, path, includePaths);
	    // Read the file lines from disk XXX: Remote file access?
	    BufferedReader reader = new BufferedReader (new FileReader(file));
	    int offset = 0;
	    int lineNum = 0;
	    
	    while (reader.ready()) {
		String text = reader.readLine();
		// XXX: detect executable lines?
		DOMLine l = new DOMLine(lineNum++, text + "\n", offset, false,
					false, Long.parseLong("deadbeef", 16));
		source.addLine(l);
		
		offset += text.length() + 1;
	    }
	    image.addSource(source);
		
	    // Parse the file and populate the DOM
	    StaticParser parser = new CDTParser();
	    parser.parse(dom, source, image);
	}
	 
	// Make sure there is not a file already there is this test
	// has been run already
	String name = getClass ().getName ();
	File pwd = new File (".");
	tmpFile = File.createTempFile (name + ".", ".tmp", pwd);
	BufferedWriter out = new BufferedWriter (new FileWriter (tmpFile));
	writeDOM(dom, out);
	assertDomMatchesBase (tmpFile);
    }

    public void tearDown ()
    {
	if (tmpFile != null) {
	    tmpFile.delete ();
	    tmpFile = null;
	}
    }

    /*
     * write out the DOM @param dom is the current DOM just created
     * @param out is a Buffered
     * 
     * @param dom is the DOMFrysk dom to write out
     * @param out is the output stream to write the dom to
     */
    public static void writeDOM (DOMFrysk dom, BufferedWriter out)
	throws IOException
    {
	Document doc = dom.getDOMFrysk();
	XMLOutputter serializer = new XMLOutputter();
	serializer.getFormat();
	serializer.output(doc, out);
    }

    /**
     * compareFiles checks to see if the generated DOM matches the
     * previously generated DOM
     * 
     * @param newfile is a String containing the path to the new dom
     * just created
     */
    public static void assertDomMatchesBase (File newfile)
	throws IOException
    {
	String basedom = Paths.getDataPrefix () + "/test_looper.xml";
	File baseDomInput = new File (basedom);
    
	FileInputStream base = new FileInputStream(newfile);
	FileInputStream newdom = new FileInputStream(baseDomInput);
    
	try
	    {
		byte[] lbuffer = new byte[4096];
		byte[] rbuffer = new byte[lbuffer.length];
		for (int lcount = 0; (lcount = newdom.read(lbuffer)) > 0;) {
		    int bytesRead = 0;
		    for (int rcount = 0; (rcount = base.read(rbuffer, bytesRead, lcount - bytesRead)) > 0;) {
			bytesRead += rcount;
		    }
		    for (int byteIndex = 0; byteIndex < lcount; byteIndex++) {
			if (lbuffer[byteIndex] != rbuffer[byteIndex])
			    fail ("Files do not match at " + byteIndex);
		    }
		}
	    }
	finally
	    {
		base.close();
		newdom.close();
	    }
    }
}
