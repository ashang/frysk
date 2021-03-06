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
 * Type for a floating-point value.
 */

public class FloatingPointType
    extends ArithmeticType
{    
	
    private FloatingPointFormat format;
	
    public FloatingPointType(String name, ByteOrder order, int size) {
	super(name, order, size);
	
	switch (size) {
	case 4: format = FloatingPoint854Format.IEEE32;
	break;
	case 8: format = FloatingPoint854Format.IEEE64;
	break;
	case 16:format = FloatingPoint854Format.IEEE128;
	break;
	case 10:format = FloatingPoint854Format.IEEE80;
	break;
	case 12:format = FloatingPoint854Format.IEEE96;
	break;
	default:format = FloatingPoint854Format.IEEE64;
	break;
	}
    }

    public void toPrint(PrintWriter writer, Location location,
			ByteBuffer memory, Format format, int indent) {
	// double-dispatch.
	format.print(writer, location, this);
    }
    
    /**
     * Prints value as a hexadecimal float constant. 
     * eg. double value 9.0 --> 0x1.2p+0
     */
    public void printAsHexConstant (PrintWriter writer, Location loc) {
	byte[] bytes = loc.get(order());
	FloatingPoint854Format f = (FloatingPoint854Format)format;
	writer.print(f.getSign(bytes)==0? "":"-");
	writer.print("0x");
	writer.print(f.getIntegralOfMantissa(bytes));
	writer.print('.');
	writer.print(f.getFraction(bytes).toString(16));
	writer.print('p');
	writer.print(f.getBiasedExponent(bytes).intValue() 
		     - f.getMaxEValue().intValue()/2);
    }
    
    /**
     * Return the raw bytes as an unsigned integer.
     */
    BigInteger getBigInteger(Location location) {
	return new BigInteger(1, location.get(order()));
    }

    void putBigInteger(Location location, BigInteger val) {
	location.put(order(), val.toByteArray(), 0);
    }

    void putBigFloatingPoint(Location location, BigFloatingPoint val) {
	location.put(order(), this.format.pack(val, this.getSize()), 0);
    }
   
    BigFloatingPoint getBigFloatingPoint(Location location) {
    	return format.unpack(location.get(order()));
    }

    BigFloatingPoint bigFloatingPointValue(Location location) {
    	return getBigFloatingPoint(location);
    }

    BigInteger bigIntegerValue (Location location) {
	return getBigFloatingPoint(location).bigIntegerValue();
    }

    void assign(Location location, Value v) {
    	BigFloatingPoint f = ((FloatingPointType)v.getType()).bigFloatingPointValue(v.getLocation());
    	location.put(order(), format.pack(f, getSize()), 0);
    }
    
    /* getALUs are double dispatch functions to determine 
     * the ArithmeticUnit for an operation between two types.
     */
    public ArithmeticUnit getALU(Type type, int wordSize) {
	return type.getALU(this, wordSize);
    }    
    public ArithmeticUnit getALU(IntegerType type, int wordSize) {
	return new FloatingPointUnit(this, wordSize);
    }    
    public ArithmeticUnit getALU(FloatingPointType type, int wordSize) {
	return new FloatingPointUnit(this, type, wordSize);
    }       
    public ArithmeticUnit getALU(PointerType type, int wordSize) {
	throw new RuntimeException("Invalid Pointer Arithmetic");
    }    
    // Use for unary operations.
    public ArithmeticUnit getALU(int wordSize) {
	return new FloatingPointUnit(this, wordSize);
    }    
}
