// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import inua.eio.ByteOrder;
import java.util.HashMap;

public class StandardTypes {
    public static final CharType byteLittleEndianType
	= new CharType("byte", ByteOrder.LITTLE_ENDIAN, 1, true);
    public static final CharType byteBigEndianType
	= new CharType("byte", ByteOrder.BIG_ENDIAN, 1, true);

    public static CharType getByteType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return byteLittleEndianType;
	else
	    return byteBigEndianType;
    }

    public static final SignedType shortLittleEndianType
	= new SignedType("short", ByteOrder.LITTLE_ENDIAN, 2);
    public static final SignedType shortBigEndianType
	= new SignedType("short", ByteOrder.BIG_ENDIAN, 2);

    public static SignedType getShortType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return shortLittleEndianType;
	else
	    return shortBigEndianType;
    }

    public static final SignedType intLittleEndianType
	= new SignedType("int", ByteOrder.LITTLE_ENDIAN, 4);
    public static final SignedType intBigEndianType
	= new SignedType("int", ByteOrder.BIG_ENDIAN, 4);

    public static SignedType getIntType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return intLittleEndianType;
	else
	    return intBigEndianType;
    }

    public static final SignedType longLittleEndianType
	= new SignedType("long", ByteOrder.LITTLE_ENDIAN, 8);
    public static final SignedType longBigEndianType
	= new SignedType("long", ByteOrder.BIG_ENDIAN, 8);

    public static SignedType getLongType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return longLittleEndianType;
	else
	    return longBigEndianType;
    }

    public static final FloatingPointType floatLittleEndianType
	= new FloatingPointType("float", ByteOrder.LITTLE_ENDIAN, 4);
    public static final FloatingPointType floatBigEndianType
	= new FloatingPointType("float", ByteOrder.BIG_ENDIAN, 4);

    public static FloatingPointType getFloatType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return floatLittleEndianType;
	else
	    return floatBigEndianType;
    }

    public static final FloatingPointType doubleLittleEndianType
	= new FloatingPointType("double", ByteOrder.LITTLE_ENDIAN, 8);
    public static final FloatingPointType doubleBigEndianType
	= new FloatingPointType("double", ByteOrder.BIG_ENDIAN, 8);

    public static FloatingPointType getDoubleType(ByteOrder order) {
	if (order == ByteOrder.LITTLE_ENDIAN)
	    return doubleLittleEndianType;
	else
	    return doubleBigEndianType;
    }


    private static class OrderMap extends HashMap {
	static final long serialVersionUID = 0;
	OrderMap put(ArithmeticType type) {
	    super.put(type.order(), type);
	    return this;
	}
	ArithmeticType get(ByteOrder order) {
	    return (ArithmeticType)super.get(order);
	}
    }

    public static final ArithmeticType INT8B_T
	= new SignedType("int8b_t", ByteOrder.BIG_ENDIAN, 1);
    public static final ArithmeticType INT8L_T
	= new SignedType("int8l_t", ByteOrder.LITTLE_ENDIAN, 1);
    private static final OrderMap int8_t = new OrderMap()
	.put(INT8B_T).put(INT8L_T);
    public static ArithmeticType int8_t(ByteOrder order) {
	return int8_t.get(order);
    }

    public static final ArithmeticType INT16B_T
	= new SignedType("int16b_t", ByteOrder.BIG_ENDIAN, 2);
    public static final ArithmeticType INT16L_T
	= new SignedType("int16l_t", ByteOrder.LITTLE_ENDIAN, 2);
    private static final OrderMap int16_t = new OrderMap()
	.put(INT16B_T).put(INT16L_T);
    public static ArithmeticType int16_t(ByteOrder order) {
	return int16_t.get(order);
    }

    public static final ArithmeticType INT32B_T
	= new SignedType("int32b_t", ByteOrder.BIG_ENDIAN, 4);
    public static final ArithmeticType INT32L_T
	= new SignedType("int32l_t", ByteOrder.LITTLE_ENDIAN, 4);
    private static final OrderMap int32_t = new OrderMap()
	.put(INT32B_T).put(INT32L_T);
    public static ArithmeticType int32_t(ByteOrder order) {
	return int32_t.get(order);
    }

    public static final ArithmeticType INT64B_T
	= new SignedType("int64b_t", ByteOrder.BIG_ENDIAN, 8);
    public static final ArithmeticType INT64L_T
	= new SignedType("int64l_t", ByteOrder.LITTLE_ENDIAN, 8);
    private static final OrderMap int64_t = new OrderMap()
	.put(INT64B_T).put(INT64L_T);
    public static ArithmeticType int64_t(ByteOrder order) {
	return int64_t.get(order);
    }

    public static final ArithmeticType INT128B_T
	= new SignedType("int128b_t", ByteOrder.BIG_ENDIAN, 16);
    public static final ArithmeticType INT128L_T
	= new SignedType("int128l_t", ByteOrder.LITTLE_ENDIAN, 16);
    private static final OrderMap int128_t = new OrderMap()
	.put(INT128B_T).put(INT128L_T);
    public static ArithmeticType int128_t(ByteOrder order) {
	return int128_t.get(order);
    }

    public static final ArithmeticType UINT8B_T
	= new UnsignedType("uint8b_t", ByteOrder.BIG_ENDIAN, 1);
    public static final ArithmeticType UINT8L_T
	= new UnsignedType("uint8l_t", ByteOrder.LITTLE_ENDIAN, 1);
    private static final OrderMap uint8_t = new OrderMap()
	.put(UINT8B_T).put(UINT8L_T);
    public static ArithmeticType uint8_t(ByteOrder order) {
	return uint8_t.get(order);
    }

    public static final ArithmeticType UINT16B_T
	= new UnsignedType("uint16b_t", ByteOrder.BIG_ENDIAN, 2);
    public static final ArithmeticType UINT16L_T
	= new UnsignedType("uint16l_t", ByteOrder.LITTLE_ENDIAN, 2);
    private static final OrderMap uint16_t = new OrderMap()
	.put(UINT16B_T).put(UINT16L_T);
    public static ArithmeticType uint16_t(ByteOrder order) {
	return uint16_t.get(order);
    }

    public static final ArithmeticType UINT32B_T
	= new UnsignedType("uint32b_t", ByteOrder.BIG_ENDIAN, 4);
    public static final ArithmeticType UINT32L_T
	= new UnsignedType("uint32l_t", ByteOrder.LITTLE_ENDIAN, 4);
    private static final OrderMap uint32_t = new OrderMap()
	.put(UINT32B_T).put(UINT32L_T);
    public static ArithmeticType uint32_t(ByteOrder order) {
	return uint32_t.get(order);
    }

    public static final ArithmeticType UINT64B_T
	= new UnsignedType("uint64b_t", ByteOrder.BIG_ENDIAN, 8);
    public static final ArithmeticType UINT64L_T
	= new UnsignedType("uint64l_t", ByteOrder.LITTLE_ENDIAN, 8);
    private static final OrderMap uint64_t = new OrderMap()
	.put(UINT64B_T).put(UINT64L_T);
    public static ArithmeticType uint64_t(ByteOrder order) {
	return uint64_t.get(order);
    }

    public static final ArithmeticType UINT128B_T
	= new UnsignedType("uint128b_t", ByteOrder.BIG_ENDIAN, 16);
    public static final ArithmeticType UINT128L_T
	= new UnsignedType("uint128l_t", ByteOrder.LITTLE_ENDIAN, 16);
    private static final OrderMap uint128_t = new OrderMap()
	.put(UINT128B_T).put(UINT128L_T);
    public static ArithmeticType uint128_t(ByteOrder order) {
	return uint128_t.get(order);
    }

    public static final ArithmeticType FLOAT32B_T
	= new FloatingPointType("float32b_t", ByteOrder.BIG_ENDIAN, 4);
    public static final ArithmeticType FLOAT32L_T
	= new FloatingPointType("float32l_t", ByteOrder.LITTLE_ENDIAN, 4);
    private static final OrderMap float32_t = new OrderMap()
	.put(FLOAT32B_T).put(FLOAT32L_T);
    public static ArithmeticType float32_t(ByteOrder order) {
	return float32_t.get(order);
    }

    public static final ArithmeticType FLOAT64B_T
	= new FloatingPointType("float64b_t", ByteOrder.BIG_ENDIAN, 8);
    public static final ArithmeticType FLOAT64L_T
	= new FloatingPointType("float64l_t", ByteOrder.LITTLE_ENDIAN, 8);
    private static final OrderMap float64_t = new OrderMap()
	.put(FLOAT64B_T).put(FLOAT64L_T);
    public static ArithmeticType float64_t(ByteOrder order) {
	return float64_t.get(order);
    }

    public static final ArithmeticType FLOAT80B_T
	= new FloatingPointType("float80b_t", ByteOrder.BIG_ENDIAN, 10);
    public static final ArithmeticType FLOAT80L_T
	= new FloatingPointType("float80l_t", ByteOrder.LITTLE_ENDIAN, 10);
    private static final OrderMap float80_t = new OrderMap()
	.put(FLOAT80B_T).put(FLOAT80L_T);
    public static ArithmeticType float80_t(ByteOrder order) {
	return float80_t.get(order);
    }

    public static final ArithmeticType VOIDPTR32B_T
	= new PointerType("voidptr32b_t", ByteOrder.BIG_ENDIAN, 4,
			  new VoidType());
    public static final ArithmeticType VOIDPTR32L_T
	= new PointerType("voidptr32l_t", ByteOrder.LITTLE_ENDIAN, 4,
			  new VoidType());
    public static final OrderMap voidptr32_t = new OrderMap()
	.put(VOIDPTR32B_T).put(VOIDPTR32L_T);
    public static ArithmeticType voidptr32_t(ByteOrder order) {
	return voidptr32_t.get(order);
    }
	
    public static final ArithmeticType VOIDPTR64B_T
	= new PointerType("VOIDPTR64B_T", ByteOrder.BIG_ENDIAN, 8,
			  new VoidType());
    public static final ArithmeticType VOIDPTR64L_T
	= new PointerType("VOIDPTR64L_T", ByteOrder.LITTLE_ENDIAN, 8,
			  new VoidType());
    public static final OrderMap voidptr64_t = new OrderMap()
	.put(VOIDPTR64B_T).put(VOIDPTR64L_T);
    public static ArithmeticType voidptr64_t(ByteOrder order) {
	return voidptr64_t.get(order);
    }
	
}
