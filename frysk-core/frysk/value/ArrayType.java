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

import java.util.ArrayList;
import java.io.PrintWriter;
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
	super(size, type.endian, 0, "array");
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
     * FIXME: What exactly does this do?  Why not pass in the indexes
     * and then separately the repeat count of that last element.
     */
    public Value get (Value v, int componentsIdx, ArrayList components) {
	if (componentsIdx >= components.size())	// want the entire array?
	    return v;
	int offset = 0;
	int d = 0;
	while (componentsIdx < components.size()) {
	    int lbound = Integer.parseInt((String)components.get(componentsIdx));
	    int hbound = Integer.parseInt((String)components.get(componentsIdx+1));
	    
	    offset += stride[d] * lbound;
	    if (lbound != hbound) {
		// FIXME: This doesn't handle multi-dimensional
		// arrays.
		int count = hbound-lbound;
		ArrayList dims = new ArrayList();
		dims.add(new Integer(count));
		ArrayType arrayType = new ArrayType(type, count * type.size,
						    dims);
		return new Value(arrayType,
				 (v.getLocation()
				  .slice(offset * type.getSize(), 
					 count * type.getSize())));
	    }
	    componentsIdx += 2;
	    d++;
	}
	return new Value(type, v.getLocation().slice(offset * type.getSize(), 
		type.getSize()));
    }
    
    void toPrint(PrintWriter writer, Location location,
		 ByteBuffer memory, Format format) {
	ArrayIterator e = new ArrayIterator(location);
	if (type instanceof IntegerType && type.getSize() == 1) {
	    // Treat it as a character string
	    writer.print("\"");
	    byte[] string = location.toByteArray();
	    for (int i = 0; i < string.length; i++) {
		if (string[i] == 0)
		    break; // NUL
		writer.print((char)string[i]);
	    }
	    writer.print("\"");
	} else {
	    for (int i = 0; i < dimension.length; i++)
		writer.print("{");
	    while (e.hasNext()) {
		if (e.idx > 0) {
		    if ((e.idx % dimension[dimension.length - 1]) == 0)
			writer.print("},{");
		    else
			writer.print(",");
		}
		Location l = (Location)e.next();
		type.toPrint(writer, l, memory, format);
	    }
	    for (int i = 0; i < dimension.length; i++)
		writer.print("}");
	}
    }

    public void toPrint(PrintWriter writer) {
	type.toPrint(writer);
	writer.print(" [");
	for(int i = 0; i < this.dimension.length; i++) {
	    if (i > 0)
		writer.print(",");
	    writer.print(dimension[i]);
	}
	writer.print("]");
    }
}
