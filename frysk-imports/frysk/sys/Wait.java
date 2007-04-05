// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import java.util.logging.Logger;

/**
 * Wait for an event from either a process, task, or all processes and
 * tasks.
 */

public final class Wait
{
    private static Logger logger;
    /**
     * Finds, and returns the logger, but only when logging is
     * enabled..
     */
    static Logger getLogger ()
    {
	// Seems that when calling a native static methods this isn't
	// initialized, force it.
	if (logger == null)
	    logger = Logger.getLogger("frysk");
	return logger;
    }

    /**
     * Set of signals checked during poll.
     */
    static protected SignalSet signalSet;
    /**
     * Add Sig to the set of signals checked during poll.
     */
    public static native void signalAdd (Sig sig);
    /**
     * Empty the set of signals, and file descriptors, checked during
     * poll.
     */
    public static native void signalEmpty ();

    /**
     * Read in all the pending wait events, and then pass them to the
     * observer.  If there is no outstanding event return immediatly.
     */
    public native static void waitAllNoHang (WaitBuilder builder);
    /**
     * Wait for a waitpid or signal event.  Returns when either timer
     * has expired or at least one event has been received.
     */
    public native static void waitAll (long millisecondTimeout,
				       WaitBuilder waitBuilder,
				       SignalBuilder signalBuilder);

    /**
     * Wait for a single process or task event.  Block if no event is
     * pending (provided that there are still potential events).
     */
    public native static void waitAll (int pid, WaitBuilder builder);

    /**
     * Non-blocking drain of all pending wait events belonging to pid.
     */
    public native static void drainNoHang (int pid);
    /**
     * Blocking drain of all pending wait events belonging to pid.
     */
    public native static void drain (int pid);
}
