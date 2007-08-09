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

package frysk.util;

import java.io.File;

import frysk.Config;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.dead.TestLinuxCore;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;

public class TestCommandlineParser extends TestLib {

    public void testCoreExe() {

	TestLinuxCore tester = new TestLinuxCore();
	SlaveOffspring funit = SlaveOffspring.createDaemon();
	Proc funitProc = funit.assertFindProcAndTasks();
	final File core = new File(tester.constructCore(funitProc));

	CommandlineParser parser = new CommandlineParser("test") {

	    public void parseCommand(String[] command) {
		fail("Shoudn't have a command");
	    }

	    public void parseCores(CoreExePair[] coreExePairs) {
		assertEquals("Should have one pair", coreExePairs.length, 1);
		assertEquals("Core file is correct", coreExePairs[0].coreFile,
			core);
		assertEquals("Exe file is correct", coreExePairs[0].exeFile,
			Config.getPkgLibFile("funit-child"));
	    }

	    public void parsePids(ProcId[] pids) {
		fail("Shouldn't have a pid");
	    }

	};

	parser.parse(new String[] { core.getPath(),
		Config.getPkgLibFile("funit-child").getPath() });

	core.delete();
    }

    public void testCore() {

	TestLinuxCore tester = new TestLinuxCore();
	SlaveOffspring funit = SlaveOffspring.createDaemon();
	Proc funitProc = funit.assertFindProcAndTasks();
	final File core = new File(tester.constructCore(funitProc));

	CommandlineParser parser = new CommandlineParser("test") {

	    public void parseCommand(String[] command) {
		fail("Shoudn't have a command");
	    }

	    public void parseCores(CoreExePair[] coreExePairs) {
		assertEquals("Should have one pair", coreExePairs.length, 1);
		assertEquals("Core file is correct", coreExePairs[0].coreFile,
			core);

	    }

	    public void parsePids(ProcId[] pids) {
		fail("Shouldn't have a pid");
	    }

	};

	parser.parse(new String[] { core.getPath() });

	core.delete();
    }

}
