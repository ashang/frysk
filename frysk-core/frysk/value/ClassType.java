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

import inua.eio.ArrayByteBuffer;
import inua.eio.ByteOrder;

import java.util.ArrayList;

import lib.dw.BaseTypes;

/**
 * Type for a class.
 */
public class ClassType
    extends Type
{
  ArrayList members;

  ArrayList names;
  
  ArrayList offsets;

  /**
   * Iterate through the class members.
   */
  class Iterator
      implements java.util.Iterator
  {
    private int idx;

    Variable v;

    Iterator (Variable v)
    {
      idx = - 1;
      this.v = v;
    }

    public boolean hasNext ()
    {
      idx += 1;
      if (idx < members.size())
        return true;
      return false;
    }

    public String nextName ()
    {
      return (String) names.get(idx);
    }

    public Object next ()
    {
      Type type = ((Type) (members.get(idx)));
      int off = ((Long)offsets.get(idx)).intValue();
      if (type.typeId == BaseTypes.baseTypeChar)
        return new Integer(v.getByte(off));
      else if (type.typeId == BaseTypes.baseTypeShort)
        return new Integer(v.getShort(off));
      else if (type.typeId == BaseTypes.baseTypeInteger)
        return new Integer(v.getInt(off));
      else if (type.typeId == BaseTypes.baseTypeLong)
        return new Integer(v.getInt(off));
      else if (type.typeId == BaseTypes.baseTypeFloat)
        return new Float(v.getFloat(off));
      else if (type.typeId == BaseTypes.baseTypeDouble)
        return new Double(v.getDouble(off));
      else
        return null;
    }

    public void remove ()
    {
    }
  }

  public Iterator getIterator (Variable v)
  {
    return new Iterator(v);
  }

  public String toString (Variable v)
  {
    StringBuffer strBuf = new StringBuffer();
    Iterator e = getIterator(v);
    while (e.hasNext())
      {
        strBuf.append(e.nextName() + "=");
        strBuf.append(e.next() + ",");
      }
    return strBuf.toString();
  }
  
  public String getName ()
  {
    StringBuffer strBuf = new StringBuffer();
    strBuf.append("{");
    for (int i = 0; i < this.members.size(); i++)
      {
	strBuf.append(((Type)this.members.get(i)).getName() + " ");
	strBuf.append((String)this.names.get(i) + ";");
      }
    strBuf.append("}");
    return strBuf.toString();
  }

  /**
   * Create an ClassType
   * 
   * @param endian - Endianness of class
   */
  public ClassType (ByteOrder endian)
  {
    super(0, endian, 0, "class");
    members = new ArrayList();
    names = new ArrayList();
    offsets = new ArrayList();
  }

  public void addMember (Type member, String name, long offset)
  {
    members.add(member);
    names.add(name);
    offsets.add(new Long(offset));
  }

  public Variable newVariable (Type type, Variable val)
  {
    return val.getType().newIntegerVariable((IntegerType) type, val);
  }

  public static Variable newClassVariable (Type type, String text,
                                           ArrayByteBuffer ab)
  {
    Location loc = new Location(ab);
    Variable returnVar = new Variable(type, text, loc);
    return returnVar;
  }

  public Variable newFloatVariable (FloatType type, Variable val)
  {
    return null;
  }

  public Variable newDoubleVariable (DoubleType type, Variable val)
  {
    return null;
  }

  public Variable newLongVariable (LongType type, Variable val)
  {
    return null;
  }

  public Variable newIntegerVariable (IntegerType type, Variable val)
  {
    return null;
  }

  public Variable newByteVariable (ByteType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putByte((byte) (val.getChar()));
    return returnVar;
  }

  public Variable newShortVariable (ShortType type, Variable val)
  {
    return null;
  }

  public Variable add (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable subtract (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable assign (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable timesEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable divideEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable minusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable plusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable modEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable shiftLeftEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable shiftRightEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseAndEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseOrEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseXorEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable multiply (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable divide (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable mod (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable shiftLeft (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable shiftRight (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable lessThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable greaterThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable greaterThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable lessThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable equal (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable notEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable bitWiseXor (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable logicalAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Variable logicalOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public boolean getLogicalValue (Variable var1)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }
}
