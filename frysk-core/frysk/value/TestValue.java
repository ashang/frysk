// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

public class TestValue
    extends TestCase
{
    public void testAdd ()
	throws InvalidOperatorException
    {
	Type intType = new IntegerType (4, ByteOrder.BIG_ENDIAN);
	Type shortType = new ShortType (2, ByteOrder.BIG_ENDIAN);
	Variable v1 = IntegerType.newIntegerVariable((IntegerType)intType, 4);
	Variable v2 = ShortType.newShortVariable((ShortType)shortType, (short)9);
	Variable v3 = v1.getType().add(v1, v2);
	assertEquals ("4 + 9", 4 + 9, v3.getInt());
    }

//     /**
//      * Stan I think the way numbers are handled here needs to be
//      * simplified.  For instance, instead of IntegerType, ShortType,
//      * ... ; just have NumberType that can be extended to handle all
//      * the diffeent variants.
//      */
//     public void testNumber ()
//     {
// 	// Create a Number, check toString returns the original
// 	// number.  Do this for all the different number variations -
// 	// big and little endian, 1-8 bytes. ...  The number should
// 	// take it's name.
//     }

//     public void testFloatingPoint ()
//     {
// 	// Create a FloatingPointType, check toString to return the
// 	// original value, well hopefully.
//     }

//     /**
//      * For the ArrayType, need to be able to fetch elements and
//      * iterate over it.
//      */
//     public void testArrayOfNumber ()
//     {
// 	// Create's an array of Number, and then uses toString to
// 	// check its contents.

// 	// Also separate tests for 0 dimentioned arrays et.al.
//     }

//     /**
//      * For an array, needs to be recursive.
//      */
//     public void testArrayOfArrayOfNumber ()
//     {
// 	// Create a two dimentional array and check toString still
// 	// works.
//     }

//     /**
//      * For a structure, similar to an array.
//      */
//     public void testStructureOfNumber ()
//     {
// 	// Create a structure containing a single number, check
// 	// toString works for that.
//     }
}
