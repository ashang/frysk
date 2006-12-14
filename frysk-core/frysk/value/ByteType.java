

package frysk.value;

import inua.eio.ByteOrder;
import lib.dw.BaseTypes;

public class ByteType
    extends Type
{

  public String toString (Variable v)
  {
    return String.valueOf((char)v.getByte());
  }

  public ByteType (int size, ByteOrder endian)
  {
    super(size, endian, BaseTypes.baseTypeChar, "short");
  }

  public static Variable newByteVariable (ByteType type, byte val)
  {
    return newByteVariable(type, "temp", val);
  }

  public static Variable newByteVariable (ByteType type, String text,
                                           byte val)
  {
    Variable returnVar = new Variable(type, text);
    returnVar.getLocation().putByte(val);
    return returnVar;
  }

  public Variable newVariable (Type type, Variable val)
  {
    return val.getType().newByteVariable((ByteType) type, val);
  }

  public Variable newFloatVariable (FloatType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putFloat((float) val.getByte());
    return returnVar;
  }

  public Variable newDoubleVariable (DoubleType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putDouble((double) val.getByte());
    return returnVar;
  }

  public Variable newByteVariable (ByteType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putByte((byte) (val.getByte()));
    return returnVar;
  }

  public Variable newShortVariable (ShortType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putShort((short) (val.getByte()));
    return returnVar;
  }

  public Variable newIntegerVariable (IntegerType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putInt((int) val.getByte());
    return returnVar;
  }

  public Variable newLongVariable (LongType type, Variable val)
  {
    Variable returnVar = new Variable(type, val.getText());
    returnVar.getLocation().putLong((long) val.getByte());
    return returnVar;
  }

  public Variable assign (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (newVariable(var1.getType(),
					 var2).getByte())
		  : (byte) (var2.getByte()));
    return var1;
  }

  public Variable timesEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() * newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() * var2.getByte()));
    return var1;
  }

  public Variable divideEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() / newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() / var2.getByte()));
    return var1;
  }

  public Variable minusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() - newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() - var2.getByte()));
    return var1;
  }

  public Variable plusEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() + newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() + var2.getByte()));
    return var1;
  }

  public Variable modEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() % newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() % var2.getByte()));
    return var1;
  }

  public Variable shiftLeftEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((byte) (var1.getByte() << longValue(var2)));
    return var1;
  }

  public Variable shiftRightEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((byte) (var1.getByte() >> longValue(var2)));
    return var1;
  }

  public Variable bitWiseAndEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() & newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() & var2.getByte()));
    return var1;
  }

  public Variable bitWiseOrEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() | newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() | var2.getByte()));
    return var1;
  }

  public Variable bitWiseXorEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    var1.putByte((var2.getType().getTypeId() != BaseTypes.baseTypeChar) 
		  ? (byte) (var1.getByte() ^ newVariable(var1.getType(),
							   var2).getByte())
		  : (byte) (var1.getByte() ^ var2.getByte()));
    return var1;
  }

  public Variable add (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().add(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  + newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 + var2.getLocation().getByte()));
  }

  public Variable subtract (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().subtract(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  - newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 - var2.getLocation().getByte()));
  }

  public Variable multiply (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().multiply(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  * newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 * var2.getLocation().getByte()));
  }

  public Variable divide (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().divide(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  / newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 / var2.getLocation().getByte()));
  }

  public Variable mod (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().mod(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  % newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 % var2.getLocation().getByte()));
  }

  public Variable shiftLeft (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    long v2;

    if (var2.getType().getTypeId() == BaseTypes.baseTypeChar)
      v2 = 0; // var2.getByte();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeChar)
      v2 = var2.getByte();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeInteger)
      v2 = var2.getInt();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeLong)
      v2 = var2.getLong();
    else
      throw new InvalidOperatorException(
                                         "binary operator << not defined for type byte, "
                                             + var2.getType().getName());

    return ByteType.newByteVariable
      ((ByteType) (var1.getType()),
       (byte) (((byte) (var1.getByte())) << v2));
  }

  public Variable shiftRight (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    long v2;

    if (var2.getType().getTypeId() == BaseTypes.baseTypeChar)
      v2 = 0; // var2.getByte();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeChar)
      v2 = var2.getByte();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeInteger)
      v2 = var2.getInt();
    else if (var2.getType().getTypeId() == BaseTypes.baseTypeLong)
      v2 = var2.getLong();
    else
      throw new InvalidOperatorException(
                                         "binary operator >> not defined for type byte, "
                                             + var2.getType().getName());

    return ByteType.newByteVariable
      ((ByteType) (var1.getType()),
       (byte) (((byte) (var1.getByte())) >> v2));
  }

  public Variable lessThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().lessThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   < newVariable(var1.getType(),
			 var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte() 
						  < var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable greaterThan (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().greaterThan(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   > newVariable(var1.getType(),
			 var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte()
						  > var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable greaterThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().greaterThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   >= newVariable(var1.getType(),
			  var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte() 
						  >= var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable lessThanOrEqualTo (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().lessThanOrEqualTo(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   <= newVariable(var1.getType(),
			  var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte() 
						  <= var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable equal (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().equal(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   == newVariable(var1.getType(),
			  var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte() 
						  == var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable notEqual (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().notEqual(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((var1.getLocation().getByte() 
	   != newVariable(var1.getType(),
			  var2).getLocation().getByte()) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((var1.getLocation().getByte() 
						  != var2.getLocation().getByte()) 
						 ? 1 : 0));
  }

  public Variable bitWiseAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().bitWiseAnd(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  & newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 & var2.getLocation().getByte()));
  }

  public Variable bitWiseOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().bitWiseOr(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  | newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 | var2.getLocation().getByte()));
  }

  public Variable bitWiseXor (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().bitWiseXor(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 (var1.getLocation().getByte() 
	  ^ newVariable(var1.getType(),
			var2).getLocation().getByte()));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) (var1.getLocation().getByte() 
						 ^ var2.getLocation().getByte()));
  }

  public Variable logicalAnd (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().logicalAnd(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((getLogicalValue(var1) 
	   && getLogicalValue(newVariable(var1.getType(),
					  var2))) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((getLogicalValue(var1) 
						  && getLogicalValue(var2)) 
						 ? 1 : 0));
  }

  public Variable logicalOr (Variable var1, Variable var2)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    if (var2.getType().getTypeId() > BaseTypes.baseTypeChar)
      return var2.getType().logicalOr(var1, var2);
    if (var2.getType().getTypeId() < BaseTypes.baseTypeChar)
      return ByteType.newByteVariable
	((ByteType) (var1.getType()),
	 (byte) 
	 ((getLogicalValue(var1) 
	   || getLogicalValue(newVariable(var1.getType(),
					  var2))) ? 1 : 0));
    else
      return ByteType.newByteVariable((ByteType) (var1.getType()),
                                        (byte) ((getLogicalValue(var1) 
						  || getLogicalValue(var2)) 
						 ? 1 : 0));
  }

  public boolean getLogicalValue (Variable var1)
      throws InvalidOperatorException
  {
    if (var1.getType().getTypeId() != BaseTypes.baseTypeChar)
      throw (new InvalidOperatorException());

    return ((var1.getByte() == 0) ? false : true);
  }

}
