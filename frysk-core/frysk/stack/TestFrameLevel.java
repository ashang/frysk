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

package frysk.stack;

import frysk.junit.TestCase;

public class TestFrameLevel extends TestCase {

    private FrameLevel oneTwoThree() {
	FrameLevel level = new FrameLevel();
	for (int i = 0; i < 3; i++) {
	    for (int j = 0; j <= i; j++)
		level = level.increment(i);
	}
	return level;
    }


    public void testInner() {
	FrameLevel inner = new FrameLevel();
	assertEquals("inner.toString()", "0", inner.toString());
	assertEquals("inner.position(1)", 0, inner.position(1));
	assertEquals("inner.size()", 1, inner.size());
    }

    public void testIncrement0() {
	FrameLevel level = new FrameLevel();
	for (int i = 1; i < 3; i++) {
	    level = level.increment(0);
	    assertEquals("inner+" + 1, "" + i, level.toString());
	}
    }

    public void testIncrement1() {
	FrameLevel level = new FrameLevel();
	for (int i = 1; i < 3; i++) {
	    level = level.increment(1);
	    assertEquals("inner+" + 1, "0." + i, level.toString());
	}
    }

    public void testIncrementN() {
	FrameLevel level = new FrameLevel();
	level = level.increment(3);
	assertEquals("increment 3", "0.0.0.1", level.toString());
    }

    public void testContract() {
	FrameLevel level = oneTwoThree();
	assertEquals("pre-increment", "1.2.3", level.toString());
	level = level.increment(1);
	assertEquals("post-increment", "1.3", level.toString());
    }

    public void testTruncate() {
	FrameLevel level = oneTwoThree();
	level = level.truncate(5);
	assertEquals("extend", "1.2.3.0.0", level.toString());
	level = level.truncate(2);
	assertEquals("truncate", "1.2", level.toString());
    }

}
