// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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
import frysk.scopes.SourceLocation;

public class TestComposite
    extends TestCase
{
    private Type bigInt32 = new SignedType("int32_t", ByteOrder.BIG_ENDIAN, 4);
    private Type littleInt32 = new SignedType("int32_t", ByteOrder.LITTLE_ENDIAN, 4);
    private Type bigInt16 = new SignedType("int16_t", ByteOrder.BIG_ENDIAN, 2);
    private Type littleInt16 = new SignedType("int16_t", ByteOrder.LITTLE_ENDIAN, 2);
    
    private final SourceLocation scratchSourceLocation = SourceLocation.UNKNOWN;
    /**
     * struct {int; int; short; int:8; int:8;}
     */
    public void testBigStructure () {
	CompositeType classType = new ClassType(null, 12)
	    .addMember("alpha", scratchSourceLocation, bigInt32, 0, null)
	    .addMember("beta", scratchSourceLocation, bigInt32, 4, null)
	    .addMember("gamma", scratchSourceLocation, bigInt16, 8, null)
	    .addBitFieldMember("iota", scratchSourceLocation, bigInt32, 8, null, 16, 8) // 0x0000ff00
	    .addBitFieldMember("epsilon", scratchSourceLocation, bigInt32, 8, null, 24, 8); // 0x000000ff
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{\n  alpha=16909060,\n  beta=84281096,\n  gamma=2320,\n  iota=17,\n  epsilon=18,\n}", s);
    }
    /**
     * struct {int; int; short; int:8; int:8;}
     */
    public void testLittleStructure () {
	CompositeType classType = new ClassType(null, 12)
	    .addMember("alpha", scratchSourceLocation, littleInt32, 0, null)
	    .addMember("beta", scratchSourceLocation, littleInt32, 4, null)
	    .addMember("gamma", scratchSourceLocation, littleInt16, 8, null)
	    .addBitFieldMember("iota", scratchSourceLocation, littleInt32, 8, null, 8, 8) // 0x00ff0000
	    .addBitFieldMember("epsilon", scratchSourceLocation, littleInt32, 8, null, 0, 8); // 0xff000000
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{\n  alpha=67305985,\n  beta=134678021,\n  gamma=4105,\n  iota=17,\n  epsilon=18,\n}", s);
    }
    /**
     * struct { struct { int, int } struct { short, int:8, int:8 }}
     */
    public void testNextedStructure () {
	CompositeType classType = new ClassType(null, 12)
	    .addMember("a", scratchSourceLocation, new ClassType(null, 8)
		       .addMember("alpha", scratchSourceLocation, littleInt32, 0, null)
		       .addMember("beta", scratchSourceLocation, littleInt32, 4, null),
		       0, null)
	    .addMember("b", scratchSourceLocation, new ClassType(null, 4)
		       .addMember("gamma", scratchSourceLocation, littleInt16, 0, null)
		       .addBitFieldMember("iota", scratchSourceLocation, littleInt32, 0, null, 8, 8) // 0x00ff0000
		       .addBitFieldMember("epsilon", scratchSourceLocation, littleInt32, 0, null, 0, 8), // 0xff000000
		       8, null);
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{\n  a={\n    alpha=67305985,\n    beta=134678021,\n  },\n  b={\n    gamma=4105,\n    iota=17,\n    epsilon=18,\n  },\n}", s);
    }
    /**
     * struct { struct { int, int } struct { short, int:8, int:8 }}
     */
    public void testNamelessFields () {
	CompositeType classType = new ClassType(null, 12)
	    .addMember(null, scratchSourceLocation, new ClassType(null, 8)
		       .addMember(null, scratchSourceLocation, littleInt32, 0, null)
		       .addMember(null, scratchSourceLocation, littleInt32, 4, null),
		       0, null)
	    .addMember(null, scratchSourceLocation, new ClassType(null, 4)
		       .addMember(null, scratchSourceLocation, littleInt16, 0, null)
		       .addBitFieldMember(null, scratchSourceLocation, littleInt32, 0, null, 8, 8) // 0x00ff0000
		       .addBitFieldMember(null, scratchSourceLocation, littleInt32, 0, null, 0, 8), // 0xff000000
		       8, null);
	byte[] buf = {
	    0x01, 0x02, 0x03, 0x04, // alpha
	    0x05, 0x06, 0x07, 0x08, // beta
	    0x09, 0x10, 0x11, 0x12  // gama, iota, epsilon
	};
	Value c1 = new Value(classType, new ScratchLocation(buf));
	String s = c1.toPrint();
	assertEquals ("class", "{\n  {\n    67305985,\n    134678021,\n  },\n  {\n    4105,\n    17,\n    18,\n  },\n}", s);
    }

    public void testUnionType() {
	CompositeType t = new UnionType("UNION", 4)
	    .addMember("a", scratchSourceLocation, bigInt32, 0, null);
	assertEquals("toPrint",
		     "union UNION {\n  int32_t a;\n}",
		     t.toPrint());
    }
    public void testClassType() {
	CompositeType t = new ClassType("CLASS", 4)
	    .addMember("a", scratchSourceLocation, bigInt32, 0, null);
	assertEquals("toPrint",
		     "class CLASS {\n  int32_t a;\n}",
		     t.toPrint());
    }
    public void testStructType() {
	CompositeType t = new StructType("STRUCT", 4)
	    .addMember("a", scratchSourceLocation, bigInt32, 0, null);
	assertEquals("toPrint",
		     "struct STRUCT {\n  int32_t a;\n}",
		     t.toPrint());
    }
    public void testConfoundedClassType() {
	CompositeType t = new GccStructOrClassType("CLASS", 4)
	    .addInheritance("XXXX", scratchSourceLocation, new ClassType("P1", 0),
			    0, Access.PUBLIC)
	    .addInheritance("XXXX", scratchSourceLocation, new ClassType("P2", 0),
			    0, Access.PRIVATE);
	assertEquals("toPrint",
		     "class CLASS : public P1, private P2 {\n}",
		     t.toPrint());
    }
    public void testConfoundedStructType() {
	CompositeType t = new GccStructOrClassType("STRUCT", 4)
	    .addMember("a", scratchSourceLocation, bigInt32, 0, null);
	assertEquals("toPrint",
		     "struct STRUCT {\n  int32_t a;\n}",
		     t.toPrint());
    }
    public void testPublicPrivateType() {
	CompositeType t = new StructType("STRUCT", 4)
	    .addMember("pub1", scratchSourceLocation, bigInt32, 0, Access.PUBLIC)
	    .addMember("priv1", scratchSourceLocation, bigInt32, 0, Access.PRIVATE)
	    .addMember("prot1", scratchSourceLocation, bigInt32, 0, Access.PROTECTED)
	    .addMember("pub2", scratchSourceLocation, bigInt32, 0, Access.PUBLIC);
	assertEquals("toPrint",
		     "struct STRUCT {\n"
		     + "  int32_t pub1;\n"
		     + " private:\n"
		     + "  int32_t priv1;\n"
		     + " protected:\n"
		     + "  int32_t prot1;\n"
		     + " public:\n"
		     + "  int32_t pub2;\n"
		     + "}",
		     t.toPrint());
    }
    public void testAnonType() {
	CompositeType t = new StructType(null, 4);
	assertEquals("toPrint", "struct {\n}", t.toPrint());
    }
    
    public void testMember()
    {
	Type t = new StructType("STRUCT", 4)
	    .addMember("pub1", scratchSourceLocation, littleInt32, 0, null)
	    .addMember("priv1", scratchSourceLocation, littleInt32, 4, null)
	    .addMember("prot1", scratchSourceLocation, littleInt16, 8, null);
	
	byte[] buf = {
		      0x01, 0x02, 0x03, 0x04, // pub1
		      0x05, 0x06, 0x07, 0x08, // priv1
		      0x09, 0x10              // prot1
		     };
	Value val = new Value(t, new ScratchLocation(buf));
	
	assertEquals ( "Member1", 67305985, t.member(val, "pub1").asLong());
	assertEquals ( "Member2", 134678021, t.member(val, "priv1").asLong());
	assertEquals ( "Member3", 4105, t.member(val, "prot1").asLong());
    }
}
