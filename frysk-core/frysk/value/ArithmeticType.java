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

import lib.dwfl.BaseTypes;
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
  
  public Value add (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() + var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	     || var2.getType() instanceof FloatingPointType)
      return newValue(type, var1.doubleValue() + var2.doubleValue());
    return null;
  }

  public Value subtract (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() - var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, var1.doubleValue() - var2.doubleValue());
    return null;
  }

  public Value multiply (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() * var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, var1.doubleValue() * var2.doubleValue());
    return null;
  }

  public Value divide (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() / var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, var1.doubleValue() / var2.doubleValue());
    return null;
  }

  public Value mod (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() % var2.asLong());
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, var1.doubleValue() % var2.doubleValue());
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
	return newValue(type, var1.asLong() << var2.asLong());
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
	return newValue(type, var1.asLong() >> var2.asLong());
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), ">>");
    return null;
  } 
  
  public Value lessThan (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() < var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() < var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value greaterThan (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() > var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() > var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Value lessThanOrEqualTo (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() <= var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() <= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value greaterThanOrEqualTo (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() >= var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() >= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value equal (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() == var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() == var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Value notEqual (Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() != var2.asLong()) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType
	|| var2.getType() instanceof FloatingPointType)
      return newValue(type, (var1.doubleValue() != var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Value bitWiseAnd(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, var1.asLong() & var2.asLong());
      else if (var1.getType() instanceof FloatingPointType)
	  throw new InvalidOperatorException(var1.getType(), "&");
    return null;
  }

  public Value bitWiseOr(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() | var2.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "|");
    return null;
  }
  
  public Value bitWiseXor(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() ^ var2.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "^");
    return null;
  }

  public Value bitWiseComplement(Value var1)
  {
    Type type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (~var1.asLong()));
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "~");
    return null;
  }

  public Value logicalAnd(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();

    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() == 0 ? false : true)
                              && (var2.asLong() == 0 ? false : true) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "&&");
    return null;
  }
  
  public Value logicalOr(Value var1, Value var2)
  {
    Type type;
    if (var1.getType().typeId < var2.getType().typeId)
      type = var2.getType();
    else
      type = var1.getType();
    
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() == 0 ? false : true)
                              || (var2.asLong() == 0 ? false : true) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "||");
    return null;
  }

  public Value logicalNegation(Value var1)
  {
    Type type = var1.getType();
    if (type instanceof IntegerType)
      return newValue(type, (var1.asLong() == 0 ? true : false) ? 1 : 0);
    else if (var1.getType() instanceof FloatingPointType)
	throw new InvalidOperatorException(var1.getType(), "||");
    return null;
  }

  public Value assign(Value var1, Value var2) 
  {
    return doAssignment(var1, var2);
  }

  public Value plusEqual(Value var1, Value var2)
  {
    return doAssignment(var1, add(var1, var2));
  }

  public Value minusEqual(Value var1, Value var2)
  {
    return doAssignment(var1, subtract(var1, var2));
  }

  public Value timesEqual(Value var1, Value var2) 
  {
    return doAssignment(var1, multiply(var1, var2));
  }

  public Value divideEqual(Value var1, Value var2)
  {
    return doAssignment(var1, divide(var1, var2));
  }

  public Value modEqual(Value var1, Value var2)
  {
    return doAssignment(var1, mod(var1, var2));
  }

  public Value shiftLeftEqual(Value var1, Value var2)
  {
    return doAssignment(var1, shiftLeft(var1, var2));
  } 

  public Value shiftRightEqual(Value var1, Value var2)
  {
    return doAssignment(var1, shiftRight(var1, var2));
  } 

  public Value bitWiseOrEqual(Value var1, Value var2)
  {
    return doAssignment(var1, bitWiseOr(var1, var2));
  }

  public Value bitWiseXorEqual(Value var1, Value var2)
  {
    return doAssignment(var1, bitWiseXor(var1, var2));
  }

  public Value bitWiseAndEqual(Value var1, Value var2)
  {
    return doAssignment(var1, bitWiseAnd(var1, var2));
  }

  public Value doAssignment(Value v1, Value v2)
  {
    switch (v1.getType().typeId)
    {
    case BaseTypes.baseTypeByte:
      v1.putByte((byte)v2.asLong());
      break;
    case BaseTypes.baseTypeShort:
      v1.putShort((short)v2.asLong());
      break;
    case BaseTypes.baseTypeInteger:
	v1.putInt((int)v2.asLong());
      break;
    case BaseTypes.baseTypeLong:
      v1.putLong(v2.asLong());
      break;
    case BaseTypes.baseTypeFloat:
      v1.putFloat((float)v2.doubleValue());
      break;
    case BaseTypes.baseTypeDouble:
      v1.putDouble(v2.doubleValue());
    }
    return v1;
  }

  public boolean getLogicalValue (Value var1) {
    if (var1.getType() instanceof FloatingPointType)
	throw (new InvalidOperatorException(this, ""));

    return ((var1.asLong() == 0) ? false : true);
  }

  public Value newValue(Type type, int val)
    {
      switch (type.typeId)
      {
	case BaseTypes.baseTypeByte:
	  return ArithmeticType.newByteValue((ArithmeticType)type, (byte)val);
	case BaseTypes.baseTypeShort:
       	  return ArithmeticType.newShortValue((ArithmeticType)type, (short)val);
	case BaseTypes.baseTypeInteger:
	  return ArithmeticType.newIntegerValue((ArithmeticType)type, val);
	case BaseTypes.baseTypeLong:
	  return ArithmeticType.newLongValue((ArithmeticType)type, (long)val);
	case BaseTypes.baseTypeFloat:
	  return ArithmeticType.newFloatValue((ArithmeticType)type, (float)val);
	case BaseTypes.baseTypeDouble:
	  return ArithmeticType.newDoubleValue((ArithmeticType)type, (double)val);
      }
      return null;
    }

  public Value newValue (Type type, long val)
  {
    if (type.typeId < BaseTypes.baseTypeLong)
      return this.newValue(type, (int) val);
    switch (type.typeId)
      {
      case BaseTypes.baseTypeLong:
	return ArithmeticType.newLongValue((ArithmeticType) type, val);
      case BaseTypes.baseTypeFloat:
	return ArithmeticType.newFloatValue((ArithmeticType) type, (float) val);
      case BaseTypes.baseTypeDouble:
	return ArithmeticType.newDoubleValue((ArithmeticType) type, (double) val);
      }
    return null;
  }

  public Value newValue (Type type, double val)
  {
    switch (type.typeId)
      {
      case BaseTypes.baseTypeFloat:
	return ArithmeticType.newFloatValue((ArithmeticType) type, (float) val);
      case BaseTypes.baseTypeDouble:
	return ArithmeticType.newDoubleValue((ArithmeticType) type, val);
      }
    return null;
  }
  
  public static Value newByteValue (ArithmeticType type, byte val)
  {
    return newByteValue (type, "temp", val);
  }

  public static Value newByteValue (ArithmeticType type, String text, 
                                          byte val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putByte(val);
    return returnVar;
  }

  public static Value newShortValue (ArithmeticType type, short val)
  {
    return newShortValue (type, "temp", val);
  }

  public static Value newShortValue (ArithmeticType type, String text, 
                                           short val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putShort(val);
    return returnVar;
  }

  public static Value newIntegerValue (ArithmeticType type, int val)
  {
    return newIntegerValue (type, "temp", val);
  }

  public static Value newIntegerValue (ArithmeticType type, String text, 
                                             int val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putInt(val);
    return returnVar;
  }

  public static Value newLongValue (ArithmeticType type, long val)
  {
    return newLongValue (type, "temp", val);
  }

  public static Value newLongValue (ArithmeticType type, String text, 
                                          long val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putLong(val);
    return returnVar;
  }

  public static Value newFloatValue (ArithmeticType type, float val)
  {
    return newFloatValue (type, "temp", val);
  }

  public static Value newFloatValue (ArithmeticType type, String text, 
                                           float val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putFloat(val);
    return returnVar;
  }

  public static Value newDoubleValue (ArithmeticType type, double val)
  {
    return newDoubleValue (type, "temp", val);
  }

  public static Value newDoubleValue (ArithmeticType type, String text, 
                                            double val)
  {
    Value returnVar = new Value(type, text);
    returnVar.getLocation().putDouble(val);
    return returnVar;
  }

    public void toPrint(PrintWriter writer) {
	writer.print(name);
    }

    /**
     * Return the entire location, interpreting it as a big integer.
     * This does not do type-conversion.  The underlying type
     * determines if the the value is signed or unsigned.
     */
    abstract BigInteger getBigInteger(Location location);

    /**
     * Re-write the entire location with the big integer value.  This
     * does not do type conversion.  The underlying type determines if
     * the value should be zero or sign extended.
     */
    abstract void putBigInteger(Location location, BigInteger val);
}
