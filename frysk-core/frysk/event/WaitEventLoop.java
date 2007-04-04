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

import frysk.sys.Wait;
import frysk.sys.WaitBuilder;
import frysk.sys.SignalBuilder;
import frysk.sys.Sig;
import frysk.sys.Tid;
import java.util.List;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * Implements an event loop using waitpid.
 */

public class WaitEventLoop
    extends EventLoop
{
    /**
     * Create an event loop, re-initialize any static data.
     */
    public WaitEventLoop ()
    {
	// Make certain that the global signal set is empty.
	Wait.signalEmpty ();
	// Sig.IO is used to wake up a blocked event loop when an
	// asynchronous event arrives.
	Wait.signalAdd (Sig.IO);
	logger.log (Level.FINE, "{0} new\n", this); 
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
	logger.log (Level.FINEST, "{0} wakeupIfBlocked\n", this); 
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
	logger.log (Level.FINEST, "{0} add TimerEvent\n", this); 
	timerEvents.put (t, t);
	wakeupIfBlocked ();
    }
    /**
     * Remove the timer event from the event queue.
     */
    public synchronized void remove (TimerEvent t)
    {
	logger.log (Level.FINEST, "{0} remove TimerEvent\n", this); 
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
	logger.log (Level.FINEST, "{0} checkForTimerEvents\n", this); 
	long time = java.lang.System.currentTimeMillis ();
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
     * Add FD to events that should be polled.
     */
    public synchronized void add (WaitBuilder waitBuilder)
    {
	logger.log (Level.FINE, "{0} add PollEvent\n", this);
	this.waitBuilder = waitBuilder;
	wakeupIfBlocked ();
    }
    private WaitBuilder waitBuilder;

    /**
     * Collection of signals; assume that very few signals are being
     * watched and hence that a small map is sufficient.
     */
    private Map signalHandlers = new HashMap ();
    /**
     * Add the signal handler, signals are processed and then
     * delivered using the event-loop.
     */
    public synchronized void add (SignalEvent signalEvent)
    {
	logger.log (Level.FINEST, "{0} add SignalEvent {1}\n",
		    new Object[] { this, signalEvent });
	Object old = signalHandlers.put (signalEvent.getSig (), signalEvent);
	if (old == null)
	    // New signal, tell Poll.
	    Wait.signalAdd (signalEvent.getSig ());
	wakeupIfBlocked ();
    }
    /**
     * Remove the signal event handler, further occurances of the
     * signal are discarded.
     */
    public synchronized void remove (SignalEvent signalEvent)
    {
	logger.log (Level.FINE, "{0} remove SignalEvent\n", this); 
	signalHandlers.remove (signalEvent.getSig ());
	// XXX: Poll.SignalSet.remove (sig.signal);
    }
    /**
     * Process the signal.  Find the applicable handler and, if
     * present, append the corresponding delivery event.  Since a
     * signal delivery un-blocks the poll, the event thread is no
     * longer going to block.
     */
    private synchronized void processSignal (Sig sig)
    {
	logger.log (Level.FINEST, "{0} processSignal Sig\n", this); 
	SignalEvent handler = (SignalEvent) signalHandlers.get (sig);
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
	logger.log (Level.FINEST, "{0} add Event\n {1}", new Object[] {this, e}); 
	pendingEvents.add (e);
	wakeupIfBlocked ();
    }
    /**
     * Remove the pending event.
     */
    public synchronized void remove (Event e)
    {
	logger.log (Level.FINE, "{0} remove Event\n", this); 
	pendingEvents.remove (e);
    }
    /**
     * Remove and return the first pending event, or null if there are
     * no pending event.  Also set the isGoingToBlock flag (after all
     * with the event queue empty there is nothing better to do).
     */
    private synchronized Event remove ()
    {
	logger.log (Level.FINEST, "{0} remove\n", this); 
	if (pendingEvents.isEmpty ()) {
	    isGoingToBlock = true;
	    return null;
	}
	else {
	    Event removed = (Event) pendingEvents.remove (0);
	    logger.log (Level.FINEST, "... return {0}\n", removed); 
	    return removed;
	}
    }

    /**
     * Handle anything that comes back from the poll call.
     */
    private SignalBuilder signalBuilder = new SignalBuilder ()
	{
	    public void signal (Sig sig)
	    {
		logger.log (Level.FINEST, "{0} PollBuilder.signal Sig\n", this); 
		processSignal (sig);
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
    protected void runEventLoop (boolean pendingOnly)
    {
	logger.log (Level.FINEST, "{0} runEventLoop\n", this); 
	try {
	    // Assert: isGoingToBlock == false
	    tid = Tid.get ();
	    stop = pendingOnly;
	    while (true) {
		// Drain any pending events.
		for (Event e = remove (); e != null; e = remove ()) {
		  logger.logp(Level.FINEST, "EventLoop", "runEventLoop",
			      "executing {0}\n", e);
		    e.execute ();
		}
		// {@link #remove()} will have set {@link
		// #isGoingToBlock}.
		if (stop)
		    break;
		long timeout = getTimerEventMillisecondTimeout ();
		Wait.waitAll (timeout, waitBuilder, signalBuilder);
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
	logger.log (Level.FINEST, "{0} requestStop\n", this); 
	stop = true;
	wakeupIfBlocked ();
    }
    private volatile boolean stop;
}
