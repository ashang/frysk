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

public class TestClass
    extends TestCase
{
    private Type bigInt32 = new SignedType("int32_t", ByteOrder.BIG_ENDIAN, 4);
    private Type littleInt32 = new SignedType("int32_t", ByteOrder.LITTLE_ENDIAN, 4);
    private Type bigInt16 = new SignedType("int16_t", ByteOrder.BIG_ENDIAN, 2);
    private Type littleInt16 = new SignedType("int16_t", ByteOrder.LITTLE_ENDIAN, 2);

    /**
     * struct {int; int; short; int:8; int:8;}
     */
    public void testBigStructure () {
	ClassType classType = new ClassType(null, 12)
	    .addMember("alpha", bigInt32, 0, 0)
	    .addMember("beta", bigInt32, 4, 0)
	    .addMember("gamma", bigInt16, 8, 0)
	    .addMember("iota", bigInt32, 8, 0, 16, 8) // 0x0000ff00
	    .addMember("epsilon", bigInt32, 8, 0, 24, 8); // 0x000000ff
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{alpha=16909060,\n beta=84281096,\n gamma=2320,\n iota=17,\n epsilon=18,\n}", s);
    }
    /**
     * struct {int; int; short; int:8; int:8;}
     */
    public void testLittleStructure () {
	ClassType classType = new ClassType(null, 12)
	    .addMember("alpha", littleInt32, 0, 0)
	    .addMember("beta", littleInt32, 4, 0)
	    .addMember("gamma", littleInt16, 8, 0)
	    .addMember("iota", littleInt32, 8, 0, 8, 8) // 0x00ff0000
	    .addMember("epsilon", littleInt32, 8, 0, 0, 8); // 0xff000000
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{alpha=67305985,\n beta=134678021,\n gamma=4105,\n iota=17,\n epsilon=18,\n}", s);
    }
    /**
     * struct { struct { int, int } struct { short, int:8, int:8 }}
     */
    public void testNextedStructure () {
	ClassType classType = new ClassType(null, 12)
	    .addMember("a", new ClassType(null, 8)
		       .addMember("alpha", littleInt32, 0, 0)
		       .addMember("beta", littleInt32, 4, 0),
		       0, 0)
	    .addMember("b", new ClassType(null, 4)
		       .addMember("gamma", littleInt16, 0, 0)
		       .addMember("iota", littleInt32, 0, 0, 8, 8) // 0x00ff0000
		       .addMember("epsilon", littleInt32, 0, 0, 0, 8), // 0xff000000
		       8, 0);
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{a={alpha=67305985,\n beta=134678021,\n},\n b={gamma=4105,\n iota=17,\n epsilon=18,\n},\n}", s);
    }
    /**
     * struct { struct { int, int } struct { short, int:8, int:8 }}
     */
    public void testNamelessFields () {
	ClassType classType = new ClassType(null, 12)
	    .addMember(null, new ClassType(null, 8)
		       .addMember(null, littleInt32, 0, 0)
		       .addMember(null, littleInt32, 4, 0),
		       0, 0)
	    .addMember(null, new ClassType(null, 4)
		       .addMember(null, littleInt16, 0, 0)
		       .addMember(null, littleInt32, 0, 0, 8, 8) // 0x00ff0000
		       .addMember(null, littleInt32, 0, 0, 0, 8), // 0xff000000
		       8, 0);
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{{67305985,\n 134678021,\n},\n {4105,\n 17,\n 18,\n},\n}", s);
    }
}
