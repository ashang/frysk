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

package frysk.rsl;

import frysk.junit.TestCase;
import java.util.LinkedList;

/**
 * Testlogging that is a sub-class of this directory.
 */

public class TestLog extends TestCase {
    private static final Log log = Log.fine(TestLog.class);

    private Node root;
    public void setUp() {
	log.log("setUp");
	root = new Node();
    }
    public void tearDown() {
	log.log("tearDown");
	root = null;
    }
    private Node get(String path) {
	return root.get(path);
    }
    private Log get(String path, Level level) {
	return root.get(path, level);
    }

    public void testRoot() {
	assertNotNull("root", root);
    }

    public void testGetSelf() {
	Log self = get("self", Level.FINE);
	assertNotNull("self", self);
    }

    public void testGetRoot() {
	assertEquals("root", root, root.get(""));
    }

    public void testPath() {
	String path = "a.long.path";
	assertEquals("path", path, get(path, Level.FINE).path());
    }

    public void testName() {
	assertEquals("name", "path",
		     get("a.long.path", Level.FINE).name());
    }

    public void testLevel() {
	assertEquals("level", Level.FINE,
		     get("a.long.path", Level.FINE).level());
    }

    public void testSingleton() {
	Log lhs = get("the.lhs", Level.FINE);
	Log rhs = get("the.rhs", Level.FINE);
	assertNotNull("the.lhs", lhs);
	assertNotNull("the.rhs", rhs);
	assertTrue("lhs != rhs", lhs != rhs);
	assertSame("the.lhs", lhs, get("the.lhs", Level.FINE));
	assertSame("the.rhs", rhs, get("the.rhs", Level.FINE));
    }

    private void checkLevel(String path, Level level) {
	for (int i = 0; i < Level.MAX.intValue(); i++) {
	    Log log = get(path, Level.valueOf(i));
	    assertEquals("level " + log, i <= level.intValue(), log.logging());
	}
    }
    private void set(String path, Level level) {
	get(path).set(level);
    }
    public void testLeveling() {
	// create a tree
	get("the.lower.left.hand.side");
	get("the.lower.right.hand.side");
	// set a level
	set("the.lower.left", Level.FINE);
	checkLevel("the", Level.NONE);
	checkLevel("the.lower", Level.NONE);
	checkLevel("the.lower.left", Level.FINE);
	checkLevel("the.lower.left.hand", Level.FINE);
	checkLevel("the.lower.left.hand.side", Level.FINE);
	checkLevel("the.lower.right", Level.NONE);
	checkLevel("the.lower.right.hand", Level.NONE);
	checkLevel("the.lower.right.hand.side", Level.NONE);
    }
    public void testRootLevelFINE() {
	// Set the root level before any children are created; should
	// propogate down.
	set("", Level.FINE);
	checkLevel("the", Level.FINE);
    }
    public void testSubLevelFINE() {
	// Set the sub-level before any children.
	set("this", Level.FINE);
	checkLevel("this.level", Level.FINE);
	checkLevel("this", Level.FINE);
	checkLevel("", Level.NONE);
    }
    
    private void checkComplete(String incomplete, int expectedCursor,
			       String[] expectedCandidates) {
	// create a tree.
	get("the.lower.left.hand.side");
	get("the.lower.left.half");
	get("the.lower.right.hand.side");
	// Perfomr a completion
	LinkedList candidates = new LinkedList();
	int cursor = root.complete(incomplete, candidates);
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

    public void testOptionFINE()
	throws gnu.classpath.tools.getopt.OptionException
    {
	checkLevel("", Level.NONE);
	LogOption.level(root, "FINE");
	checkLevel("", Level.FINE);
    }
    public void testOptionSubFINE()
	throws gnu.classpath.tools.getopt.OptionException
    {
	checkLevel("the", Level.NONE);
	LogOption.level(root, "the=FINE");
	checkLevel("the", Level.FINE);
    }
    public void testOptionCommaOption()
	throws gnu.classpath.tools.getopt.OptionException
    {
	checkLevel("lhs", Level.NONE);
	checkLevel("rhs", Level.NONE);
	LogOption.level(root, "lhs=FINE,rhs=FINEST");
	checkLevel("lhs", Level.FINE);
	checkLevel("rhs", Level.FINEST);

    }
}
