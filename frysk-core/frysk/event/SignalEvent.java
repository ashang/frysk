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


package frysk.event;

public class SignalEvent
    implements Event
{
    int signal;
    public int hashCode ()
    {
	return signal;
    }
    public boolean equals (Object o)
    {
	return (o instanceof SignalEvent
		&& ((SignalEvent)o).signal == signal);
    }
    public SignalEvent (int signal)
    {
	this.signal = signal;
    }
    public void execute ()
    {
	throw new RuntimeException ("Unhandled signal " + signal);
    }
}
