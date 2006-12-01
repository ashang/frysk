

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
      else
        return false;
    }

    public String nextName ()
    {
      return (String) names.get(idx);
    }

    public Object next ()
    {
      Type type = ((Type) (members.get(idx)));
      if (type._typeId == BaseTypes.baseTypeChar)
        return new Integer(0);
      else if (type._typeId == BaseTypes.baseTypeShort)
        return new Integer(v.getShort(idx * type.getSize()));
      else if (type._typeId == BaseTypes.baseTypeInteger)
        return new Integer(v.getInt(idx * type.getSize()));
      else if (type._typeId == BaseTypes.baseTypeLong)
        return new Integer(v.getInt(idx * type.getSize()));
      else if (type._typeId == BaseTypes.baseTypeFloat)
        return new Float(v.getFloat(idx * type.getSize()));
      else if (type._typeId == BaseTypes.baseTypeDouble)
        return new Double(v.getDouble(idx * type.getSize()));
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
  }

  public void addMember (Type member, String name)
  {
    members.add(member);
    names.add(name);
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
