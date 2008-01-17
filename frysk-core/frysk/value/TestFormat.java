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
import frysk.junit.TestCase;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Test the formats
 */
public class TestFormat
    extends TestCase
{
    private IntegerType int_t
	= new SignedType("i", ByteOrder.BIG_ENDIAN, 4);
    private IntegerType uint_t
	= new UnsignedType("u", ByteOrder.BIG_ENDIAN, 4);
    private FloatingPointType float_t
	= new FloatingPointType("f", ByteOrder.BIG_ENDIAN, 4);
    private FloatingPointType double_t
	= new FloatingPointType("d", ByteOrder.BIG_ENDIAN, 8);
    private PointerType pointer_t
	= new PointerType("p", ByteOrder.BIG_ENDIAN, 4, int_t);

    private void checkInteger(Format format, byte[] bytes,
			      IntegerType type, String value) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	format.print(writer, new ScratchLocation(bytes), type);
	assertEquals(type.toString(), value, stringWriter.toString());
    }
    private void checkFloatingPoint(Format format, byte[] bytes,
				    FloatingPointType type, String value) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	format.print(writer, new ScratchLocation(bytes), type);
	assertEquals(type.toString(), value, stringWriter.toString());
    }
    private void checkPointer(Format format, byte[] bytes,
			      String value) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	format.print(writer, new ScratchLocation(bytes), pointer_t);
	assertEquals(pointer_t.toString(), value, stringWriter.toString());
    }

    public void checkFormat(Format format, String i, String u, String f,
			    String d, String p) {
	checkInteger(format, new byte[] {
			 (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
		     }, int_t, i);
	checkInteger(format, new byte[] {
			 (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
		     }, uint_t, u);
	checkFloatingPoint(format, TestFloatingPoint854Format.FLOAT_ONE,
			   float_t, f);
	checkFloatingPoint(format, TestFloatingPoint854Format.DOUBLE_TWO,
			   double_t, d);
	checkPointer(format, new byte[] {
			 1, 2, 3, 4
		     }, p);
			       
    }
    public void testNatural() {
	checkFormat(Format.NATURAL, "-1", "4294967295", "1.0", "2.0", "0x1020304");
    }

    public void testDecimal() {
	checkFormat(Format.DECIMAL, "-1", "-1", "1065353216",
		    "4611686018427387904", "16909060");
    }
    public void testHexadecimal() {
	checkFormat(Format.HEXADECIMAL, "0xffffffff", "0xffffffff",
		    "0x3f800000", "0x4000000000000000",
		    "0x1020304");
    }
    public void testOctal() {
	checkFormat(Format.OCTAL, "037777777777", "037777777777",
		    "07740000000", "0400000000000000000000", "0100401404");
    }
    public void testBinary() {
	checkFormat(Format.BINARY,
		    "11111111111111111111111111111111",
		    "11111111111111111111111111111111",
		    "111111100000000000000000000000",
		    "100000000000000000000000000000000000000000000000000000000000000",
		    "1000000100000001100000100");
    }
}
