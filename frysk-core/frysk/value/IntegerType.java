
  package frysk.value;

  import inua.eio.ByteOrder;
  import lib.dw.BaseTypes;

  //import inua.eio.*;
  //import java.lang.reflect.Constructor;


  public class IntegerType extends Type
  {

    public String toString  (Variable v)  {
      return String.valueOf(v.getInt());
    }

    public IntegerType(int size, ByteOrder endian)  {
      super(size, endian, BaseTypes.baseTypeInteger, "int");
    }

    public static Variable newIntegerVariable(IntegerType type, int val)  {
      return newIntegerVariable(type, "temp", val);
    }

    public static Variable newIntegerVariable(IntegerType type, String text, int val)  {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putInt(val);
      return returnVar;
    }

    public Variable newVariable(Type type, Variable val)  {
	return val.getType().newIntegerVariable((IntegerType)type, val);
    }

    public Variable newFloatVariable(FloatType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putFloat(val.getInt());
      return returnVar;
      }
    public Variable newDoubleVariable(DoubleType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putDouble(val.getInt());
      return returnVar;
      }
    public Variable newIntegerVariable(IntegerType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putInt(val.getInt());
      return returnVar;
    }

    public Variable newShortVariable(ShortType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putInt(val.getShort());
      return returnVar;
    }

    public Variable add(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().add(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() + newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() + var2.getLocation().getInt()));
    }

    public Variable subtract(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().subtract(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() - newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() - var2.getLocation().getInt()));
    }

    public Variable assign(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (newVariable(var1.getType(), var2).getInt()) 
	  : var2.getInt());
      return var1;
    }

    public Variable timesEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() * newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() * var2.getInt());
      return var1;
    }

    public Variable divideEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() / newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() / var2.getInt());
      return var1;
    }

    public Variable minusEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() - newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() - var2.getInt());
      return var1;
    }

    public Variable plusEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() + newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() + var2.getInt());
      return var1;
    }

    public Variable modEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() % newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() % var2.getInt());
      return var1;
    }

    public Variable shiftLeftEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt(var1.getInt() << longValue(var2));
      return var1;
    }

    public Variable shiftRightEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt(var1.getInt() >> longValue(var2));
      return var1;
    }

    public Variable bitWiseAndEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() & newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() & var2.getInt());
      return var1;
    }

    public Variable bitWiseOrEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() | newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() | var2.getInt());
      return var1;
    }

    public Variable bitWiseXorEqual(Variable var1, Variable var2) throws InvalidOperatorException {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	  ? (var1.getInt() ^ newVariable(var1.getType(), var2).getInt()) 
	  : var1.getInt() ^ var2.getInt());
      return var1;
    }

    public Variable multiply(Variable var1, Variable var2) throws InvalidOperatorException{
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().multiply(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() * newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() * var2.getLocation().getInt()));
    }

    public Variable divide(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().divide(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() / newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() / var2.getLocation().getInt()));
    }

    public Variable mod(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().mod(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() % newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() % var2.getLocation().getInt()));
    }
    
    public Variable shiftLeft(Variable var1, Variable var2) throws InvalidOperatorException  {
      int resultVar = var1.getInt() << longValue(var2);
      return  IntegerType.newIntegerVariable((IntegerType)var1.getType(), resultVar);
    }

    public Variable shiftRight(Variable var1, Variable var2) throws InvalidOperatorException  {
      long v2=0;

      if(var2.getType().getTypeId() == BaseTypes.baseTypeChar)
	  v2 = 0 ;//var2.getChar();
      else if(var2.getType().getTypeId() == BaseTypes.baseTypeShort)
	  v2 = var2.getShort();
      else if(var2.getType().getTypeId() == BaseTypes.baseTypeInteger)
	  v2 = var2.getInt();
      else if(var2.getType().getTypeId() == BaseTypes.baseTypeLong)
	  v2 = var2.getLong();
      else
	throw new InvalidOperatorException("binary operator >> not defined for type int, " + var2.getType().getName());

      int resultVar = var1.getInt() >> v2;
      return  IntegerType.newIntegerVariable((IntegerType)var1.getType(), resultVar);
    }

    public Variable lessThan(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().lessThan(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() < newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0);
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() < var2.getLocation().getInt()) ? 1 : 0);
    }

    public Variable greaterThan(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().greaterThan(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() > newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() > var2.getLocation().getInt()) ? 1 : 0));
    }

    public Variable greaterThanOrEqualTo(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().greaterThanOrEqualTo(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() >= newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() >= var2.getLocation().getInt()) ? 1 : 0));
    }

    public Variable lessThanOrEqualTo(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().lessThanOrEqualTo(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() <= newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() <= var2.getLocation().getInt()) ? 1 : 0));
    }

    public Variable equal(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().equal(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() == newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() == var2.getLocation().getInt()) ? 1 : 0));
    }

    public Variable notEqual(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().notEqual(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() != newVariable(var1.getType(), var2).getLocation().getInt()) ? 1 : 0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((var1.getLocation().getInt() != var2.getLocation().getInt()) ? 1 : 0));
    }

    public Variable bitWiseAnd(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().bitWiseAnd(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() & newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() & var2.getLocation().getInt()));
    }

    public Variable bitWiseOr(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().bitWiseOr(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() | newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() | var2.getLocation().getInt()));
    }

    public Variable bitWiseXor(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().bitWiseXor(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() ^ newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)(var1.getLocation().getInt() ^ var2.getLocation().getInt()));
    }

    public Variable logicalAnd(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().logicalAnd(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((getLogicalValue(var1) && getLogicalValue(newVariable(var1.getType(), var2)))?1:0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((getLogicalValue(var1) && getLogicalValue(var2))?1:0));
    }

    public Variable logicalOr(Variable var1, Variable var2) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().logicalOr(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((getLogicalValue(var1) || getLogicalValue(newVariable(var1.getType(), var2)))?1:0));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (int)((getLogicalValue(var1) || getLogicalValue(var2))?1:0));
    }

    public boolean getLogicalValue(Variable var1) throws InvalidOperatorException  {
      if(var1.getType().getTypeId() != BaseTypes.baseTypeInteger)
	throw (new InvalidOperatorException());

	return ((var1.getInt() == 0)?false:true);
    }


    /*Type add(Type type) {
      Type returnType = null;
      try {
	Class objTypeClass = type.getClass();
	Class objCons[] = {Class.forName("IntegerType")};
	Object objConsArgs[] = {this};
	returnType = type.addTo(((Type)(objTypeClass.getConstructor(objCons).newInstance(objConsArgs))));
      }	catch (Exception e){
	System.err.println("caught exception: " + e);
      }
      return returnType;
    }

    Type addTo(Type type) {
      return (Type)(new IntegerType(_size, (int)(_location.getInt() + type.getLocation().getInt())));
    }*/
  }

