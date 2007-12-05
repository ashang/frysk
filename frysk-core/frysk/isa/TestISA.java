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

package frysk.isa;

import frysk.junit.TestCase;
import frysk.Config;

/**
 * Searchable, hashable key sufficient for identifying the supported
 * Instruction Set Architectures
 *
 * Client code, rather than extending this key should implement local
 * or more global structures indexed by this key.
 */

public class TestISA extends TestCase {
    public void testEquals() {
	assertTrue("equals", ISA.IA32.equals(ISA.IA32));
	assertFalse("!equals", ISA.IA32.equals(ISA.X8664));
    }
    public void testToString() {
	assertEquals("toString", 
		     "32-bit big-endian PowerPC32BE",
		     ISA.PPC32BE.toString());
	assertEquals("toString",
		     "64-bit big-endian PowerPC64BE",
		     ISA.PPC64BE.toString());
    }
    public void testElfGet() {
	assertSame("IA32 core", ISA.IA32,
		   ElfMap.getISA(Config.getPkgDataFile("test-core-x86")));
	assertSame("X8664 core", ISA.X8664,
		   ElfMap.getISA(Config.getPkgDataFile("test-core-x8664")));

	assertSame("IA32 exe", ISA.IA32,
		   ElfMap.getISA(Config.getPkgDataFile("test-exe-x86")));
    }
    public void testMappedIsa() {
	ISAMap map = new ISAMap("good")
	    .put(ISA.IA32, "ia32");
	assertEquals("get", "ia32", (String)(map.get(ISA.IA32)));
	assertTrue("containsKey", map.containsKey(ISA.IA32));
    }
    public void testUnmappedIsa() {
	ISAMap map = new ISAMap("BAD")
	    .put(ISA.IA32, "ia32");
	assertFalse("containsKey", map.containsKey(ISA.X8664));
	Object o = null;
	RuntimeException e = null;
	try {
	    o = map.get(ISA.X8664);
	} catch (RuntimeException r) {
	    e = r;
	}
	assertNull("no result", o);
	assertEquals("exception",
		     "The "+ISA.X8664+" is not supported (required by BAD)",
		     e.getMessage());
    }
}
