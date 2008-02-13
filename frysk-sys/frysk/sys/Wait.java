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

package frysk.sys;

import frysk.rsl.Log;

/**
 * Wait for an event from either a process, task, or all processes and
 * tasks.
 */

public final class Wait {
    /**
     * Finds, and returns the logger, but only when logging is
     * enabled..
     */
    static Log logFine() {
	// Seems that when calling a native static methods this isn't
	// initialized, force it.
	if (fine == null)
	    fine = Log.fine(Wait.class);
	return fine;
    }
    private static Log fine;
    /**
     * Finds, and returns the logger, but only when logging is
     * enabled..
     */
    static Log logFinest() {
	// Seems that when calling a native static methods this isn't
	// initialized, force it.
	if (finest == null)
	    finest = Log.finest(Wait.class);
	return finest;
    }
    private static Log finest;

    /**
     * Set of signals checked during poll.
     */
    static protected SignalSet signalSet;
    /**
     * Add Sig to the set of signals checked during poll.
     */
    public static native void signalAdd(Signal sig);
    /**
     * Empty the set of signals, and file descriptors, checked during
     * poll.
     */
    public static native void signalEmpty();

    /**
     * Read in all the pending wait events, and then pass them to the
     * observer.  If there is no outstanding event return immediatly.
     */
    public native static void waitAllNoHang(WaitBuilder builder);
    /**
     * Wait for a waitpid or signal event.  Returns when at least one
     * event has been received, or the specified timeout has expired.
     *
     * Specify a -ve timeout to block until any event; a zero timeout
     * to not block.
     *
     * Specify ignoreECHILD to block even when there are no children;
     * if you're implementing an event-loop, this is what you want.
     *
     * Note that this implements the timeout using ITIMER_REAL and
     * SIGALRM.
     *
     * Return true if the timeout expired; note that waitpid events
     * may have also been processed.
     */
    public static boolean waitChild(WaitBuilder waitBuilder,
				    SignalBuilder signalBuilder,
				    long millisecondTimeout) {
	return wait(-1, waitBuilder, signalBuilder, millisecondTimeout, false);
    }
    public static boolean wait(ProcessIdentifier pid,
			       WaitBuilder waitBuilder,
			       SignalBuilder signalBuilder,
			       long millisecondTimeout) {
	return wait(pid.intValue(), waitBuilder, signalBuilder,
		    millisecondTimeout, true);
    }
    public static boolean wait(WaitBuilder waitBuilder,
			       SignalBuilder signalBuilder,
			       long millisecondTimeout) {
	return wait(-1, waitBuilder, signalBuilder, millisecondTimeout, true);
    }
    private static native boolean wait(int pid,
				       WaitBuilder waitBuilder,
				       SignalBuilder signalBuilder,
				       long millisecondTimeout,
				       boolean ignoreECHILD);
    /**
     * Wait for a single process or task event.  Block if no event is
     * pending (provided that there are still potential events).
     */
    public static void waitOnce(ProcessIdentifier pid,
				WaitBuilder builder) {
	waitOnce(pid.intValue(), builder);
    }
    private static native void waitOnce(int pid, WaitBuilder builder);

    /**
     * Non-blocking drain of all pending wait events belonging to pid.
     */
    public static void drainNoHang(ProcessIdentifier pid) {
	drainNoHang(pid.intValue());
    }
    private static native void drainNoHang(int pid);
    /**
     * Blocking drain of all pending wait events belonging to pid.
     */
    public static void drain(ProcessIdentifier pid) {
	drain(pid.intValue());
    }
    private static native void drain(int pid);
}
