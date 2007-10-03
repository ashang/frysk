// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

public class LongType
    extends Type
{

  public String toString (Variable v)
  {
    return String.valueOf(v.getLong());
  }

  public LongType (int size, ByteOrder endian)
  {
    super(size, endian, BaseTypes.baseTypeLong, "long");
  }

  public static Variable newLongVariable (LongType type, long val)
  {
    return newLongVariable(type, "temp", val);
  }

  public static Variable newLongVariable (LongType type, String text, long val)
  {
    Variable returnVar = new Variable(type, text);
    returnVar.getLocation().putLong(val);
    return returnVar;
  }

  public Variable newVariable (Type type, Variable val)
  {
    return val.getType().newLongVariable ((LongType) type, val);
  }

  public Variable newFloatVariable (FloatType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putFloat((float) val.getLong());
    return returnVar;
  }

  public Variable newDoubleVariable (DoubleType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putDouble((double) val.getLong());
    return returnVar;
  }

  public Variable newLongVariable (LongType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putLong((long) val.getLong());
    return returnVar;
  }

  public Variable newIntegerVariable (IntegerType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putInt((int) val.getLong());
    return returnVar;
  }

  public Variable newByteVariable (ByteType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putByte((byte) (val.getChar()));
    return returnVar;
  }

  public Variable newShortVariable (ShortType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putShort((short) val.getLong());
    return returnVar;
  }

  public Variable add (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (var1.getLocation().getLong() 
	  + newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (var1.getLocation().getLong() 
				       + var2.getLocation().getLong()));
  }

  public Variable subtract (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (var1.getLocation().getLong() 
	  - newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (var1.getLocation().getLong() 
				       - var2.getLocation().getLong()));
  }

  public Variable assign (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (newVariable(var1.getType(),
				var2).getLong())
		 : var2.getLong());
    return var1;
  }

  public Variable timesEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() * newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 * var2.getLong());
    return var1;
  }

  public Variable divideEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() / newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 / var2.getLong());
    return var1;
  }

  public Variable minusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() - newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 - var2.getLong());
    return var1;
  }

  public Variable plusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() + newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 + var2.getLong());
    return var1;
  }

  public Variable modEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() % newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 % var2.getLong());
    return var1;
  }

  public Variable shiftLeftEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong(var1.getLong() << longValue(var2));
    return var1;
  }

  public Variable shiftRightEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong(var1.getLong() >> longValue(var2));
    return var1;
  }

  public Variable bitWiseAndEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() & newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 & var2.getLong());
    return var1;
  }

  public Variable bitWiseOrEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() | newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 | var2.getLong());
    return var1;
  }

  public Variable bitWiseXorEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    var1.putLong((var2.getType().getTypeId() != BaseTypes.baseTypeLong) 
		 ? (var1.getLong() ^ newVariable(var1.getType(),
						 var2).getLong())
		 : var1.getLong()
		 ^ var2.getLong());
    return var1;
  }

  public Variable multiply (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().multiply(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) 
	 (var1.getType()),
	 (var1.getLocation().getLong() 
	  * newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (var1.getLocation().getLong() 
				       * var2.getLocation().getLong()));
  }

  public Variable divide (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().divide(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) 
	 (var1.getType()),
	 (var1.getLocation().getLong() 
	  / newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (var1.getLocation().getLong() 
				       / var2.getLocation().getLong()));
  }

  public Variable mod (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().mod(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) 
	 (var1.getType()),
	 (var1.getLocation().getLong() 
	  % newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (var1.getLocation().getLong() 
				       % var2.getLocation().getLong()));
  }

  public Variable shiftLeft (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    long resultVar = var1.getLong() << longValue(var2);
    return LongType.newLongVariable
      ((LongType) var1.getType(), resultVar);
  }

  public Variable shiftRight (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    long v2 = 0;

    if (var2.getType().getTypeId() == BaseTypes.baseTypeChar)
      v2 = 0;// var2.getChar();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeShort)
      v2 = var2.getShort();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeLong)
      v2 = var2.getLong();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeLong)
      v2 = var2.getLong();
    else
      throw new InvalidOperatorException(
                                         "binary operator >> not defined for type int, "
                                             + var2.getType().getName());

    long resultVar = var1.getLong() >> v2;
    return LongType.newLongVariable((LongType) var1.getType(), resultVar);
  }

  public Variable lessThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().lessThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (var1.getLocation().getLong() 
	  < newVariable(var1.getType(),
			var2).getLocation().getLong()) 
	 ? 1 : 0);
    else
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (var1.getLocation().getLong() < var2.getLocation().getLong()) ? 1 : 0);
  }

  public Variable greaterThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().greaterThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable

	((LongType) (var1.getType()),
	 (long) ((var1.getLocation().getLong() 
		  > newVariable(var1.getType(),
				var2).getLocation().getLong()) ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((var1.getLocation().getLong() 
					      > var2.getLocation().getLong()) 
					     ? 1 : 0));
  }

  public Variable greaterThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().greaterThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((var1.getLocation().getLong() 
	   >= newVariable(var1.getType(),
			  var2).getLocation().getLong()) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((var1.getLocation().getLong() 
					      >= var2.getLocation().getLong()) 
					     ? 1 : 0));
  }

  public Variable lessThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().lessThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((var1.getLocation().getLong() 
	   <= newVariable(var1.getType(),
			  var2).getLocation().getLong()) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((var1.getLocation().getLong() 
					      <= var2.getLocation().getLong()) 
					     ? 1 : 0));
  }

  public Variable equal (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().equal(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((var1.getLocation().getLong() 
	   == newVariable(var1.getType(),
			  var2).getLocation().getLong()) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((var1.getLocation().getLong() 
					      == var2.getLocation().getLong()) 
					     ? 1 : 0));
  }

  public Variable notEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().notEqual(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((var1.getLocation().getLong() 
	   != newVariable(var1.getType(),
			  var2).getLocation().getLong()) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((var1.getLocation().getLong() 
					      != var2.getLocation().getLong()) 

					     ? 1 : 0));
  }

  public Variable bitWiseAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().bitWiseAnd(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 (var1.getLocation().getLong() 
	  & newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) (var1.getLocation().getLong() 
					     & var2.getLocation().getLong()));
  }

  public Variable bitWiseOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().bitWiseOr(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 (var1.getLocation().getLong() 
	  | newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) (var1.getLocation().getLong() 
					     | var2.getLocation().getLong()));
  }

  public Variable bitWiseXor (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().bitWiseXor(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 (var1.getLocation().getLong() 
	  ^ newVariable(var1.getType(),
			var2).getLocation().getLong()));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) (var1.getLocation().getLong() 
					     ^ var2.getLocation().getLong()));
  }

  public Variable logicalAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().logicalAnd(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((getLogicalValue(var1) 
	   && getLogicalValue(newVariable(var1.getType(),
					  var2))) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((getLogicalValue(var1) 
					      && getLogicalValue(var2)) 
					     ? 1 : 0));
  }

  public Variable logicalOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeLong)
      return var2.getType().logicalOr(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeLong)
      return LongType.newLongVariable
	((LongType) (var1.getType()),
	 (long) 
	 ((getLogicalValue(var1) 
	   || getLogicalValue(newVariable(var1.getType(),
					  var2))) 
	  ? 1 : 0));
    else
      return LongType.newLongVariable((LongType) (var1.getType()),
                                      (long) ((getLogicalValue(var1) 
					      || getLogicalValue(var2)) 
					     ? 1 : 0));
  }

  public boolean getLogicalValue (Variable var1)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeLong)
      throw (new InvalidOperatorException());

    return ((var1.getLong() == 0) 
	    ? false : true);
  }

  /*
   * Type add(Type type) { Type returnType = null; try { Class objTypeClass =
   * type.getClass(); Class objCons[] = {Class.forName("LongType")}; Object
   * objConsArgs[] = {this}; returnType =
   * type.addTo(((Type)(objTypeClass.getConstructor(objCons).newInstance(objConsArgs)))); }
   * catch (Exception e){ System.err.println("caught exception: " + e); } return
   * returnType; } Type addTo(Type type) { return (Type)(new LongType(_size,
   * (int)(_location.getLong() + type.getLocation().getLong()))); }
   */
}
