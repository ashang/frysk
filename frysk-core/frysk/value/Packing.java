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

import java.math.BigInteger;

/**
 * Utility methods for packing and unpacking BigInteger bit-fields
 * from a big-endian byte[] buffer.
 */

class Packing
{
    // Sign, or most significant bit.
    private final int signBit;
    // Number of bits to trim of the right-hand or least-significant
    // side of the packed value.
    private final int shift;
    // Mask of bits of the packed value.
    private final BigInteger mask;
    /**
     * Create a packing object.  SIZE is the byte size of the value
     * containing the bit field.  BIT_SIZE is the number of bits in
     * the packed field.  BIT_OFFSET is the number bits to the right
     * of the packed field
     */
    Packing(int size, int bitOffset, int bitLength) {
	signBit = size * 8 - bitOffset - 1;
	shift = size * 8 - bitLength - bitOffset;
	mask = (BigInteger.ONE
		.shiftLeft(bitLength)
		.subtract(BigInteger.ONE)
		.shiftLeft(shift));
    }

    public String toString() {
	return ("{"
		+ super.toString()
		+ ",signBit=" + BigInteger.ZERO.setBit(signBit).toString(2)
		+ ",mask=" + mask.toString(2)
		+ ",shift=" + shift
		+ "}");
    }
    /**
     * Pack VALUE into the byte buffer.
     *
     * Note that the returned byte[] buffer may differ in length from
     * the original: leading zeros will be stripped (e.g.,
     * byte[]{000,0x01} becomes byte[]{0x01}); and any leading large
     * unsigned byte will be prefixed with a zero (e.g., byte[]{0xff}
     * becomes byte[]{0x00,0xff}).  The client may need to truncate or
     * zero extend as necessary.
     *
     * Since the receiving method Location.put(ByteOrder,byte[],int)
     * (with zero fill) used to store the packed result handles all
     * this correctly, this is not normally a problem.
     */
    byte[] pack(byte[] buffer, BigInteger value) {
	// See comment above about return lenght being different.
	return (new BigInteger(1, buffer)
		.andNot(mask)
		.or(value.shiftLeft(shift).and(mask))
		.toByteArray());
    }
    /**
     * Unpack the value as a signed.
     */
    BigInteger unpackSigned(byte[] buffer) {
	BigInteger b = new BigInteger(1, buffer);
	if (b.testBit(signBit)) {
	    // Re-pack the value into -1 integer.
	    return (BigInteger.ONE
		    .negate()
		    .andNot(mask)
		    .or(b.and(mask))
		    .shiftRight(shift));
	}
	else {
	    return b.and(mask).shiftRight(shift);
	}
    }
    BigInteger unpackUnsigned(byte[] buffer) {
	return (new BigInteger(1, buffer)
		.and(mask)
		.shiftRight(shift));
    }
}
