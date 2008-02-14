// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.sys.proc;

import frysk.junit.TestCase;
import frysk.sys.Pid;

/**
 * Test the Status getUID() and getGID() a predefined set of
 * <tt>/proc$$/status</tt> buffer.
 */

public class TestStat extends TestCase {
    public void testParse() {
	// Construct valid status buffer
	String[] statBuf = new String[] {
	    "21023 (cat) R 19210 21023 19210 34821 21023",
	    " 4194304 173 0 1 0 0 0 0 0 20 0 1",
	    " 0 52915522 81854464 120 18446744073",
	    " 4194304 4212676 140736230287520",
	    " 3709551615 210676510208",
	    " 0 0 0 0 0 0 0 17 1 0 0 2\n"
	};
	Stat stat = new Stat().scan(TestLib.stringsToBytes(statBuf));
	assertNotNull("stat", stat);

	assertEquals("pid", 21023, stat.pid.intValue());
	assertEquals("comm", "cat", stat.comm);
	assertEquals("state", 'R', stat.state);
	assertEquals("ppid", 19210, stat.ppid.intValue());
	assertEquals("pgrp", 21023, stat.pgrp);
	assertEquals("session", 19210, stat.session);
	assertEquals("ttyNr", 34821, stat.ttyNr);
	assertEquals("tpgid", 21023, stat.tpgid);

	assertEquals("flags", 4194304, stat.flags);
	assertEquals("minflt", 173, stat.minflt);
	assertEquals("cminflt", 0, stat.cminflt);
	assertEquals("majflt", 1, stat.majflt);
	assertEquals("cmajflt", 0, stat.cmajflt);
	assertEquals("utime", 0, stat.utime);
	assertEquals("stime", 0, stat.stime);
	assertEquals("cutime", 0, stat.cutime);
	assertEquals("cstime", 0, stat.cstime);
	assertEquals("priority", 20, stat.priority);
	assertEquals("nice", 0, stat.nice);
	assertEquals("numThreads", 1, stat.numThreads);

	assertEquals("irealvalue", 0, stat.irealvalue);
	assertEquals("starttime", 52915522, stat.starttime);
	assertEquals("vsize", 81854464, stat.vsize);
	assertEquals("rss", 120, stat.rss);
	assertEquals("rlim", 18446744073L, stat.rlim);

	assertEquals("startcode",  4194304, stat.startcode);
	assertEquals("endcode",  4212676, stat.endcode);
	assertEquals("startstack",  140736230287520L, stat.startstack);

	assertEquals("kstkesp", 3709551615L, stat.kstkesp);
	assertEquals("kstkeip", 210676510208L, stat.kstkeip);

	assertEquals("signal", 0, stat.signal);
	assertEquals("blocked", 0, stat.blocked);
	assertEquals("sigignore", 0, stat.sigignore);
	assertEquals("sigcatch", 0, stat.sigcatch);
	assertEquals("wchan", 0, stat.wchan);
	assertEquals("nswap", 0, stat.nswap);
	assertEquals("cnswap", 0, stat.cnswap);
	assertEquals("exitSignal", 17, stat.exitSignal);
	assertEquals("processor", 1, stat.processor);
    }

    public void testSelf() {
	Stat stat = new Stat();
	assertNotNull("stat value", stat.scan(Pid.get()));
    }
}
