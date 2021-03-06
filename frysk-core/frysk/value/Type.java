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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Holds the type of a Value and also defines possible operations.
 * Classes extended from this type will have to define the individual
 * operation that are defined on those types. e.g. addition operation
 * may be defined for the integer type.
 */

public abstract class Type {
    private final int size;

    // XXX: Is NAME a more target dependant attribute?
    private final String name;
  
    Type (String name, int size) {
	this.name = name;
	this.size = size;
    }

    public int getSize() {
	return size;
    }

    public String getName() {
	return name;
    }

    /**
     * Return the ultimate type (ignoring any decorator and other
     * attributes).
     */
    public Type getUltimateType() {
	return this;
    }

    /**
     * For debugging and tracing; just dump the Type's name.
     */
    public String toString() {
	return ("{"
		+ super.toString()
		+ ",name=" + name
		+ ",size=" + size
		+ "}");
    }

    /**
     * Print Location as Type in user-readable form; use Format to
     * print basic types.  If needed, and when memory is non-NULL, it
     * can be used for dereferencing pointers.  Indent before printing. 
     */
    abstract void toPrint(PrintWriter writer, Location location,
			  ByteBuffer memory, Format format, int indent);
    /*
     * Print Location as Type in user-readable form to a StringBuffer;
     * use Format to print basic types.  If needed, and when memory is
     * non-NULL, it can be used for dereferencing pointers.  Return
     * the StringBuffer as a String.
     */
    final String toPrint(Location location, ByteBuffer memory, Format format) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	toPrint(writer, location, memory, format, 0);
	return stringWriter.toString();
    }

    /**
     * Print this Type after indenting INDENT spaces.
     * @param stringBuilder TODO
     */
    public abstract void toPrint(StringBuilder stringBuilder, int indent);

    /**
     * Print this Type, possibly briefly, after indenting INDENT spaces.
     * @param stringBuilder TODO
     */
    public void toPrintBrief(StringBuilder stringBuilder, int indent) {
	toPrint(stringBuilder, indent);
    }

    /**
     * Print this Type to a StringBuffer and return the String.
     */
    public final String toPrint() {
	StringBuilder stringBuilder = new StringBuilder();
	toPrint(stringBuilder, 0);
	return stringBuilder.toString();
    }

    /* getALUs are double dispatch functions to determine the 
     * ArithmeticUnit for an arithmetic or logical operation
     * between two types.
     */  
    public ArithmeticUnit getALU(Type type, int wordSize) {
	throw new RuntimeException("Invalid Arithmetic Unit");
    }
    public ArithmeticUnit getALU(IntegerType type, int wordSize) {
	throw new RuntimeException("Invalid Arithmetic Unit");
    }
    public ArithmeticUnit getALU(FloatingPointType type, int wordSize){
	throw new RuntimeException("Invalid Arithmetic Unit");
    }
    public ArithmeticUnit getALU(PointerType type, int wordSize) {
	throw new RuntimeException("Invalid Arithmetic Unit");
    }    
    public ArithmeticUnit getALU(ArrayType type, int wordSize) {
	throw new RuntimeException("Invalid Arithmetic Unit");
    }  
    public ArithmeticUnit getALU(int wordSize) {
	throw new RuntimeException("Invalid Arithmetic Unit");
    }      
    
    /**
     * Evaluates the address of a variable.
     */
    public Value addressOf(Value var1, ByteOrder order, int wordSize) {
    	PointerType pType = new PointerType("AddressPtr", order, wordSize, this);
    	return pType.createValue(var1.getLocation().getAddress());
    }
    /**
     * Implements dereference operation for a pointer type.
     */    
    public Value dereference(Value var1, ByteBuffer taskMem) {
    	throw new InvalidOperatorException(this, "*");
    }
    /**
     * Implements dot operation on a composite type.
     */
    public Value member(Value var1, String member) {
    	throw new InvalidOperatorException(this, ".");
    } 
    /**
     * Implements subscript operation for a pointer or array type.
     */
    public Value index(Value var1, Value var2, ByteBuffer taskMem) {
	// In C, var1[var2] = var2[var1]
	if (var2.getType() instanceof ArrayType || 
	    var2.getType() instanceof PointerType)
	    return var2.getType().index (var2, var1, taskMem);
    	throw new InvalidOperatorException(this, "[]");
    }  
    
    /**
     * Implements slice operation for a pointer or array type - slice
     * the array from index I to index J.
     */
    public Value slice(Value var, Value i, Value j, ByteBuffer taskMem) {
    	throw new InvalidOperatorException(this, "slice");	
    }
    
    /**
     * Get the type of slice.
     */
    public Type getSliceType() {
	throw new InvalidOperatorException(this, "sliceType");
    }
    
    /**
     * Return the element type for array or pointer type.
     */
    public Type getType () {
	throw new InvalidOperatorException(this, "getElementType");
    }
    
   /**
     * Assign VALUE to LOCATION; possibly performing type-conversion.
     */
    void assign(Location location, Value value) {
	throw new InvalidOperatorException(this, "");
    }

    /**
     * Pack this TYPE into bigSize and bitOffset.
     */
    public Type pack(int bitSize, int bitOffset) {
	throw new InvalidOperatorException(this, "pack");
    }

    /**
     * Complete the type's member; return false if nothing completed.
     */
    public boolean completeMember(String incomplete, List candidates) {
	return false;
    }

    /**
     * Complete the type's next token; return false if nothing
     * completed.
     */
    public boolean completeFollowSym(String incomplete, List candidates) {
	return false;
    }
}
