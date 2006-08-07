// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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


package lib.dw.tests;

import junit.framework.TestCase;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;

import frysk.imports.Build;

public class TestDwfl
    extends TestCase
{
    /**
     * A variable that has the value true.  Used by code trying to
     * stop the optimizer realise that there's dead code around.
     */
    static boolean trueXXX = true;
    /**
     * A function that returns true, and prints skip.  Used by test
     * cases that want to be skipped (vis: if(broken()) return) while
     * trying to avoid the compiler's optimizer realizing that the
     * rest of the function is dead.
     */
    protected static boolean brokenXXX (int bug)
    {
	System.out.print ("<<BROKEN http://sourceware.org/bugzilla/show_bug.cgi?id=" + bug + " >>");
	return trueXXX;
    }


  public void testGetLine ()
  {
    Dwfl dwfl = new Dwfl(TestLib.getPid());
    assertNotNull(dwfl);
    DwflLine line = dwfl.getSourceLine(TestLib.getFuncAddr());
    assertNotNull(line);
    String filename = line.getSourceFile();
    assertEquals("TestLib.cxx",
                 filename.substring(filename.lastIndexOf("/") + 1));

    if(Build.BUILD_ARCH.indexOf("x86_64") != -1)
      assertEquals(55, line.getLineNum());
    else if (Build.BUILD_ARCH.indexOf("powerpc") != -1)
      assertEquals(53, line.getLineNum());
    else
      assertEquals(51, line.getLineNum());

    assertEquals(0, line.getColumn());
  }

  public void testGetDie ()
  {
    Dwfl dwfl = new Dwfl(TestLib.getPid());
    assertNotNull(dwfl);
    
    DwflDieBias bias = dwfl.getDie(TestLib.getFuncAddr());
    assertNotNull(bias);
    
    assertEquals(0, bias.bias);
    
    DwarfDie die = bias.die;
    assertNotNull(die);
    
    assertEquals("TestLib.cxx",
                 die.getName().substring(die.getName().lastIndexOf("/") + 1));

    DwarfDie[] allDies = die.getScopes(TestLib.getFuncAddr() - bias.bias);
    assertNotNull(allDies);

    String[] names = { "getFuncAddr", "TestLib.cxx" };

    for (int i = 0; i < allDies.length; i++)
      {
        assertNotNull(allDies[i]);
        if (i == 1)
          assertEquals(
                       names[i],
                       allDies[i].getName().substring(
                                                      die.getName().lastIndexOf(
                                                                                "/") + 1));
        else
          assertEquals(names[i], allDies[i].getName());
      }
  }

}
