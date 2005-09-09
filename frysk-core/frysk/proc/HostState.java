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
 * Possible host states.
 */

package frysk.proc;

class HostState
    extends State
{
    protected HostState (String state)
    {
	super (state);
    }
    HostState process (Host host, HostEvent.RequestRefresh event)
    {
	throw unhandled (host, event);
    }
    HostState process (Host host, HostEvent.RequestCreateProc event)
    {
	throw unhandled (host, event);
    }
    HostState process (Host host, HostEvent.RequestAttachProc event)
    {
	throw unhandled (host, event);
    }

    static HostState running = new HostState ("running")
	{
	    HostState process (Host host, HostEvent.RequestRefresh event)
	    {
		host.sendRefresh (event.refreshAll);
		return running;
	    }
	    HostState process (Host host, HostEvent.RequestCreateProc event)
	    {
		host.sendCreateProc (event.stdin, event.stdout, event.stderr,
				     event.args);
		return running;
	    }
	    HostState process (Host host, HostEvent.RequestAttachProc event)
	    {
		ProcId id = event.id;
		Proc proc = host.getProc (id);
		if (proc != null)
		    // The process is already known, pass the request
		    // on.
		    proc.requestAttachedContinue ();
		else
		    host.sendAttachProc (id);
		return running;
	    }
	};
}
