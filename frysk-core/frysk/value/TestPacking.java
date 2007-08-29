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
import java.math.BigInteger;

/**
 * Test packing and unpacking of bit-fields.
 */

public class TestPacking
    extends TestCase
{
    private void checkUnpack(int bitOffset, int bitLength,
			     byte[] memory, long signed, long unsigned) {
	Packing packing = new Packing(memory.length, bitOffset, bitLength);
	assertEquals("signed", signed, packing.unpackSigned(memory));
	assertEquals("unsigned", unsigned, packing.unpackUnsigned(memory));
    }
    public void testUnpack9() {
	checkUnpack(2, 4, new byte[] { (byte) 0x24 }, -7, 9);
    }
    public void testUnpack7() {
	checkUnpack(2, 4, new byte[] { (byte) 0x1c }, 7, 7);
    }
    public void testUnpack1() {
	checkUnpack(2, 4, new byte[] { (byte) 0x04 }, 1, 1);
    }
    public void testUnpack8() {
	checkUnpack(2, 4, new byte[] { (byte) 0x20 }, -8, 8);
    }
    public void testUnpack15() {
	checkUnpack(2, 4, new byte[] { (byte) 0x3c }, -1, 15);
    }
    public void testUnpackLHS() {
	checkUnpack(0, 4, new byte[] { (byte) 0x90 }, -7, 9);
    }
    public void testUnpackRHS() {
	checkUnpack(4, 4, new byte[] { (byte) 0x09 }, -7, 9);
    }

    public void checkPack(int bitOffset, int bitLength,
			  byte[] memory, long value, byte[] expected) {
	Packing packing = new Packing(memory.length, bitOffset, bitLength);
	assertEquals("pack", expected,
		     packing.pack(memory, BigInteger.valueOf(value)));
    }
    public void testPack0() {
	// ByteBuffer convers byte[]{0xc3} to byte[]{0x00,0x3c}; this
	// is ok as Location.put(ByteOrder,byte[],int) handles this
	// correctly.
	checkPack(2, 4, new byte[] { (byte) 0xff }, 0,
		  new byte[] { (byte) 0x00, (byte) 0xc3 });
    }
    public void testPackN1() {
	// ByteBuffer convers byte[]{0xc3} to byte[]{0x00,0x3c}; this
	// is ok as Location.put(ByteOrder,byte[],int) handles this
	// correctly.
	checkPack(2, 4, new byte[] { (byte) 0x00 }, -1,
		  new byte[] { (byte) 0x3c });
    }
    public void testPack15() {
	checkPack(2, 4, new byte[] { (byte) 0x00 }, 15,
		  new byte[] { (byte) 0x3c });
    }
    public void testPack8() {
	checkPack(2, 4, new byte[] { (byte) 0x00 }, 8,
		  new byte[] { (byte) 0x20 });
    }
    public void testPack1() {
	checkPack(2, 4, new byte[] { (byte) 0x00 }, 1,
		  new byte[] { (byte) 0x04 });
    }
}
