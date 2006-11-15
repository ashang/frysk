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


package frysk.gui.test;

import frysk.junit.TestCase;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.dom.DOMLine;
import frysk.dom.DOMFactory;
import frysk.dom.StaticParser;
import frysk.dom.cparser.CDTParser;
import frysk.gui.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * This is a test for the parsing capabilities. Frysk uses a snapshot of the
 * Eclipse CDT parser 2.0 which is used in standalone mode.
 */

public class TestParser
    extends TestCase
{

  private static Element root = new Element("Parser_test");

  private static Document data = new Document(root);

  private static DOMFrysk dom = new DOMFrysk(data);

  private static String IMAGE_FILENAME = "Task 28428";

  private static String CC_FILE = "./test";

  private static String[] sourcelist;

  private static String installed_list = "/usr/share/frysk/samples/test_looper.c";

  private static String[] includepaths;

  private static String[] INCLUDEPATHS = { "/usr/local/include", "/usr/include" };

  private static String[] exec_path = {
                                       "./frysk/gui/srcwin/testfiles/test_main_looper",
                                       "/usr/share/frysk/samples/test_main_looper" };

  private static String[] NEWDOM = {
                                    "./frysk/gui/srcwin/testfiles/new_dom.xml",
                                    "/usr/share/frysk/samples/new_dom.xml" };

  private static String[] BASEDOM = {
                                     Build.SRCDIR + "/frysk/gui/srcwin/testfiles/test_looper.xml",
                                     "/usr/share/frysk/samples/test_looper.xml" };

  private static int which_file;

  public static void testParser ()
  {

    if (DOMFactory.pathFound(Build.SRCDIR))
      {
        // Figure out whether we are doing an in-tree check or an installed check
        // which_file = 0 means in-tree, = 1 means an installed check
        which_file = 0;
        assertNotNull(
                      "testing getting of source file paths",
                      (sourcelist = DOMFactory.getSrcFiles(exec_path[which_file])));
      }
    else
      {
        which_file = 1;
        assertTrue("testing source file existence",
                   (DOMFactory.pathFound(installed_list)));
      }

    if (sourcelist == null)
        sourcelist[0] = installed_list;

    // Get the list of include file paths associated with this image
    includepaths = DOMFactory.getIncludePaths(exec_path[which_file]);
    if (includepaths.equals(null))
      {
        includepaths = INCLUDEPATHS;
      }

    dom = new DOMFrysk("TaskTask");
    dom.addImage(IMAGE_FILENAME, CC_FILE, CC_FILE);
    DOMImage image = dom.getImage(IMAGE_FILENAME);
    int i = 0;
    while (sourcelist[i] != null)
      {
        String filename = sourcelist[i].substring(sourcelist[i].lastIndexOf("/") + 1);
        String path = sourcelist[i].substring(0, sourcelist[i].lastIndexOf("/"));
        DOMSource source = new DOMSource(filename, path, includepaths);
        // Read the file lines from disk
        // XXX: Remote file access?
        try
          {
            BufferedReader reader = new BufferedReader(
                                                       new FileReader(
                                                                      new File(
                                                                               sourcelist[i])));
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
            image.addSource(source);

            // Parse the file and populate the DOM
            StaticParser parser = new CDTParser();
            parser.parse(dom, source, image);

          }
        catch (IOException e)
          {
            System.out.println("Error reading source file " + sourcelist[i]
                               + "\nerror = " + e.getMessage());
          }
        i++;
      }

    try
      {
        // Make sure there is not a file already there is this test has been run already
        checkPath(NEWDOM[which_file]);
        BufferedWriter out = new BufferedWriter(new FileWriter(NEWDOM[which_file]));
        writeDOM(dom, out);
      }
    catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  }

  /*
   * write out the DOM @param dom is the current DOM just created @param out is
   * a Buffered
   * 
   * @param dom is the DOMFrysk dom to write out
   * @param out is the output stream to write the dom to
   */

  public static void writeDOM (DOMFrysk dom, BufferedWriter out)
  {
    Document doc = dom.getDOMFrysk();
    try
      {
        XMLOutputter serializer = new XMLOutputter();
        serializer.getFormat();
        serializer.output(doc, out);
      }
    catch (IOException e)
      {
        System.err.println(e);
      }
    
    try
      {
        assertTrue("testing DOM files comparison", compareFiles(NEWDOM[which_file], 
                                                                BASEDOM[which_file]));
      }
    catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
        assertTrue("IO Error on opening the DOM files", false);
      }
  }

  /**
   * checkPath checks to see if a new DOM has already been created and
   * if it has, delete it
   * 
   * @param path is a String containing the path to check
   */
  public static void checkPath (String path)
  {
    File f = new File(path);
    if (f.exists())
      f.delete();
    return;
  }

  /**
   * compareFiles checks to see if the generated DOM matches the previously
   * generated DOM
   * 
   * @param newfile is a String containing the path to the new dom just created
   * @param basefile is a String containing the path to the base dom to compare with
   */
  public static boolean compareFiles(String newfile, String basefile) throws IOException
  {
    File new_dom_input = new File(newfile);
    File base_dom_input = new File(basefile);
    
    FileInputStream base = new FileInputStream(new_dom_input);
    FileInputStream newdom = new FileInputStream(base_dom_input);
    
    try
    {
        byte[] lbuffer = new byte[4096];
        byte[] rbuffer = new byte[lbuffer.length];
        for (int lcount = 0; (lcount = newdom.read(lbuffer)) > 0;)
        {
            int bytesRead = 0;
            for (int rcount = 0; (rcount = base.read(rbuffer, bytesRead, lcount - bytesRead)) > 0;)
            {
                bytesRead += rcount;
            }
            for (int byteIndex = 0; byteIndex < lcount; byteIndex++)
            {
                if (lbuffer[byteIndex] != rbuffer[byteIndex])
                    return false;
            }
        }
    }
    finally
    {
        base.close();
        newdom.close();
    }
    return true;
  }
}
