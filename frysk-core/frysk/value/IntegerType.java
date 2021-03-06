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
import inua.eio.ByteBuffer;
import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * Type for an integer value.
 */
public abstract class IntegerType
    extends ArithmeticType
{    
    protected IntegerType(String name, ByteOrder order, int size) {
	super(name, order, size);
    }

    void toPrint(PrintWriter writer, Location location,
		 ByteBuffer memory, Format format, int indent) {
	// double-dispatch.
	format.print(writer, location, this);
    }

    BigInteger bigIntegerValue(Location location) {
	return getBigInteger(location);
    }
    
    BigFloatingPoint getBigFloatingPoint(Location location) {
    	return new BigFloatingPoint(bigIntegerValue(location));
    }
    BigFloatingPoint bigFloatingPointValue(Location location) {
    	return new BigFloatingPoint(getBigInteger(location));
    }
    
    void putBigFloatingPoint(Location location, BigFloatingPoint val) {
    	putBigInteger (location, val.bigIntegerValue());
    }
    
    void assign(Location location, Value value) {
	BigInteger i = ((ArithmeticType)value.getType())
	    .bigIntegerValue(value.getLocation());
	// Let sub-type sort out sign extension et.al.
	putBigInteger(location, i);
    }

    /* getALUs are double dispatch functions to determine 
     * the ArithmeticUnit for an operation between two types.
     */
    public ArithmeticUnit getALU(Type type, int wordSize) {
	return type.getALU(this, wordSize);
    }    
    public ArithmeticUnit getALU(IntegerType type, int wordSize) {
	return new IntegerUnit(this, type, wordSize);
    }    
    public ArithmeticUnit getALU(PointerType type, int wordSize) {
	return new AddressUnit(type, wordSize);
    }    
    public ArithmeticUnit getALU(FloatingPointType type, int wordSize) {
	return new FloatingPointUnit(type, wordSize);
    }   
    public ArithmeticUnit getALU(ArrayType type, int wordSize) {
	return new AddressUnit(type, wordSize);
    }  
    // Use for unary operations.
    public ArithmeticUnit getALU(int wordSize) {
	return new IntegerUnit(this, wordSize);
    }    
}
