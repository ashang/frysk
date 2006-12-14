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

/**
 * DOMCompilerSuffixes contains the suffixes associated with each type of file the
 * compiler recognizes.  This list was created from the command:
 * 
 *                   info gcc Invoking Overall
 *
 * This command provides a list of all the files associated with gcc and
 * g++, that is, which suffixes are assumed to be "c" files and which files
 * are assumed to be "c++" files.
 */

public class DOMCompilerSuffixes
{
  public static final String[] CPP =
    { ".c++", ".cpp", ".cxx", ".ii", ".mm", ".M", ".C", ".CPP", ".cc", ".cp" };
  
  public static final String[] C =
    { ".c", ".i", ".m", ".mi" };
  
  public static final String[] CPPHEADER = { ".h", ".H", ".hh" };
  
  public static final String[] CHEADER = { ".h" };
  
  /**
   * checkCPP checks for the validity of file suffixes for source files
   * that the g++ compiler recognizes.
   * 
   * @param file is a String containing the name of the source file to be checked
   * @return true if it is a valid name, false if not
   * 
   */
  public static boolean checkCPP(String file)
  {
   for (int i = 0; i < CPP.length; i++)
     {
       if (file.endsWith(CPP[i]))
         return true;
     }
   return false;
  }
  
  /**
   * checkC checks for the validity of file suffixes for source files
   * that the gcc compiler recognizes.
   * 
   * @param file is a String containing the name of the source file to be checked
   * @return true if it is a valid name, false if not
   * 
   */
  public static boolean checkC(String file)
  {
   for (int i = 0; i < C.length; i++)
     {
       if (file.endsWith(C[i]))
         return true;
     }
   return false;
  }
   
   /**
    * checkCHeader checks for the validity of file suffixes for source file
    * headers that the gcc compiler recognizes.
    * 
    * @param file is a String containing the name of the source file to be checked
    * @return true if it is a valid name, false if not
    * 
    */
   public static boolean checkCHeader(String file)
   {
    for (int i = 0; i < CHEADER.length; i++)
      {
        if (file.endsWith(CHEADER[i]))
          return true;
      }
    return false;
  }
   
   /**
    * checkCPPHeader checks for the validity of file suffixes for source file
    * headers that the g++ compiler recognizes.
    * 
    * @param file is a String containing the name of the source file to be checked
    * @return true if it is a valid name, false if not
    * 
    */
   public static boolean checkCPPHeader(String file)
   {
    for (int i = 0; i < CPPHEADER.length; i++)
      {
        if (file.endsWith(CPPHEADER[i]))
          return true;
      }
    return false;
  }
}