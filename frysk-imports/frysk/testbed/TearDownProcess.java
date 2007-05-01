// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import frysk.sys.Errno;
import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.sys.Wait;
import frysk.sys.proc.ProcBuilder;
import frysk.sys.ProcessIdentifier;
import java.util.Iterator;
import frysk.sys.Ptrace;
import frysk.sys.WaitBuilder;

/**
 * Framework for cleaning up temporary processes created as part of a
 * test run.
 */

public class TearDownProcess
{
    private static final Logger logger = Logger.getLogger ("frysk");

    /**
     * Log the integer ARG squeezed between PREFIX and SUFFIX.
     */
    private static void log (Object arg1)
    {
	if (logger.isLoggable(Level.FINE))
	    logger.log(Level.FINE, "{0} {1}\n",
		       new Object[] { TearDownProcess.class, arg1 });
    }
    /**
     * Log the integer ARG squeezed between PREFIX and SUFFIX.
     */
    private static void log (Object arg1, Object arg2)
    {
	if (logger.isLoggable(Level.FINE))
	    logger.log(Level.FINE, "{0} {1} {2}\n",
		       new Object[] { TearDownProcess.class, arg1, arg2 });
    }
    /**
     * Log the integer ARG squeezed between PREFIX and SUFFIX.
     */
    private static void log (Object arg1, Object arg2, String arg3)
    {
	if (logger.isLoggable(Level.FINE))
	    logger.log(Level.FINE, "{0} {1} {2} {3}\n",
		       new Object[] { TearDownProcess.class, arg1,
				      arg2, arg3 });
    }

    /**
     * A set of children that are to be killed off at the end of a
     * test run during tearDown.
     */
    private static final Set pidsToKillDuringTearDown = new HashSet ();

    /**
     * Add the pid to the set of pidsToKillDuringTearDown that should
     * be killed off during tearDown.
     */
    public static void add (ProcessIdentifier pid)
    {
	log("killDuringTearDown", pid);
	// Had better not try to register process one.
	if (pid.hashCode() == 1)
	    throw new RuntimeException("killing process one during teardown");
	pidsToKillDuringTearDown.add(pid);
    }

    /**
     * Add the pid to the set of pidsToKillDuringTearDown that should
     * be killed off during tearDown.
     */
    public static void add (int pid)
    {
	add (new ProcessIdentifier (pid));
    }

    /**
     * Return true if PID is a process identified for kill during
     * tearDown.
     */
    public static boolean contains (ProcessIdentifier pid)
    {
	return pidsToKillDuringTearDown.contains (pid);
    }
    public static boolean contains (int pid)
    {
	return contains (new ProcessIdentifier (pid));
    }

    /**
     * Try to blow away the child, catch a failure.
     */
    private static boolean capturedSendTkill (ProcessIdentifier pid)
    {
	try {
	    pid.kill ();
	    log("kill", pid, "(SUCCESS)");
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log("kill -KILL", pid, "(failed - ESRCH)");
	    return false;
	}
	return true;
    }

    /**
     * Sequence a task through CONT, detach, and KILL. Return false if
     * it is suspected that the task no longer exists. Detaching with
     * KILL on early utrace kernels has proven problematic - nothing
     * happened (now fixed) - avoid any such issues by doing a simple
     * detach followed by a KILL. There is a problem with both stopped
     * and attached tasks. The Sig.KILL won't be delivered, and
     * consequently the task won't exit, until that task has been
     * continued. Work around this by first sending all tasks a
     * continue ...
     */
    private static ProcessIdentifier capturedSendDetachContKill (ProcessIdentifier pid)
    {
	// Do the detach
	try {
	    Ptrace.detach(pid, 0);
	    log("detach", pid);
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log("detach", pid, "(failed - ESRCH)");
	}
	// Unblock the thread
	try {
	    pid.tkill(Sig.CONT);
	    log("tkill -CONT", pid);
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log("tkill -CONT", pid, "(failed - ESRCH)\n");
	}
	// Finally send it a kill to finish things off.
	capturedSendTkill(pid);
	return pid;
    }

    private static ProcessIdentifier capturedSendDetachContKill (int pid)
    {
	return capturedSendDetachContKill (new ProcessIdentifier (pid));
    }

