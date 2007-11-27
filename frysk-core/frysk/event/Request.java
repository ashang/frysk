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

/**
 * Framework for implementing remote-procedure-call requests executed
 * by the event-loop thread.
 */
public abstract class Request
    implements Event
{
    private final EventLoop eventLoop;
    /**
     * Create an event-loop RPC framework.
     */
    protected Request (EventLoop eventLoop)
    {
	this.eventLoop = eventLoop;
    }

    /**
     * Class that does the request dispatch and execute.
     */
    private class Handler
	implements Event
    {
	private RuntimeException runtimeException;
        private boolean executed = false;
	public synchronized void execute ()
	{
	    try {
		Request.this.execute();
	    }
	    catch (RuntimeException r) {
		runtimeException = r;
	    }
            executed = true;
	    notify();
	}
	private synchronized void request ()
	{
	    runtimeException = null;
	    eventLoop.add(this);
            while (!executed) {
                try {
                    wait();
                    if (executed)
                        break;
                }
                catch (InterruptedException r) {
                }   
            }
            executed = false;   // Can requests ever be recycled?
	    if (runtimeException != null)
		throw runtimeException;
	}
    }
    private final Handler handler = new Handler ();

    /**
     * Is this the event-loop thread?  If it is the caller should just
     * execute the request directly.
     */
    protected final boolean isEventLoopThread()
    {
	return eventLoop.isCurrentThread();
    }

    /**
     * Make a request to run execute() on the eventLoop thread.
     *
     * The sub-class will look something like:
     *
     *<pre>
     *if (isEventLoopThread())
     *  execute(with, args)
     *else {
     *  synchronized(this) {
     *    super.request();
     *  }
     *}
     *</pre>
     */
    protected void request ()
    {
	// Code must check isEventLoopThread before calling here.
	if (eventLoop.isCurrentThread())
	    throw new RuntimeException("must not be event-loop thread");
	// Code must serialize the requests by locking this object
	// before calling here.
	if (!Thread.holdsLock(this))
	    throw new RuntimeException("must hold object's lock");
	// Pass the request on down.
	handler.request ();
    }
}
