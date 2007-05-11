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

import java.util.ArrayList;

import lib.dw.BaseTypes;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteOrder;
import frysk.junit.TestCase;

public class TestValue
    extends TestCase
{
  Type longBEType = new ArithmeticType (8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeLong, "long");
  Type longBEUnsignedType = new ArithmeticType (8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedLong, "unsigned long");
  Type intBEType = new ArithmeticType (4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeInteger, "int");
  Type intBEUnsignedType = new ArithmeticType (4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedInteger, "unsigned int");
  Type shortBEType = new ArithmeticType (2, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeShort, "short");
  Type shortBEUnsignedType = new ArithmeticType (2, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedShort, "unsigned short");
  Type byteBEType = new ArithmeticType (1, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeByte, "byte");
  Type byteBEUnsignedType = new ArithmeticType (1, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedByte, "unsigned byte");
  Type floatBEType = new ArithmeticType (4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeFloat, "float");
  Type doubleBEType = new ArithmeticType (8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeDouble, "double");
  
  Type longType = new ArithmeticType (8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeLong, "long");
  Type longUnsignedType = new ArithmeticType (8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedLong, "unsigned long");
  Type intType = new ArithmeticType (4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeInteger, "int");
  Type intUnsignedType = new ArithmeticType (4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedInteger, "unsigned int");
  Type shortType = new ArithmeticType (2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeShort, "short");
  Type shortUnsignedType = new ArithmeticType (2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedShort, "unsigned short");
  Type byteType = new ArithmeticType (1, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeByte, "byte");
  Type byteUnsignedType = new ArithmeticType (1, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedByte, "unsigned byte");
  Type floatType = new ArithmeticType (4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeFloat, "float");
  Type doubleType = new ArithmeticType (8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeDouble, "double");
  
  public void testNumber ()
  {
    Value v1 = ArithmeticType.newByteValue((ArithmeticType)byteType, (byte)1);
    Value v2 = ArithmeticType.newShortValue((ArithmeticType)shortType, (short)2);
    Value v3 = ArithmeticType.newIntegerValue((ArithmeticType)intType, 3);
    Value v4 = ArithmeticType.newLongValue((ArithmeticType)longType, 4);
    Value v5 = ArithmeticType.newByteValue((ArithmeticType)byteBEType, (byte)1);
    Value v6 = ArithmeticType.newShortValue((ArithmeticType)shortBEType, (short)2);
    Value v7 = ArithmeticType.newIntegerValue((ArithmeticType)intBEType, 3);
    Value v8 = ArithmeticType.newLongValue((ArithmeticType)longBEType, 4);
    Value v9 = ArithmeticType.newFloatValue((ArithmeticType)floatType, (float)1.0);
    Value v10 = ArithmeticType.newDoubleValue((ArithmeticType)doubleType, 2.0);
    assertEquals ("1", 1, v1.getByte());
    assertEquals ("2", 2, v2.getShort());
    assertEquals ("3", 3, v3.getInt());
    assertEquals ("4", 4, v4.getLong());
    assertEquals ("1", 1, v5.getByte());
    assertEquals ("2", 2, v6.getShort());
    assertEquals ("3", 3, v7.getInt());
    assertEquals ("4", 4, v8.getLong());
    assertEquals ("1.0", 1.0, 1.0, v9.getFloat());
    assertEquals ("2.0", 2.0, 2.0, v10.getDouble());
  }

  private boolean isTrue (int i)
  {
    if (i != 0)
      return true;
    else
      return false;
  }

  private boolean isTrue (double d)
  {
    if (d != 0)
      return true;
    else
      return false;
  }
  
  public void testIntOps ()
    throws InvalidOperatorException
  {
    Value v1 = ArithmeticType.newIntegerValue((ArithmeticType)intType, 4);
    Value v2 = ArithmeticType.newShortValue((ArithmeticType)shortType, (short)9);
    Value v3 = v1.getType().add(v1, v2);
    assertEquals ("4 + 9", 4 + 9, v3.getInt());
    v3 = v1.getType().subtract(v2, v1);
    assertEquals ("9 - 4", 9 - 4, v3.getInt());
    v3 = v1.getType().multiply(v2, v1);
    assertEquals ("9 * 4", 9 * 4, v3.getInt());
    v3 = v1.getType().mod(v2, v1);
    assertEquals ("9 % 4", 9 % 4, v3.getInt());
    v3 = v1.getType().shiftLeft(v2, v1);
    assertEquals ("9 << 4", 9 << 4, v3.getInt());
    v3 = v1.getType().shiftRight(v2, v1);
    assertEquals ("9 >> 4", 9 >> 4, v3.getInt());
    v3 = v1.getType().lessThan(v2, v1);
    assertEquals ("9 < 4", 9 < 4, isTrue(v3.getInt()));
    v3 = v1.getType().greaterThan(v2, v1);
    assertEquals ("9 > 4", 9 > 4, isTrue(v3.getInt()));
    v3 = v1.getType().lessThanOrEqualTo(v2, v1);
    assertEquals ("9 <= 4", 9 <= 4, isTrue(v3.getInt()));
    v3 = v1.getType().greaterThanOrEqualTo(v2, v1);
    assertEquals ("9 >= 4", 9 >= 4, isTrue(v3.getInt()));
    v3 = v1.getType().equal(v2, v1);
    assertEquals ("9 == 4", 9 == 4, isTrue(v3.getInt()));
    v3 = v1.getType().notEqual(v2, v1);
    assertEquals ("9 != 4", 9 != 4, isTrue(v3.getInt()));
    v3 = v1.getType().bitWiseAnd(v2, v1);
    assertEquals ("9 && 4", 9 & 4, v3.getInt());
    v3 = v1.getType().bitWiseOr(v2, v1);
    assertEquals ("9 || 4", 9 | 4, v3.getInt());
    v3 = v1.getType().bitWiseXor(v2, v1);
    assertEquals ("9 ^ 4", 9 ^ 4, v3.getInt());
    v3 = v1.getType().bitWiseComplement(v1);
    assertEquals ("~4", ~4, v3.getInt());
    v3 = v1.getType().logicalAnd(v2, v1);
    assertEquals ("9 & 4", 1, v3.getInt());
    v3 = v1.getType().logicalOr(v2, v1);
    assertEquals ("9 | 4", 1, v3.getInt());
    v3 = v1.getType().assign(v3, v1);
    assertEquals ("v3 = 4", 4, v3.getInt());
    v3 = v1.getType().plusEqual(v3, v1);
    assertEquals ("v3 += 4", 8, v3.getInt());
    v3 = v1.getType().minusEqual(v3, v1);
    assertEquals ("v3 -= 4", 4, v3.getInt());
    v3 = v1.getType().timesEqual(v3, v1);
    assertEquals ("v3 *= 4", 16, v3.getInt());
    v3 = v1.getType().divideEqual(v3, v1);
    assertEquals ("v3 /= 4", 4, v3.getInt());
    v3 = v1.getType().modEqual(v3, v1);
    assertEquals ("v3 %= 4", 0, v3.getInt());
    v3 = v1.getType().shiftLeftEqual(v3, v1);
    assertEquals ("v3 <<= 4", 0, v3.getInt());
    v3 = v1.getType().shiftRightEqual(v3, v1);
    assertEquals ("v3 >>= 4", 0, v3.getInt());
    v3 = v1.getType().bitWiseOrEqual(v3, v1);
    assertEquals ("v3 ||= 4", 4, v3.getInt());
    v3 = v1.getType().bitWiseXorEqual(v3, v1);
    assertEquals ("v3 ^= 4", 0, v3.getInt());
    v3 = v1.getType().bitWiseAndEqual(v3, v1);
    assertEquals ("v3 &&= 4", 0, v3.getInt());
  }

  public void testFloatOps ()
    throws InvalidOperatorException
  {
    Value v1 = ArithmeticType.newFloatValue((ArithmeticType)floatType, (float)4.0);
    Value v2 = ArithmeticType.newDoubleValue((ArithmeticType)doubleType, 9.0);
    Value v3 = v1.getType().add(v1, v2);
    assertEquals ("4 + 9", 4 + 9, v3.getDouble(), 0);
    v3 = v1.getType().subtract(v2, v1);
    assertEquals ("9 - 4", 9 - 4, v3.getDouble(), 0);
    v3 = v1.getType().multiply(v2, v1);
    assertEquals ("9 * 4", 9 * 4, v3.getDouble(), 0);
    v3 = v1.getType().mod(v2, v1);
    assertEquals ("9 % 4", 9 % 4, v3.getDouble(), 0);
    v3 = v1.getType().lessThan(v2, v1);
    assertEquals ("9 < 4", 9 < 4, isTrue(v3.getDouble()));
    v3 = v1.getType().greaterThan(v2, v1);
    assertEquals ("9 > 4", 9 > 4, isTrue(v3.getDouble()));
    v3 = v1.getType().lessThanOrEqualTo(v2, v1);
    assertEquals ("9 <= 4", 9 <= 4, isTrue(v3.getDouble()));
    v3 = v1.getType().greaterThanOrEqualTo(v2, v1);
    assertEquals ("9 >= 4", 9 >= 4, isTrue(v3.getDouble()));
    v3 = v1.getType().equal(v2, v1);
    assertEquals ("9 == 4", 9 == 4, isTrue(v3.getDouble()));
    v3 = v1.getType().notEqual(v2, v1);
    assertEquals ("9 != 4", v2 != v1, isTrue(v3.getDouble()));
    v3 = v1.getType().assign(v3, v1);
    assertEquals ("v3 = 4", 4, v3.getDouble(), 0);
    v3 = v1.getType().plusEqual(v3, v1);
    assertEquals ("v3 += 4", 8, v3.getDouble(), 0);
    v3 = v1.getType().minusEqual(v3, v1);
    assertEquals ("v3 -= 4", 4, v3.getDouble(), 0);
    v3 = v1.getType().timesEqual(v3, v1);
    assertEquals ("v3 *= 4", 16, v3.getDouble(), 0);
    v3 = v1.getType().divideEqual(v3, v1);
    assertEquals ("v3 /= 4", 4, v3.getDouble(), 0);
    v3 = v1.getType().modEqual(v3, v1);
    assertEquals ("v3 %= 4", 0, v3.getDouble(), 0);
  }

  /**
   * enum {}
   */
  public void testEnum ()
  {
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    EnumType enumType = new EnumType(byteOrder);
    enumType.addMember(byteType, "red", 1);
    enumType.addMember(byteType, "orange", 2);
    enumType.addMember(byteType, "yellow", 3);
    enumType.addMember(byteType, "green", 4);
    enumType.addMember(byteType, "blue", 5);
    enumType.addMember(byteType, "violet", 6);
        Value e1 = EnumType.newEnumValue(enumType, "e1");
    String s = e1.toString();
    assertEquals ("enum", "{red=1,orange=2,yellow=3,green=4,blue=5,violet=6}", s);
  }
  
  /**
   * int[] array 
   */
  public void testArrayOfNumber ()
  {
    // Also separate tests for 0 dimensioned arrays et.al.
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    ArrayList dims = new ArrayList();
    dims.add(new Integer(3));
    ArrayType arrayType = new ArrayType(intType, 16, dims);
    int [] intbuf = {0x1020304, 0x5060708, 0x090a0b0c, 0x0d0e0f10};
    byte [] buf = new byte [intbuf.length * 4];
    for (int i = 0; i < intbuf.length; i++)
      {
	buf[i*4] = (byte)((intbuf[i] & 0xff000000) >>> 24);
	buf[i*4+1] = (byte)((intbuf[i] & 0x00ff0000) >>> 16);
	buf[i*4+2] = (byte)((intbuf[i] & 0x0000ff00) >>> 8);
	buf[i*4+3] = (byte)(intbuf[i] & 0x000000ff);
      }
    ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, buf.length);
    abb.order(byteOrder);
    Value c1 = new Value(arrayType, "a1", abb);
    String s = c1.toString();
    assertEquals ("array1dim", "{16909060,84281096,151653132,219025168}", s);
  }

  /**
   * int[][]
   */
  public void testArrayOfArrayOfNumber ()
  {
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    ArrayList dims = new ArrayList();
    dims.add(new Integer(1));
    dims.add(new Integer(3));
    ArrayType arrayType = new ArrayType(intType, 32, dims);
    int [] intbuf = {0x0, 0x1, 0x2, 0x3, 0x0, 0x10, 0x20, 0x30};
    byte [] buf = new byte [intbuf.length * 4];
    for (int i = 0; i < intbuf.length; i++)
      {
	buf[i*4] = (byte)((intbuf[i] & 0xff000000) >>> 24);
	buf[i*4+1] = (byte)((intbuf[i] & 0x00ff0000) >>> 16);
	buf[i*4+2] = (byte)((intbuf[i] & 0x0000ff00) >>> 8);
	buf[i*4+3] = (byte)(intbuf[i] & 0x000000ff);
      }
    ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, buf.length);
    abb.order(byteOrder);
    Value c1 = new Value(arrayType, "a2", abb);
    String s = c1.toString();
    assertEquals ("array2dim", "{{0,1,2,3},{0,16,32,48}}", s);
  }

  /**
   * struct {int; int; short; int:8; int:8;}
   */
  public void testStructure ()
  {
    // Also separate tests for 0 dimensioned arrays et.al.
    ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    ClassType classType = new ClassType(byteOrder);
    classType.addMember(intType, "alpha", 0, 0);
    classType.addMember(intType, "beta", 4, 0);
    classType.addMember(shortType, "gamma", 8, 0);
    classType.addMember(intType, "iota", 8, 0x0000ff00);
    classType.addMember(intType, "epsilon", 8, 0x000000ff);
    int [] intbuf = {0x1020304, 0x5060708, 0x9101112};
    byte [] buf = new byte [intbuf.length * 4];
    for (int i = 0; i < intbuf.length; i++)
      {
	buf[i*4] = (byte)((intbuf[i] & 0xff000000) >>> 24);
	buf[i*4+1] = (byte)((intbuf[i] & 0x00ff0000) >>> 16);
	buf[i*4+2] = (byte)((intbuf[i] & 0x0000ff00) >>> 8);
	buf[i*4+3] = (byte)(intbuf[i] & 0x000000ff);
      }
    ArrayByteBuffer abb = new ArrayByteBuffer(buf, 0, buf.length);
    abb.order(byteOrder);
    Value c1 = new Value(classType, "c1", abb);
    String s = c1.toString();
    assertEquals ("class", "{alpha=16909060,beta=84281096,gamma=2320,iota=17,epsilon=18}", s);
  }
}
