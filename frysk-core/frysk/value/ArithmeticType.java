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

import inua.eio.ByteOrder;
import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * Type for a arithmetic.
 */
public abstract class ArithmeticType
    extends Type
{    
    private final ByteOrder order;
  
    protected ArithmeticType(String name, ByteOrder order, int size) {
	super(name, size);
	this.order = order;
    }
  
    public String toString() {
	return ("{"
		+ super.toString()
		+ "}");
    }

    public ByteOrder order() {
	return order;
    }
   
    public Value assign(Value var1, Value var2) {
	return var1.assign(var2);
    }

    public void toPrint(PrintWriter writer, int indent) {
	writer.print(getName());
    }

    /**
     * Create a new Value of THIS Type, initialized to VAL.
     *
     * This is a convenience method for creating a simple arithmetic
     * type from a constant.  In general code should be creating a
     * Value using a Type and a Location.
     */
    public Value createValue(float val) {
	return createValue(Float.floatToRawIntBits(val));
    }
    /**
     * Create a new Value of THIS Type, initialized to VAL.
     *
     * This is a convenience method for creating a simple arithmetic
     * type from a constant.  In general code should be creating a
     * Value using a Type and a Location.
     */
    public Value createValue(double val) {
	return createValue(Double.doubleToRawLongBits(val));
    }

    /**
     * Create a new Value of THIS Type, initialized to VAL.
     */
    Value createValue(BigInteger val) {
	Location l = new ScratchLocation(getSize());
	putBigInteger(l, val);
	return new Value(this, l);
    }
    /**
     * Create a new Value of THIS type, initialized to the long VAL.
     *
     * This is a convenience method for creating a simple arithmetic
     * type from a constant.  In general code should be creating a
     * Value using a Type and a Location.
     */ 
    public Value createValue(long val) {
	return createValue(BigInteger.valueOf(val));
    }

    /**
     * Return the entire location, interpreting it as a big integer.
     * This does not do type-conversion.  The underlying type
     * determines if the the value is signed or unsigned.
     */
    abstract BigInteger getBigInteger(Location location);
    /**
     * Return the entire location, interpreting the raw bytes as a
     * floating-point value.  This does not do type-conversion.
     */
    abstract BigFloat getBigFloat(Location location);

    /**
     * Re-write the entire location with the big integer value.  This
     * does not do type conversion.  The underlying type determines if
     * the value should be zero or sign extended.
     */
    abstract void putBigInteger(Location location, BigInteger val);

    /**
     * Return the arthmetic type converted to a BigInteger, this may
     * involve truncation and/or rounding.
     */
    abstract BigInteger bigIntegerValue(Location location);
    /**
     * Return the arthmetic type converted to a BigFloat, this may
     * involve truncation and/or rounding.
     */
    abstract BigFloat bigFloatValue(Location location);
}
