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

import java.math.BigInteger;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.List;
import frysk.debuginfo.PieceLocation;
import inua.eio.ByteBuffer;

/**
 * Type for an array.
 */
public class ArrayType
    extends Type
{
    private final Type type;
    private final int[] dimension;
    // The number of TYPE sized units between successive elements of
    // each Nth index of the array.  e.g. stride[0] is the count to the
    // beginning of the next row and stride[1] is the count to the next column.
    private final int stride[];
    // Total number of TYPE elements.
    private int nrElements;

    /**
     * Create an ArrayType
     * 
     * XXX: dimensions needs to be a set of ranges; this upper bound
     * is sooooo confusing.
     *
     * @param typep - Type of each array element
     * @param dimensions - ArrayList of dimension upper bounds.
     */
    public ArrayType (Type type, int size, ArrayList dimensions) {
	super("array", size);
	this.type = type;
	dimension = new int[dimensions.size()];
	for (int i = 0; i < dimensions.size(); i++) {
	    dimension[i] = ((Integer)dimensions.get(i)).intValue() + 1;
	}
	stride = new int[dimensions.size()];
	stride[stride.length - 1] = 1;
	for (int i = stride.length - 2; i >= 0; i--) {
	    stride[i] = dimension[i + 1] * stride[i + 1];
	}
	nrElements = stride[0] * dimension[0];
    }

    public String toString() {
	StringBuffer b = new StringBuffer();
	b.append("{");
	b.append(type.toString());
	for (int i = 0; i < dimension.length; i++) {
	    b.append("[");
	    b.append("" + dimension[i]);
	    b.append("/");
	    b.append("" + stride[i]);
	    b.append("]");
	}
	b.append("" + nrElements);
	b.append("}");
	return b.toString();
    }

    public Type getType ()
    {
	return type;
    }

    /**
     * Iterate through the array members.
     */
    private class ArrayIterator
	implements java.util.Iterator
    {
	private int idx;
	private Location location;

	ArrayIterator (Location location) {
	    idx = 0;
	    this.location = location;
	}

	public boolean hasNext () {
	    return idx < nrElements;
	}

	public Object next () {
	    return slice(location, idx++, 1);
	}
	
	public void remove ()
	{
	}
    }

    private Location slice(Location location, int idx, int count) {
	int off = idx * type.getSize();
	return location.slice(off, type.getSize() * count);
    }
    
    /**
     * Index Operation on array V and index IDX.
     * 
     * @param taskMem - unused here.
     */
    public Value index (Value v, Value idx, ByteBuffer taskMem)
    {       
        if (dimension.length > 1)
        {            
            ArrayList dims = new ArrayList();
            // For an n-dimensional array, create (n-1) dimensional array, where n>1
            dims.add(new Integer(dimension[dimension.length - 1]-1));
            ArrayType arrayType = new ArrayType(type, dimension[dimension.length - 1] 
                                                      * type.getSize(), dims);
            return new Value(arrayType, v.getLocation().slice(idx.asLong() 
        	             * arrayType.getSize(), arrayType.getSize()));
        }
	return new Value(type, v.getLocation().slice(idx.asLong() * type.getSize(), 
		         type.getSize()));        
    }

    /**
     * Slice returns a slice of an array. For example:
     * SLICE[1:2] on {9, 8, 7, 6} returns {8, 7}
     * SLICE[1:2] on { {11, 11}, {0, 0}, {9, 9} } returns { {0, 0}, {9, 9} }
     * 
     * Note: j should be greater than i.
     */
    public Value slice (Value v, Value i, Value j, ByteBuffer taskMem)
    {     
	int len = (int)(j.asLong() - i.asLong() + 1);
	
	// FIXME: Allow this case instead of throwing error?
	if (len < 0) {
	    throw new RuntimeException("Error: Index 1 should be less than Index 2");
	}
	
	// Create a new dimensions list
        ArrayList dims = new ArrayList();  
        // Length of elements at 0th dimension changes.
        // Rest remain same.
        dims.add(new Integer(len-1));
        for (int k=1; k<dimension.length; k++)
            dims.add(new Integer(dimension[k]-1));        
        
        // Case of one dimensional array.
        if (dimension.length == 1) {
            ArrayType arrayType = new ArrayType(type, len * type.getSize(), dims);
            return new Value(arrayType, v.getLocation().slice(i.asLong() * type.getSize(), 
        	                                              arrayType.getSize())); 
        }    
        
        // Case of multi dimensional array.
        ArrayType arrayType = new ArrayType(type, dimension[dimension.length - 1] * len 
        	                                  * type.getSize(), dims);
        return new Value(arrayType, v.getLocation().slice(i.asLong() * arrayType.getSize(), 
    	                                                  arrayType.getSize()));         
        
    }
    
    /**
     * Dereference operation on array type.
     */
    public Value dereference(Value var1, ByteBuffer taskMem) {
	Location loc = PieceLocation.createSimpleLoc
		       (var1.getLocation().getAddress(), type.getSize(), taskMem);
	return new Value (type, loc);  
    }

    void toPrint(PrintWriter writer, Location location,
		 ByteBuffer memory, Format format, int indent) {
	// XXX: Add dimension start/end string instead of assuming {}
	for (int i = 0; i < dimension.length - 1; i++)
	    writer.print("{");
	for (ArrayIterator e = new ArrayIterator(location); e.hasNext(); ) {
	    if (e.idx > 0) {
		for (int i = 0; i < stride.length - 2; i++)
		    if ((e.idx % stride[i]) == 0)
			writer.print("}");
		if ((e.idx % stride[stride.length - 2]) == 0)
		    writer.print(",");
		for (int i = 0; i < stride.length - 2; i++)
		    if ((e.idx % stride[i]) == 0)
			writer.print("{");
	    }
	    if (! toPrintVector(writer, type, e, memory, format, indent))
	      break;
	}
	for (int i = 0; i < dimension.length - 1; i++)
	    writer.print("}");
    }

    private boolean toPrintVector(PrintWriter writer, Type type, ArrayIterator e,
	    ByteBuffer memory, Format format, int indent)
    {
	boolean isVector = dimension.length == 1;
	int vectorLength = dimension[dimension.length - 1];
	boolean haveCharType;
	boolean noNullByte = true;
	if (type instanceof CharType)
	    haveCharType = true;
	else
	    haveCharType = false;
	
	if (haveCharType) {
	    if (! isVector)
		writer.print("{");
	    writer.print("\"");
	}
	else
	    writer.print("{");

	for (int i = 0; i < vectorLength; i++) {
	    Location l = (Location)e.next();
	    if (haveCharType) {
		BigInteger c = ((CharType)type).getBigInteger(l);
		if (c.equals(BigInteger.ZERO)) {
		    noNullByte = false;
		    break; // NUL
		}
		writer.print((char)c.longValue());
	    }
	    else {
		type.toPrint(writer, l, memory, format, indent);
		if (i < vectorLength - 1)
		    writer.print(",");
	    }
	}

	if (haveCharType) {
	    writer.print("\"");
	    if (! isVector)
		writer.print("}");
	}
	else
	    writer.print("}");
	return noNullByte;
    }

    public void toPrint(PrintWriter writer, String s, int indent) {
	type.toPrint(writer, indent);
	writer.print(" " + s);
	for(int i = 0; i < this.dimension.length; i++) {
	    writer.print("[");
	    writer.print(dimension[i]);
	    writer.print("]");
	}
    }
    
    public void toPrint(PrintWriter writer, int indent) {
	this.toPrint(writer, "", indent);
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
    // Use for unary operations.
    public ArithmeticUnit getALU(int wordSize) {
	return new AddressUnit(this, wordSize);
    }      

    public boolean completeMember(String incomplete, List candidates) {
	return type.completeMember(incomplete, candidates);
    }
}
