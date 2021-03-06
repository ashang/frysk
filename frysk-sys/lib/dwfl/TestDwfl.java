// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package lib.dwfl;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import frysk.rsl.Log;
import frysk.junit.Runner;
import frysk.junit.TestCase;
import frysk.testbed.LocalMemory;
import frysk.config.Runtime;

public class TestDwfl extends TestCase {
    public static final Log finest = Log.finest(TestDwfl.class);

  public void testDwfl()
  {
    Dwfl dwfl = new Dwfl("");
    assertNotNull("dwfl", dwfl);
  }
  
  public void testDwflReporting()
  {
    Dwfl dwfl = new Dwfl("");
    assertNotNull("dwfl", dwfl);
    
    dwfl.reportBegin();
    dwfl.reportModule("module", 0, 1);
    dwfl.reportEnd();
  }
  
  public void testDwflModule()
  {
    Dwfl dwfl = new Dwfl("");
    assertNotNull("dwfl", dwfl);
    
    String moduleName = "module";
    
    dwfl.reportBegin();    
    dwfl.reportModule(moduleName, 0, 1);
    dwfl.reportEnd();
    
    DwflModule module = dwfl.getModule(0);
    assertNotNull("dwflModule", module);
    assertTrue("found dwfl module", 
               module.getName().equals(moduleName));
  }
  
    public void testModuleReuse() {
	Dwfl dwfl = new Dwfl("");
	assertNotNull("dwfl", dwfl);
	dwfl.reportBegin();
	dwfl.reportModule("module", 0, 1);
	dwfl.reportEnd();
	DwflModule module = dwfl.getModule(0);
	assertEquals("module name", "module", module.getName());
	// now refresh the info; should get the _same_ module.
	dwfl.reportBegin();
	dwfl.reportModule("module", 0, 1);
	dwfl.reportEnd();
	assertSame("refreshed module", module, dwfl.getModule(0));
    }

  public void testDwflGetModule2()
  {
    Dwfl dwfl = new Dwfl("");
    assertNotNull("dwfl", dwfl);
    
    dwfl.reportBegin();
    dwfl.reportModule("module1", 0, 1);
    dwfl.reportModule("module2", 1, 3);
    dwfl.reportEnd();
    
    DwflModule module = dwfl.getModule(2);
    assertNotNull("dwflModule", module);
    assertTrue("found dwfl module", 
               module.getName().equals("module2"));
  }
  
  public void testDwflGetModules()
  {
    Dwfl dwfl = new Dwfl("");
    assertNotNull("dwfl", dwfl);
    
    dwfl.reportBegin();
    dwfl.reportModule("module1", 0, 1);
    dwfl.reportModule("module2", 1, 3);
    dwfl.reportEnd();
    
    DwflModule[] modules = dwfl.getModules();
    assertEquals("Two modules", 2, modules.length);
  }
  
  public void testGetLine ()
  {
    Dwfl dwfl = DwflTestbed.createFromSelf();
    assertNotNull("dwfl", dwfl);
    DwflLine line = dwfl.getSourceLine(LocalMemory.getCodeAddr());
    assertNotNull("line", line);
    String filename = line.getSourceFile();
    assertEquals("file",
		 new File(LocalMemory.getCodeFile()).getName(),
                 new File(filename).getName());
    assertEquals("line", LocalMemory.getCodeLine(), line.getLineNum());

    assertEquals("column", 0, line.getColumn());
  }

    public void testGetDie() {
	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull("dwfl", dwfl);
    
	DwflDie die = dwfl.getCompilationUnit(LocalMemory.getCodeAddr());
	assertNotNull("die", die);
	// assertEquals("bias", 0, die.getBias());
	assertEquals("file",
		     new File(LocalMemory.getCodeFile()).getName(),
		     new File(die.getName()).getName());

	DwarfDie[] allDies
	    = die.getScopes(LocalMemory.getCodeAddr());
	assertNotNull("allDies", allDies);
	String[] names = {
	    LocalMemory.getCodeName(),
	    new File(LocalMemory.getCodeFile()).getName()
	};
	for (int i = 0; i < allDies.length; i++) {
	    assertNotNull("allDies[i]", allDies[i]);
	    // Enable this line if you think that checking for inlined
	    // code in a test suite is ok
	    //        assertEquals(false, allDies[i].isInlinedFunction());        
	    if (i == 1)
		assertEquals("names " + i, names[i],
			     new File(allDies[i].getName()).getName());
	    else
		assertEquals("names " + i, names[i], allDies[i].getName());
	}
    }

    /**
     * Get all the modules of the test application; look for some that
     * should be there.
     */
    public void testGetModules() {
	Dwfl dwfl = DwflTestbed.createFromSelf();
	DwflModule[] modules = dwfl.getModules();
	assertNotNull("modules", modules);
	// Look for some modules that should be there.
	boolean foundSelf = false;
	boolean foundlibc = false;
	for (int i = 0; i < modules.length; i++) {
	    String modName = modules[i].getName();
	    if (modName.lastIndexOf("libc") >= 0) {
		foundlibc = true;
		continue;
	    }
	    if (Runtime.get() == Runtime.JAVA_NATIVE_INTERFACE) {
		if (modName.lastIndexOf("libfrysk-sys-jni.so") >= 0) {
		    foundSelf = true;
		}
	    }
	    if (Runtime.get() == Runtime.COMPILER_NATIVE_INTERFACE) {
		if (modName.lastIndexOf (Runner.getProgramBasename ()) >= 0) {
		    foundSelf = true;
		}
	    }
	}
	assertTrue("found libc", foundlibc);
	assertTrue("found self", foundSelf);
    }

    /**
     * Get a line from an address, then see that the address is
     * included in the DwflLine records returned for a line.
     */
    public void testGetAddresses() {
	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull("dwfl", dwfl);
	long addr = LocalMemory.getCodeAddr();
	finest.log("addr", addr);
	DwflLine line = dwfl.getSourceLine(addr);
	assertNotNull("line", line);
	List lines = dwfl.getLineAddresses(line.getSourceFile(), 
					   line.getLineNum(),
					   0);
	Iterator linesIterator = lines.iterator();
	boolean foundAddress = false;
	while (linesIterator.hasNext()) {
	    DwflLine addrLine = (DwflLine)linesIterator.next();
	    if (addrLine.getAddress() == addr) {
		foundAddress = true;
		break;
	    }
	}
	assertTrue(foundAddress);
    }

    public void testMapContainsVdso() {
	Dwfl dwfl = DwflTestbed.createFromSelf();
	DwflModule[] modules = dwfl.getModules();
	DwflModule vdso = null;
	for (int i = 0; i < modules.length; i++) {
	    DwflModule module = modules[i];
	    finest.log("module", i, "name", module);
	    if (module.getName().equals("[vdso]")) {
		vdso = module;
		break;
	    }
	}
	assertNotNull("vdso", vdso);
	assertNotNull("elf", vdso.getElf());
    }
}
