// This file is part of the program FRYSK.
//
// Copyright 2008 Red Hat Inc.
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

package frysk.proc;

import frysk.proc.dummy.DummyHost;
import frysk.proc.dummy.DummyProc;
import frysk.proc.dummy.DummyTask;
import frysk.junit.TestCase;

public class TestComparable extends TestCase {
    private DummyHost h1 = new DummyHost("h1");
    private DummyProc h1p1 = new DummyProc(h1, 1);
    private DummyTask h1p1t1 = new DummyTask(h1p1, 1);
    private DummyTask h1p1t2 = new DummyTask(h1p1, 2);
    private DummyProc h1p2 = new DummyProc(h1, 2);
    private DummyTask h1p2t1 = new DummyTask(h1p2, 1);
    private DummyTask h1p2t2 = new DummyTask(h1p2, 2);
    private DummyHost h2 = new DummyHost("h2");
    private DummyProc h2p1 = new DummyProc(h2, 1);
    private DummyTask h2p1t1 = new DummyTask(h2p1, 1);
    // private DummyTask h2p1t2 = new DummyTask(h2p1, 2);
    private DummyProc h2p2 = new DummyProc(h2, 2);
    // private DummyTask h2p2t1 = new DummyTask(h2p2, 1);
    private DummyTask h2p2t2 = new DummyTask(h2p2, 2);

    public void testCompareHost() {
	assertTrue("h1 == h1", h1.compareTo(h1) == 0);
	assertTrue("h1 < h2", h1.compareTo(h2) < 0);
	assertTrue("h2 > h1", h2.compareTo(h1) > 0);
    }

    public void testCompareProcsOnSameHost() {
	assertTrue("h1p1 == h1p1", h1p1.compareTo(h1p1) == 0);
	assertTrue("h1p1 < h1p2", h1p1.compareTo(h1p2) < 0);
	assertTrue("h1p2 > h1p1", h1p2.compareTo(h1p1) > 0);
    }

    public void testCompareTasksOnSameProc() {
	assertTrue("h1p1t1 == h1p1t1", h1p1t1.compareTo(h1p1t1) == 0);
	assertTrue("h1p1t1 < h1p1t2", h1p1t1.compareTo(h1p1t2) < 0);
	assertTrue("h1p1t2 > h1p1t1", h1p1t2.compareTo(h1p1t1) > 0);
    }

    public void testCompareProcsOnDifferentHost() {
	assertTrue("h1p1 < h2p1", h1p1.compareTo(h2p1) < 0);
	assertTrue("h1p2 < h2p1", h1p2.compareTo(h2p1) < 0);
	assertTrue("h2p1 > h1p2", h2p1.compareTo(h1p2) > 0);
	assertTrue("h2p2 > h1p2", h2p2.compareTo(h1p2) > 0);
    }

    public void testCompareTasksOnDifferentProcsOnSameHost() {
	assertTrue("h1p1t1 < h1p2t1", h1p1t1.compareTo(h1p2t1) < 0);
	assertTrue("h1p1t2 < h1p2t1", h1p1t2.compareTo(h1p2t1) < 0);
	assertTrue("h1p2t1 > h1p1t2", h1p2t1.compareTo(h1p1t2) > 0);
	assertTrue("h1p2t2 > h1p1t2", h1p2t2.compareTo(h1p1t2) > 0);
    }

    public void testCompareTasksOnDifferentHosts() {
	assertTrue("h1p1t1 < h2p2t2", h1p1t1.compareTo(h2p2t2) < 0);
	assertTrue("h2p2t2 > h1p1t1", h2p2t2.compareTo(h1p1t1) > 0);
	assertTrue("h1p2t2 < h2p1t1", h1p2t2.compareTo(h2p1t1) < 0);
	assertTrue("h2p1t1 > h1p2t2", h2p1t1.compareTo(h1p2t2) > 0);
    }
}
