// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

public class DoubleType
    extends Type
{

  public String toString (Variable v)
  {
    return String.valueOf(v.getDouble());
  }

  public DoubleType (int size, ByteOrder endian)
  {
    super(size, endian, BaseTypes.baseTypeDouble, "double");
  }

  public static Variable newDoubleVariable (DoubleType type, double val)
  {
    return newDoubleVariable(type, "temp", val);
  }

  public static Variable newDoubleVariable (DoubleType type, String text,
                                            double val)
  {
    Variable returnVar = new Variable(type, text);
    returnVar.getLocation().putDouble(val);
    return returnVar;
  }

  public Variable newVariable (Type type, Variable val)
  {
    return val.getType().newDoubleVariable((DoubleType) type, val);
  }

  public Variable newFloatVariable (FloatType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putDouble((double) val.getDouble());
    return returnVar;
  }

  public Variable newDoubleVariable (DoubleType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putDouble((double) val.getDouble());
    return returnVar;
  }

  public Variable newShortVariable (ShortType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putShort((short) (val.getDouble()));
    return returnVar;
  }

  public Variable newIntegerVariable (IntegerType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putInt((int) val.getDouble());
    return returnVar;
  }

  public Variable newLongVariable (LongType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putLong((long) val.getDouble());
    return returnVar;
  }

  public Variable add (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().add(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	(
	 (DoubleType) 
	 (var1.getType()),
	 (var1.getLocation().getDouble() 
	  + newVariable(var1.getType(),
			var2).getLocation().getDouble()));
    else
      return DoubleType.newDoubleVariable(
                                          (DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   + var2.getLocation().getDouble()));
  }

  public Variable subtract (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().subtract(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	(
	 (DoubleType) (var1.getType()),
	 (var1.getLocation().getDouble() 
	  - newVariable(var1.getType(),
			var2).getLocation().getDouble()));
    else
      return DoubleType.newDoubleVariable(
                                          (DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   - var2.getLocation().getDouble()));
  }

  public Variable assign (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (newVariable(var1.getType(),
				  var2).getDouble())
		   : var2.getDouble());
    return var1;
  }

  public Variable timesEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (var1.getDouble() * newVariable(var1.getType(),
						     var2).getDouble())
		   : var1.getDouble()
		   * var2.getDouble());
    return var1;
  }

  public Variable divideEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (var1.getDouble() / newVariable(var1.getType(),
						     var2).getDouble())
		   : var1.getDouble()
		   / var2.getDouble());
    return var1;
  }

  public Variable minusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (var1.getDouble() - newVariable(var1.getType(),
						     var2).getDouble())
		   : var1.getDouble()
		   - var2.getDouble());
    return var1;
  }

  public Variable plusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (var1.getDouble() + newVariable(var1.getType(),
						     var2).getDouble())
		   : var1.getDouble()
		   + var2.getDouble());
    return var1;
  }

  public Variable modEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    var1.putDouble((var2.getType().getTypeId() != BaseTypes.baseTypeDouble) 
		   ? (var1.getDouble() % newVariable(var1.getType(),
						     var2).getDouble())
		   : var1.getDouble()
		   % var2.getDouble());
    return var1;
  }

  public Variable shiftLeftEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable shiftRightEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable bitWiseAndEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable bitWiseOrEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable bitWiseXorEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable multiply (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().multiply(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (var1.getLocation().getDouble() 
	  * newVariable(var1.getType(),
			var2).getLocation().getDouble()));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   * var2.getLocation().getDouble()));
  }

  public Variable divide (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().divide(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (var1.getLocation().getDouble() 
	  / newVariable(var1.getType(),
			var2).getLocation().getDouble()));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   / var2.getLocation().getDouble()));
  }

  public Variable mod (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().mod(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (var1.getLocation().getDouble() 
	  % newVariable(var1.getType(),
			var2).getLocation().getDouble()));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   % var2.getLocation().getDouble()));
  }

  public Variable shiftLeft (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable shiftRight (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable lessThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().lessThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (var1.getLocation().getDouble() 
	  < newVariable(var1.getType(),
			var2).getLocation().getDouble()) ? 1 : 0);
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (var1.getLocation().getDouble() 
					   < var2.getLocation().getDouble()) 
					  ? 1 : 0);
  }

  public Variable greaterThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().greaterThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) ((var1.getLocation().getDouble() 
		 > newVariable(var1.getType(),
			       var2).getLocation().getDouble()) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((var1.getLocation().getDouble() 
						  > var2.getLocation().getDouble()) 
						 ? 1 : 0));
  }

  public Variable greaterThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().greaterThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) ((var1.getLocation().getDouble() 
		 >= newVariable(var1.getType(),
				var2).getLocation().getDouble()) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((var1.getLocation().getDouble() 
						  >= var2.getLocation().getDouble()) 
						 ? 1 : 0));
  }

  public Variable lessThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().lessThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) ((var1.getLocation().getDouble() 
		 <= newVariable(var1.getType(),
				var2).getLocation().getDouble()) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((var1.getLocation().getDouble() 
						  <= var2.getLocation().getDouble()) 
						 ? 1 : 0));
  }

  public Variable equal (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().equal(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) ((var1.getLocation().getDouble() 
		 == newVariable(var1.getType(),
				var2).getLocation().getDouble()) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((var1.getLocation().getDouble() 
						  == var2.getLocation().getDouble()) 
						 ? 1 : 0));
  }

  public Variable notEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().notEqual(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) ((var1.getLocation().getDouble() 
		 != newVariable(var1.getType(),
				var2).getLocation().getDouble()) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((var1.getLocation().getDouble() 
						  != var2.getLocation().getDouble()) 
						 ? 1 : 0));
  }

  public Variable bitWiseAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable bitWiseOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable bitWiseXor (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    return var1;
  }

  public Variable logicalAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().logicalAnd(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) 
	 ((getLogicalValue(var1) 
	   && getLogicalValue(newVariable(var1.getType(),
					  var2))) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((getLogicalValue(var1) 
						  && getLogicalValue(var2)) 
						 ? 1 : 0));
  }

  public Variable logicalOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeDouble)
      return var2.getType().logicalOr(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeDouble)
      return DoubleType.newDoubleVariable
	((DoubleType) (var1.getType()),
	 (int) 
	 ((getLogicalValue(var1) 
	   || getLogicalValue(newVariable(var1.getType(),
					  var2))) ? 1 : 0));
    else
      return DoubleType.newDoubleVariable((DoubleType) (var1.getType()),
                                          (int) ((getLogicalValue(var1) 
						  || getLogicalValue(var2)) 
						 ? 1 : 0));
  }

  public boolean getLogicalValue (Variable var1)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeDouble)
      throw (new InvalidOperatorException());

    return ((var1.getDouble() == 0) ? false : true);
  }

  /*
   * Type add(Type type) { Type returnType = null; try { Class objTypeClass =
   * type.getClass(); Class objCons[] = {Class.forName("DoubleType")}; Object
   * objConsArgs[] = {this}; returnType =
   * type.addTo(((Type)(objTypeClass.getConstructor(objCons).newInstance(objConsArgs)))); }
   * catch (Exception e){ System.err.println("caught exception: " + e); } return
   * returnType; } Type addTo(Type type) { return (Type)(new DoubleType(_size,
   * (int)(_location.getDouble() + type.getLocation().getDouble()))); }
   */
}
