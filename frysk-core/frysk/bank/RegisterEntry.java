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

package frysk.bank;

import inua.eio.ByteBuffer;
import frysk.isa.Register;

/**
 * A register within a ByteBuffer register bank.
 */

public class RegisterEntry {

    private final int offset;
    private final int length;
    private final String name;
    private final Register register;

    private RegisterEntry(int offset, int length, String name, Register register) {
	this.offset = offset;
	this.length = length;
	this.name = name;
	this.register = register;
    }

    RegisterEntry(int offset, int length, String name) {
	this(offset, length, name, null);
    }

    RegisterEntry(int offset, int length, Register register) {
	this(offset, length, register.getName(), register);
    }

    Register getRegister() {
	return register;
    }

    public String toString() {
	return (super.toString() + ",offset=" + offset + ",length=" + length
		+ ",name=" + name);
    }

    /**
     * Get the name of the register.
     * 
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * Get the length of the register in bytes.
     * 
     * @return the length
     */
    public int getLength() {
	return length;
    }

    /**
     * Return the offset into the register bank.
     */
    public int getOffset() {
	return offset;
    }

    public void access(ByteBuffer byteBuffer, long offset, long size,
	    byte[] bytes, int start, boolean write) {
	if (write)
	    // XXX: Should be directly supported by ByteBuffer.
	    throw new RuntimeException("write not implemented");
	else
	    byteBuffer.get(this.offset + offset, bytes, start, (int) size);
    }

    long get(ByteBuffer byteBuffer) {
	switch (length) {
	case 1:
	    return byteBuffer.getUByte(offset);
	case 2:
	    return byteBuffer.getUShort(offset);
	case 4:
	    return byteBuffer.getUInt(offset);
	case 8:
	    return byteBuffer.getULong(offset);
	default:
	    throw new RuntimeException("unhandled size: " + length);
	}
    }

    void set(ByteBuffer byteBuffer, long value) {
	switch (length) {
	case 1:
	    byteBuffer.putUByte(offset, (byte) value);
	    break;
	case 2:
	    byteBuffer.putUShort(offset, (short) value);
	    break;
	case 4:
	    byteBuffer.putUInt(offset, (int) value);
	    break;
	case 8:
	    byteBuffer.putULong(offset, value);
	    break;
	default:
	    throw new RuntimeException("unhandled size: " + length);
	}
    }

}
