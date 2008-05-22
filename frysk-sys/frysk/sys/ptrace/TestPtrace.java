// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

package frysk.sys.ptrace;

import frysk.sys.ProcessIdentifier;
import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;
import frysk.testbed.ForkFactory;
import frysk.sys.Fork;
import frysk.sys.Errno;
import frysk.sys.Wait;
import frysk.sys.Signal;
import frysk.sys.UnhandledWaitBuilder;

/**
 * Check the plumming of Ptrace.
 */

public class TestPtrace extends TestCase {
    /**
     * Rip down everything related to PID.
     */
    public void tearDown() {
	TearDownProcess.tearDown ();
    }
 
    public void testChildContinue() {
	final ProcessIdentifier pid
	    = Fork.ptrace(new String[] {
			      "/bin/true"
			  });
	assertTrue("pid", pid.intValue() > 0);
	TearDownProcess.add(pid);
	
	// The initial stop.
	Wait.waitOnce(pid, new UnhandledWaitBuilder() {
		private final ProcessIdentifier id = pid;
		protected void unhandled(String why) {
		    fail (why);
		}
		public void stopped(ProcessIdentifier pid, Signal signal) {
		    assertSame("stopped pid", id, pid);
		    assertSame("stopped sig", Signal.TRAP, signal);
		}
	    });

	Ptrace.singleStep(pid, Signal.NONE);
	Wait.waitOnce(pid, new UnhandledWaitBuilder() {
		private final ProcessIdentifier id = pid;
		protected void unhandled(String why) {
		    fail (why);
		}
		public void stopped(ProcessIdentifier pid, Signal signal) {
		    assertSame("stopped pid", id, pid);
		    assertSame("stopped sig", Signal.TRAP, signal);
		}
	    });

	Ptrace.cont(pid, Signal.TERM);
	Wait.waitOnce(pid, new UnhandledWaitBuilder() {
		private final ProcessIdentifier id = pid;
		protected void unhandled(String why) {
		    fail (why);
		}
		public void terminated(ProcessIdentifier pid, Signal signal,
				       int status, boolean coreDumped) {
		    assertSame("terminated pid", id, pid);
		    assertSame("terminated signal", Signal.TERM, signal);
		    assertEquals("terminated status", -Signal.TERM.intValue(),
				 status);
		}
	    });
    }
	
    /**
     * Check attach (to oneself).
     */
    public void testAttachDetach() {
	final ProcessIdentifier pid = ForkFactory.detachedDaemon();
	TearDownProcess.add(pid);
	assertTrue ("pid", pid.intValue() > 0);

	Ptrace.attach(pid);
	Wait.waitOnce(pid, new UnhandledWaitBuilder() {
		private final ProcessIdentifier id = pid;
		protected void unhandled(String why) {
		    fail (why);
		}
		public void stopped(ProcessIdentifier pid, Signal signal) {
		    assertSame("stopped pid", id, pid);
		    assertSame("stopped sig", Signal.STOP, signal);
		}
	    });

	Ptrace.detach(pid, Signal.NONE);
	Errno errno = null;
	try {
	    Wait.waitOnce(pid, new UnhandledWaitBuilder() {
		    protected void unhandled(String why) {
			fail (why);
		    }
		});
	} catch (Errno e) {
	    errno = e;
	}
	assertEquals("Errno", Errno.Echild.class, errno.getClass());
    }
}
