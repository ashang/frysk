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
import java.math.BigInteger;

/**
 * Decorate an IntegerType (signed or unsigned).
 *
 * For instance a Char, which can be either be a SignedType or an
 * UnsignedType, can be implemented by decorating that type.
 */
abstract class IntegerTypeDecorator
    extends IntegerType
{    
    // Used to perform get/put/pack operations.
    private final IntegerType accessor;
    protected IntegerTypeDecorator(String name, ByteOrder order, int size,
				   IntegerType accessor) {
	super(name, order, size);
	this.accessor = accessor;
    }
    BigInteger getBigInteger(Location location) {
	return accessor.getBigInteger(location);
    }
    void putBigInteger(Location location, BigInteger val) {
	accessor.putBigInteger(location, val);
    }
    /**
     * Create a clone of this type, but with the specified acdcessor.
     */
    protected abstract Type clone(IntegerType accessor);
    /**
     * Pack the type into bitfields.
     */
    public Type pack(int bitSize, int bitOffset) {
	// An alternative implementation would be to make this
	// cloneable and use Object.clone.
	return clone((IntegerType)accessor.pack(bitSize, bitOffset));
    }
}
