
  package frysk.lang;

  //import inua.eio.*;
  //import java.lang.reflect.Constructor;

  public class IntegerType extends Type
  {
    public IntegerType(int size, int endian)  {
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

    public Variable add(Variable var1, Variable var2) {
      if(var2.getType().getTypeId() > BaseTypes.baseTypeInteger)
	return var2.getType().add(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeInteger)
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() + newVariable(var1.getType(), var2).getLocation().getInt()));
      else
	return IntegerType.newIntegerVariable((IntegerType)(var1.getType()), (var1.getLocation().getInt() + var2.getLocation().getInt()));
    }

    public Variable assign(Variable var1, Variable var2)  {
	var1.putInt((var2.getType().getTypeId() != BaseTypes.baseTypeInteger) 
	    ? (newVariable(var1.getType(), var2).getInt()) 
	    : var2.getInt());
	return var1;
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

