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
import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * Formats a base type sending it to the printer.
 */

public abstract class Format
{
    private static void printDecimal(PrintWriter writer, Location location,
				     ByteOrder order) {
	writer.print(new BigInteger(location.get(order)).toString());
    }
    private static void printHexadecimal(PrintWriter writer, Location location,
					 ByteOrder order) {
	writer.print("0x");
	writer.print(new BigInteger(1, location.get(order)).toString(16));
    }
    private static void printOctal(PrintWriter writer, Location location,
				   ByteOrder order) {
	writer.print("0");
	writer.print(new BigInteger(1, location.get(order)).toString(8));
    }
    private static void printBinary(PrintWriter writer, Location location,
				    ByteOrder order) {
	writer.print(new BigInteger(1, location.get(order)).toString(2));
    }
    private static void printFloatingPoint(PrintWriter writer,
					   Location location,
					   ArithmeticType type) {
	BigFloat f = type.getBigFloat(location);
	if (type.getSize() < 8)
	    writer.print(f.floatValue());
	else
	    writer.print(f.doubleValue());
    }

    /**
     * Print the integer at LOCATION.
     */
    abstract void print(PrintWriter writer, Location location,
			IntegerType type);

    /**
     * Print the floating-point at LOCATION.
     */
    abstract void print(PrintWriter writer, Location location,
			FloatingPointType type);

    /**
     * Print the pointer at location.
     */
    abstract void print(PrintWriter writer, Location location,
			PointerType type);

    public static final Format NATURAL = new Format() {
	    void print(PrintWriter writer, Location location,
		       IntegerType type) {
		writer.print(type.getBigInteger(location).toString());
	    }
	    void print(PrintWriter writer, Location location,
		       FloatingPointType type) {
		printFloatingPoint(writer, location, type);
	    }
	    void print(PrintWriter writer, Location location,
		       PointerType type) {
		printHexadecimal(writer, location, type.getEndian());
	    }
	};
    public static final Format HEXADECIMAL = new Format() {
	    void print(PrintWriter writer, Location location,
		       IntegerType type) {
		printHexadecimal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       FloatingPointType type) {
		printHexadecimal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       PointerType type) {
		printHexadecimal(writer, location, type.getEndian());
	    }
	};
    public static final Format OCTAL = new Format() {
	    void print(PrintWriter writer, Location location,
		       IntegerType type) {
		printOctal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       FloatingPointType type) {
		printOctal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       PointerType type) {
		printOctal(writer, location, type.getEndian());
	    }
	};
    public static final Format DECIMAL = new Format() {
	    void print(PrintWriter writer, Location location,
		       IntegerType type) {
		printDecimal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       FloatingPointType type) {
		printDecimal(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       PointerType type) {
		printDecimal(writer, location, type.getEndian());
	    }
	};
    public static final Format BINARY = new Format() {
	    void print(PrintWriter writer, Location location,
		       IntegerType type) {
		printBinary(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       FloatingPointType type) {
		printBinary(writer, location, type.getEndian());
	    }
	    void print(PrintWriter writer, Location location,
		       PointerType type) {
		printBinary(writer, location, type.getEndian());
	    }
	};
}
