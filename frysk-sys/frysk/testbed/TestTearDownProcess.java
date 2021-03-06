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

package frysk.testbed;

import frysk.junit.TestCase;
import frysk.sys.Errno;
import frysk.sys.Fork;
import frysk.sys.Wait;
import frysk.sys.Signal;
import frysk.sys.UnhandledWaitBuilder;
import frysk.sys.SignalBuilder;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

public class TestTearDownProcess
    extends TestCase
{
    private void assertGone(ProcessIdentifier pid) {
	boolean gone = false;
	try {
	    Signal.NONE.kill(pid);
	} catch (Errno.Esrch e) {
	    gone = true;
	}
	assertTrue("process gone", gone);
    }

    public void testForkPtraceUnattached() {
	ProcessIdentifier pid = Fork.ptrace(new String[] { "/bin/true" });
	TearDownProcess.add(pid);
	TearDownProcess.tearDown ();
	assertGone(pid);
    }

    public void testInvalidPid() {
	ProcessIdentifier zero = ProcessIdentifierFactory.create(0);
	try {
	    TearDownProcess.add(zero);
	    fail("add of zero should not succeed");
	} catch (RuntimeException e) {
	}
    }

    public void testInitPid() {
	ProcessIdentifier init = ProcessIdentifierFactory.create(1);
	try {
	    TearDownProcess.add(init);
	    fail("add of init should not succeed");
	} catch (RuntimeException e) {
	}
    }

    public void testForkPtraceAttached() {
	ProcessIdentifier pid = Fork.ptrace(new String[] { "/bin/true" });
	long maxTime = System.currentTimeMillis() + getTimeoutMilliseconds();
	Wait.wait(pid,
		  new UnhandledWaitBuilder() {
		      protected void unhandled(String why) {
			  fail (why);
		      }
		      public void stopped(ProcessIdentifier pid,
					  Signal signal) {
			  // Toss.
		      }
		  },
		  new SignalBuilder() {
		      public void signal(Signal sig) {
			  fail("signal " + sig + " received");
		      }
		  },
		  getTimeoutMilliseconds());
	assertFalse("timeout", System.currentTimeMillis() >= maxTime);
	TearDownProcess.add (pid);
	TearDownProcess.tearDown();
	assertGone(pid);
    }
}
