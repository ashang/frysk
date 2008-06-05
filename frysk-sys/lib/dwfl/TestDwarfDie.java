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

import frysk.junit.TestCase;
import frysk.testbed.LocalMemory;

public class TestDwarfDie extends TestCase {

    
    public void testHasAttribute() {
	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull(dwfl);

	// get CUDIE
	DwflDieBias bias = dwfl.getCompilationUnit(LocalMemory.getCodeAddr());
	assertNotNull(bias);
	DwarfDie die = bias.die;
	assertEquals("Die has the correct tag", DwTag.COMPILE_UNIT, die
		.getTag());

	assertTrue("Has name attribute", die.hasAttribute(DwAt.NAME));
	assertFalse("Does not have location attribute", die
		.hasAttribute(DwAt.LOCATION));
    }
    
    
    public void testGetCompilationUnit(){
	long pc = LocalMemory.getCodeAddr();
	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull(dwfl);

	// get CUDIE
	DwflDieBias bias = dwfl.getCompilationUnit(pc);
	assertNotNull(bias);
	
	DwarfDie cuDie = bias.die;
	assertNotNull(cuDie);
	
	DwarfDie die = cuDie;
	
	assertTrue("cudie returned", die.getCompilationUnit().getName().equals(cuDie.getName()));
	
	die = cuDie.getScopes(pc)[0];
	assertNotNull(die);
	
	assertTrue("cudie returned", die.getCompilationUnit().getName().equals(cuDie.getName()));
    }

    public void testGetModule(){
	
	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull(dwfl);
	long addr = LocalMemory.getCodeAddr();

	DwflDieBias dwflDie = dwfl.getCompilationUnit(addr);
	assertNotNull("dwflDie", dwflDie);
	DwarfDie cuDie = dwflDie.die;
	assertNotNull("cuDie", cuDie);
	    
	DwarfDie die = cuDie.getScopes(addr)[0];
	DwflModule dwflModule = die.getModule();
	assertNotNull(dwflModule);
	
	assertTrue("Found correct module", dwflModule.getName().contains("TestRunner"));
    }

    public void testGetOffset(){

	Dwfl dwfl = DwflTestbed.createFromSelf();
	assertNotNull(dwfl);
	long addr = LocalMemory.getCodeAddr();

	DwarfDie cuDie = dwfl.getCompilationUnit(addr).die;
	assertNotNull(cuDie);
	    
	DwarfDie die = cuDie.getScopes(addr)[0];
	
	long offset = die.getOffset();
	
	DwarfDie retrievedDie = die.getModule().getDieByOffset(offset);
	assertNotNull(retrievedDie);
	
	assertEquals("dies have the same name", die.getName(), retrievedDie.getName());
	assertEquals("dies have the same tag", die.getTag(), retrievedDie.getTag());
	
	die = retrievedDie;
	
    }

    
}
