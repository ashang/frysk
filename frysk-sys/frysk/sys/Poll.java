// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/**
 * Poll like interface for waiting on kernel events.
 *
 * This object, loosely based on the poll and pselect interfaces,
 * provides a call blocks until a UNIX event (signal, FD ready), the
 * timeout expires, or an unexpected interrupt occures.  The client
 * (which extends this object) is notified via the abstract notify
 * methods.
 */

package frysk.sys;

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
	private static gnu.gcj.RawDataManaged signalSet;
	static native gnu.gcj.RawDataManaged get ();
	public static native void add (int signum);
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

    public static native void poll (Fds pollFds,
				    Observer observer,
				    long timeout);
}
