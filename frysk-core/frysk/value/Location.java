// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import java.io.PrintWriter;

import inua.eio.ByteOrder;

/**
 * Location of a variable.
 *
 * An sequence of bytes accessable using get/put methods.  The
 * underlying buffer does NOT have an implict byte-ordering
 * (technically this makes the buffer big-endian).  Instead, code
 * needing to interpret a sequence of bytes as having a particular
 * ordering (little, big, et.al.) must specify that ordering
 * explicitly.
 */

public abstract class Location
{
    /**
     * Permit extension by anyone.
     */
    protected Location() {
    }

    public String toString() {
	return ("{"
		+ super.toString()
		+ ",length=" + length()
		+ "}");
    }

    /**
     * Get the entire contents of the location as a big-endian array
     * of bytes.  If ORDER is little-endian, the byte array will first
     * be converted to big-endian (i.e., its order will be reversed)
     * before returning.
     *
     * Useful for code trying to extract byte-order dependent data
     * such as that needed by BigInteger and BigDecimal.
     *
     * Could re-implement this by decorating byte-order with a
     * location specific get/put method.
     */    
    public byte[] get(ByteOrder order) {
	byte[] bytes = new byte[(int)length()];
	for (int i=0; i < bytes.length; i++)
	    bytes[i] = getByte(i);

	if (order == ByteOrder.LITTLE_ENDIAN) {
	    for (int i = 0; i < bytes.length / 2; i++) {
		int j = bytes.length - 1 - i;
		byte tmp = bytes[i];
		bytes[i] = bytes[j];
		bytes[j] = tmp;
	    }
	}
	return bytes;
    }

    /**
     * Return the entire contents of the location as a sequence of raw
     * bytes.
     */
    public byte[] toByteArray() {
	return get(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Put the big-endian BYTES into the least-significant end of
     * Location; if there insufficient bytes, pad the most sigificant
     * end with FILL (treated as a byte); if there are too may bytes
     * truncate the most significant end.  If ORDER is little-endian,
     * first convert the big-endian byte array into litte-endian
     * (i.e., , reverse the byte order).
     *
     * Useful for code trying to store byte-order dependent data such
     * as that returned by BigInteger and BigDecimal.
     *
     * Can re-implement this by decorating byte-order with a location
     * specific get/put method.
     */
    public void put(ByteOrder order, byte[] bytes, int fill) {
	// FIXME: Bug introduced into inua.nio.ByteBuffer forces this
	// cast; it should not be needed.
	int xfer = (int)(bytes.length > length()
			 ? length()
			 : bytes.length);
	// FIXME: There isn't a useful bulk transfer method that can store a
	// sub-section of a byte array.
	if (order == ByteOrder.LITTLE_ENDIAN) {
	    // Write to the least significant end of little-endian
	    // memory (i.e., LHS) using bytes from the least
	    // significant end of the big-endian byte array (i.e.,
	    // RHS).
	    for (int i = 0; i < xfer; i++) {
		putByte(i, bytes[bytes.length - i - 1]);
	    }
	    // Pad the little-endian most significant end with FILL.
	    for (int i = xfer; i < length(); i++) {
		putByte(i, (byte)fill);
	    }
	} else {
	    // Write to the least significant end of big-endian memory
	    // (i.e., RHS) using bytes from the least significant end
	    // of the byte array (i.e., RHS also).
	    for (int i = 0; i < xfer; i++) {
		putByte(length() - i - 1,
				 bytes[bytes.length - i - 1]);
	    }
	    // Pad the big-endian most significant end with FILL.
	    for (int i = xfer; i < length(); i++) {
		putByte(i - xfer, (byte)fill);
	    }
	}
    }

    /**
     * Return a slice of this Location starting at byte OFFSET, and
     * going for LENGTH bytes.
     */
    protected abstract Location slice(long offset, long length);
    /**
     * Return the byte at IDX.
     */
    protected abstract byte getByte(long idx);
    /**
     * Poke the byte at IDX.
     */
    protected abstract void putByte(long idx, byte value);

    /**
     * The length of the location, in bytes.  This MUST be a long (or
     * BigInteger) as a value's size could be bigger than 32-bits.
     */
    public abstract long length();
    
    /**
     * Print this Location.
     */
    public abstract void toPrint(PrintWriter writer);
    
    public abstract long getAddress();
}
