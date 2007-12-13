// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.hpd;

import frysk.Config;
//import frysk.testbed.FunitThreadsOffspring;

/**
 * This class tests the "load" command basics of both loading a correct
 * executable and trying to load a non-existent executable.
 */

public class TestKillCommand extends TestLib {
    public void testKillCommand() {
	/* In the future when fhpd can accept parameters to pass to 
	 * programs, we should probably use the testing stuff below.
	 * until then, we'll have to use a parameterless program.
	String[] args = FunitThreadsOffspring.funitThreadsCommand(2, 
		FunitThreadsOffspring.Type.LOOP);
	String cmdLine = "";
	for (int i = 0; i < args.length; i++) {
	    cmdLine = cmdLine + args[i] + " ";
	} 
	 */
	e = new HpdTestbed();
	e.send("load " + Config.getPkgLibFile("funit-threads-looper").getPath()
		+ "\n");
	e.expect(5, "Loaded executable file*");
	e.send("run\n");
	e.expect(5, "Attached to process*");
	e.send("go\n");
	e.expect(5, "Running process*");
	e.send("kill\n");
	e.expect(5, "Killing process*");
	e.expect(5, "Loaded executable file*");
	/* Make sure you run again to make sure all has been cleaned up properly
	 * from the last run.
	 */
	/*****************************************************
	 * 
         *  There seems to be a problem with the test harness that will not allow
         *  more than the set of commands you see here in one sequence.  Just 
         *  uncommenting the next 2 statements after this comment causes this 
         *  test to fail for no good reason.  A bug will be filed on this and the
         *  lines can be uncommented when fixed. */
	/*
	e.send(5, "run", "Attached to process*");
	e.expect(5, "Attached to process*");
	e.send("go\n");
	e.expect(5, "Running process*");
	e.send("kill\n");
	e.expect(5, "Killing process*");
	e.expect(5, "Loaded executable file*");
	/* Make sure we can quit gracefully  */
	/*
	e.send("quit\n");
	e.expect(5, "Quitting*"); */
	e.close();
    }
    
    /**
     * Test when all you have done is loaded/run the process and not
     * have done a "go" on it.
     */
    public void testLoadKill() {
	e = new HpdTestbed();
	e.send("load " + Config.getPkgLibFile("funit-threads-looper").getPath()
		+ "\n");
	e.expect(5, "Loaded executable file*");
	e.send("run\n");
	e.expect(5, "Attached to process*");
	e.send("kill\n");
	e.expect(5, "Killing process*");
	//e.send("quit\n");
	//e.expect("Quitting*");
	e.close();
    }
    
}
