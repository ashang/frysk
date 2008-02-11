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

package frysk.sys;

import frysk.Config;
import frysk.junit.TestCase;
import frysk.rsl.Log;
import frysk.testbed.TearDownProcess;

/**
 * Check the plumming of Fork/exec.
 */

public class TestFork extends TestCase {
    private static final Log fine = Log.fine(TestFork.class);

    private final SignalSet defaultSignalSet
	= new SignalSet().getProcMask();
    /**
     * Rip down everything related to PID.
     */
    public void tearDown ()
    {
	TearDownProcess.tearDown ();
	defaultSignalSet.setProcMask(null);
    }
 
    /**
     * Check that a masked signal doesn't propogate through to the
     * child process.
     */
    public void testProcMask () {
	fine.log("Masking SIGHUP");
	SignalSet set = new SignalSet(Signal.HUP);
	set.blockProcMask();
	assertTrue("SIGHUP masked",
		   new SignalSet().getProcMask().contains(Signal.HUP));
	fine.log("Creating funit-procmask to check the mask");
	ProcessIdentifier pid
	    = Fork.exec(null, "/dev/null", null,
			new String[] {
			    Config.getPkgLibFile("funit-procmask")
			    .getPath(),
			    "-n",
			    "1"
			});
	TearDownProcess.add(pid);
	// Capture the child's status; to see if it was correct.
	class ExitStatus extends UnhandledWaitBuilder {
	    ProcessIdentifier pid;
	    Signal signal;
	    int status;
	    public void terminated(ProcessIdentifier pid, Signal signal,
				   int status, boolean coreDumped) {
		fine.log("exited with status", status);
		this.pid = pid;
		this.signal = signal;
		this.status = status;
	    }
	    public void unhandled(String reason) {
		fail(reason);
	    }
	}
	ExitStatus exitStatus = new ExitStatus();
	fine.log("Capturing funit-procmask's exit status");
	Wait.wait(pid, exitStatus,
		  new SignalBuilder() {
		      public void signal(Signal sig) {
			  fail("unexpected signal " + sig);
		      }
		  },
		  getTimeoutMilliseconds());
	// (a timeout will also fail with the below)
	assertSame("pid", pid, exitStatus.pid);
	assertEquals("signal", null, exitStatus.signal);
	assertEquals("status", 0, exitStatus.status);
    }
}
