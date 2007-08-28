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

import lib.dwfl.BaseTypes;
import inua.eio.ByteOrder;
import frysk.junit.TestCase;

public class TestValue
    extends TestCase
{
    ArithmeticType longBEType = new SignedType(8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeLong, "long", false);
    ArithmeticType longBEUnsignedType = new UnsignedType(8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedLong, "unsigned long", false);
    ArithmeticType intBEType = new SignedType(4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeInteger, "int", false);
    ArithmeticType intBEUnsignedType = new UnsignedType(4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedInteger, "unsigned int", false);
    ArithmeticType shortBEType = new SignedType(2, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeShort, "short", false);
    ArithmeticType shortBEUnsignedType = new UnsignedType(2, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedShort, "unsigned short", false);
    ArithmeticType byteBEType = new SignedType(1, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeByte, "byte", false);
    ArithmeticType byteBEUnsignedType = new UnsignedType(1, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeUnsignedByte, "unsigned byte", false);
    ArithmeticType floatBEType = new FloatingPointType(4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeFloat, "float", false);
    ArithmeticType doubleBEType = new FloatingPointType(8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeDouble, "double", false);
  
    ArithmeticType longType = new SignedType(8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeLong, "long", false);
    ArithmeticType longUnsignedType = new UnsignedType(8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedLong, "unsigned long", false);
    ArithmeticType intType = new SignedType(4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeInteger, "int", false);
    ArithmeticType intUnsignedType = new UnsignedType(4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedInteger, "unsigned int", false);
    ArithmeticType shortType = new SignedType(2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeShort, "short", false);
    ArithmeticType shortUnsignedType = new UnsignedType(2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedShort, "unsigned short", false);
    ArithmeticType byteType = new SignedType(1, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeByte, "byte", false);
    ArithmeticType byteUnsignedType = new UnsignedType(1, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeUnsignedByte, "unsigned byte", false);
    ArithmeticType floatType = new FloatingPointType(4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeFloat, "float", false);
    ArithmeticType doubleType = new FloatingPointType(8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeDouble, "double", false);
  
    public void testNumber ()
    {
	Value v1 = byteType.createValue(1);
	Value v2 = shortType.createValue(2);
	Value v3 = intType.createValue(3);
	Value v4 = longType.createValue(4);
	Value v5 = byteBEType.createValue(5);
	Value v6 = shortBEType.createValue(6);
	Value v7 = intBEType.createValue(7);
	Value v8 = longBEType.createValue(8);
	// Use 1.0 and 2.0 as they have exact representations in
	// IEEE-754.
	Value f1 = floatType.createValue((float)1.0);
	Value f2 = doubleType.createValue(2.0);
	assertEquals("v1", 1, v1.asLong());
	assertEquals("v2", 2, v2.asLong());
	assertEquals("v3", 3, v3.asLong());
	assertEquals("v4", 4, v4.asLong());
	assertEquals("v5", 5, v5.asLong());
	assertEquals("v6", 6, v6.asLong());
	assertEquals("v7", 7, v7.asLong());
	assertEquals("v8", 8, v8.asLong());
	assertEquals("f1.0", 1.0, 1.0, f1.doubleValue());
	assertEquals("f2.0", 2.0, 2.0, f2.doubleValue());
    }

    private boolean isTrue (long i)
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
    {
	Value v1 = intType.createValue(4);
	Value v2 = shortType.createValue(9);
	Value v3 = v1.getType().add(v1, v2);
	assertEquals ("4 + 9", 4 + 9, v3.asLong());
	v3 = v1.getType().subtract(v2, v1);
	assertEquals ("9 - 4", 9 - 4, v3.asLong());
	v3 = v1.getType().multiply(v2, v1);
	assertEquals ("9 * 4", 9 * 4, v3.asLong());
	v3 = v1.getType().mod(v2, v1);
	assertEquals ("9 % 4", 9 % 4, v3.asLong());
	v3 = v1.getType().shiftLeft(v2, v1);
	assertEquals ("9 << 4", 9 << 4, v3.asLong());
	v3 = v1.getType().shiftRight(v2, v1);
	assertEquals ("9 >> 4", 9 >> 4, v3.asLong());
	v3 = v1.getType().lessThan(v2, v1);
	assertEquals ("9 < 4", 9 < 4, isTrue(v3.asLong()));
	v3 = v1.getType().greaterThan(v2, v1);
	assertEquals ("9 > 4", 9 > 4, isTrue(v3.asLong()));
	v3 = v1.getType().lessThanOrEqualTo(v2, v1);
	assertEquals ("9 <= 4", 9 <= 4, isTrue(v3.asLong()));
	v3 = v1.getType().greaterThanOrEqualTo(v2, v1);
	assertEquals ("9 >= 4", 9 >= 4, isTrue(v3.asLong()));
	v3 = v1.getType().equal(v2, v1);
	assertEquals ("9 == 4", 9 == 4, isTrue(v3.asLong()));
	v3 = v1.getType().notEqual(v2, v1);
	assertEquals ("9 != 4", 9 != 4, isTrue(v3.asLong()));
	v3 = v1.getType().bitWiseAnd(v2, v1);
	assertEquals ("9 && 4", 9 & 4, v3.asLong());
	v3 = v1.getType().bitWiseOr(v2, v1);
	assertEquals ("9 || 4", 9 | 4, v3.asLong());
	v3 = v1.getType().bitWiseXor(v2, v1);
	assertEquals ("9 ^ 4", 9 ^ 4, v3.asLong());
	v3 = v1.getType().bitWiseComplement(v1);
	assertEquals ("~4", ~4, v3.asLong());
	v3 = v1.getType().logicalAnd(v2, v1);
	assertEquals ("9 & 4", 1, v3.asLong());
	v3 = v1.getType().logicalOr(v2, v1);
	assertEquals ("9 | 4", 1, v3.asLong());
	v3 = v3.assign(v1);
	assertEquals ("v3 = 4", 4, v3.asLong());
	v3 = v1.getType().plusEqual(v3, v1);
	assertEquals ("v3 += 4", 8, v3.asLong());
	v3 = v1.getType().minusEqual(v3, v1);
	assertEquals ("v3 -= 4", 4, v3.asLong());
	v3 = v1.getType().timesEqual(v3, v1);
	assertEquals ("v3 *= 4", 16, v3.asLong());
	v3 = v1.getType().divideEqual(v3, v1);
	assertEquals ("v3 /= 4", 4, v3.asLong());
	v3 = v1.getType().modEqual(v3, v1);
	assertEquals ("v3 %= 4", 0, v3.asLong());
	v3 = v1.getType().shiftLeftEqual(v3, v1);
	assertEquals ("v3 <<= 4", 0, v3.asLong());
	v3 = v1.getType().shiftRightEqual(v3, v1);
	assertEquals ("v3 >>= 4", 0, v3.asLong());
	v3 = v1.getType().bitWiseOrEqual(v3, v1);
	assertEquals ("v3 ||= 4", 4, v3.asLong());
	v3 = v1.getType().bitWiseXorEqual(v3, v1);
	assertEquals ("v3 ^= 4", 0, v3.asLong());
	v3 = v1.getType().bitWiseAndEqual(v3, v1);
	assertEquals ("v3 &&= 4", 0, v3.asLong());
    }

    public void testFloatOps ()
    {
	Value v1 = floatType.createValue((float)4.0);
	Value v2 = doubleType.createValue(9.0);
	Value v3 = v1.getType().add(v1, v2);
	assertEquals ("4 + 9", 4 + 9, v3.doubleValue(), 0);
	v3 = v1.getType().subtract(v2, v1);
	assertEquals ("9 - 4", 9 - 4, v3.doubleValue(), 0);
	v3 = v1.getType().multiply(v2, v1);
	assertEquals ("9 * 4", 9 * 4, v3.doubleValue(), 0);
	v3 = v1.getType().mod(v2, v1);
	assertEquals ("9 % 4", 9 % 4, v3.doubleValue(), 0);
	v3 = v1.getType().lessThan(v2, v1);
	assertEquals ("9 < 4", 9 < 4, isTrue(v3.doubleValue()));
	v3 = v1.getType().greaterThan(v2, v1);
	assertEquals ("9 > 4", 9 > 4, isTrue(v3.doubleValue()));
	v3 = v1.getType().lessThanOrEqualTo(v2, v1);
	assertEquals ("9 <= 4", 9 <= 4, isTrue(v3.doubleValue()));
	v3 = v1.getType().greaterThanOrEqualTo(v2, v1);
	assertEquals ("9 >= 4", 9 >= 4, isTrue(v3.doubleValue()));
	v3 = v1.getType().equal(v2, v1);
	assertEquals ("9 == 4", 9 == 4, isTrue(v3.doubleValue()));
	v3 = v1.getType().notEqual(v2, v1);
	assertEquals ("9 != 4", v2 != v1, isTrue(v3.doubleValue()));
	v3 = v3.assign(v1);
	assertEquals ("v3 = 4", 4, v3.doubleValue(), 0);
	v3 = v1.getType().plusEqual(v3, v1);
	assertEquals ("v3 += 4", 8, v3.doubleValue(), 0);
	v3 = v1.getType().minusEqual(v3, v1);
	assertEquals ("v3 -= 4", 4, v3.doubleValue(), 0);
	v3 = v1.getType().timesEqual(v3, v1);
	assertEquals ("v3 *= 4", 16, v3.doubleValue(), 0);
	v3 = v1.getType().divideEqual(v3, v1);
	assertEquals ("v3 /= 4", 4, v3.doubleValue(), 0);
	v3 = v1.getType().modEqual(v3, v1);
	assertEquals ("v3 %= 4", 0, v3.doubleValue(), 0);
    }
}
