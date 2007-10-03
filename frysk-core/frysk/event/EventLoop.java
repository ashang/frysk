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
// import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Tid;
import java.util.List;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Long;
import java.lang.Integer;
import frysk.Config;

/**
 * Implements an event loop.
 */

public class EventLoop
    extends Thread
{
    private static Logger logger;
    /**
     * Create an event loop, re-initialize any static data.
     */
    public EventLoop ()
    {
	logger = Config.EventLogger.get ("logs/", "frysk_core_event.log");
	logger.log (Level.FINE, "initializing\n", ""); 
	// Make certain that the global signal set is empty.
	Poll.SignalSet.empty ();
	// Sig.IO is used to wake up a blocked event loop when an
	// asynchronous event arrives.
	Poll.SignalSet.add (Sig.IO);
    }
    

    /**
     * The EventLoop's thread ID.  If a thread, other than the
     * EventLoop thread modifies any of the event queues, the event
     * thread will need to be woken up.
     */
    private int tid = -1;
    /**
     * The event loop will enter a blocking poll.  This state is
     * entered after the pending event queue, along with any other
     * tasks (signals, timers), have been processed leaving the event
     * thread with nothing to do.  Should some new asynchronus task
     * come along (such as a new event, timer, or signal handler), the
     * event thread will need to be woken up.
     */
    private volatile boolean isGoingToBlock = false;
    /**
     * If the event loop is blocking, wake it up.  The event loop
     * blocks when it has nothing better to do (app pending events
     * have been processed, all timers and signals have been handled).
     * See {@link #remove()} for where this flag is set.
     */
    private synchronized void wakeupIfBlocked ()
    {
	logger.log (Level.FINEST, "wake up {0}\n", this); 
	if (isGoingToBlock) {
	    // Assert: tid > 0
	    frysk.sys.Signal.tkill (tid, Sig.IO);
	    isGoingToBlock = false;
	}
    }

    /**
     * List of timer events.  Ordered by the timer's expiry time (that
     * way the first timer event always determines the next timeout).
     */
    private SortedMap timerEvents = new TreeMap ();
    /**
     * Add a timer event that will expire at some stage in the future.
     * The actual time is determined by {@link
     * TimerEvent.getTimeMillis}.
     */
    public synchronized void add (TimerEvent t)
    {
	logger.log (Level.FINE, "add timer event {0}\n", t); 
	timerEvents.put (t, t);
	wakeupIfBlocked ();
    }
    /**
     * Remove the timer event from the event queue.
     */
    public synchronized void remove (TimerEvent t)
    {
	logger.log (Level.FINE, "remove timer event {0}\n", t); 
	timerEvents.remove (t);
	pendingEvents.remove (t);
    }
    /**
     * Return the number of milliseconds until the next timer, or -1
     * if there is no pending timer (-1 implies an infinite timeout).
     */
    private synchronized long getTimerEventMillisecondTimeout ()
    {
	if (timerEvents.isEmpty ())
	    return -1; // MilliSeconds; no timeout.
	else {
	    // Since the timerEvents are sorted by time just need to
	    // pull off and check the first event.
	    TimerEvent nextTimer = (TimerEvent) timerEvents.firstKey ();
	    long timeout = (nextTimer.getTimeMillis ()
			    - java.lang.System.currentTimeMillis ());
	    if (timeout < 0) {
		// A zero timer implies no block.
		isGoingToBlock = false;
		timeout = 0;
	    }
	    return timeout;
	}
    }
    /**
     * Move any expired timer events onto the event queue.  Since the
     * timer queue is ordered by time, the loop need only check the
     * front of the queue.
     */
    private synchronized void checkForTimerEvents ()
    {
	long time = java.lang.System.currentTimeMillis ();
	logger.log (Level.FINEST, "move to event queue {0}\n", this); 
	while (!timerEvents.isEmpty ()) {
	    TimerEvent timer = (TimerEvent) timerEvents.firstKey ();
	    if (timer.getTimeMillis () > time)
		break;
	    timerEvents.remove (timer);
	    pendingEvents.add (timer);
	    // See if the timer wants to re-schedule itself, if so
	    // re-insert it into the timer queue.
	    if (timer.reSchedule (time))
		timerEvents.put (timer, timer);
	}
    }


    /**
     * Set of FDs that should be polled, not fully implemented.
     */
    private Poll.Fds pollFds = new Poll.Fds ();
    /**
     * Add FD to events that should be polled.
     */
    public synchronized void add (PollEvent fd)
    {
	logger.log (Level.FINE, "add {0}\n", fd); 
	wakeupIfBlocked ();
    }


    /**
     * Collection of signals; assume that very few signals are being
     * watched and hence that a small map is sufficient.
     */
    private Map signalHandlers = new HashMap ();
    /**
     * Add the signal handler, signals are processed and then
     * delivered using the event-loop.
     */
    public synchronized void add (SignalEvent sig)
    {
	Object old = signalHandlers.put (sig, sig);
	logger.log (Level.FINE, "add signal handler {0}\n", sig); 
	if (old == null)
	    // New signal, tell Poll.
	    Poll.SignalSet.add (sig.getSignal ());
	wakeupIfBlocked ();
    }
    /**
     * Remove the signal event handler, further occurances of the
     * signal are discarded.
     */
    public synchronized void remove (SignalEvent sig)
    {
	logger.log (Level.FINE, "remove signal handler {0}\n", sig); 
	signalHandlers.remove (sig);
	// XXX: Poll.SignalSet.remove (sig.signal);
    }
    /**
     * Process the signal.  Find the applicable handler and, if
     * present, append the corresponding delivery event.  Since a
     * signal delivery un-blocks the poll, the event thread is no
     * longer going to block.
     */
    private synchronized void processSignal (int signum)
    {
	logger.log (Level.FINE, "process signal {0}\n", new Integer(signum)); 
	Signal lookup = new Signal (signum);
	SignalEvent handler = (SignalEvent) signalHandlers.get (lookup);
	if (handler != null)
	    pendingEvents.add (handler);
	isGoingToBlock = false;
    }


    /** 
     * Maintain a FIFO of events that are ready to be processed.
     */
    private List pendingEvents = new LinkedList ();
    /**
     * Add the event to the end of the queue of events that need to be
     * processed.  If necessary, interrupt the event thread.
     */
    public synchronized void add (Event e)
    {
	logger.log (Level.FINEST, "add event {0}\n", e); 
	pendingEvents.add (e);
	wakeupIfBlocked ();
    }
    /**
     * Remove the pending event.
     */
    public synchronized void remove (Event e)
    {
	logger.log (Level.FINE, "remove event {0}\n", e); 
	pendingEvents.remove (e);
    }
    /**
     * Remove and return the first pending event, or null if there are
     * no pending event.  Also set the isGoingToBlock flag (after all
     * with the event queue empty there is nothing better to do).
     */
    private synchronized Event remove ()
    {
	if (pendingEvents.isEmpty ()) {
	    isGoingToBlock = true;
	    return null;
	}
	else {
	    logger.log (Level.FINEST, "remove first pending event {0}\n", pendingEvents.get(0)); 
	    return (Event) pendingEvents.remove (0);
	}
    }

    /**
     * Handle anything that comes back from the poll call.
     */
    private Poll.Observer pollObserver = new Poll.Observer () {
	    public void signal (int signum) {
		logger.log (Level.FINE, "poll {0}\n", this); 
		processSignal (signum);
	    }
	    // Not yet using file descriptors.
	    public void pollIn (int fd) {
		throw new RuntimeException ("should not happen");
	    }
	};
    /**
     * Run the event-loop.  If pendingOnly, stop after processing all
     * pending events.
     *
     * The event loop is stopped by calling requestStop (that stops
     * the event loop once all pending events have been processed).
     * Any existing pending events are always processed before
     * performing the first poll.
     */
    private void runEventLoop (boolean pendingOnly)
    {
	logger.log (Level.FINE, "run event loop {0}\n", this); 
	try {
	    // Assert: isGoingToBlock == false
	    tid = Tid.get ();
	    stop = pendingOnly;
	    while (true) {
		// Drain any pending events.
		for (Event e = remove (); e != null; e = remove ()) {
		    e.execute ();
		}
		// {@link #remove()} will have set {@link
		// #isGoingToBlock}.
		if (stop)
		    break;
		long timeout = getTimerEventMillisecondTimeout ();
		Poll.poll (pollFds, pollObserver, timeout);
		isGoingToBlock = false;
		checkForTimerEvents ();
	    }
	}
	finally {
	    isGoingToBlock = false;
	}
    }
    /**
     * Request that the event-loop stop.  The event loop stops once
     * all pending events have been processed.
     *
     * Can be called asynchronously.
     */
    public void requestStop ()
    {
	logger.log (Level.FINE, "stop event loop {0}\n", this); 
	stop = true;
	wakeupIfBlocked ();
    }
    private volatile boolean stop;


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
    public void runPending ()
    {
	logger.log (Level.FINE, "run event loop until no pending events {0}\n", this); 
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
    public boolean runPolling (long timeout)
    {
	class Timeout
	    extends TimerEvent
	{
	    Timeout (long timeout)
	    {
		super (timeout);
		logger.log (Level.FINE, "timeout {0}\n", new Long(timeout)); 
	    }
	    boolean expired = false;
	    public void execute ()
	    {
		logger.log (Level.FINE, "execute {0}\n", this); 
		expired = true;
		requestStop ();
	    }
	}
	logger.log (Level.FINE, "run polling {0}\n", this); 
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
    public void run ()
    {
	logger.log (Level.FINE, "run {0}\n", this); 
	runEventLoop (false);
    }

    public String toString ()
    {
        return ("{" + super.toString ()
                + "}");
    }
}
