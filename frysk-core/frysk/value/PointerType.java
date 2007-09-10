// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * Type for a pointer (or address) of another type.
 */
public class PointerType
    extends IntegerTypeDecorator
{
    private final Type type;
    private PointerType(String name, ByteOrder order, int size, Type type,
			IntegerType accessor) {
	super(name, order, size, accessor);
	this.type = type;
    }
    /**
     * Create a PointerType.
     * 
     * @param typep - Type of pointed to value
     *
     * FIXME: Name is redundant here.
     */
    public PointerType(String name, ByteOrder order, int size, Type type) {
	// For moment assume that all pointers are unsigned (which
	// isn't true for MIPS ;-).
	this(name, order, size, type, new UnsignedType("name", order, size));
    }

    public Type getType () {
	return type;
    }
    
    void toPrint(PrintWriter writer, Location location, ByteBuffer memory,
		 Format format) {
	format.print(writer, location, this);
	if (type instanceof CharType) {
	    // XXX: ByteBuffer.slice wants longs.
	    long addr = getBigInteger(location).longValue();
	    writer.print(" \"");
	    while (true) {
		Location l = new ByteBufferLocation(memory, addr,
						    type.getSize());
		BigInteger c = ((CharType)type).getBigInteger(l);
		if (c.equals(BigInteger.ZERO))
		    break; // NUL
		writer.print((char)c.longValue());
		addr += type.getSize();
	    }
	    writer.print("\"");
	}
    }

    public void toPrint(PrintWriter writer) {
	type.toPrint(writer);
	writer.print(" *");
    }

    public Value createValue (long val)
    {
	Value returnValue = new Value(this,  new ScratchLocation(type.getSize()));
	BigInteger bigInteger = BigInteger.valueOf(val);
	returnValue.getLocation().put(order(), bigInteger.toByteArray(), 0);
	return returnValue;
    }

    protected Type clone(IntegerType accessor) {
	return new PointerType(getName(), order(), getSize(), type, accessor);
    }
}
