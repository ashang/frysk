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

package frysk.rsl;

import java.util.LinkedList;

/**
 * Testlogging that is a sub-class of this directory.
 */

public class TestCompletion extends TestLib {
    private void checkComplete(String incomplete, int expectedCursor,
			       String[] expectedCandidates) {
	// create a tree.
	get("the.lower.left.hand.side");
	get("the.lower.left.half");
	get("the.lower.right.hand.side");
	// Perfomr a completion
	LinkedList candidates = new LinkedList();
	int cursor = complete(incomplete, candidates);
	assertEquals("candidate.size", expectedCandidates.length,
		     candidates.size());
	for (int i = 0; i < expectedCandidates.length; i++) {
	    assertEquals("candidate[" + i + "]", expectedCandidates[i],
			 candidates.get(i));
	}
	assertEquals("cursor", expectedCursor, cursor);
    }
    public void testCompleteChildPresent() {
	// children present, expand the dot
	checkComplete("the", 3, new String[] {"." });
    }
    public void testCompleteChildMissing() {
	// no children present, expand the space
	checkComplete("the.lower.left.half", 19, new String[] {" "});
    }
    public void testCompleteSingle() {
	// single completion
	checkComplete("the.", 4, new String[] {"lower" });
    }
    public void testCompleteMultiple() {
	// multiple completion
	checkComplete("the.lower.",10,  new String[] { "left", "right" });
    }
    public void testCompleteMidway() {
	// mid completion
	checkComplete("the.lower.left.h", 15, new String[] { "half", "hand" });
    }
    public void testCompleteNothing() {
	checkComplete("", 0, new String[] { "the" });
    }
    public void testCompleteBogus() {
	// bogus completion
	checkComplete("the.upper", -1, new String[0]);
    }
}
