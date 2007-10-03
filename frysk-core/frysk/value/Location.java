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

/**
 * Location of a variable.
 */

package frysk.value;

import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

public class Location
{
    private final ByteBuffer location;
    private final int index;

    Location (long capacity)  {
	this (new ArrayByteBuffer(capacity));
    }

    Location (ByteBuffer location) {
	this (location, 0);
    }

    Location(ByteBuffer location, int index) {
	this.location = location;
	this.index = 0;
    }

    /**
     * Create a location containing BYTES.
     */
    Location(byte[] bytes) {
	this(new ArrayByteBuffer(bytes));
    }

    /**
     * Return the contents of the location as an array of bytes.  If
     * little-endian, reverse the byte-order making it big-endian.
     *
     * Useful for code trying to extract byte-order dependant values.
     */
    byte[] asByteArray(ByteOrder order) {
	byte[] bytes = new byte[(int)location.capacity()];
	location.get(0, bytes, 0, bytes.length);
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

    public ByteBuffer getByteBuffer() { return location;}
    double getDouble() { return location.getDouble(index); }
    float getFloat() { return location.getFloat(index); }
    long getLong() { return location.getLong(index); }
    int getInt() { return location.getInt(index); }
    short getShort() { return location.getShort(index); }
    byte getByte() { return (byte)location.getByte(index); }

    double getDouble(ByteOrder order) { return location.getDouble(order, index); }
    float getFloat(ByteOrder order) { return location.getFloat(order, index); }
    long getLong(ByteOrder order) { return location.getLong(order, index); }
    int getInt(ByteOrder order) { return location.getInt(order, index); }
    short getShort(ByteOrder order) { return location.getShort(order, index); }
    
    double getDouble(int idx) { return location.getDouble(idx); }
    float getFloat(int idx) { return location.getFloat(idx); }
    long getLong(int idx) { return location.getLong(idx); }
    int getInt(int idx) { return location.getInt(idx); }
    short getShort(int idx) { return location.getShort(idx); }
    byte getByte(int idx) { return (byte)location.getByte(idx); }
  
    double getDouble(ByteOrder order, int idx) { return location.getDouble(order, idx); }
    float getFloat(ByteOrder order, int idx) { return location.getFloat(order, idx); }
    long getLong(ByteOrder order, int idx) { return location.getLong(order, idx); }
    int getInt(ByteOrder order, int idx) { return location.getInt(order, idx); }
    short getShort(ByteOrder order, int idx) { return location.getShort(order, idx); }
    
    void putDouble(double value)  {location.putDouble(index, value);}
    void putFloat(float value)  {location.putFloat(index, value);}
    void putLong(long value)  {location.putLong(index, value);}
    void putInt(int value)  {location.putInt(index, value);}
    void putShort(short value)  {location.putShort(index, value);}
    void putByte(byte value)  {location.putByte(index, (byte)value);}
    
    void putDouble(ByteOrder order, double value)  {location.putDouble(order, index, value);}
    void putFloat(ByteOrder order, float value)  {location.putFloat(order, index, value);}
    void putLong(ByteOrder order, long value)  {location.putLong(order, index, value);}
    void putInt(ByteOrder order, int value)  {location.putInt(order, index, value);}
    void putShort(ByteOrder order, short value)  {location.putShort(order, index, value);}
}
