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

/**
 * A timer event.
 *
 * Fires MILLISECONDS into the future (possibly repeated at PERIOD
 * intervals).
 */

public abstract class TimerEvent
    implements Event, Comparable
{
    long value;
    long period = 0;

    /**
     * Create a once-only timer that schedules an event VALUE
     * milliseconds into the future.
     */
    public TimerEvent (long value)
    {
	this.value = value + System.currentTimeMillis ();
    }

    /**
     * Create an interval timer that schedules its first event VALUE
     * milliseconds into the future, and then schedules further events
     * every PERIOD milliseconds after that.  Should a backlog of
     * events form (where the next event becomes due before the
     * previous event has been delivered) then only a single event
     * will be delivered.  The method getCount returns the number of
     * events that should have been delivered.
     */
    public TimerEvent (long value, long period)
    {
	this.value = value + System.currentTimeMillis ();
	this.period = period;
    }

    public int compareTo (Object o)
    {
	return (int) (value - ((TimerEvent)o).value);
    }

    /**
     * For the interval timer, return the number of intervals since
     * the last period timer event was delivered.
     */
    public long getCount ()
    {
	return count;
    }
    private long count = 1;

    /**
     * For the interval timer, compute the time until the next event.
     */
    void reSchedule (long currentTime)
    {
	count = (currentTime - value) / period + 1;
	value = value + period * count;
    }
}
