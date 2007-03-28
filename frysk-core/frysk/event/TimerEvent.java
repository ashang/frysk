// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.event;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A timer event.
 *
 * Fires MILLISECONDS into the future (possibly repeated at PERIODMILLIS
 * intervals).
 */

public abstract class TimerEvent
    implements Event, Comparable
{
    private static Logger logger = Logger.getLogger ("frysk.event");
    private long timeMillis;
    private long periodMillis = 0;

    /**
     * Create a once-only timer that schedules an event TIMEMILLIS
     * milliseconds into the future.
     */
    public TimerEvent (long offsetMillis)
    {
	this.timeMillis = offsetMillis + System.currentTimeMillis ();
	logger.log (Level.FINE, "{0} new long\n", this); 
    }

    /**
     * Create an interval timer that schedules its first event OFFSETMILLIS
     * milliseconds into the future, and then schedules further events
     * every PERIODMILLIS milliseconds after that.  Should a backlog of
     * events form (where the next event becomes due before the
     * previous event has been delivered) then only a single event
     * will be delivered.  The method getCount returns the number of
     * events that should have been delivered.
     */
    public TimerEvent (long offsetMillis, long periodMillis)
    {
	this.timeMillis = offsetMillis + System.currentTimeMillis ();
	this.periodMillis = periodMillis;
	logger.log (Level.FINEST, "{0} new long long\n", this); 
    }

    /**
     * Timer events are ordered by time.
     */
    public int compareTo (Object o)
    {
	return (int) (timeMillis - ((TimerEvent)o).timeMillis);
    }

    /**
     * Return the time, in milliseconds, that this timer event is next
     * expected to fire.
     */
    public long getTimeMillis ()
    {
	return timeMillis;
    }

    /**
     * For the interval timer, return the number of intervals since
     * the last periodMillis timer event was delivered.  See {@link
     * #TimerEvent (long, long)}.
     */
    public long getCount ()
    {
	return count;
    }
    private long count = 1;

    /**
     * Update the timer expired timer (setting the count since last
     * fire, and any re-schedule time).  Return true if the timer
     * needs to be scheduled further.
     */
    boolean reSchedule (long currentTimeMillis)
    {
	logger.log (Level.FINEST, "{0} reSchedule\n", this);
	if (periodMillis > 0) {
	    count = (currentTimeMillis - timeMillis) / periodMillis + 1;
	    timeMillis = timeMillis + periodMillis * count;
	    return true;
	}
	else {
	    return false;
	}
    }
    /**
     * Return the TimerEvent as a string.
     */
    public String toString ()
    {
        return ("{"
		+ super.toString ()
		+ ",timeMillis=" + timeMillis
		+ ",periodMillis=" + periodMillis
                + "}");
    }
}
