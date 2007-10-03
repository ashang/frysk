// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Tid;
import frysk.junit.TestCase;

/**
 * Framework for testing different event loops.
 */

abstract class EventLoopTestBed
    extends TestCase
{
    private EventLoop eventLoop;
    private int eventTid;

    /**
     * Return the event loop to be tested.
     */
    protected abstract EventLoop newEventLoop ();

    /**
     * Re-create the event loop ready for the next test.  Always
     * include a CNTRL-C handler so that the tests can be aborted.
     */
    public void setUp ()
    {
	eventLoop = newEventLoop ();
	eventLoop.add (new SignalEvent (Sig.INT)
	    {
		public void execute ()
		{
		    fail ("Got CNTRL-C");
		}
	    });
	eventTid = Tid.get ();
    }

    /**
     * Reap any stray signals.
     */
    public void tearDown ()
    {
	// Make certain that the event loop died.
	eventLoop.requestStop();
	Signal.drain (Sig.USR1);
	Signal.drain (Sig.CHLD);
    }

    /**
     * Test countdown timers alternating with signals.
     */
    public void testCountDownTimersAndSignals ()
    {
	class Counters
	{
	    int numberOfSignalEvents;
	    int numberOfTimerEvents;
	}
	final Counters counters = new Counters ();
	eventLoop.add (new TimerEvent (250)
	    {
		Counters count = counters;
		public void execute ()
		{
		    count.numberOfTimerEvents += 1;
		    assertEquals ("count.numberOfSignalEvents",
				  0, count.numberOfSignalEvents);
		    assertEquals ("count.numberOfTimerEvents",
				  1, count.numberOfTimerEvents);
		    Signal.tkill (eventTid, Sig.USR1);
		}
	    });
	eventLoop.add (new SignalEvent (Sig.USR1)
	    {
		Counters count = counters;
		public void execute ()
		{
		    count.numberOfSignalEvents += 1;
		    assertEquals ("count.numberOfSignalEvents",
				  1, count.numberOfSignalEvents);
		    assertEquals ("count.numberOfTimerEvents",
				  1, count.numberOfTimerEvents);
		}
	    });
	eventLoop.add (new TimerEvent (750)
	    {
		Counters count = counters;
		public void execute ()
		{
		    count.numberOfTimerEvents++;
		    assertEquals ("count.numberOfSignalEvents",
				  1, count.numberOfSignalEvents);
		    assertEquals ("count.numberOfTimerEvents",
				  2, count.numberOfTimerEvents);
		    eventLoop.requestStop ();
		}
	    });
	eventLoop.run ();

	// Check the final count.
	assertEquals ("SignalEvent count", 1, counters.numberOfSignalEvents);
	assertEquals ("TimerEvent count", 2, counters.numberOfTimerEvents);
    }

    /**
     * Test the periodic timer.
     */
    public void testPeriodicTimer ()
    {
	class CountingTimer extends TimerEvent
	{
	    int count;
	    int limit;
	    CountingTimer (long first, long period, int limit)
	    {
		super (first, period);
		this.limit = limit;
	    }
	    public void execute ()
	    {
		assertTrue ("Timer Runaway", count < limit);
		count += getCount ();
	    }
	}
	CountingTimer countingTimer = new CountingTimer (50, 200, 3);

	eventLoop.add (countingTimer);
	eventLoop.add (new TimerEvent (500)
	    {
		public void execute ()
		{
		    eventLoop.requestStop ();
		}
	    });
	eventLoop.run ();
				 
	// Check the final count.
	assertEquals ("Period Timer Count", 3, countingTimer.count);
    }

    /**
     * Test the removal of a timer.
     */
    public void testTimerRemoval ()
    {
	// A timer that removes it's "timerToRemove" member.
	class TimerRemover
	    extends TimerEvent
	{
	    TimerEvent timerToRemove;
	    TimerRemover (long time)
	    {
		super (time);
	    }
	    public void execute ()
	    {
		assertNotNull ("There is a timer to remove", timerToRemove);
		eventLoop.remove (timerToRemove);
	    }
	}

	// A timer that fails if executed.
	class FailTimer extends TimerEvent
	{
	    FailTimer (long time)
	    {
		super (time);
	    }
	    public void execute ()
	    {
		fail ("Timer Should Not Execute");
	    }
	}

	// Create two timers scheduled to run in sequence. The first
	// ("timerRemover"), when executes, removes the second
	// ("timerToRemove").  The removeTimer must be created before
	// "timerToRemove" to guarantee that its scheduled run-time is
	// earlier.
	TimerRemover timerRemover = new TimerRemover (1);
	TimerEvent timerToRemove = new FailTimer (2);
	timerRemover.timerToRemove = timerToRemove;

	// Insert these timers into the list.
	eventLoop.add (timerToRemove);
	eventLoop.add (timerRemover);

	// Finally, create a timer that shuts the event-loop down.
	eventLoop.add (new TimerEvent (3)
	    {
		public void execute ()
		{
		    eventLoop.requestStop ();
		}
	    });

	eventLoop.run ();
    }


    /**
     * Test that events scheduled before run are processed.
     *
     * This checks that any synchronous events appended before the
     * event loop is started are always run.
     */
    public void testScheduleBeforeRun ()
    {
	class DidExecute
	    implements Event
	{
	    boolean ran = false;
	    public void execute ()
	    {
		ran = true;
	    }
        
        public String toString()
        {
          return ("[DidExecute " + ran + " ]");
        }
	}

	// Schedule an immediate event that sets a marker indicating
	// that it ran.
	DidExecute firstExecute = new DidExecute ();
	eventLoop.add (firstExecute);

	// Schedule an immediate event that shuts down the event loop.
	eventLoop.add (new RequestStopEvent(eventLoop));


	// Schedule a further immediate event that sets a marker
	// indicating that it ran.
	DidExecute secondExecute = new DidExecute ();
	eventLoop.add (secondExecute);

	// Schedule a timer event that, here, will never be executed
	// since the above stop event will first be processed
	// preventing the processing of asynchronous events.
	eventLoop.add (new TimerEvent (0)
	    {
		public void execute ()
		{
		    fail ("Timer event should not run");
		}
	    });

	eventLoop.run ();

	assertTrue ("First synchronous event executed", firstExecute.ran);
	assertTrue ("Second synchronous event executed", firstExecute.ran);
    }

    /**
     * Test adding and removing a signal handler.
     *
     * This adds / removes / adds a SIGCHLD handler (by default
     * SIGCHLD signals are ignored).and then checks that when it is
     * installed the event occurs, but when it is un-installed, the
     * event is lost.
     */
    public void testSignalHandler ()
    {
	class SignalFired 
	    extends SignalEvent
	{
	    SignalFired (Sig sig)
	    {
		super (sig);
	    }
	    int count;
	    public void execute ()
	    {
		count++;
		eventLoop.requestStop ();		
	    }
	}
	SignalFired handler = new SignalFired (Sig.CHLD);

	// Add a handler for SIGCHILD, shoot the signal (which makes
	// it pending since there is a handler), and then run the loop
	// checking that it did, indeed fire.
	eventLoop.add (handler);
	Signal.tkill (eventTid, Sig.CHLD);
	eventLoop.runPolling (0);
	assertEquals ("One Sig.CHLD was received.", 1, handler.count);

	// Remove the handler, send a further signal, check that it
	// wasn't received.
	eventLoop.remove (handler);
	Signal.tkill (eventTid, Sig.CHLD);
	eventLoop.runPolling (0);
	assertEquals ("Still only one Sig.CHLD (no additions).",
		      1, handler.count);
	
	// Re-add the CHLD handler, but this time twice - the
	// second add should be ignored, make certain it's again
	// receiving signal events.
	eventLoop.add (handler);
	eventLoop.add (handler);
	Signal.tkill (eventTid, Sig.CHLD);
	eventLoop.runPolling (0);
	assertEquals ("Second Sig.CHLD received.", 2, handler.count);

	// Finally remove the handler and again check no signal was
	// received (if the handler was duplicated in the signal pool
	// then it might still see the signal).
	eventLoop.remove (handler);
	Signal.tkill (eventTid, Sig.CHLD);
	eventLoop.runPolling (0);
	assertEquals ("No further SIGCHLDs.", 2, handler.count);
    }

    /**
     * Check that asynchronous events wake up the event loop.
     *
     * Create a chain of events, each relying on the previous, and
     * each relying on being processed in a timely manner -- the
     * entire operation must complete before the event-loop master
     * timer expires.
     */
    public void testAsync ()
    {
	// Set up a dummy Sig.CHLD handler, this should never occur
	// as it is overridden by an asynchronous thread before the
	// signal is delivered.
	eventLoop.add (new SignalEvent (Sig.CHLD)
	    {
		public void execute ()
		{
		    fail ("dummy signal handler run");
		}
	    });
	// Get the ball rolling, create the first thread from within
	// the running event loop.
	eventLoop.add (new TimerEvent (0)
	    {
		public void execute ()
		{
		    new EventThread ().start ();
		}
	    });
	assertTrue ("run events before master timer expires",
		    eventLoop.runPolling (5000));
    }
    /**
     * This thread asynchronously adds an event to the event loop.
     */
    private class EventThread
	extends Thread
    {
	public void run ()
	{
	    eventLoop.add (new Event ()
		{
		    public void execute ()
		    {
			new TimerThread ().start ();
		    }
		});
	}
    }
    /**
     * This thread asynchronously adds a timer to the event loop.
     */
    private class TimerThread
	extends Thread
    {
	public void run ()
	{
	    eventLoop.add (new TimerEvent (0)
		{
		    public void execute ()
		    {
			new SignalThread ().start ();
		    }
		});
	}
    }
    /**
     * This thread asynchronously adds a signal handler, and code to
     * trigger the signal, to the event loop.
     */
    private class SignalThread 
	extends Thread
    {
	public void run ()
	{
	    eventLoop.add (new SignalEvent (Sig.CHLD)
		{
		    public void execute ()
		    {
			new SleepThread ().start ();
		    }
		});
	    Signal.tkill (eventTid, Sig.CHLD);
	}
    }
    /**
     * This thread asynchronously, and after 100 milliseconds, adds a
     * final stop request.
     */
    private class SleepThread
	extends Thread
    {
	public void run ()
	{
	    try {
		sleep (100);
	    }
	    catch (InterruptedException e) {
		fail ("sleep interrupted");
	    }
	    eventLoop.add (new RequestStopEvent(eventLoop));
	    Signal.tkill (eventTid, Sig.CHLD);
	}
    }



    /**
     * Wrapper class to Request that re-dispatches to the specified
     * event.
     */
    private final class EventRequest
	extends Request
    {
	Event e;
	EventRequest (EventLoop eventLoop, Event e)
	{
	    super(eventLoop);
	    this.e = e;
	}
	public void execute()
	{
	    e.execute();
	}
	public void request()
	{
	    if (isEventLoopThread())
		execute();
	    else
		synchronized(this) {
		    super.request();
		}
	}
    }

    private abstract class RunnableEvent
	extends Thread
	implements Event
    {
	boolean executed;
	boolean ran;
	int i;
	RunnableEvent (int i)
	{
	    this.i = i;
	}
	public void execute ()
	{
	    executed = true;
	}
	public void run ()
	{
	    request ();
	    ran = true;
	}
	public abstract void request();
	public abstract RunnableEvent create(int i);
    }
    private class RunnableExecuteEvent
	extends RunnableEvent
    {
	RunnableExecuteEvent(int i)
	{
	    super(i);
	}
	public void request()
	{
	    eventLoop.execute(this);
	}
	public RunnableEvent create(int i)
	{
	    return new RunnableExecuteEvent(i);
	}
    }
    private class RunnableRequestEvent
	extends RunnableEvent
    {
	private final EventRequest r;
	RunnableRequestEvent(int i)
	{
	    super(i);
	    r = new EventRequest(eventLoop, this);
	}
	public void request()
	{
	    r.request();
	}
	public RunnableEvent create (int i)
	{
	    return new RunnableRequestEvent(i);
	}
    }
    /**
     * Test that a simple request is handled by the event-loop thread.
     */
    private void verifyRunnableEvent (RunnableEvent request)
    {
	eventLoop.start();
	request.request();
	assertTrue ("executed", request.executed);
    }
    public void testExecuteRunnable()
    {
	verifyRunnableEvent (new RunnableExecuteEvent(0));
    }
    public void testRequestRunnable()
    {
	verifyRunnableEvent (new RunnableRequestEvent(0));
    }

    /**
     * Test that many simultaneous requests, from different threads,
     * are eventually all handled.
     */
    private void verifyMany (RunnableEvent request)
    {
	eventLoop.start();
	long now = System.currentTimeMillis();
	RunnableEvent[] requests = new RunnableEvent[10];
	for (int i = 0; i < requests.length; i++) {
	    requests[i] = request.create(i);
	}
	for (int i = 0; i < requests.length; i++) {
	    requests[i].start();
	}
	for (int i = 0; i < requests.length; i++) {
	    try {
		requests[i].join (getTimeoutMilliseconds ());
	    }
	    catch (InterruptedException e) {
		throw new RuntimeException (e);
	    }
	    if (System.currentTimeMillis ()
		> now + getTimeoutMilliseconds ())
		fail ("timeout");
	    assertTrue ("executed", requests[i].executed);
	    assertTrue ("ran", requests[i].ran);
	}
    }
    public void testManyExecutes()
    {
	verifyMany (new RunnableExecuteEvent(0));
    }
    public void testManyRequests()
    {
	verifyMany (new RunnableRequestEvent(0));
    }

    private abstract class Throw
	implements Event
    {
	final RuntimeException runtimeException = new RuntimeException();
	public void execute ()
	{
	    throw runtimeException;
	}
	public abstract void request();
    }
    /**
     * Test that a throw from within the event-loop thread is
     * propogated back to the requesting thread.
     */
    private void verifyThrow (Throw request)
    {
	eventLoop.start();
	RuntimeException runtimeException = null;
	try {
	    request.request ();
	}
	catch (RuntimeException r) {
	    runtimeException = r;
	}
	assertEquals ("exception", request.runtimeException,
		      runtimeException);
    }
    public void testExecuteThrow ()
    {
	verifyThrow (new Throw ()
	    {
		public void request()
		{
		    eventLoop.execute(this);
		}
	    });
    }
    public void testRequestThrow()
    {
	verifyThrow (new Throw ()
	    {
		Request r = new EventRequest(eventLoop, this);
		public void request()
		{
		    r.request();
		}
	    });
    }

    /**
     * Test that multiple requests from the event-loop thread get
     * proccessed immediatly.
     */
    private abstract class Immediate
	implements Event
    {
	int count = 0;
	public abstract void request();
	public void execute()
	{
	    count++;
	    if (count < 5)
		request();
	}
    }
    private void verifyImmediate(Immediate immediate)
    {
	eventLoop.start();
	immediate.request();
	assertEquals ("immediate.count", 5, immediate.count);
    }
    public void testExecuteImmediate()
    {
	verifyImmediate (new Immediate ()
	    {
		public void request()
		{
		    eventLoop.execute(this);
		}
	    });
    }
    public void testRequestImmediate()
    {
	verifyImmediate (new Immediate ()
	    {
		Request r = new EventRequest (eventLoop, this);
		public void request()
		{
		    r.request();
		}
	    });
    }
}
