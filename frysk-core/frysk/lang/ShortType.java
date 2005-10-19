
  package frysk.lang;

  public class ShortType extends Type
  {
    public ShortType(int size, int endian)  {
      super(size, endian, BaseTypes.baseTypeShort, "short");
    }

    public static Variable newShortVariable(ShortType type, short val)  {
      return newShortVariable(type, "temp", val);
    }
    
    public static Variable newShortVariable(ShortType type, String text, short val)  {
      Variable returnVar = new Variable(type, text);
      returnVar.getLocation().putShort(val);
      return returnVar;
    }

    public Variable newVariable(Type type, Variable val)  {
	return val.getType().newShortVariable((ShortType)type, val);
    }

    public Variable newShortVariable(ShortType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putShort((short)(val.getShort()));
      return returnVar;
    }

    public Variable newIntegerVariable(IntegerType type, Variable val) {
      Variable returnVar = new Variable(type, val.getText());
      returnVar.getLocation().putInt(val.getShort());
      return returnVar;
    }

    public Variable add(Variable var1, Variable var2) {
      if(var2.getType().getTypeId() > BaseTypes.baseTypeShort)
	return var2.getType().add(var1, var2);
      if(var2.getType().getTypeId() < BaseTypes.baseTypeShort)
	return ShortType.newShortVariable((ShortType)(var1.getType()), (short)(var1.getLocation().getShort() + newVariable(var1.getType(), var2).getLocation().getShort()));
      else
	return ShortType.newShortVariable((ShortType)(var1.getType()), (short)(var1.getLocation().getShort() + var2.getLocation().getShort()));
    }

    public Variable assign(Variable var1, Variable var2)  {
      var1.putShort((var2.getType().getTypeId() != BaseTypes.baseTypeShort)
	  ?  (short)(newVariable(var1.getType(), var2).getShort())
	  : (short)(var2.getShort()));
      return var1;
    }

    /*ShortType(int size, ShortType var)  {
      super(var.getSize(), var.getEndian(), (new String(var.getName())));
      _location.putShort(_index, (short)(var.getLocation().getShort()));
    }*/

    /*Type add(Type type) {
      Type returnType = null;
      try {
	System.out.println("Begin...");

	Class objTypeClass = type.getClass();
	System.out.println(objTypeClass.getName());

	Class objConsArgClass[] = {Integer.TYPE, Class.forName("frysk.lang.ShortType")};
	Object objConsArgs[] = {(new Integer(type.getSize())), this};

	Constructor objCons = objTypeClass.getDeclaredConstructor(objConsArgClass);
	System.out.println(objCons);

	Type tempType = (Type)(objCons.newInstance(objConsArgs));
	System.out.println(tempType);

	returnType = type.addTo(tempType);
      }	catch (Exception e){
	System.err.println("caught this exception: " + e);
      }
      return returnType;
    }*/

    /*Type addTo(Type type) {
      return (Type)(new ShortType(_size, (short)(_location.getShort() + type.getLocation().getShort())));
    }*/

  }
