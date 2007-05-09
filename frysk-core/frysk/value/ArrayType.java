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

import java.util.ArrayList;
import java.util.Iterator;

import lib.dw.BaseTypes;
import inua.eio.ArrayByteBuffer;

/**
 * Type for an array.
 */
public class ArrayType
    extends Type
{
    private final Type type;

    private final ArrayList dimensions;

    public Type getType ()
    {
      return type;
    }

    /**
     * Iterate through the array members.
     */
    class ArrayIterator
	implements java.util.Iterator
    {
	int dimCount = dimensions.size();

	int stride[] = new int[dimCount + 1];

	private int idx, dim, element;

	Variable v;

	ArrayIterator (Variable v)
	{
	    idx = - 1;
	    stride[0] = 1;
	    this.v = v;

	    for (int i = 1; i < dimCount; i++)
		{
		    int d = ((Integer) (dimensions.get(dimCount - i))).intValue() + 1;
		    stride[i] = d * stride[i - 1];
		}
	    stride[dimCount] = (((Integer) (dimensions.get(0))).intValue() + 1) * stride[dimCount - 1]; 
	}

	public boolean hasNext ()
	{
	    idx += 1;
	    element = idx;
	    dim = dimCount;

	    if (idx < stride[dimCount])
		return true;
	    return false;
	}

	/**
	 * @return The next dimension index for the corresponding array element
	 */
	public int nextIdx ()
	{
	  dim -= 1;
	  if (dim > 0)
	    {
	      if (element >= stride[dim])
		{
		  int newDim = element / (stride[dim]);
		  element = element % (stride[dim]);
		  return newDim;
		}
	      return 0;
	    }
	  return element;
	}

	public Object next ()
	{
	  return getVariable (v, idx);
	}

	public void remove ()
	{
	}
    }

    private Variable getVariable (Variable v, int idx)
    {
      int off = idx * type.getSize();
      switch (type.typeId)
      {
      case (BaseTypes.baseTypeByte):
	return ArithmeticType.newByteVariable((ArithmeticType)type, v.getByte((off)));
      case (BaseTypes.baseTypeShort):
	return ArithmeticType.newShortVariable((ArithmeticType)type, v.getShort(off));
      case (BaseTypes.baseTypeInteger):
	return ArithmeticType.newIntegerVariable((ArithmeticType)type, v.getInt(off));
      case (BaseTypes.baseTypeLong):
	return ArithmeticType.newLongVariable((ArithmeticType)type, v.getLong(off));
      case (BaseTypes.baseTypeFloat):
	return ArithmeticType.newFloatVariable((ArithmeticType)type, v.getFloat(off));
      case (BaseTypes.baseTypeDouble):
	return ArithmeticType.newDoubleVariable((ArithmeticType)type, v.getDouble(off));
      }
      if (type instanceof ClassType)
	{
	  byte [] buf = new byte[type.size];
	  v.getLocation().getByteBuffer().get(off, buf, 0, type.size);
	  ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, type.size);
	  abb.order(type.getEndian());
	  return new Variable((ClassType)type, v.getText(), (ArrayByteBuffer)abb);
	}
      return null;
    }
    
    public ArrayIterator iterator (Variable v)
    {
	return new ArrayIterator(v);
    }

    public Variable get (Variable v, ArrayList components)
    {
      int dimCount = dimensions.size();
      int stride[] = new int[dimCount + 1];
      
      stride[0] = 1;
      for (int i = 1; i < dimCount; i++)
	{
	  int d = ((Integer) (dimensions.get(dimCount - i))).intValue() + 1;
	  stride[i] = d * stride[i - 1];
	}
      stride[dimCount] = (((Integer) (dimensions.get(0))).intValue() + 1) * stride[dimCount - 1]; 
      
    Iterator ci = components.iterator();
    int offset = 0;
    for (int d = dimCount - 1;
         ci.hasNext();
         d -= 1)
      {
	String component = (String)ci.next();
	offset += stride[d] * (Integer.parseInt(component));
      }
    return getVariable (v, offset);
    }
    
    public String toString (Variable v)
    {
      StringBuffer strBuf = new StringBuffer();
      ArrayIterator e = iterator(v);
      boolean isString = false;
      if (type.typeId == BaseTypes.baseTypeByte)
	{
	  isString = true;
	  strBuf.append("\"");
	}
      else
	for (int i = 1; i <= e.dimCount; i++)
	  strBuf.append("{");
      boolean firstTime = true;
      while (e.hasNext())
	{
	  if (!isString)
	    {
	      int dimCount = e.dimCount;
	      boolean putBraces = false;
	      for (int j = dimCount; j >= 1; j--)
		{
		  int nextIdx = e.nextIdx();
		  if (j != dimCount && nextIdx == 0)
		    {
		      putBraces = true;
		      if (firstTime)
			{
			  firstTime = false;
			  putBraces = false;
			}
		    }
		}
	      if (putBraces)
		strBuf.append("},{");
	      else if (e.idx != 0)
		strBuf.append(",");
	      strBuf.append(e.next());
	    }
	  else	
	    {
	      char ch = (char)((Variable)e.next()).getByte();
	      if (ch != 0)
		strBuf.append(ch);
	    }
	}
      if (isString)
	strBuf.append("\"");
      else
	for (int i = 1; i <= e.dimCount; i++)
	  strBuf.append("}");
      return strBuf.toString();
    }

    public String getName ()
    {
      StringBuffer strBuf = new StringBuffer();
      strBuf.append(type.getName());
      strBuf.append(" [");
      for(int i = 0; i < this.dimensions.size(); i++)
        {
          if (i > 0)
            strBuf.append(",");
          strBuf.append(((Integer)this.dimensions.get(i)).intValue() + 1);
        }
      strBuf.append("]");
      return strBuf.toString();
    }

    /**
     * Create an ArrayType
     * 
     * @param typep - Type of each array element
     * @param dimensionsp - ArrayList of dimension upper bounds.
     */
    public ArrayType (Type typep, int size, ArrayList dimensionsp)
    {
	super(size, typep.endian, 0, "array");
	type = typep;
	dimensions = dimensionsp;
    }

    public static Variable newArrayVariable (Type type, String text,
					     ArrayByteBuffer ab)
    {
	Location loc = new Location(ab);
	Variable returnVar = new Variable(type, text, null, loc);
	return returnVar;
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

    public Variable bitWiseComplement (Variable var1)
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
