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
 * Possible host events.
 *
 * Eventually this can encompass things like host down, et.al.
 */

package frysk.proc;

import frysk.event.Event;

abstract class HostEvent
    implements Event
{
    Host host;
    protected HostEvent (Host host)
    {
	this.host = host;
    }
    public String toString ()
    {
	return ("[HostEvent"
		+ ",host" + host
		+ "]");
    }

    /**
     * Update the Host's state.
     *
     * Some of the Host's state is tracked synchronously (using
     * attach) while the rest is tracked asynchronously (using polls).
     * This requests that any polled state be refreshed.
     */
    static class RequestRefresh
	extends HostEvent
    {
	boolean refreshAll;
	RequestRefresh (Host host, boolean refreshAll)
	{
	    super (host);
	    this.refreshAll = refreshAll;
	}
	public void execute ()
	{
	    host.state = host.state.process (host, this);
	}
    }

    /**
     * Request the creation of a running, but attached process on the
     * host.
     */
    static class RequestCreateProc
	extends HostEvent
    {
	String stdin;
	String stdout;
	String stderr;
	String[] args;
	RequestCreateProc (Host host, String stdin, String stdout,
			   String stderr, String[] args)
	{
	    super (host);
	    this.stdin = stdin;
	    this.stdout = stdout;
	    this.stderr = stderr;
	    this.args = args;
	}
	public void execute ()
	{
	    host.state = host.state.process (host, this);
	}
    }

    /**
     * Request an attach to the specified process.
     */
    static class RequestAttachProc
	extends HostEvent
    {
	ProcId id;
	RequestAttachProc (Host host, ProcId id)
	{
	    super (host);
	    this.id = id;
	}
	public void execute ()
	{
	    host.state = host.state.process (host, this);
	}
    }

}
