// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import frysk.dom.DOMCompilerSuffixes;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.dw.Dwarf;
import lib.dw.DwarfCommand;

import java.io.File;
import java.util.ArrayList;

/**
 * DOMCommon contains some static methods needed by various pieces of the
 * DOM/Parser code. It is called from several places in the code to perform
 * such duties as getting the include files for an executable and it can
 * pull the list of source files from the executable if need be.
 *
 */

public class DOMCommon
{
  
  private static final String GLOBAL_INCLUDE = "/usr/include";
  private static final String LOCAL_INCLUDE = "/usr/local/include";
  
  /*
   * getSrcFiles gets the source files for this image from the elf/dwarf header
   * The path from the ELF/DWARF header is used for the path for the initial
   * initial search of the source file.  If that search fails the path to the
   * executable is prepended to the beginning of that path to see if the file
   * can be found that way.
   *
   * @param executable is a String containing the path to the executable
   * @return an ArrayList with the path(s) of the source file(s)
   */
  
  public static ArrayList getSrcFiles (String executable)
  {
    ArrayList sourcefiles = new ArrayList();
    if (pathFound(executable))
      {
        try
          {
            Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
            Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
            String[] files = dw.getSourceFiles();

            // Since this call returns a lot of non-source file info, we must
            // parse it and glean the source paths from it
            for (int i = 0; i < files.length; i++)
              {
                if (DOMCompilerSuffixes.checkCPP(files[i]) || DOMCompilerSuffixes.checkC(files[i]))
                  {
                    if (pathFound(files[i]))
                      {
                        sourcefiles.add(files[i]);
                      }
                    // If we have not found the file and it has a relative path, prepend the path
                    // to the executable to the front of the paths and see what happens
                    else if (files[i].startsWith("..")) {
                      if (pathFound(executable.substring(0,executable.lastIndexOf("/")) + "/" + files[i]))
                        {
                          sourcefiles.add(executable.substring(0,executable.lastIndexOf("/")+1) + files[i]);
                        }
                    }
                  }
              }
          }
        catch (lib.elf.ElfException ee)
          {
            System.err.println("Error getting sourcefile paths: "
                               + ee.getMessage());
            return sourcefiles;
          }
      }
      return sourcefiles;
  }
  
  /*
   * get a list of the include files for this source file
   * 
   * @param executable is a String containing the path to the executable
   * @return an ArrayList containing a list of the include path(s)
   */
  public static ArrayList getIncludePaths (String executable)
  {
    ArrayList incpaths = new ArrayList();
    try
      {
        Elf elf = new Elf(executable, ElfCommand.ELF_C_READ);
        Dwarf dw = new Dwarf(elf, DwarfCommand.READ, null);
        String[] files = dw.getSourceFiles();

        // Since this call returns a lot of non-include file info, we must parse
        // it and glean the include paths from it
        for (int i = 0; i < files.length; i++)
          { 
            if ((DOMCompilerSuffixes.checkCHeader(files[i]) || 
                                                  DOMCompilerSuffixes.checkCPPHeader(files[i])) &&
                ! alreadyAdded(incpaths, files[i]))
              {
                int j = files[i].lastIndexOf("/");
                if (pathFound(files[i].substring(0, j)))
                  {
                    incpaths.add(files[i].substring(0, j));
                  }
              }
          }
        // Add the default includes used for all systems
        if (pathFound(LOCAL_INCLUDE))
          {
            incpaths.add(LOCAL_INCLUDE);
          }
        if (pathFound(GLOBAL_INCLUDE))
          {
            incpaths.add(GLOBAL_INCLUDE);
          }
        return incpaths;
      }

    catch (lib.elf.ElfException ee)
      {
        System.err.println("Error getting include paths: " + ee.getMessage());
        return null;
      }
  }
  
  /**
   * alreadyAdded checks to see if an include path is already in the list before adding it.
   * "/usr/include" and "/usr/local/include" are special cases and are added at the end
   * automatically.
   * 
   * @param filelist is an ArrayList containing the heretofore added include files
   * @param newfile is a String with the candidate include path to be added
   * @return true if the include is already in the list, false if not
   * 
   */
  public static boolean alreadyAdded(ArrayList filelist, String newfile )
  {
    int j = newfile.lastIndexOf("/");
    // Loop thru the already accumulated list to see if the new one is already there
    for (int i = 0; i < filelist.size(); i++)
      {
        if (filelist.get(i).equals(newfile.substring(0, j))) {
              return true;
            }
      }
    // See if the file is the global/local include file, if so say we have it
    if (newfile.substring(0,j).equals(GLOBAL_INCLUDE) ||
            newfile.substring(0,j).equals(LOCAL_INCLUDE))
      return true;
    else
      return false;
  }
  
  /**
   * pathFound checks to be sure the source file is where the executable thinks it is
   * 
   * @param path contains a String of the path to check to see if it exists
   * @return true if the file is found, false if not
   * 
   */
  public static boolean pathFound(String path) {
    File f = new File(path);
    return f.exists();
  }
}