    public static void tearDown ()
    {
	// Make a preliminary pass through all the registered
	// pidsToKillDuringTearDown trying to simply kill
	// each. Someone else may have waited on their deaths already.
	for (Iterator i = pidsToKillDuringTearDown.iterator(); i.hasNext();) {
	    ProcessIdentifier child = (ProcessIdentifier) i.next();
	    capturedSendTkill(child);
	}

	// Go through all registered processes / tasks adding any of
	// their clones to the kill-list. Do this after the initial
	// blast as, hopefully, that has stopped many of the threads
	// dead in their tracks.
	ProcBuilder missingTidsToKillDuringTearDown = new ProcBuilder()
	    {
		public void buildId (int id)
		{
		    TearDownProcess.add(id);
		}
	    };
	// Iterate over a copy of the tids's collection as the
	// missingTidsToKillDuringTearDown may modify the underlying
	// collection.
	Object[] pidsToKill = pidsToKillDuringTearDown.toArray();
	for (int i = 0; i < pidsToKill.length; i++) {
	    ProcessIdentifier child = (ProcessIdentifier) pidsToKill[i];
	    missingTidsToKillDuringTearDown.construct(child);
	}

	// Blast all the processes for real.
	for (Iterator i = pidsToKillDuringTearDown.iterator(); i.hasNext();) {
	    ProcessIdentifier child = (ProcessIdentifier) i.next();
	    capturedSendDetachContKill(child);
	}

	// Drain the wait event queue. This ensures that: there are
	// no outstanding events to confuse the next test run; all
	// child zombies have been reaped (and eliminated); and
	// finally makes certain that all attached tasks have been
	// terminated.
	//
	// For attached tasks, which will generate non-exit wait
	// events (clone et.al.), the task is detached / killed.
	// Doing that frees up the task so that it can run to exit.
	try {
	    while (! pidsToKillDuringTearDown.isEmpty()) {
		log("waitAll -1 ....");
		Wait.waitAll(-1, new WaitBuilder() {
			public void cloneEvent (int pid, int clone)
			{
			    capturedSendDetachContKill(pid);
			}

			public void forkEvent (int pid, int child)
			{
			    capturedSendDetachContKill(pid);
			}

			public void exitEvent (int pid, boolean signal, int value,
					       boolean coreDumped)
			{
			    capturedSendDetachContKill(pid);
			    // Do not remove PID from
			    // pidsToKillDuringTearDown list; need to
			    // let the terminated event behind it
			    // bubble up.
			}

			public void execEvent (int pid)
			{
			    capturedSendDetachContKill(pid);
			}

			public void syscallEvent (int pid)
			{
			    capturedSendDetachContKill(pid);
			}

			public void stopped (int pid, int signal)
			{
			    capturedSendDetachContKill(pid);
			}

			private void drainTerminated (int pid)
			{
			    // To be absolutly sure, again make
			    // certain that the thread is detached.
			    ProcessIdentifier id = capturedSendDetachContKill(pid);
			    // True pidsToKillDuringTearDown can have
			    // a second exit status behind this first
			    // one, drain that also. Give up when
			    // this PID has no outstanding events.
			    log("Wait.drain", id, "\n");
			    id.blockingDrain ();
			    // Hopefully done with this PID.
			    pidsToKillDuringTearDown.remove(id);
			}

			public void terminated (int pid, boolean signal,
						int value,
						boolean coreDumped)
			{
			    drainTerminated(pid);
			}

			public void disappeared (int pid, Throwable w)
			{
			    // The task vanished somehow, drain it.
			    drainTerminated(pid);
			}
		    });
	    }
	}
	catch (Errno.Echild e) {
	    // No more events.
	}

	// Drain all the pending signals. Note that the process of killing
	// off the processes used in the test can generate extra signals -
	// for instance a SIGUSR1 from a detached child that notices that
	// it's parent just exited.
	Signal.drain (Sig.CHLD);
	Signal.drain (Sig.HUP);
	Signal.drain (Sig.USR1);
	Signal.drain (Sig.USR2);

	pidsToKillDuringTearDown.clear ();
    }
}
