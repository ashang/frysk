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

package frysk.testbed;

import frysk.event.EventLoop;
import frysk.event.SignalEvent;
import frysk.sys.SignalSet;
import frysk.junit.TestCase;
import frysk.sys.Signal;
import frysk.rsl.Log;

/**
 * Installs signal-handlers for, and then runs the event loop until
 * the specified set of signals have all been received.
 */

public final class SignalWaiter extends TestCase {
    private static final Log fine = Log.fine(SignalWaiter.class);

    private final String reason;
    private final Signal[] sigs;
    private final EventLoop eventLoop;
    private final SignalSet outstanding;
    
    private class AckSignal
        extends SignalEvent
    {
	private final SignalSet outstanding;
	private final EventLoop eventLoop;
	AckSignal(Signal sig, SignalSet outstanding, EventLoop eventLoop) {
	    super(sig);
	    this.outstanding = outstanding;
	    this.eventLoop = eventLoop;
	}
	
	public void execute() {
	    fine.log(this, "execute", reason);
	    eventLoop.requestStop();
	    eventLoop.remove(this);
	    outstanding.remove(this.getSignal());
	}
    }
    
    /**
     * Install signal-handlers for the specified set of events.
     */
    public SignalWaiter (EventLoop eventLoop, Signal[] sigs, String why) {
	this.eventLoop = eventLoop;
	this.sigs = sigs;
	this.reason = why + "(signals " + new SignalSet(this.sigs) + ")";
	outstanding = new SignalSet (sigs);
	// Install signal handlers for all signals
	for (int i = 0; i < sigs.length; i++) {
	    eventLoop.add (new AckSignal (sigs[i], outstanding, eventLoop));
	}
    }

    public SignalWaiter (EventLoop eventLoop, Signal sig, String why) {
	this(eventLoop, new Signal[] {sig}, why);
    }

    public String toString ()
    {
	return super.toString() + "," + reason;
    }

    /**
     * Runs the EventLoop until all specified signals have been
     * received.
     */
    public void assertRunUntilSignaled() {
	// Run the event loop.
	while (outstanding.size() > 0) {
	    fine.log(this, "start; outstanding", outstanding);
	    assertTrue("run until signal: " + reason
		       + "; outstanding: " + outstanding,
		       eventLoop.runPolling(getTimeoutMilliseconds()));
	    fine.log(this, "stop; outstanding", outstanding);
	}
    }
}
