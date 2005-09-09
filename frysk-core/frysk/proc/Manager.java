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

package frysk.proc;

import java.util.*;
import frysk.event.EventLoop;

/**
 * Manager of all operations within the proc model.
 *
 * Come here first; there's only one manager (well at least for the
 * moment).  */

public class Manager
{
    public static class ProcObservable
	extends Observable
    {
	protected void notify (Proc proc)
	{
	    setChanged ();
	    notifyObservers (proc);
	}
    }

    /**
     * Use host.procAdded.
     */
    static public ProcObservable procDiscovered = new ProcObservable ();
    /**
     * Use host.procRemoved.
     */
    static public ProcObservable procRemoved = new ProcObservable ();
	
    // static public PsObserver     psObserver = new PsObserver ();


    // The host (for moment only the local native host).

    // XXX: Should have the LinuxHost, along with any other host
    // types, register themselves and then have HOST set itself to the
    // most appropriate.

    public static EventLoop eventLoop = new EventLoop ();
    public static Host host = new LinuxHost (eventLoop);

    static void resetXXX ()
    {
	procDiscovered = new ProcObservable ();
	procRemoved = new ProcObservable ();
	eventLoop = new EventLoop ();
	host = new LinuxHost (eventLoop);
    }
}
