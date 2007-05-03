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

import frysk.sys.Poll;
import frysk.sys.PollBuilder;
import frysk.sys.Sig;
import frysk.sys.Wait;
import frysk.sys.WaitBuilder;
import java.util.logging.Level;

/**
 * Implements an event loop.
 */

class PollEventLoop
    extends EventLoop
{
    protected void signalEmpty()
    {
	Poll.empty ();
    }
    protected void signalAdd(Sig sig)
    {
	Poll.add(sig);
    }

    /**
     * Add FD to events that should be polled.
     */
    public synchronized void add (PollEvent fd)
    {
	logger.log (Level.FINE, "{0} add PollEvent\n", this);
	throw new RuntimeException ("not implemented");
    }

    /**
     * Add support for the notification of waitpid events.
     */
    public void add (WaitBuilder waitBuilder)
    {
	// When there's a SIGCHLD, poll the kernel's waitpid() queue
	// appending all the read events to event-queue as WaitEvents.
	// The are then later processed by the event-loop. The two
	// step process ensures that the underlying event-loop doesn't
	// suffer starvation - some actions such as continue lead to
	// further waitpid events and those new events should only be
	// processed after all existing events have been handled.
	class PollWaitOnSigChild
	    extends SignalEvent
	{
	    final WaitBuilder waitBuilder;
	    PollWaitOnSigChild (WaitBuilder waitBuilder)
	    {
		super(Sig.CHLD);
		this.waitBuilder = waitBuilder;
		logger.log(Level.FINE, "{0} PollWaitOnSigChld\n", this);
	    }
	    public final void execute ()
	    {
		logger.log(Level.FINE, "{0} execute\n", this);
		Wait.waitAllNoHang(waitBuilder);
	    }
	}
	add (new PollWaitOnSigChild (waitBuilder));
    }

    private PollBuilder pollObserver = new PollBuilder ()
	{
	    public String toString ()
	    {
		return ("{" + super.toString () + "}");
	    }
	    public void signal (Sig sig) {
		logger.log (Level.FINEST, "{0} PollBuilder.signal Sig\n", this); 
		processSignal (sig);
	    }
	    // Not yet using file descriptors.
	    public void pollIn (int fd) {
		throw new RuntimeException ("should not happen");
	    }
	};
    
    protected void block (long millisecondTimeout)
    {
	logger.log (Level.FINEST, "{0} block\n", this); 
	Poll.poll (pollObserver, millisecondTimeout);
    }
}
