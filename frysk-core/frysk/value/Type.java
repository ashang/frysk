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

import inua.eio.ByteOrder;
import lib.dw.BaseTypes;

/**
 * Holds the type of a Variable and also defines possible operations. Classes
 * extended from this type will have to define the individual operation that are
 * defined on those types. e.g. addition operation may be defined for the
 * integer type.
 */

public abstract class Type
{
  protected final int size;

  protected final ByteOrder endian;

  protected final int typeId;

  protected final String name;

  Type (int size, ByteOrder endian, int typeId)
  {
    this(size, endian, typeId, "");
  }

  Type (int size, ByteOrder endian, int typeId, String name)
  {
    this.size = size;
    this.endian = endian;
    this.typeId = typeId;
    this.name = name;
  }

  public int getSize ()
  {
    return size;
  }

  public ByteOrder getEndian ()
  {
    return endian;
  }

  public int getTypeId ()
  {
    return typeId;
  }

  public String getName ()
  {
    return name;
  }

  public String toString ()
  {
    return name;
  }

  public abstract String toString (Variable v);

  public long longValue (Variable v) throws InvalidOperatorException
  {
    switch (v.getType().getTypeId())
      {
      case BaseTypes.baseTypeByte:
	return v.getByte();
      case BaseTypes.baseTypeShort:
	return v.getShort();
      case BaseTypes.baseTypeInteger:
	return v.getInt();
      case BaseTypes.baseTypeLong:
	return v.getLong();
      }
    throw new InvalidOperatorException("binary operation not defined for type "
	                               + name + "," + v.getType().getName());
  }

  public abstract Variable add (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable subtract (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable multiply (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable divide (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable mod (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable shiftLeft (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable shiftRight (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable lessThan (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable greaterThan (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable lessThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable greaterThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable equal (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable notEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseAnd (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseXor (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseOr (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable logicalAnd (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable logicalOr (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable assign (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable timesEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable divideEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable modEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable plusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable minusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable shiftLeftEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable shiftRightEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseOrEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseXorEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract Variable bitWiseAndEqual (Variable var1, Variable var2)
      throws InvalidOperatorException;

  public abstract boolean getLogicalValue (Variable var) throws InvalidOperatorException;
  
  public Variable newVariable(Type type, int val)
    {
      switch (type.getTypeId())
      {
	case BaseTypes.baseTypeByte:
	  return ArithmeticType.newByteVariable((ArithmeticType)type, "byte", (byte)val);
	case BaseTypes.baseTypeShort:
       	  return ArithmeticType.newShortVariable((ArithmeticType)type, "short", (short)val);
	case BaseTypes.baseTypeInteger:
	  return ArithmeticType.newIntegerVariable((ArithmeticType)type, "int", val);
	case BaseTypes.baseTypeLong:
	  return ArithmeticType.newLongVariable((ArithmeticType)type, "long", (long)val);
	case BaseTypes.baseTypeFloat:
	  return ArithmeticType.newFloatVariable((ArithmeticType)type, "float", (float)val);
	case BaseTypes.baseTypeDouble:
	  return ArithmeticType.newDoubleVariable((ArithmeticType)type, "double", (double)val);
      }
      return null;
    }

  public Variable newVariable (Type type, long val)
  {
    if (type.getTypeId() < BaseTypes.baseTypeLong)
      return this.newVariable(type, (int) val);
    switch (type.getTypeId())
      {
      case BaseTypes.baseTypeLong:
	return ArithmeticType.newLongVariable((ArithmeticType) type, "long", val);
      case BaseTypes.baseTypeFloat:
	return ArithmeticType.newFloatVariable((ArithmeticType) type, "float", (float) val);
      case BaseTypes.baseTypeDouble:
	return ArithmeticType.newDoubleVariable((ArithmeticType) type, "double", (double) val);
      }
    return null;
  }

  public Variable newVariable (Type type, double val)
  {
    switch (type.getTypeId())
      {
      case BaseTypes.baseTypeFloat:
	return ArithmeticType.newFloatVariable((ArithmeticType) type, "float", (float) val);
      case BaseTypes.baseTypeDouble:
	return ArithmeticType.newDoubleVariable((ArithmeticType) type, "double", val);
      }
    return null;
  }
}
