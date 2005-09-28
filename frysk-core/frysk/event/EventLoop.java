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

package frysk.event;

import frysk.sys.Poll;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Iterator;

/**
 * Implements an event loop.
 */

public class EventLoop
{
    public EventLoop ()
    {
	// Make certain that the global signal set is empty.
	Poll.SignalSet.empty ();
    }
    
    // List of timer events.  Sorted by the timer's date so that the
    // first event always defines the next timeout.

    private TreeMap timerEvents = new TreeMap ();
    public void addTimerEvent (TimerEvent t)
    {
	timerEvents.put (t, t);
    }
    public void remove (TimerEvent t)
    {
	timerEvents.remove (t);
	pendingEvents.remove (t);
    }


    // Array of poll() events; not implemented.

    private ArrayList pollEvents = new ArrayList ();
    public void addPollEvent (PollEvent fd)
    {
	pollEvents.add (fd);
    }


    // Array of signals; assume that very few signals are being
    // watched and hence that a small array is sufficient.
    private List signalHandlers = new ArrayList ();
    /**
     * Add the signal handler, signals are processed and then
     * delivered using the event-loop.
     */
    public void addHandler (SignalEvent sig)
    {
	int index = signalHandlers.indexOf (sig);
	if (index < 0) {
	    // New.
	    signalHandlers.add (sig);
	    Poll.SignalSet.add (sig.signal);
	}
	else
	    signalHandlers.set (index, sig);
    }
    /**
     * Remove the signal event handler, further occurances of the
     * signal are discarded.
     */
    public void remove (SignalEvent sig)
    {
	signalHandlers.remove (sig);
	// Poll.SignalSet.remove (sig.signal);
    }
    /**
     * Process the signal; find the applicable handler and append the
     * corresponding delivery event.
     */
    void processSignal (int signum)
    {
	Iterator handlers = signalHandlers.iterator ();
	while (handlers.hasNext ()) {
	    SignalEvent event = (SignalEvent) handlers.next ();
	    if (event.signal == signum) {
		appendEvent (event);
		return;
	    }
	}
    }

    /**
     * Append an event to the end of the queue of pending events.
     */
    public void appendEvent (Event e)
    {
	pendingEvents.add (e);
    }
    private List pendingEvents = new LinkedList ();

    Poll.Fds pollFds = new Poll.Fds ();
    Poll.Observer pollObserver = new Poll.Observer () {
	    public void signal (int signum) {
		processSignal (signum);
	    }
	    // Not yet using file descriptors.
	    public void pollIn (int fd) {
		throw new RuntimeException ("should not happen");
	    }
	};

    // Compute the number of milliseconds until the next timer, or -1
    // if there is no pending timer (that implies infinite time).
    private long millisecondsToNextTimer ()
    {
	if (timerEvents.isEmpty ())
	    return -1; // MilliSeconds; no timeout.
	else {
	    // Since the timerEvents are sorted by time just need to
	    // pull off and check the first event.
	    TimerEvent nextTimer = (TimerEvent) timerEvents.firstKey ();
	    long timeout = (nextTimer.value
			    - java.lang.System.currentTimeMillis ());
	    if (timeout < 0)
		timeout = 0;
	    return timeout;
	}
    }

    // Move any expired timer events onto the event queue.  Since
    // these are ordered the loop only needs to iterate over the first
    // few timers.
    private void checkForTimerEvents ()
    {
	long time = java.lang.System.currentTimeMillis ();
	while (!timerEvents.isEmpty ()) {
	    TimerEvent timer = (TimerEvent) timerEvents.firstKey ();
	    if (timer.value > time)
		break;
	    timerEvents.remove (timer);
	    // If it's a periodic timer re-schedule it for
	    // some time into the future.
	    if (timer.period > 0) {
		timer.reSchedule (time);
		timerEvents.put (timer, timer);
	    }
	    appendEvent (timer);
	}
    }

    /**
     * Run the event loop until there are no pending events.  Of
     * course, if more events are appended to the pending queue, then
     * they two are processed.
     *
     * For testing only.
     *
     * This method does not check for asynchronous events (Signals,
     * Poll, Timers) is made.  For that see runPolling.
     */
    public void runPending ()
    {
	while (pendingEvents.size () > 0) {
	    Event e = (Event) pendingEvents.remove (0);
	    e.execute ();
	}
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
    public boolean runPolling (long timeout)
    {
	class Timeout
	    extends TimerEvent
	{
	    Timeout (long timeout)
	    {
		super (timeout);
	    }
	    boolean expired = false;
	    public void execute ()
	    {
		expired = true;
		requestStop ();
	    }
	}
	Timeout timer = new Timeout (timeout);
	addTimerEvent (timer);
	run ();
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
    public void run ()
    {
	stop = false;
	while (true) {
	    runPending ();
	    if (stop) break;
	    Poll.poll (pollFds, pollObserver, millisecondsToNextTimer ());
	    checkForTimerEvents ();
	}
    }

    /**
     * Request that the event-loop stop.
     *
     * Can be called asynchronously.
     */
    public void requestStop ()
    {
	stop = true;
    }

    private volatile boolean stop;
}
