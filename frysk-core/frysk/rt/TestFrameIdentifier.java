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

package frysk.rt;

import frysk.junit.TestCase;

public class TestFrameIdentifier
    extends TestCase
{
    // PARAM: CallFrameAddress; FunctionAddress.
    FrameIdentifier inner = new FrameIdentifier (400, 100);
    FrameIdentifier middleInner = new FrameIdentifier (300, 200);
    FrameIdentifier middleOuter = new FrameIdentifier (200, 200);
    FrameIdentifier outer = new FrameIdentifier (100, 300);

    private void validateInnerTo (String what, FrameIdentifier lhs,
				  boolean[] expected)
    {
	assertEquals (what + " .innerTo inner", expected[0],
		      lhs.innerTo (inner));
	assertEquals (what + " .innerTo middleInner", expected[1],
		      lhs.innerTo (middleInner));
	assertEquals (what + " .innerTo middleOuter", expected[2],
		      lhs.innerTo (middleOuter));
	assertEquals (what + " .innerTo outer", expected[3],
		      lhs.innerTo (outer));
    }

    private void validateOuterTo (String what, FrameIdentifier lhs,
				  boolean[] expected)
    {
	assertEquals (what + " .outerTo inner", expected[0],
		      lhs.outerTo (inner));
	assertEquals (what + " .outerTo middleInner", expected[1],
		      lhs.outerTo (middleInner));
	assertEquals (what + " .outerTo middleOuter", expected[2],
		      lhs.outerTo (middleOuter));
	assertEquals (what + " .outerTo outer", expected[3],
		      lhs.outerTo (outer));
    }

    private void validateEquals (String what, Object lhs,
				 boolean[] expected)
    {
	assertEquals (what + " .equals inner", expected[0],
		      lhs.equals (inner));
	assertEquals (what + " .equals middleInner", expected[1],
		      lhs.equals (middleInner));
	assertEquals (what + " .equals middleOuter", expected[2],
		      lhs.equals (middleOuter));
	assertEquals (what + " .equals outer", expected[3],
		      lhs.equals (outer));
    }

    public void testInnerTo ()
    {
	validateInnerTo ("inner", inner, new boolean[] {
			     false, true, true, true
			 });
	validateInnerTo ("middleInner", middleInner, new boolean[] {
			     false, false, false, true
			 });
	validateInnerTo ("middleOuter", middleOuter, new boolean[] {
			     false, false, false, true
			 });
	validateInnerTo ("outer", outer, new boolean[] {
			     false, false, false, false
			 });
    }

    public void testOuterTo ()
    {
	validateOuterTo ("inner", inner, new boolean[] {
			     false, false, false, false
			 });
	validateOuterTo ("middleInner", middleInner, new boolean[] {
			     true, false, false, false
			 });
	validateOuterTo ("middleOuter", middleOuter, new boolean[] {
			     true, false, false, false
			 });
	validateOuterTo ("outer", outer, new boolean[] {
			     true, true, true, false
			 });
    }

    public void testEquals ()
    {
	validateEquals ("inner", inner, new boolean[] {
			    true, false, false, false
			});
	validateEquals ("middleInner", middleInner, new boolean[] {
			    false, true, false, false
			});
	validateEquals ("middleOuter", middleOuter, new boolean[] {
			    false, false, true, false
			});
	validateEquals ("outer", outer, new boolean[] {
			    false, false, false, true
			});
    }
}
