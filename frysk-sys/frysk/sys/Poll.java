// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

/**
 * Poll like interface for waiting on kernel events.
 *
 * This object, loosely based on the poll and pselect interfaces,
 * provides a call blocks until a UNIX event (signal, FD ready), the
 * timeout expires, or an unexpected interrupt occures.  The client
 * (which extends this object) is notified via the abstract notify
 * methods.
 */

public final class Poll
{
    public static interface Observer
    {
	void signal (int sig);
	void pollIn (int fd);
    }
    Observer observer;

    /**
     * Manage the signal set that can interrupt the poll call.
     */
    public static final class SignalSet
    {
	private SignalSet () {} // Disallow construction.
	// XXX: ECJ 3.1 can't see that the CNI code uses this and,
	// hence, complains that it is never used.
	protected static gnu.gcj.RawDataManaged signalSet;
	static native gnu.gcj.RawDataManaged get ();
	public native static void add (Sig sig);
	public static native void empty ();
    }

    /**
     * Manage the file descriptors watched by the poll call.
     */
    public static final class Fds
    {
	gnu.gcj.RawDataManaged fds;
	int numFds;
	private native void init ();
	public final native void addPollIn (int fd);
	public Fds ()
	{
	    init ();
	}
    }

    /**
     * Poll the system for either FD, or signal events.  Block for
     * timeout milliseconds (if timeout is +ve or zero), or until the
     * next event (if timeout is -ve).  Return when an event might
     * have occured.
     */
    public static native void poll (Fds pollFds,
				    Observer observer,
				    long timeout);
}
