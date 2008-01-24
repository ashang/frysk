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

package frysk.isa.registers;

import frysk.junit.TestCase;

/**
 * Test the mapping between registers and numbers.
 */
public class TestRegisterMap extends TestCase {
    private final RegisterMap map
	= new RegisterMap("testing")
	.add(IA32Registers.EAX, new Long(0))
	.add(IA32Registers.EBX, new Long(1))
	.add(IA32Registers.ECX, new Long(2))
	.add(IA32Registers.EDX, new Long(3));

    public void testContainsRegister() {
	assertEquals("contains EAX", true,
		     map.containsKey(IA32Registers.EAX));
	assertEquals("contains ESP", false,
		     map.containsKey(IA32Registers.ESP));
    }
    public void testContainsNumber() {
	assertEquals("contains 1", true, map.containsKey(new Long(1)));
	assertEquals("contains 5", false, map.containsKey(new Long(4)));
    }
    public void testContainsInt() {
	assertEquals("contains 1", true, map.containsKey(1));
	assertEquals("contains 5", false, map.containsKey(4));
    }

    public void testRegisterToNumber() {
	assertEquals("EAX to 0", new Long(0),
		     map.getRegisterNumber(IA32Registers.EAX));
    }
    public void testNumberToRegister() {
	assertEquals("1 to EBX", IA32Registers.EBX,
		     map.getRegister(new Long(1)));
    }
    public void testIntToRegister() {
	assertEquals("2 to ECX", IA32Registers.ECX, map.getRegister(2));
		     
    }

    public void testNoSuchRegister() {
	boolean npe = false;
	try {
	    map.getRegisterNumber(IA32Registers.ESP);
	} catch (NullPointerException e) {
	    npe = true;
	}
	assertTrue("npe", npe);
    }
    public void testNoSuchNumber() {
	boolean npe = false;
	try {
	    map.getRegister(new Long(4));
	} catch (NullPointerException e) {
	    npe = true;
	}
	assertTrue("npe", npe);
    }
    public void testNoSuchInt() {
	boolean npe = false;
	try {
	    map.getRegister(4);
	} catch (NullPointerException e) {
	    npe = true;
	}
	assertTrue("npe", npe);
    }
}
