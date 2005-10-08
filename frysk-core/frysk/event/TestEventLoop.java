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

import frysk.sys.Itimer;
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Tid;
import junit.framework.TestCase;

/**
 * Test the EventLoop object.
 *
 * Creates two timer events, then uses the underlying OS to also
 * schedule a signal tiggered by an operating system timer.  Confirms
 * that all three are delivered.
 */

public class TestEventLoop
    extends TestCase
{
    EventLoop eventLoop;
    int eventTid;

    public void setUp ()
    {
	eventLoop = new EventLoop ();
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
     * Test countdown timers, and signals.
     */
    public void testEventLoop ()
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
		    assertEquals (0, count.numberOfSignalEvents);
		    assertEquals (0, count.numberOfTimerEvents);
		    count.numberOfTimerEvents += 1;
		}
	    });
	eventLoop.add (new SignalEvent (Itimer.real (500))
	    {
		Counters count = counters;
		public void execute ()
		{
		    assertEquals (0, count.numberOfSignalEvents);
		    assertEquals (1, count.numberOfTimerEvents);
		    count.numberOfSignalEvents += 1;
		}
	    });
	eventLoop.add (new TimerEvent (750)
	    {
		Counters count = counters;
		public void execute ()
		{
		    assertEquals (1, count.numberOfSignalEvents);
		    assertEquals (1, count.numberOfTimerEvents);
		    count.numberOfTimerEvents++;
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
	// "timerToRemove" to guarentee that its scheduled run-time is
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
	}

	// Schedule an immediate event that sets a marker indicating
	// that it ran.
	DidExecute firstExecute = new DidExecute ();
	eventLoop.add (firstExecute);

	// Schedule an immediate event that shuts down the event loop.
	eventLoop.add (new Event ()
	    {
		public void execute ()
		{
		    eventLoop.requestStop ();
		}
	    });


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
     * installed the event occures, but when it is un-installed, the
     * event is lost.
     */
    public void testSignalHandler ()
    {
	class SignalFired 
	    extends SignalEvent
	{
	    SignalFired (int sig)
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
	// checking that it did, indeed fire.
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
	// Set up a dummy Sig.CHLD handler, this should never occure
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
			eventLoop.requestStop ();
		    }
		});
	    Signal.tkill (eventTid, Sig.CHLD);
	}
    }
}
