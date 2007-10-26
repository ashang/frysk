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
import java.util.ArrayList;
import frysk.junit.TestCase;

public class TestArray extends TestCase {
    private final Type int4_t = new SignedType("int", ByteOrder.BIG_ENDIAN, 4);
    private final Type int2_t = new SignedType("int", ByteOrder.BIG_ENDIAN, 2);
    private final byte[] buf = new byte[] {
	0x01, 0x02, 0x03, 0x04,
	0x05, 0x06, 0x07, 0x08,
	0x09, 0x0a, 0x0b, 0x0c,
	0x0d, 0x0e, 0x0f, 0x10
    };

    /**
     * int4_t[4] array 
     */
    public void testArrayOfNumber () {
	ArrayList dims = new ArrayList();
	dims.add(new Integer(4 - 1));
	ArrayType arrayType = new ArrayType(int4_t, buf.length, dims);
	Value c1 = new Value(arrayType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("int[]", "{16909060,84281096,151653132,219025168}", s);
    }

    /**
     * int2_t[2][4]
     */
    public void testArrayOfArrayOfNumber () {
	ArrayList dims = new ArrayList();
	dims.add(new Integer(2-1));
	dims.add(new Integer(4-1));
	ArrayType arrayType = new ArrayType(int2_t, 16, dims);
	Value c1 = new Value(arrayType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("array2dim", "{{258,772,1286,1800},{2314,2828,3342,3856}}", s);
	ArrayList components = new ArrayList();
	components.add(new String("a2"));
	components.add(new String("1"));
	components.add(new String("1"));
	components.add(new String("2"));
	components.add(new String("2"));
	Value c2 = arrayType.get (c1, 1, components);
	s = c2.toPrint();
	assertEquals ("1 element slice", "3342", s);
	components.remove(4);
	components.add(new String("3"));
	Value c3 = arrayType.get (c1, 1, components);
	s = c3.toPrint();
	assertEquals ("2 element slice", "{3342,3856}", s);
    }

    /**
     * Check that an array of characters is treated special.
     */
    public void testString() {
	// Create a string with a '0' in the middle
	byte[] helloWorld = "Hello World".getBytes();
	helloWorld["Hello".length()] = 0;
	// Create a string value
	ArrayList dims = new ArrayList();
	dims.add(new Integer(helloWorld.length - 1));
	Type char_t = new CharType("char", ByteOrder.BIG_ENDIAN, 1, true);
	ArrayType t = new ArrayType(char_t, helloWorld.length, dims);
	Value v = new Value(t, new ScratchLocation(helloWorld));
	// Now print it
	assertEquals("char[]", "\"Hello\"", v.toPrint());
    }
    
    /**
     * Test for index-of operation.
     */
    public void testIndexOneD() {
	ArrayList dims = new ArrayList();
	dims.add(new Integer(4 - 1));
	ArrayType arrayType = new ArrayType(int4_t, buf.length , dims);
	Value arr = new Value(arrayType, new ScratchLocation(buf));
	Location l = new ScratchLocation(new byte[] { 2 });
	IntegerType t = new UnsignedType("type", ByteOrder.BIG_ENDIAN, 1);
	Value index = new Value(t, l);
	assertEquals("IndexOneD", 151653132, arrayType.index(arr, index).asLong());
    }
    
   public void testIndexTwoD() {
        // Create array
	ArrayList dims = new ArrayList();
	dims.add(new Integer(2-1));
	dims.add(new Integer(4-1));
	ArrayType arrayType = new ArrayType(int2_t, 16, dims);
	Value arr = new Value(arrayType, new ScratchLocation(buf));
	// Create indices 1 and 3
	Location l_idx1 = new ScratchLocation(new byte[] { 1 });
	IntegerType t = new UnsignedType("type", ByteOrder.BIG_ENDIAN, 1);
	Value idx_1 = new Value(t, l_idx1);
	Location l_idx3 = new ScratchLocation(new byte[] { 3 });
	Value idx_3 = new Value(t, l_idx3);
	// Evaluate arr[1]
	Value arr_1 = arrayType.index(arr, idx_1);
	assertEquals ("IndexTwoD[]", "{2314,2828,3342,3856}", arr_1.toPrint()); 
	// Evaluate arr[1][3]
	Value arr_1_3 = arr_1.getType().index(arr_1, idx_3);
	assertEquals ("IndexTwoD", "3856", arr_1_3.toPrint());
   } 
}
