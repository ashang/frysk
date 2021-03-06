// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.sys.ProcessIdentifier;
import frysk.sys.Signal;
import frysk.sys.Tid;
import frysk.sys.WaitBuilder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import frysk.rsl.Log;

/**
 * Implements an event loop.
 */

public abstract class EventLoop extends Thread {
    private static final Log fine = Log.fine(EventLoop.class);

    /**
     * The EventLoop uses Signal.IO to wake up, or unblock, the
     * event-loop thread when a request comes in.
     */
    protected EventLoop()
    {
	signalEmpty ();
	// Signal.IO is used to wake up a blocked event loop when an
	// asynchronous event arrives.
	signalAdd(Signal.IO);
	fine.log(this, "new"); 
    }


    /**
     * Clear the signal set being used by the event-loop.
     */
    protected abstract void signalEmpty();
    /**
     * Add Signal to the signals that can be received.
     */
    protected abstract void signalAdd(Signal sig);


    /**
     * Add support for the notification of waitpid events.
     */
    public abstract void add (WaitBuilder waitBuilder);
    /**
     * Block for up-to TIMEOUT, or until an event arrives, or possibly
     * no-reason.
     */
    protected abstract void block (long millisecondTimeout);


    /**
     * The EventLoop's thread ID.  If a thread, other than the
     * EventLoop thread modifies any of the event queues, the event
     * thread will need to be woken up using a Signal.IO.
     */
    private ProcessIdentifier tid = null; // can change once
    final boolean isCurrentThread() {
	if (tid == null) {
	    updateTid();
	    return true;
	}
	return tid == Tid.get();
    }
    private void wakeupBlockedEventLoop() {
	// Some how got into a state where both the event-loop is
	// running (isGoingToBlock) and the event-loop thread-id
	// wasn't set.
	if (tid == null)
	    throw new RuntimeException ("EventLoop.tid botch");
	Signal.IO.tkill(tid);
    }
    private Exception firstSet;
    private void updateTid() {
	ProcessIdentifier newTid = Tid.get();
	if (tid == null) {
 	    firstSet = new Exception();
	    tid = newTid;
	    return;
	}
	if (tid != newTid) {
	    throw new RuntimeException ("EventLoop.tid changing from "
					+ tid + " to " + newTid, firstSet);
	}
    }
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
	fine.log(this, "wakeupIfBlocked"); 
	if (isGoingToBlock) {
	    wakeupBlockedEventLoop();
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
	fine.log(this, "add TimerEvent"); 
	timerEvents.put (t, t);
	wakeupIfBlocked ();
    }
    /**
     * Remove the timer event from the event queue.
     */
    public synchronized void remove (TimerEvent t)
    {
	fine.log(this, "remove TimerEvent"); 
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
	fine.log(this, "checkForTimerEvents"); 
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
	fine.log(this, "add SignalEvent", signalEvent);
	Object old = signalHandlers.put (signalEvent.getSignal(), signalEvent);
	if (old == null)
	    // New signal, tell Poll.
	    signalAdd (signalEvent.getSignal());
	wakeupIfBlocked ();
    }
    /**
     * Remove the signal event handler, further occurances of the
     * signal are discarded.
     */
    public synchronized void remove (SignalEvent signalEvent)
    {
	fine.log(this, "remove SignalEvent", signalEvent); 
	signalHandlers.remove (signalEvent.getSignal());
	// XXX: Poll.SignalSet.remove (sig.signal);
    }
    /**
     * Process the signal.  Find the applicable handler and, if
     * present, append the corresponding delivery event.  Since a
     * signal delivery un-blocks the poll, the event thread is no
     * longer going to block.
     */
    protected synchronized void processSignal(Signal sig) {
	fine.log(this, "processSignal", sig); 
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
	fine.log(this, "add Event", e);
	pendingEvents.add (e);
	wakeupIfBlocked ();
    }
    /**
     * Remove the pending event.
     */
    public synchronized void remove (Event e)
    {
	fine.log(this, "remove Event", e); 
	pendingEvents.remove (e);
    }
    /**
     * Execute the event on the event-loop thread; return when
     * completed.  If these requests are ongoing then consider
     * creating a dedicated Request object.
     */
    public void execute (Event e)
    {
	request.request(e);
    }
    private class ExecuteRequest
	extends Request
    {
	ExecuteRequest ()
	{
	    super (EventLoop.this);
	}
	private Event op;
	public void execute ()
	{
	    op.execute();
	}
	void request (Event op)
	{
	    if (isEventLoopThread()) {
		// On event-loop thread, dispatch immediatly.
		op.execute();
	    }
	    else synchronized (this) {
		this.op = op;
		request ();
	    }
	}
    }
    private final ExecuteRequest request = new ExecuteRequest();


    /**
     * Remove and return the first pending event, or null if there are
     * no pending event.  Also set the isGoingToBlock flag (after all
     * with the event queue empty there is nothing better to do).
     */
    private synchronized Event remove ()
    {
	fine.log(this, "remove ..."); 
	if (pendingEvents.isEmpty ()) {
	    isGoingToBlock = true;
	    return null;
	}
	else {
	    Event removed = (Event) pendingEvents.remove (0);
	    fine.log(this, "remove ... return", removed); 
	    return removed;
	}
    }


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
	fine.log(this, "runEventLoop pendingOnly", pendingOnly); 
	try {
	    // Assert: isGoingToBlock == false
	    stop = pendingOnly;
	    while (true) {
		// Drain any pending events.
		for (Event e = remove (); e != null; e = remove ()) {
		    fine.log(this, "runEventLoop executing", e);
		    e.execute ();
		}
		// {@link #remove()} will have set {@link
		// #isGoingToBlock}.
		if (stop)
		    break;
		long timeout = getTimerEventMillisecondTimeout ();
		block (timeout);
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
	fine.log(this, "requestStop"); 
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
    public final void runPending ()
    {
	fine.log(this, "runPending"); 
	updateTid();
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
	fine.log(this, "runPolling timeout", timeout); 
	updateTid();
	class Timeout
	    extends TimerEvent
	{
	    Timeout (long timeout)
	    {
		super (timeout);
		fine.log(this, "timeout");
	    }
	    boolean expired = false;
	    public void execute ()
	    {
		fine.log(this, "execute"); 
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
    private class Running {
        boolean isRunning = false;
    }
    
    public final void run ()
    {
	fine.log(this, "run"); 
	// Finish initializing, and then ack that the thread is
	// running.
	synchronized (running) {
	    updateTid();
            running.isRunning = true;
	    running.notify();
	}
	runEventLoop (false);
    }
    /**
     * Start the EventLoop thread; note that this forces a brief
     * synchronization with that thread to ensure that it is ready.
     */
    public synchronized void start()
    {
	fine.log(this, "start"); 
	synchronized (running) {
	    setDaemon(true);
	    super.start();
	    // Make certain that the server really is running.
            while (!running.isRunning) {
                try {
                    running.wait();
                }
                catch (InterruptedException ie) {
                }
            }
	}
    }
    private Running running = new Running();


    /**
     * Return the current prefered flavour of the event-loop.
     */
    public static EventLoop factory()
    {
	return new WaitEventLoop();
    }
}
