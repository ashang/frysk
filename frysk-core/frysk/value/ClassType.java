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
import java.util.Iterator;

import lib.dw.BaseTypes;

/**
 * Type for a class.
 */
public class ClassType
    extends Type
{
  ArrayList types;	// Type of member

  ArrayList names;	// String
  
  ArrayList offsets;	// Long offset into class
  
  ArrayList masks;	// Integer mask for bitfields

  /**
   * Iterate through the class types.
   */
  class ClassIterator
      implements java.util.Iterator
  {
    private int idx;

    Value v;

    ClassIterator (Value v)
    {
      idx = - 1;
      this.v = v;
    }

    public boolean hasNext ()
    {
      idx += 1;
      if (idx < types.size())
        return true;
      return false;
    }

    public String nextName ()
    {
      return (String) names.get(idx);
    }

    public Object next ()
    {
      return getValue(v, idx);
    }

    public void remove ()
    {
    }
  }

  private Value getValue (Value v, int idx)
  {
    Type type = ((Type) (types.get(idx)));
    int off = ((Long)offsets.get(idx)).intValue();

    switch (((Type)types.get(idx)).typeId)
    {
    case BaseTypes.baseTypeByte:
      return ArithmeticType.newByteValue((ArithmeticType)type, v.getByte(off));
    case BaseTypes.baseTypeShort:
      return ArithmeticType.newShortValue((ArithmeticType)type, v.getShort(off));
    case BaseTypes.baseTypeInteger:
      int val = v.getInt(off);
      int mask = ((Integer)masks.get(idx)).intValue();
      if (mask != 0)
	{
	  int shift = 0;
	  int tmpMask = mask;
	  // ??? substitute numberOfTrailingZeros() for 1.5
	  while ((tmpMask & 0x1) == 0)
	    {
	      shift += 1;
	      tmpMask = tmpMask >>> 1;
	    }
	  val = (val & mask) >>> shift;
	}
      return ArithmeticType.newIntegerValue((ArithmeticType)type, val);
    case BaseTypes.baseTypeLong:
      return ArithmeticType.newLongValue((ArithmeticType)type, v.getLong(off));
    case BaseTypes.baseTypeFloat:
      return ArithmeticType.newFloatValue((ArithmeticType)type, v.getFloat(off));
    case BaseTypes.baseTypeDouble:
      return ArithmeticType.newDoubleValue((ArithmeticType)type, v.getDouble(off));
    }
    if (type instanceof ClassType)
      {
	byte [] buf = new byte[type.size];
	v.getLocation().getByteBuffer().get(off, buf, 0, type.size);
	ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, type.size);
	abb.order(type.getEndian());
	return new Value((ClassType)type, type.name, (ArrayByteBuffer)abb);
      }
    else if (type instanceof ArrayType)
      {
	byte [] buf = new byte[type.size];
	v.getLocation().getByteBuffer().get(off, buf, 0, type.size);
	ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, type.size);
	abb.order(type.getEndian());
	return new Value((ArrayType)type, type.name, (ArrayByteBuffer)abb);
      }
    return null;
  }
  
  public ClassIterator iterator (Value v)
  {
    return new ClassIterator(v);
  }

  public Value get (Value v, ArrayList components)
  {
    Iterator ci = components.iterator();
    while (ci.hasNext())
      {
	String component = (String)ci.next();
	for (int i = 0; i < names.size(); i++)
	  {
	      if (((String)names.get(i)).equals(component))
	        return getValue (v, i);
	  }
      }
    return null;
  }
  
  public String toString (Value v)
  {
    StringBuffer strBuf = new StringBuffer();
    ClassIterator e = iterator(v);
    strBuf.append("{");
    while (e.hasNext())
      {
        strBuf.append(e.nextName() + "=");
        strBuf.append(e.next() + ",");
      }
    strBuf.replace(strBuf.length() - 1, strBuf.length(), "}");
    return strBuf.toString();
  }
  
  public String getName ()
  {
    StringBuffer strBuf = new StringBuffer();
    strBuf.append("{");
    for (int i = 0; i < this.types.size(); i++)
      {
	strBuf.append(((Type)this.types.get(i)).getName() + " ");
	strBuf.append((String)this.names.get(i));
	int mask = ((Integer)this.masks.get(i)).intValue();
	int bitCount = 0;
	// ??? substitute numberOfBits() for 1.5
	while (mask != 0)
	  {
	    if ((mask & 0x1) == 1)
	      bitCount += 1;
	    mask = mask >>> 1;
	  }
	if (bitCount > 0)
	  strBuf.append(":" + bitCount);
	strBuf.append(";");
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
    types = new ArrayList();
    names = new ArrayList();
    offsets = new ArrayList();
    masks = new ArrayList();
  }

  public void addMember (Type member, String name, long offset, int mask)
  {
    types.add(member);
    names.add(name);
    offsets.add(new Long(offset));
    masks.add(new Integer(mask));
  }

  public void setSize (int size)
  {
    this.size = size;
  }
  
  public static Value newClassValue (Type type, String text,
                                           ArrayByteBuffer ab)
  {
    Location loc = new Location(ab);
    Value returnVar = new Value(type, text, loc);
    return returnVar;
  }

  public Value add (Value var1, Value var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
  }

  public Value subtract (Value var1, Value var2)
      throws InvalidOperatorException
  {
    throw (new InvalidOperatorException());
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
