// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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
 * Implements an event loop.
 */

public abstract class EventLoop
    extends Thread
{
    protected static Logger logger = Logger.getLogger("frysk");
    /**
     * Add a timer event that will expire at some stage in the future.
     * The actual time is determined by {@link
     * TimerEvent.getTimeMillis}.
     */
    public abstract void add (TimerEvent t);
    /**
     * Remove the timer event from the event queue.
     */
    public abstract void remove (TimerEvent t);
    /**
     * Add the signal handler, signals are processed and then
     * delivered using the event-loop.
     */
    public abstract void add (SignalEvent signalEvent);
    /**
     * Remove the signal event handler, further occurances of the
     * signal are discarded.
     */
    public abstract void remove (SignalEvent signalEvent);
    /**
     * Add the event to the end of the queue of events that need to be
     * processed.  If necessary, interrupt the event thread.
     */
    public abstract void add (Event e);
    /**
     * Remove the pending event.
     */
    public abstract void remove (Event e);
    /**
     * Run the event-loop.  If pendingOnly, stop after processing all
     * pending events.
     *
     * The event loop is stopped by calling requestStop (that stops
     * the event loop once all pending events have been processed).
     * Any existing pending events are always processed before
     * performing the first poll.
     */
    protected abstract void runEventLoop (boolean pendingOnly);
    /**
     * Request that the event-loop stop.  The event loop stops once
     * all pending events have been processed.
     *
     * Can be called asynchronously.
     */
    public abstract void requestStop ();

    /**
     * Run the event loop until there are no pending events.  Of
     * course, if more events are appended to the pending queue, then
     * they too are processed.
     *
     * For testing only.
     *
     * This method does not check for asynchronous events (Signals,
     * Poll, Timers) is made.  For that see runPolling.
     */
    public final void runPending ()
    {
	logger.log (Level.FINE, "{0} runPending\n", this); 
	runEventLoop (true);
    }
    /**
     * Run the full event loop (checking for both synchronous and
     * asynchronous events) for at most TIMEOUT milliseconds.  Return
     * true if the event loop was stopped before the timer expired.
     *
     * For testing only.
     *
     * Should only be used thus:
     *
     * eventLoop.add<<xxx>>Event (new <<xxx>>Event ()
     *    { public void execute () { eventLoop.stop (); }});
     * assertTrue ("Waiting for <<xxx>>",
     *              eventLoop.runPolling (<<timeout>>));
     */
    public final boolean runPolling (long timeout)
    {
	logger.log (Level.FINE, "{0} runPolling long\n", this); 
	class Timeout
	    extends TimerEvent
	{
	    Timeout (long timeout)
	    {
		super (timeout);
		logger.log (Level.FINE, "{0} timeout\n", this);
	    }
	    boolean expired = false;
	    public void execute ()
	    {
		logger.log (Level.FINE, "{0} execute\n", this); 
		expired = true;
		requestStop ();
	    }
	    public String toString ()
	    {
		return ("{"
			+ super.toString ()
			+ ",expired" + expired
			+ "}");
	    }
	}
	Timeout timer = new Timeout (timeout);
	add (timer);
	runEventLoop (false);
	// Always remove the timer, even when it didn't expire.
	remove (timer);
	return !timer.expired;
    }
    /**
     * Run the event-loop.
     *
     * The event loop is stopped by calling requestStop, and is
     * stopped after all pending events have been processed.  Any
     * existing pending events are always processed before performing
     * the first poll.
     */
    public final void run ()
    {
	logger.log (Level.FINE, "{0} run\n", this); 
	runEventLoop (false);
    }
}
