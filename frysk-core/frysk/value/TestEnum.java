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

package frysk.value;

import inua.eio.ByteOrder;
import frysk.junit.TestCase;

public class TestEnum extends TestCase {
    private EnumType anEnumType() {
	EnumType enumType = new EnumType(null, ByteOrder.BIG_ENDIAN, 1)
	    .addMember("red", 1)
	    .addMember("orange", 2)
	    .addMember("yellow", 3)
	    .addMember("green", 4)
	    .addMember("blue", 5)
	    .addMember("violet", 6);
	return enumType;
    }

    /**
     * enum {};
     */
    public void testEnumType() {
	EnumType t = anEnumType();
	assertEquals("toPrint",
		     "enum {\n"
		     + "  red = 1,\n"
		     + "  orange = 2,\n"
		     + "  yellow = 3,\n"
		     + "  green = 4,\n"
		     + "  blue = 5,\n"
		     + "  violet = 6\n"
		     + "}",
		     t.toPrint());
    }
    /**
     * enum { ... } v = orange;
     */
    public void testEnum() {
	EnumType t = anEnumType();
	Value v = new Value(t, new ScratchLocation(new byte[] { 2 }));
	assertEquals("toPrint", "orange", v.toPrint());
    }
    /**
     * enum { ... } v = 0; // not valid
     */
    public void testEnumInt() {
	EnumType t = anEnumType();
	Value v = new Value(t, new ScratchLocation(new byte[] { 0 }));
	assertEquals("toPrint", "0", v.toPrint());
    }


    public void testPrintAnonEnumType() {
	EnumType e = new EnumType(null, ByteOrder.BIG_ENDIAN, 4)
	    .addMember("e", 1);
	assertEquals("toPrint", "enum {\n  e = 1\n}", e.toPrint());
    }
    public void testPrintEmptyEnumType() {
	EnumType e = new EnumType("ENUM", ByteOrder.BIG_ENDIAN, 4);
	assertEquals("toPrint", "enum ENUM {\n}", e.toPrint());
    }
}
