// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.sys;

import frysk.junit.TestCase;

/**
 * Test the wait interface, in particular waitAll with blocking.
 */

public class TestWait
    extends TestCase
{
    /**
     * Builder that fails any wait event that it sees.
     */
    private final UnhandledWaitBuilder unhandledWaitBuilder =
	new UnhandledWaitBuilder ()
	{
	    protected void unhandled (String what)
	    {
		fail (what);
	    }
	};
    /**
     * Builder that fails any signal that it sees.
     */
    private final SignalBuilder unhandledSignalBuilder =
	new SignalBuilder ()
	{
	    public void signal (Sig sig)
	    {
		fail ("signal " + sig + " received");
	    }
	};


    private final int shortTimeout = 100;
    private long startTime;
    private long endTime;

    public void setUp ()
    {
	Wait.signalEmpty ();
	startTime = System.currentTimeMillis ();
    }

    public void tearDown ()
    {
	endTime = System.currentTimeMillis ();
	assertTrue ("timeout",
		     startTime + getTimeoutMilliseconds () > endTime);
    }

    public void testZeroTimeout ()
    {
	Wait.waitAll (0, unhandledWaitBuilder, unhandledSignalBuilder);
    }

    public void testShortTimeout ()
    {
	Wait.waitAll (shortTimeout, unhandledWaitBuilder,
		      unhandledSignalBuilder);
	assertTrue ("more than shortTime passed",
		    System.currentTimeMillis () > startTime + shortTimeout);
    }
    
    public void testSignals ()
    {
	Wait.signalAdd (Sig.USR1);
	class Signals
	    implements SignalBuilder
	{
	    boolean received = false;
	    public void signal (Sig sig)
	    {
		received = (sig == Sig.USR1);
	    }
	}
	Signals signals = new Signals ();
	Signal.tkill (Tid.get (), Sig.USR1);
	Wait.waitAll (getTimeoutMilliseconds (),
		      unhandledWaitBuilder, signals);
	assertTrue ("signals.received", signals.received);
    }

    /**
     * Class to capture termination information.
     */
    private static class WaitOnChild
	extends UnhandledWaitBuilder
    {
	int pid = 0;
	boolean signal;
	int value;
	protected void unhandled (String what)
	{
	    fail (what);
	}
	public void terminated (int pid, boolean signal, int value,
				boolean coreDumped)
	{
	    this.pid = pid;
	    this.signal = signal;
	    this.value = value;
	}
    }

    public void testWaitExit0 ()
    {
	WaitOnChild waitOnChild = new WaitOnChild ();
	int pid = Fork.exec (new String[] { "/bin/true" });
	Wait.waitAll (getTimeoutMilliseconds (), waitOnChild,
		      unhandledSignalBuilder);
	assertEquals ("pid", pid, waitOnChild.pid);
	assertEquals ("signal", false, waitOnChild.signal);
	assertEquals ("value", 0, waitOnChild.value);
    }

    public void testWaitExit1 ()
    {
	WaitOnChild waitOnChild = new WaitOnChild ();
	int pid = Fork.exec (new String[] { "/bin/false" });
	Wait.waitAll (getTimeoutMilliseconds (), waitOnChild,
		      unhandledSignalBuilder);
	assertEquals ("pid", pid, waitOnChild.pid);
	assertEquals ("signal", false, waitOnChild.signal);
	assertEquals ("value", 1, waitOnChild.value);
    }

    public void testNoWaitBuilder ()
    {
	Wait.waitAll (0, null, unhandledSignalBuilder);
    }
}
