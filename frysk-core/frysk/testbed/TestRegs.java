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

package frysk.testbed;

import java.util.Map.Entry;
import frysk.isa.Register;
import java.util.LinkedList;
import java.util.Random;
import java.util.Iterator;
import frysk.isa.RegistersFactory;
import frysk.isa.RegisterGroup;

/**
 * Sanity check of the RegsCase - that everything needed is present.
 */

public class TestRegs extends TestLib {

    private RegsCase regsCase;

    public void setUp() {
	regsCase = new RegsCase() {
		// Should not be called.
		public void access(Register register, int offset, int length,
				   byte[] bytes, int start, boolean write) {
		    fail("getRegister called");
		}
		public long getRegister(Object taskObject, Register register) {
		    fail("getRegister called");
		    return 0;
		}
	    };
	regsCase.setUp();
    }
    
    public void tearDown() {
	regsCase.tearDown();
	regsCase = null;
    }

    /**
     * Check that there is a value to test.
     */
    public void testValuesAvailable() {
	assertNotNull(regsCase.isa().toString(), regsCase.values());
    }

    /**
     * Create a byte array with all elements filled in with random
     * non-zero values.
     */
    private byte[] nonzeroBytes(Random random, int size) {
	byte[] bytes = new byte[size];
	for (int i = 0; i < bytes.length; i++) {
	    bytes[i] = (byte)(random.nextInt(255) + 1);
	}
	return bytes;
    }

    public void testValues() {
	for (Iterator i = regsCase.values().iterator(); i.hasNext(); ) {
	    Entry entry = (Entry)i.next();
	    Register register = (Register)entry.getKey();
	    RegsCase.Value value = (RegsCase.Value)entry.getValue();
	    value.checkValue(register);
	}
    }

    /**
     * Called by TestRegsCase; to verify that all registers are
     * present; if some are missing it dumps out suggested test
     * values.
     */
    public void testGeneralRegistersPresent() {
	checkRegisterGroupPresent(RegistersFactory.getRegisters(regsCase.isa())
				  .getGeneralRegisterGroup());
    }
    public void testFloatRegistersPresent() {
	checkRegisterGroupPresent("float");
    }
    public void testVectorRegistersPresent() {
	checkRegisterGroupPresent("vector");
    }

    private void checkRegisterGroupPresent(String what) {
	RegisterGroup registerGroup = RegistersFactory
	    .getRegisters(regsCase.isa())
	    .getGroup(what);
	if (unsupported("no " + what + " registers", registerGroup == null))
	    return;
	checkRegisterGroupPresent(registerGroup);
    }

    private void checkRegisterGroupPresent(RegisterGroup group) {
	Register[] registers = group.getRegisters();
	LinkedList missing = new LinkedList();
	for (int i = 0; i < registers.length; i++) {
	    Register r = registers[i];
	    if (regsCase.values() == null || !regsCase.values().containsKey(r))
		missing.add(r);
	}
	// Helpful, if naughty.  Dump out suggested code for the
	// missing registers.
	if (missing.size() > 0) {
	    System.out.println();
	    Random random = new Random();
	    for (Iterator i = missing.iterator(); i.hasNext(); ) {
		Register r = (Register)i.next();
		byte[] bytes = nonzeroBytes(random, r.getType().getSize());
		System.out.print("\t.put(Registers.");
		System.out.print(r.getName().toUpperCase());
		System.out.print(", // 0x");
		System.out.print(regsCase.toBigInteger(bytes).toString(16));
		System.out.println();
		System.out.print("\t     ");
		System.out.print("new byte[] { ");
		for (int j = 0; j < bytes.length; j++) {
		    if (j > 0) {
			System.out.print(",");
			if (j % 4 == 0) {
			    System.out.println();
			    System.out.print("\t\t\t  ");
			}
		    }
		    if (bytes[j] > 0) {
			System.out.print("0x");
			System.out.print(Integer.toHexString(bytes[j]));
		    } else {
			System.out.print("(byte)0x");
			System.out.print(Integer.toHexString(bytes[j] & 0xff));
		    }
		}
		System.out.print(" })");
		System.out.println();
	    }
	}
	assertEquals("missing " + regsCase.isa().toString() + " registers",
		     0, missing.size());
    }
}
