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

import java.util.Iterator;
import java.util.List;
import frysk.junit.TestCase;
import frysk.junit.Paths;
import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;
import lib.dw.DwflModule;
import frysk.junit.TestCase;


public class TestDwfl
    extends TestCase
{
  public void testGetLine ()
  {
    Dwfl dwfl = new Dwfl(TestLib.getPid());
    assertNotNull(dwfl);
    DwflLine line = dwfl.getSourceLine(TestLib.getFuncAddr());
    assertNotNull(line);
    String filename = line.getSourceFile();
    assertEquals("TestLib.cxx",
                 filename.substring(filename.lastIndexOf("/") + 1));

    assertEquals(58, line.getLineNum());

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
        /* Enable this line if you think that checking for inlined code in a test suite
         * is ok
         */
//        assertEquals(false, allDies[i].isInlinedFunction());        
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

  // Get all the modules of the test application; look for some that
  // should be there. 
  public void testGetModules() 
  {
    Dwfl dwfl = new Dwfl(TestLib.getPid());
    DwflModule[] modules = dwfl.getModules();
    assertNotNull(modules);
    // Look for some modules that should be there.
    boolean foundTestRunner = false;
    boolean foundlibc = false;
    boolean foundlibgcj = false;
    for (int i = 0; i < modules.length; i++) 
      {
	String modName = modules[i].getName();
	if (modName.lastIndexOf (Paths.getRunnerBasename ()) >= 0)
	  foundTestRunner = true;
	else if (modName.lastIndexOf("libc") >= 0)
	  foundlibc = true;
	else if (modName.lastIndexOf("libgcj") >= 0)
	  foundlibgcj = true;
      }
    assertTrue(foundTestRunner && foundlibc && foundlibgcj);
  }

  // Get a line from an address, then see that the address is included
  // in the DwflLine records returned for a line.
  public void testGetAddresses() 
  {
    Dwfl dwfl = new Dwfl(TestLib.getPid());
    assertNotNull(dwfl);
    long addr = TestLib.getFuncAddr();
    DwflLine line = dwfl.getSourceLine(addr);
    assertNotNull(line);
    List lines = dwfl.getLineAddresses(line.getSourceFile(), 
					 line.getLineNum(),
					 0);
    Iterator linesIterator = lines.iterator();
    boolean foundAddress = false;
    while (linesIterator.hasNext()) 
      {
	DwflLine addrLine = (DwflLine)linesIterator.next();
	if (addrLine.getAddress() == addr) 
	  {
	    foundAddress = true;
	    break;
	  }
      }
    assertTrue(foundAddress);
  }
}
