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

/**
 * Test location code.
 *
 * Note, for big-endian the most significant bytes are on the LHS
 * and the least significant are on the RHS.
 *
 * Note, and for little-endian the most significant are on the RHS and
 * the least significant are on the LHS.
 */

public class TestLocation extends TestCase
{
    private Location l;
    public void setUp() {
	l = new Location(new byte[] { 1, 2, 3, 4 });
    }
    public void tearDown() {
	l = null;
    }
    public void testGetBig() {
	assertEquals("byte array", new byte[] { 1, 2, 3, 4 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }
    public void testGetLittle() {
	assertEquals("byte array", new byte[] { 4, 3, 2, 1 },
		     l.get(ByteOrder.LITTLE_ENDIAN));
    }

    /**
     * Since the write and location size are the same, this checks for
     * a direct write.
     */
    public void testPutBig() {
	l.put(ByteOrder.BIG_ENDIAN, new byte[] { 5, 6, 7, 8 }, 77);
	assertEquals("byte array", new byte[] { 5, 6, 7, 8 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }
    /**
     * Since the write and location size are the same, this checks for
     * a direct write but with the order reversed.
     */
    public void testPutLittle() {
	l.put(ByteOrder.LITTLE_ENDIAN, new byte[] { 5, 6, 7, 8 }, 77);
	assertEquals("byte array", new byte[] { 8, 7, 6, 5 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }

    /**
     * Since the most significant big-endian bytes are on the left,
     * this test checks that those excess bytes are discarded during
     * the store.
     */
    public void testPutBigLong() {
	l.put(ByteOrder.BIG_ENDIAN, new byte[] { 5, 6, 7, 8, 9 }, 77);
	assertEquals("byte array", new byte[] { 6, 7, 8, 9 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }
    /**
     * Since the most sigificant big-endian bytes are on the left,
     * this test checks that both those excess bytes are discared and
     * that the least significant bytes are reversed (little-endian).
     */
    public void testPutLittleLong() {
	l.put(ByteOrder.LITTLE_ENDIAN, new byte[] { 5, 6, 7, 8, 9 }, 77);
	assertEquals("byte array", new byte[] { 9, 8, 7, 6 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }

    /**
     * Since the least significant big-endian bytes are on the right,
     * this test checks that the short value is stored in the RHS of
     * the location with the LHS (most sigificant bytes) padded with
     * FILL.
     */
    public void testPutBigShort() {
	l.put(ByteOrder.BIG_ENDIAN, new byte[] { 5, 6 }, 77);
	assertEquals("byte array", new byte[] { 77, 77, 5, 6 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }
    /**
     * Since the least significant little-endian bytes are on the
     * left, this test checks that the short value is stored in the
     * LHS of the location (order reversed) with the RHS (most
     * sigificant bytes) padded with FILL.
     */
    public void testPutLittleShort() {
	l.put(ByteOrder.LITTLE_ENDIAN, new byte[] { 5, 6 }, 77);
	assertEquals("byte array", new byte[] { 6, 5, 77, 77 },
		     l.get(ByteOrder.BIG_ENDIAN));
    }

    public void testSlice() {
	Location s = l.slice(1, 2); // 2,3
	assertEquals("slice", new byte[] { 2, 3 },
		     s.get(ByteOrder.BIG_ENDIAN));
    }
}
