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

import lib.dw.BaseTypes;
import inua.eio.ByteOrder;

/**
 * Type for a arithmetic.
 */
public class ArithmeticType
    extends Type
{
  public ArithmeticType (int size, ByteOrder endian,
                         int typeId, String typeStr)
  {
    super(size, endian, typeId, typeStr);
  }
  
  public Variable add (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() + var2.intValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() + var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, var1.doubleValue() + var2.doubleValue());
    return null;
  }

  public Variable subtract (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() - var2.intValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() - var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, var1.doubleValue() - var2.doubleValue());
    return null;
  }

  public Variable multiply (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() * var2.intValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() * var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, var1.doubleValue() * var2.doubleValue());
    return null;
  }

  public Variable divide (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() / var2.intValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() / var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, var1.doubleValue() / var2.doubleValue());
    return null;
  }

  public Variable mod (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() % var2.intValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() % var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, var1.doubleValue() % var2.doubleValue());
    return null;
  }

  public Variable shiftLeft(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() << var2.longValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() << var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator << not defined for type "
					 + var1.getType().getName());
    return null;
  } 

  public Variable shiftRight(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() >> var2.longValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() >> var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator >> not defined for type "
					 + var1.getType().getName());
    return null;
  } 
  
  public Variable lessThan (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() < var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() < var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() < var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Variable greaterThan (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() > var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() > var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() > var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Variable lessThanOrEqualTo (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() <= var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() <= var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() <= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Variable greaterThanOrEqualTo (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() >= var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() >= var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() >= var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Variable equal (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() == var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() == var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() == var2.doubleValue()) ? 1 : 0);
    return null;
  }

  public Variable notEqual (Variable var1, Variable var2)
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() != var2.intValue()) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() != var2.longValue()) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId())
	|| BaseTypes.isFloat(var2.getType().getTypeId()))
      return type.newVariable(type, (var1.doubleValue() != var2.doubleValue()) ? 1 : 0);
    return null;
  }
  
  public Variable bitWiseAnd(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, var1.intValue() & var2.longValue());
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, var1.longValue() & var2.longValue());
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator & not defined for type "
					 + var1.getType().getName());
    return null;
  }

  public Variable bitWiseOr(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() | var2.intValue()));
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() | var2.longValue()));
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator | not defined for type "
					 + var1.getType().getName());
    return null;
  }
  
  public Variable bitWiseXor(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() ^ var2.intValue()));
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() ^ var2.longValue()));
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator ^ not defined for type "
					 + var1.getType().getName());
    return null;
  }

  public Variable bitWiseComplement(Variable var1) throws InvalidOperatorException 
  {
    Type type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (~var1.intValue()));
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (~var1.longValue()));
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "unary operator ~ not defined for type "
					 + var1.getType().getName());
    return null;
  }

  public Variable logicalAnd(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();

    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() == 0 ? false : true)
                              && (var2.intValue() == 0 ? false : true) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() == 0 ? false : true)
                              && (var2.longValue() == 0 ? false : true) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator && not defined for type "
					 + var1.getType().getName());
    return null;
  }
  
  public Variable logicalOr(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    Type type;
    if (var1.getType().getTypeId() < var2.getType().getTypeId())
      type = var2.getType();
    else
      type = var1.getType();
    
    if (BaseTypes.isInteger(type.getTypeId()))
      return type.newVariable(type, (var1.intValue() == 0 ? false : true)
                              || (var2.intValue() == 0 ? false : true) ? 1 : 0);
    else if (BaseTypes.isLong(type.getTypeId()))
      return type.newVariable(type, (var1.longValue() == 0 ? false : true)
                              || (var2.longValue() == 0 ? false : true) ? 1 : 0);
    else if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw new InvalidOperatorException(
                                         "binary operator || not defined for type "
					 + var1.getType().getName());
    return null;
  }

  public Variable assign(Variable var1, Variable var2) 
  {
    return doAssignment(var1, var2);
  }

  public Variable plusEqual(Variable var1, Variable var2)
  {
    return doAssignment(var1, add(var1, var2));
  }

  public Variable minusEqual(Variable var1, Variable var2)
  {
    return doAssignment(var1, subtract(var1, var2));
  }

  public Variable timesEqual(Variable var1, Variable var2) 
  {
    return doAssignment(var1, multiply(var1, var2));
  }

  public Variable divideEqual(Variable var1, Variable var2)
  {
    return doAssignment(var1, divide(var1, var2));
  }

  public Variable modEqual(Variable var1, Variable var2)
  {
    return doAssignment(var1, mod(var1, var2));
  }

  public Variable shiftLeftEqual(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    return doAssignment(var1, shiftLeft(var1, var2));
  } 

  public Variable shiftRightEqual(Variable var1, Variable var2) throws InvalidOperatorException 
  {
    return doAssignment(var1, shiftRight(var1, var2));
  } 

  public Variable bitWiseOrEqual(Variable var1, Variable var2) throws InvalidOperatorException
  {
    return doAssignment(var1, bitWiseOr(var1, var2));
  }

  public Variable bitWiseXorEqual(Variable var1, Variable var2) throws InvalidOperatorException
  {
    return doAssignment(var1, bitWiseXor(var1, var2));
  }

  public Variable bitWiseAndEqual(Variable var1, Variable var2) throws InvalidOperatorException
  {
    return doAssignment(var1, bitWiseAnd(var1, var2));
  }

  public Variable doAssignment(Variable v1, Variable v2)
  {
    switch (v1.getType().getTypeId())
    {
    case BaseTypes.baseTypeByte:
      v1.putByte((byte)v2.intValue());
      break;
    case BaseTypes.baseTypeShort:
      v1.putShort((short)v2.intValue());
      break;
    case BaseTypes.baseTypeInteger:
      v1.putInt(v2.intValue());
      break;
    case BaseTypes.baseTypeLong:
      v1.putLong(v2.longValue());
      break;
    case BaseTypes.baseTypeFloat:
      v1.putFloat((float)v2.doubleValue());
      break;
    case BaseTypes.baseTypeDouble:
      v1.putDouble(v2.doubleValue());
    }
    return v1;
  }

  public boolean getLogicalValue (Variable var1)
  throws InvalidOperatorException
  {
    if (BaseTypes.isFloat(var1.getType().getTypeId()))
      throw (new InvalidOperatorException());

    return ((var1.longValue() == 0) ? false : true);
  }

    public static Variable newByteVariable (ArithmeticType type, String text,
                                            byte val)
   {
     Variable returnVar = new Variable(type, text);
     returnVar.getLocation().putByte(val);
     return returnVar;
   }
    
    public static Variable newShortVariable (ArithmeticType type, String text,
                                             short val)
    {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putShort(val);
      return returnVar;
    }
    
    public static Variable newIntegerVariable (ArithmeticType type, String text,
                                               int val)
    {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putInt(val);
      return returnVar;
    }
    
    public static Variable newLongVariable (ArithmeticType type, String text,
                                            long val)
    {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putLong(val);
      return returnVar;
    }

    public static Variable newFloatVariable (ArithmeticType type, String text,
                                             float val)
    {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putFloat(val);
      return returnVar;
    }
    
    public static Variable newDoubleVariable (ArithmeticType type, String text,
                                              double val)
    {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putDouble(val);
      return returnVar;
    }

    public String toString (Variable v)
    {
      switch (typeId)
      {
	case BaseTypes.baseTypeByte:
	  return String.valueOf(v.getByte());
	case BaseTypes.baseTypeShort:
       	  return String.valueOf(v.getShort());
	case BaseTypes.baseTypeInteger:
	  return String.valueOf(v.getInt());
	case BaseTypes.baseTypeLong:
	  return String.valueOf(v.getLong());
	case BaseTypes.baseTypeFloat:
	  return String.valueOf(v.getFloat());
	case BaseTypes.baseTypeDouble:
	  return String.valueOf(v.getDouble());
	default:
	  return "";
      }
    }
}


