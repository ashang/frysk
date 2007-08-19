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

import frysk.junit.TestCase;
import inua.eio.ByteOrder;

/**
 * Type for an integer value.
 */
public class TestIntegerType
    extends TestCase
{    
    private void checkAsBigInteger(IntegerType type, int xff00, int x0102) {
	assertEquals("0xff00", xff00,
		     type.asBigInteger(new Location(new byte[] { (byte)0xff, 0x00 }))
		     .intValue());
	assertEquals("0x0102", x0102,
		     type.asBigInteger(new Location(new byte[] { 0x01, 0x02 }))
		     .intValue());
	
    }

    public void testAsSignedBig() {
	checkAsBigInteger(new SignedType(2, ByteOrder.BIG_ENDIAN, -1,
					 "signed-big-endian", false),
			  (short)0xff00, 0x0102);
    }
    public void testAsSignedLittle() {
	checkAsBigInteger(new SignedType(2, ByteOrder.LITTLE_ENDIAN, -1,
					 "signed-little-endian", false),
			  0x00ff, 0x0201);
    }

    public void testAsUnsignedBig() {
	checkAsBigInteger(new UnsignedType(2, ByteOrder.BIG_ENDIAN, -1,
					   "unsigned-big-endian", false),
			  0xff00, 0x0102);
    }
    public void testAsUnsignedLittle() {
	checkAsBigInteger(new UnsignedType(2, ByteOrder.LITTLE_ENDIAN, -1,
					   "unsigned-little-endian", false),
			  0x00ff, 0x0201);
    }

    public void testAsEnumBig() {
	checkAsBigInteger(new EnumType(ByteOrder.BIG_ENDIAN),
			  (short)0xff00, 0x0102);
    }
    public void testAsEnumLittle() {
	checkAsBigInteger(new EnumType(ByteOrder.LITTLE_ENDIAN),
			  0x00ff, 0x0201);
    }
}
