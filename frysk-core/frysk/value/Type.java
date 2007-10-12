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

import frysk.debuginfo.PieceLocation;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

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
  
    // XXX: Not needed; made redundant by TypeDef.
    private boolean isTypedef;

    Type (String name, int size) {
	this.name = name;
	this.size = size;
	this.isTypedef = false;
    }

    public int getSize() {
	return size;
    }

    String getName() {
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
     * can be used for dereferencing pointers.
     */
    abstract void toPrint(PrintWriter writer, Location location,
			  ByteBuffer memory, Format format);
    /*
     * Print Location as Type in user-readable form to a StringBuffer;
     * use Format to print basic types.  If needed, and when memory is
     * non-NULL, it can be used for dereferencing pointers.  Return
     * the StringBuffer as a String.
     */
    final String toPrint(Location location, ByteBuffer memory, Format format) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	toPrint(writer, location, memory, format);
	return stringWriter.toString();
    }

    /**
     * Print this Type.
     */
    public abstract void toPrint(PrintWriter writer);
    /**
     * Print this Type to a StringBuffer and return the String.
     */
    public final String toPrint() {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	toPrint(writer);
	return stringWriter.toString();
    }

    public Value add (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "+");
    }
    public Value subtract (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "-");
    }
    public Value multiply (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "*");
    }
    public Value divide (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "/");
    }
    public Value mod (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "%");
    }
    public Value shiftLeft (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "<<");
    }
    public Value shiftRight (Value var1, Value var2) {
        throw new InvalidOperatorException(this, ">>");
    }
    public Value lessThan (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "<");
    }
    public Value greaterThan (Value var1, Value var2) {
        throw new InvalidOperatorException(this, ">");
    }
    public Value lessThanOrEqualTo (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "<=");
    }
    public Value greaterThanOrEqualTo (Value var1, Value var2) {
        throw new InvalidOperatorException(this, ">=");
    }
    public Value equal (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "==");
    }
    public Value notEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "!=");
    }
    public Value bitWiseAnd (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "&");
    }
    public Value bitWiseXor (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "^");
    }
    public Value bitWiseOr (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "|");
    }
    public Value bitWiseComplement (Value var1) {
        throw new InvalidOperatorException(this, "~");
    }
    public Value logicalAnd (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "&&");
    }
    public Value logicalOr (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "||");
    }
    public Value logicalNegation(Value var1) {
        throw new InvalidOperatorException(this, "!");
    } 
    public Value timesEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "*=");
    }
    public Value divideEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "/=");
    }
    public Value modEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "%=");
    }
    public Value plusEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "+=");
    }
    public Value minusEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "-=");
    }
    public Value shiftLeftEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "<<=");
    }
    public Value shiftRightEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, ">>=");
    }
    public Value bitWiseOrEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "|=");
    }
    public Value bitWiseXorEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "^=");
    }
    public Value bitWiseAndEqual (Value var1, Value var2) {
        throw new InvalidOperatorException(this, "&=");
    }
    public boolean getLogicalValue (Value var) {
        throw new InvalidOperatorException(this, "");
    }
    public Value addressOf(Value var1, ByteOrder order) {
	PointerType pType = new PointerType("AddressPtr", order, this.getSize(), this);
	return pType.createValue(var1.getLocation().getAddress());
    }
    public Value dereference(Value var1, ByteBuffer taskMem) {
	Location loc = PieceLocation.createSimpleLoc
		       (var1.asLong(), (long)this.getSize(), taskMem);
	Type type = ((PointerType)var1.getType()).getType();
	return new Value (type, loc);  
    }
    public Value member(Value var1, String member) {
	throw new InvalidOperatorException(this, ".");
    }
   /**
     * Assign VALUE to LOCATION; possibly performing type-conversion.
     */
    void assign(Location location, Value value) {
	throw new InvalidOperatorException(this, "");
    }

    public boolean isTypedef()
    {
	return isTypedef;
    }

    /**
     * FIXME: Instead of setting the typedef, a TypeDef type can be
     * created.
     */
    public void setTypedefFIXME(boolean isTypedef)
    {
	this.isTypedef = isTypedef;
    }

    /**
     * Pack this TYPE into bigSize and bitOffset.
     */
    public Type pack(int bitSize, int bitOffset) {
	throw new InvalidOperatorException(this, "pack");
    }
}
