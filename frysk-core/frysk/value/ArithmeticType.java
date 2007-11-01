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

    /**
     * Examine the underlying types to determine the result's Type;
     * callers then use the result type to determine the type system
     * to use, and conversion to perform, when applying the arithmetic
     * operation.
     *
     * XXX: Is ther a better to handle this whole arithmetic thing;
     * for instance Operator classes responsible for performing each
     * operation?  That would let us construct an expression tree and
     * evaluate that using traversal.
     */
    private ArithmeticType returnType(Value var1, Value var2) {
	Type t1 = var1.getType().getUltimateType();
	Type t2 = var2.getType().getUltimateType();

	// Floating point trumps everything else.
	if (t1 instanceof FloatingPointType) {
	    if (t2 instanceof FloatingPointType) {
		if (t1.getSize() > t2.getSize())
		    return (ArithmeticType)t1;
		else
		    return (ArithmeticType)t2;
	    } else if (t2 instanceof IntegerType) {
		return (ArithmeticType)t1;
	    } else {
		// Can't apply cast: (t1)t2
		throw new InvalidOperatorException(t2,
						   "(" + t1.toPrint() + ")");
	    }
	} else if (t1 instanceof IntegerType) {
	    if (t2 instanceof FloatingPointType) {
		return (ArithmeticType)t2;
	    } else if (t2 instanceof IntegerType) {
		if (t1.getSize() > t2.getSize())
		    return (ArithmeticType)t1;
		else
		    return (ArithmeticType)t2;
	    } else {
		// Can't apply cast: (t1)t2
		throw new InvalidOperatorException(t2,
						   "(" + t1.toPrint() + ")");
	    }
	} else {
	    if (t2 instanceof ArithmeticType) {
		// Can't apply cast: (t2)t1
		throw new InvalidOperatorException(t1,
						   "(" + t2.toPrint() + ")");
	    } else {
		throw new InvalidOperatorException(t1, "invalid type");
	    }
	}
    }

    public Value add(Value v1, Value v2) {
	return v1.getType().getALU(v2.getType()).add(v1, v2);	    
    }
    
    public Value plusEqual(Value var1, Value var2) {
	return var1.assign(add(var1, var2));
    }
    
    public Value subtract(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() - var2.asLong());
	else if (type instanceof FloatingPointType)
	    return type.createValue(var1.doubleValue() - var2.doubleValue());
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value multiply(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() * var2.asLong());
	else if (type instanceof FloatingPointType)
	    return type.createValue(var1.doubleValue() * var2.doubleValue());
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value divide(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() / var2.asLong());
	else if (type instanceof FloatingPointType)
	    return type.createValue(var1.doubleValue() / var2.doubleValue());
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value mod(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() % var2.asLong());
	else if (type instanceof FloatingPointType)
	    return type.createValue(var1.doubleValue() % var2.doubleValue());
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value shiftLeft(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() << var2.asLong());
	else
	    throw new InvalidOperatorException(type, "<<");
    } 

    public Value shiftRight(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() >> var2.asLong());
	else
	    throw new InvalidOperatorException(type, ">>");
    } 
  
    public Value lessThan(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() < var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() < var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }
  
    public Value greaterThan(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() > var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() > var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value lessThanOrEqualTo(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() <= var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() <= var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }
  
    public Value greaterThanOrEqualTo(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() >= var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() >= var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }
  
    public Value equal(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() == var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() == var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }

    public Value notEqual(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() != var2.asLong()) ? 1 : 0);
	else if (type instanceof FloatingPointType)
	    return type.createValue((var1.doubleValue() != var2.doubleValue()) ? 1 : 0);
	else
	    throw new RuntimeException("type conversion botch");
    }
  
    public Value bitWiseAnd(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue(var1.asLong() & var2.asLong());
	else
	    throw new InvalidOperatorException(type, "&");
    }

    public Value bitWiseOr(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() | var2.asLong()));
	else
	    throw new InvalidOperatorException(type, "|");
    }
  
    public Value bitWiseXor(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() ^ var2.asLong()));
	else
	    throw new InvalidOperatorException(type, "^");
    }

    public Value bitWiseComplement(Value var1) {
	Type type = var1.getType().getUltimateType();
	if (type instanceof IntegerType)
	    return ((ArithmeticType)type).createValue((~var1.asLong()));
	else
	    throw new InvalidOperatorException(type, "~");
    }

    public Value logicalAnd(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() == 0 ? false : true)
						      && (var2.asLong() == 0 ? false : true) ? 1 : 0);
	else
	    throw new InvalidOperatorException(type, "&&");
    }
  
    public Value logicalOr(Value var1, Value var2) {
	ArithmeticType type = returnType(var1, var2);
	if (type instanceof IntegerType)
	    return type.createValue((var1.asLong() == 0 ? false : true)
						      || (var2.asLong() == 0 ? false : true) ? 1 : 0);
	else
	    throw new InvalidOperatorException(var1.getType(), "||");
    }

    public Value logicalNegation(Value var1) {
	Type type = var1.getType().getUltimateType();
	if (type instanceof IntegerType)
	    return ((ArithmeticType)type).createValue((var1.asLong() == 0 ? true : false) ? 1 : 0);
	else
	    throw new InvalidOperatorException(var1.getType(), "||");
    }

    public Value assign(Value var1, Value var2) {
	return var1.assign(var2);
    }

    public Value minusEqual(Value var1, Value var2) {
	return var1.assign(subtract(var1, var2));
    }

    public Value timesEqual(Value var1, Value var2) {
	return var1.assign(multiply(var1, var2));
    }

    public Value divideEqual(Value var1, Value var2) {
	return var1.assign(divide(var1, var2));
    }

    public Value modEqual(Value var1, Value var2) {
	return var1.assign(mod(var1, var2));
    }

    public Value shiftLeftEqual(Value var1, Value var2) {
	return var1.assign(shiftLeft(var1, var2));
    } 

    public Value shiftRightEqual(Value var1, Value var2) {
	return var1.assign(shiftRight(var1, var2));
    }

    public Value bitWiseOrEqual(Value var1, Value var2) {
	return var1.assign(bitWiseOr(var1, var2));
    }

    public Value bitWiseXorEqual(Value var1, Value var2) {
	return var1.assign(bitWiseXor(var1, var2));
    }

    public Value bitWiseAndEqual(Value var1, Value var2) {
	return var1.assign(bitWiseAnd(var1, var2));
    }

    public boolean getLogicalValue (Value var1) {
	Type type = var1.getType().getUltimateType();
	if (type instanceof IntegerType)
	    return ((var1.asLong() == 0) ? false : true);
	else
	    throw new InvalidOperatorException(this, "(bool)");
    }

    public void toPrint(PrintWriter writer) {
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
