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
    protected ArithmeticType (int size, ByteOrder endian,
			      int typeId, String typeStr)
    {
	super(size, endian, typeId, typeStr);
    }
  
    protected ArithmeticType (int size, ByteOrder endian, 
			      int typeId, String typeStr, boolean haveTypeDef)
    {
	super(size, endian, typeId, typeStr, haveTypeDef);
    }
  
    public String toString() {
	return ("{"
		+ super.toString()
		+ ",endian=" + endian
		+ "}");
    }

  public Value add (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() + var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	     || var2.getType() instanceof FloatingPointType)
	return ((ArithmeticType)type).createValue(var1.doubleValue() + var2.doubleValue());
    return null;
  }

  public Value subtract (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() - var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue(var1.doubleValue() - var2.doubleValue());
    return null;
  }

  public Value multiply (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() * var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue(var1.doubleValue() * var2.doubleValue());
    return null;
  }

  public Value divide (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() / var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue(var1.doubleValue() / var2.doubleValue());
    return null;
  }

  public Value mod (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() % var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue(var1.doubleValue() % var2.doubleValue());
    return null;
  }

  public Value shiftLeft(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
	return ((ArithmeticType)type).createValue(var1.asLong() << var2.asLong());
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "<<");
    return null;
  } 

  public Value shiftRight(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
	return ((ArithmeticType)type).createValue(var1.asLong() >> var2.asLong());
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), ">>");
    return null;
  } 
  
  public Value lessThan (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() < var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() < var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value greaterThan (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() > var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() > var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Value lessThanOrEqualTo (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() <= var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() <= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value greaterThanOrEqualTo (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() >= var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() >= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value equal (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() == var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() == var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Value notEqual (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() != var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return ((ArithmeticType)type).createValue((var1.doubleValue() != var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value bitWiseAnd(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue(var1.asLong() & var2.asLong());
      else if (var1.getType() instanceof FloatingPointType)
	  throw new InvalidOperatorException(var1.getType(), "&");
    return null;
  }

  public Value bitWiseOr(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() | var2.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "|");
    return null;
  }
  
  public Value bitWiseXor(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() ^ var2.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "^");
    return null;
  }

  public Value bitWiseComplement(Value var1)
  {
    Type type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((~var1.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "~");
    return null;
  }

  public Value logicalAnd(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();

    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() == 0 ? false : true)
                              && (var2.asLong() == 0 ? false : true) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "&&");
    return null;
  }
  
  public Value logicalOr(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().getTypeIdFIXME() < var2.getType().getTypeIdFIXME())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() == 0 ? false : true)
                              || (var2.asLong() == 0 ? false : true) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "||");
    return null;
  }

  public Value logicalNegation(Value var1)
  {
    Type type = var1.getType();
    if (type instanceof IntegerType)
      return ((ArithmeticType)type).createValue((var1.asLong() == 0 ? true : false) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "||");
    return null;
  }

    public Value assign(Value var1, Value var2) {
	return var1.assign(var2);
    }

    public Value plusEqual(Value var1, Value var2) {
	return var1.assign(add(var1, var2));
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
    if (var1.getType() instanceof FloatingPointType)
	throw (new InvalidOperatorException(this, ""));

    return ((var1.asLong() == 0) ? false : true);
  }

    public void toPrint(PrintWriter writer) {
	writer.print(name);
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
     * FIXME: Code should be directly constructing the Value using
     * Type and Location; that change is waiting on the implementation
     * of a DWARF location-expression parser that returns Locations
     * and not values .  FIXME: Code should not be trying to give a
     * Value a name; that is a legacy from when Value and Variable
     * were the same class.
     */
    public Value createValueFIXME(String name, float val) {
	return createValueFIXME(name, Float.floatToRawIntBits(val));
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
     *
     * FIXME: Code should be directly constructing the Value using
     * Type and Location; that change is waiting on the implementation
     * of a DWARF location-expression parser that returns Locations
     * and not values .  FIXME: Code should not be trying to give a
     * Value a name; that is a legacy from when Value and Variable
     * were the same class.
     */
    public Value createValueFIXME(String name, double val) {
	return createValueFIXME(name, Double.doubleToRawLongBits(val));
    }

    /**
     * Create a new Value of THIS Type, initialized to VAL.
     */
    Value createValue(BigInteger val) {
	Location l = new Location(new byte[getSize()]);
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
     * Create a new Value of THIS Type, initialized to VAL.
     *
     * FIXME: Code should be directly constructing the Value using
     * Type and Location; that change is waiting on the implementation
     * of a DWARF location-expression parser that returns Locations
     * and not values .  FIXME: Code should not be trying to give a
     * Value a name; that is a legacy from when Value and Variable
     * were the same class.
     */
    public Value createValueFIXME(String nameFIXME, long val) {
	Location l = new Location(new byte[getSize()]);
	putBigInteger(l, BigInteger.valueOf(val));
	return new Value(this, nameFIXME, l);
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
