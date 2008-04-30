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

import frysk.debuginfo.PieceLocation;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
		 Format format, int indent) {
	// Print type of pointer
	StringBuilder stringBuilder = new StringBuilder();
	this.toPrint(stringBuilder, indent);
	stringBuilder.append(") ");
	stringBuilder.insert(0, "(");
	writer.print(stringBuilder);
	try {
	   format.print(writer, location, this);
	} catch (RuntimeException e) {
	    throw new RuntimeException("Peek Memory");
	}
	Type ultimateType = type.getUltimateType();
	if (ultimateType instanceof CharType) {
	    // XXX: ByteBuffer.slice wants longs.
	    long addr = getBigInteger(location).longValue();
	    writer.print(" \"");
	    while (true) {
		Location l = new ByteBufferLocation
		    (memory, addr, ultimateType.getSize());
		BigInteger c = BigInteger.ZERO;
		try {
		   c = ((CharType)ultimateType).getBigInteger(l);
		} catch (RuntimeException e) {
		    writer.print(" < Memory Error > ");
		    break;
		}
		if (c.equals(BigInteger.ZERO))
		    break; // NUL
		writer.print((char)c.longValue());
		addr += ultimateType.getSize();
	    }
	    writer.print("\"");
	}
    }

    public void toPrint(StringBuilder stringBuilder, int indent) {
	// For handling int (*x)[2]
	if (type instanceof ArrayType || type instanceof FunctionType) {
	    if (indent > 0 && stringBuilder.length() > 0
		    && stringBuilder.charAt(0) == ' ') {
		stringBuilder.deleteCharAt(0);
		stringBuilder.insert(0, " (*");
	    }
	    else
		stringBuilder.insert(0, "(*");
	    stringBuilder.append(")");
	    type.toPrint(stringBuilder, indent);
	}
	else {
	    if (indent > 0 && stringBuilder.length() > 0
		    && stringBuilder.charAt(0) == ' ')
		stringBuilder.deleteCharAt(0);
	    stringBuilder.insert(0, "*");
	    if (! (type instanceof PointerType))
		stringBuilder.insert(0, " ");
	    type.toPrint(stringBuilder, indent);
	}
    }

    public void toPrintBrief(StringBuilder stringBuilder, int indent) {
	    stringBuilder.insert(0, getName());
	    stringBuilder.insert(0, " ");
	    type.toPrintBrief(stringBuilder, indent);
}

    protected Type clone(IntegerType accessor) {
	return new PointerType(getName(), order(), getSize(), type, accessor);
    }

    /**
     * Dereference operation on pointer type.
     */
    public Value dereference(Value var1, ByteBuffer taskMem) {
	Location loc = PieceLocation.createSimpleLoc
		       (var1.asLong(), type.getSize(), taskMem);
	return new Value(type, loc);
    }

    /**
     * Index Operation for pointers.
     */
    public Value index(Value v, Value idx, ByteBuffer taskMem) {
	Value offset = createValue(v.asLong() + idx.asLong() * type.getSize());
	return dereference(offset, taskMem);
    }

    /**
     * Slice operation for pointers.
     */
    public Value slice(Value v, Value i, Value j, ByteBuffer taskMem) {
	// Evaluate length and offset of slice.
	long offset = v.asLong() + i.asLong()*type.getSize();
	int len = (int)(j.asLong() - i.asLong() + 1);
	if (len < 0) {
	    throw new RuntimeException("Error: Index 1 should be <= than Index 2");
	}

	// Create a simple memory location with it.
	Location loc = PieceLocation.createSimpleLoc
	               (offset, len*type.getSize(), taskMem);

	ArrayList dims = new ArrayList();
	dims.add(new Integer(len - 1));
	Type resultType = new ArrayType(type, len * type.getSize(), dims);

	return new Value(resultType, loc);
    }
    
    public Type getSliceType() {
	ArrayList dims = new ArrayList();
	dims.add(new Integer(-1));
	return new ArrayType(type, 0, dims);
    }
    
    /* getALUs are double dispatch functions to determine
     * the ArithmeticUnit for an operation between two types.
     */
    public ArithmeticUnit getALU(Type type, int wordSize) {
	return type.getALU(this, wordSize);
    }
    public ArithmeticUnit getALU(IntegerType type, int wordSize) {
	return new AddressUnit(this, wordSize);
    }
    public ArithmeticUnit getALU(PointerType type, int wordSize) {
	throw new RuntimeException("Invalid Pointer Arithmetic");
    }
    public ArithmeticUnit getALU(FloatingPointType type, int wordSize) {
	throw new RuntimeException("Invalid Pointer Arithmetic");
    }
    public ArithmeticUnit getALU(ArrayType type, int wordSize) {
	throw new RuntimeException("Invalid Pointer Arithmetic");
    }
    // Use for unary operations.
    public ArithmeticUnit getALU(int wordSize) {
	return new AddressUnit(this, wordSize);
    }

    public boolean completeMember(String incomplete, List candidates) {
	return type.completeMember(incomplete, candidates);
    }
}
