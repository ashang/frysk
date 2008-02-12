// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import frysk.proc.Proc;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.dwfl.DwflCache;
import frysk.junit.TestCase;
import frysk.Config;
import frysk.sys.Pid;
import frysk.sys.SignalSet;
import frysk.sys.Signal;
import frysk.sys.proc.Stat;
import java.util.Observable;
import java.util.Observer;
import frysk.rsl.Log;

/**
 * Utility for JUnit tests.
 */

public class TestLib extends TestCase {
    protected final static java.util.logging.Logger logger
	= java.util.logging.Logger.getLogger("frysk");
    private static final Log fine = Log.fine(TestLib.class);

    /**
     * Return a String specifying the absolute path of the executable.
     */
    protected static String getExecPath(String program) {
	return Config.getPkgLibFile(program).getAbsolutePath();
    }

    /**
     * Run the event loop for a short period of time until it is
     * explicitly stopped (using EventLoop . requestStop). During this
     * period poll for external events. XXX: Static to avoid gcc bugs.
     */
    protected static void assertRunUntilStop (long timeout, String reason) {
	fine.log("assertRunUntilStop start", reason);
	assertTrue("event loop run explictly stopped (" + reason + ")",
		   Manager.eventLoop.runPolling(timeout * 1000));
	fine.log("assertRunUntilStop stop", reason);
    }

    /**
     * Run the event loop for a short period of time until it is
     * explicitly stopped (using EventLoop . requestStop). During this
     * period poll for external events. XXX: Static to avoid gcc bugs.
     */
    protected static void assertRunUntilStop (String reason) {
	assertRunUntilStop(getTimeoutSeconds (), reason);
    }

    /**
     * Process all the pending events; no polling of external events
     * is performed.  XXX: Static to avoid gcc bugs.
     */
    protected static void runPending () {
	Manager.eventLoop.runPending();
    }

    /**
     * Is the Proc an immediate child of PID? XXX: Static to avoid gcc
     * bugs.
     */
    static public boolean isChildOf (int pid, Proc proc) {
	fine.log("isChildOf pid", pid, "proc", proc);

	// Process 1 has no parent so can't be a child of mine.
	if (proc.getPid() == 1) {
	    fine.log("isChildOf proc is init");
	    return false;
	}

	// If the parent's pid matches this processes pid, assume that
	// is sufficient. Would need a very very long running system
	// for that to not be the case.

	Stat stat = new Stat();
	stat.refresh(proc.getPid());

	if (stat.ppid.intValue() == pid) {
	    fine.log("isChildOf proc is child");
	    return true;
	}
	fine.log("isChildOf proc not child pid", pid, "ppid", stat.ppid,
		 "parent", proc.getParent(), "proc", proc);
	return false;
    }

    /**
     * Is the Proc an immediate child of this Proc? Do not use
     * host.getSelf() as that, in certain situtations, can lead to
     * infinite recursion. XXX: Static to avoid gcc bugs.
     */
    static public boolean isChildOfMine (Proc proc) {
	return isChildOf(Pid.get(), proc);
    }

    /**
     * Is Proc a descendant of PID? XXX: Static to avoid gcc bugs.
     */
    static public boolean isDescendantOf (int pid, Proc proc) {
	// Climb the process tree looking for this process.
	while (proc.getPid() > 1) {
	    // The parent's pid match this process, assume that is
	    // sufficient. Would need a very very long running system
	    // for that to not be the case.
	    if (proc.parent.getPid() == pid)
		return true;
	    proc = proc.parent;
	}
	// Process 1 has no parent so can't be a child of mine. Do
	// this first as no parent implies .parent==null and that
	// would match a later check.
	return false;
    }

    /**
     * Is the process a descendant of this process? Do not use
     * host.getSelf() as that, in certain situtations, can lead to
     * infinite recursion. XXX: Static to avoid gcc bugs.
     */
    static public boolean isDescendantOfMine (Proc proc) {
	return isDescendantOf(Pid.get(), proc);
    }

    /**
     * The host being used by the current test.
     */
    protected Host host;

    public void setUp () {
	fine.log(this, "<<<<<<<<<<<<<<<< start setUp");
	// Extract a fresh new Host and EventLoop from the Manager.
	host = Manager.resetXXX();
	// Detect all test processes added to the process tree,
	// registering each with TearDownProcess list. Look both for
	// children of this process, and children of any processes
	// already marked to be killed. The latter is to catch
	// children of children, such as daemons.
	//
	// Note that, in addition to this, the Child code also
	// directly registers its process. That is to ensure that
	// children that never get entered into the process tree also
	// get registered with TearDownProcess.
	host.observableProcAddedXXX.addObserver(new Observer() {
		public void update (Observable o, Object obj) {
		    Proc proc = (Proc) obj;
		    if (isChildOfMine(proc)) {
			TearDownProcess.add
			    (ProcessIdentifierFactory.create(proc.getPid()));
			return;
		    }
		    Proc parent = proc.getParent();
		    if (parent != null) {
			ProcessIdentifier parentPid
			    = ProcessIdentifierFactory.create(proc.getParent()
							      .getPid());
			if (TearDownProcess.contains(parentPid)) {
			    TearDownProcess.add(ProcessIdentifierFactory
						.create(proc.getPid()));
			    return;
			}
		    }
		}
	    });
	fine.log(this, "<<<<<<<<<<<<<<<< end setUp");
    }

    public void tearDown () {
	fine.log(">>>>>>>>>>>>>>>> start tearDown");

	// Check that there are no pending signals that should have
	// been drained as part of testing. Do this <em>before</em>
	// any tasks are killed off so that the check isn't confused
	// by additional signals generated by the dieing tasks.
	Signal[] checkSigs = new Signal[] { Signal.USR1, Signal.USR2 };
	SignalSet pendingSignals = new SignalSet().getPending();
	for (int i = 0; i < checkSigs.length; i++) {
	    Signal sig = checkSigs[i];
	    assertFalse("pending signal " + sig, pendingSignals.contains(sig));
	}

	// Do this first, it tends to make the most mess :-)
	TearDownProcess.tearDown();

	// Remove any stray Expects
	TearDownExpect.tearDown();

	// Remove any stray files.
	TearDownFile.tearDown();

	// Drain all the pending signals used by children to notify
	// this parent. Note that the process of killing off the
	// processes used in the test can generate extra signals - for
	// instance a SIGUSR1 from a detached child that notices that
	// it's parent just exited.
	Signal.CHLD.drain();
	Signal.HUP.drain();
	Signal.USR1.drain();
	Signal.USR2.drain();

	// Drain the event-loops interrupt signal.  Could be an
	// internal IO request outstanding.
	Signal.IO.drain();
	
	DwflCache.clear();
	
	fine.log(">>>>>>>>>>>>>>>> end tearDown");
    }
}
