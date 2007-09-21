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

import frysk.junit.TestCase;

/**
 * Check that LocalMemory addresses are pointing where expected.
 */
public class TestLocalMemory
    extends TestCase
{
    /**
     * Check that the stack address changes as new stack frames are
     * created.
     */
    public void testStackChanging() {
	// Capture a sequence of stack addresses.
	class Stacks implements LocalMemory.StackBuilder {
		int level = 0;
		long[] addresses = new long[2];
		public void stack(long addr, byte[] bytes) {
		    if (level >= addresses.length)
			return;
		    addresses[level++] = addr;
		    LocalMemory.constructStack(this);
		}
	}
	Stacks stacks = new Stacks();
	LocalMemory.constructStack(stacks);
	assertEquals("level", stacks.addresses.length, stacks.level);
	for (int i = 0; i < stacks.addresses.length - 1; i++) {
	    for (int j = i + 1; j < stacks.addresses.length; j++) {
		assertTrue("stack address " + i + " and " + j,
			   stacks.addresses[i] != stacks.addresses[j]);
	    }
	}
    }

    /**
     * Check that the stack contents are as expected.
     */
    public void testStackContents() {
	LocalMemory.constructStack(new LocalMemory.StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    // The stack contains a copy of the data.
		    assertEquals("bytes", LocalMemory.getDataBytes(), bytes);
		    // The stack isn't the same address as the data or code.
		    assertTrue("data", LocalMemory.getDataAddr() != addr);
		    assertTrue("code", LocalMemory.getCodeAddr() != addr);
		}
	    });
    }

    /**
     * Check that at least the first data byte is as expected.
     */
    public void testDataContents() {
	byte[] bytes = LocalMemory.getDataBytes();
	assertEquals("data byte[0]", LocalMemory.byteData, bytes[0]);
    }
    /**
     * Check the data addresses have the expected offsets.
     */
    public void testDataAddresses() {
	assertEquals("byteData address", LocalMemory.getDataAddr(),
		     LocalMemory.getByteDataAddr());
    }
}
