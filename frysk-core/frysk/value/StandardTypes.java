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

import frysk.proc.Isa;
import inua.eio.ByteOrder;
import lib.dwfl.BaseTypes;

public class StandardTypes {
    public static final IntegerType byteLittleEndianType = new IntegerType(
	    1, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeByte, "byte", false);

    public static final IntegerType byteBigEndianType = new IntegerType(
	    1, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeByte, "byte", false);

    public static IntegerType getByteType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return byteLittleEndianType;
	else
	    return byteBigEndianType;
    }

    public static final IntegerType shortLittleEndianType = new IntegerType(
	    2, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeShort, "short", false);

    public static final IntegerType shortBigEndianType = new IntegerType(
	    2, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeShort, "short", false);

    public static IntegerType getShortType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return shortLittleEndianType;
	else
	    return shortBigEndianType;
    }

    public static final IntegerType intLittleEndianType = new IntegerType(
	    4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeInteger, "int", false);

    public static final IntegerType intBigEndianType = new IntegerType(
	    4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeInteger, "int", false);

    public static IntegerType getIntType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return intLittleEndianType;
	else
	    return intBigEndianType;
    }

    public static final IntegerType longLittleEndianType = new IntegerType(
	    8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeLong, "long", false);

    public static final IntegerType longBigEndianType = new IntegerType(
	    8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeLong, "long", false);

    public static IntegerType getLongType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return longLittleEndianType;
	else
	    return longBigEndianType;
    }

    public static final FloatingPointType floatLittleEndianType = new FloatingPointType(
	    4, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeFloat, "float", false);

    public static final FloatingPointType floatBigEndianType = new FloatingPointType(
	    4, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeFloat, "float", false);

    public static FloatingPointType getFloatType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return floatLittleEndianType;
	else
	    return floatBigEndianType;
    }

    public static final FloatingPointType doubleLittleEndianType = new FloatingPointType(
	    8, ByteOrder.LITTLE_ENDIAN, BaseTypes.baseTypeDouble, "double", false);

    public static final FloatingPointType doubleBigEndianType = new FloatingPointType(
	    8, ByteOrder.BIG_ENDIAN, BaseTypes.baseTypeDouble, "double", false);

    public static FloatingPointType getDoubleType(Isa isa) {
	if (isa.getByteOrder() == ByteOrder.LITTLE_ENDIAN)
	    return doubleLittleEndianType;
	else
	    return doubleBigEndianType;
    }
}
