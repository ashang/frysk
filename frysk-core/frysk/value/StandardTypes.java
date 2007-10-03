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
}
