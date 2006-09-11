
  package frysk.lang;

import inua.eio.ByteOrder;

  public class TypeTest	{
    public static void main(String[]  args) {
      Type intType = new IntegerType(4, ByteOrder.BIG_ENDIAN);
      Type shortType = new ShortType(2, ByteOrder.BIG_ENDIAN);

      try {
	Variable v1 = IntegerType.newIntegerVariable((IntegerType)intType, 4);

	Variable v2 = ShortType.newShortVariable((ShortType)shortType, (short)9);


	Variable v3 = v1.getType().add(v1, v2);

	System.out.println(v3.getInt());
      }	catch(Exception e)  {
	System.out.println("caught exception: " + e);
      }
    }
  }
