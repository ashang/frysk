// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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
 * Type for a pointer.
 */
public class PointerType
    extends Type
{
    private final Type type;

    public Type getType ()
    {
      return type;
    }
    
    public String toString (Value v)
    {
      return v.toString();
    }

    public String getName ()
    {
      StringBuffer strBuf = new StringBuffer();
      if (type == null)
	strBuf.append("void");
      else
	strBuf.append(type.getName());
      strBuf.append(" *");
      return strBuf.toString();
    }

    /**
     * Create an PointerType
     * 
     * @param typep - Type of pointed to value
     */
    public PointerType (ByteOrder endian, Type type, String typeStr)
    {
      super(8, endian, BaseTypes.baseTypeLong, typeStr);
      this.type = type;
    }

    public Value add (Value var1, Value var2)
	throws InvalidOperatorException
    {
      return ArithmeticType.newLongValue((ArithmeticType)type, 
                                         var1.longValue() + var2.longValue());
    }

    public Value subtract (Value var1, Value var2)
	throws InvalidOperatorException
    {
      return ArithmeticType.newLongValue((ArithmeticType)type, 
                                         var1.longValue() - var2.longValue());
    }

    public Value assign (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value timesEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value divideEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value minusEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value plusEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value modEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftLeftEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftRightEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseAndEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseOrEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseXorEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value multiply (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value divide (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value mod (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftLeft (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftRight (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value lessThan (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value greaterThan (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value greaterThanOrEqualTo (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value lessThanOrEqualTo (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value equal (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value notEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseAnd (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseOr (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseXor (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseComplement (Value var1)
        throws InvalidOperatorException
    {
      throw (new InvalidOperatorException());
    }
    
    public Value logicalAnd (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value logicalOr (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public boolean getLogicalValue (Value var1)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }
}