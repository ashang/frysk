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

import frysk.dom.DOMFrysk;
import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.dom.DOMLine;
import frysk.dom.StaticParser;
import frysk.dom.cparser.CDTParser;
import frysk.dom.DOMCommon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * This is a standalone test of the parsing capabilities. Frysk uses a snapshot
 * of the Eclipse CDT parser 2.0 which is used in standalone mode. fparser tests
 * the ability of the CDTParser to correctly parse source code and generate a
 * DOM (Document Object Model) for use by the Frysk GUI to create a viable
 * source window that is properly marked up(highlighted). fparser allows a user
 * to enter a path to either an executable file which should have been compiled
 * with the -g option. The paths to the source and include files can be gleaned
 * from the executable's header if it has been compiled with -g. If a copy of
 * the DOM created from the parsing it can be written to a file using the -o
 * option. A file path to the DOM will be created named "executablename.xml" in
 * the directory where the executable resides if a path is not entered after the
 * -o.
 */

public class fparser
{
  private static final String ERROR_MSG1 = "Usage is: \n     fparser [-o path-to-dom] path-to-executable\n";

  private static final String ERROR_MSG2 = "fparser cannot find the source files for the executable entered.\n"
                                           + "Either the files have moved, the executable was not compiled with\n"
                                           + "the '-g' option or the paths to to the source files cannot be\n"
                                           + "ascertained.  If you are sure the '-g' option was used, try running\n"
                                           + "fparser in the directory the executable was created in or compiled from.";
  
  private static final String ERROR_MSG3 = "The path to write the DOM to either does not exist or you do not\n" +
                                           "have permission to write to.";

  private static Element root = new Element("Parser_test");

  private static Document data = new Document(root);

  private static DOMFrysk dom = new DOMFrysk(data);

  private static final String IMAGE_FILENAME = "Task 28428";

  private static boolean output_dom = false;

  private static String dompath;

  public static void main (String args[])
  {
    // Make sure we have been passed at least 1 parameter, -o is optional
    if (args.length == 0 || args.length > 3 || 
        args[0].equals("-help") || args.length == 2)
      {
        System.out.println(ERROR_MSG1);
        return;
      }
    String filepath;
    if (args.length == 1)
      {
        filepath = args[0];
      }
    else
      {
        filepath = args[2];
      }

    if (args.length == 3 && args[0].equals("-o"))
      {
        output_dom = true;
        dompath = args[1];
        // Check to see if the path exists and is writeable by the current user
        File f = new File(dompath.substring(0,dompath.lastIndexOf("/")));
        System.out.println("dom output file = " + f.toString());
        if (!f.exists() || !f.canWrite())
          {
            System.out.println(ERROR_MSG3);
            return;
          }
      }

    // Check to see if the path to the executable file entered is valid
    if (! checkPath(filepath))
      {
        System.out.println("The path to " + filepath + " does not exist "
                           + " or you do not have permission to access it"
                           + " or the path is a directory.");
        return;
      }
    // See what kind of file we have, if it does not end in ".c" or ".cpp"
    // then we will assume we have an executable
    if (filepath.endsWith(".c") || filepath.endsWith(".cpp"))
      {
        // XXX We'll work on a source file later
        System.out.println("We don't do source files right now.  Try again later");
        return;
      }

    ArrayList arraysourcelist = DOMCommon.getSrcFiles(filepath);

    if (arraysourcelist.size() == 0)
      {
        System.out.println(ERROR_MSG2);
        return;
      }

    // Convert ArrayList to String Array
    String sourcelist[] = (String[]) arraysourcelist.toArray(new String[0]);

    // Get the list of include file paths associated with this image
    ArrayList arrayincludepaths = DOMCommon.getIncludePaths(filepath);
    String includepaths[] = (String[]) arrayincludepaths.toArray(new String[0]);

    dom = new DOMFrysk("TaskTask");
    dom.addImage(IMAGE_FILENAME, filepath, filepath);
    DOMImage image = dom.getImage(IMAGE_FILENAME);
    int i = 0;
    while (i < sourcelist.length)
      {
        String filename = sourcelist[i].substring(sourcelist[i].lastIndexOf("/") + 1);
        String path = sourcelist[i].substring(0, sourcelist[i].lastIndexOf("/"));
        DOMSource source = new DOMSource(filename, path, includepaths);
        // Read the file lines from disk
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
                DOMLine l = new DOMLine(lineNum++, text + "\n", offset, false,
                                        false, Long.parseLong("deadbeef", 16));
                source.addLine(l);

                offset += text.length() + 1;
              }
            image.addSource(source);
            System.out.println("Now parsing file..... " + sourcelist[i]);

            // Parse the file and populate the DOM
            StaticParser parser = new CDTParser();
            parser.parse(dom, source, image);
            System.out.println("The file '" + sourcelist[i]
                               + "' has been successfully parsed.");

          }
        catch (IOException e)
          {
            System.out.println("Error reading source file " + sourcelist[i]
                               + "\nerror = " + e.getMessage());
          }
        i++;
      }

    // See if the output of a DOM has been requested
    if (output_dom)
      {
        try
          {
            // Make sure there is not a file already there if this test has been
            // run already
            if (checkPath(dompath))
              {

                BufferedReader in = new BufferedReader(
                                                       new InputStreamReader(
                                                                             System.in));
                String input = "";
                System.out.println("A file already exists by the name of "
                                   + dompath
                                   + "\nDo you want to overwrite? y/n");
                while (!input.equals("y") && !input.equals("n") && !input.equals("Y")
                       && !input.equals("N"))
                  {
                    input = in.readLine();
                  }
                if (input.equals("y") || input.equals("Y"))
                  {
                    (new File(dompath)).delete();
                  }
                else
                  return;
              }
            BufferedWriter out = new BufferedWriter(new FileWriter(filepath
                                                                   + ".xml"));
            writeDOM(dom, out);
            System.out.println("A DOM has been generated and output to "
                               + dompath);
          }
        catch (IOException e)
          {
            e.printStackTrace();
          }
      }
  }

  /**
   * writeDOM writes out the DOM to a file for perusal by the user 
   * 
   * @param dom is the current DOM just created 
   * @param out is a Buffered Output Stream to write the DOM to 
   *
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
  }

  /**
   * checkPath checks to be sure the file sent in the path actually exists
   * 
   * @param path contains a String of the path to check to see if it exists
   * @return true if the file is found, false if not
   */
  public static boolean checkPath (String path)
  {
    File f = new File(path);
    if (f.exists() && f.isFile() && f.canRead())
      return true;
    else
      return false;
  }
}
