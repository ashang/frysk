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

package frysk.sys;

/**
 * Server thread for executing things like ptrace calls which, due to
 * a Linux miss-feature, must all be initiated from a single thread.
 */

public class Server
    extends Thread
{
    /**
     * Do not allow extension.
     */
    private Server ()
    {
    }
    /**
     * Create a running server thread.
     */
    static private Server newServer ()
    {
	Server server = new Server ();
	synchronized (server) {
	    server.setDaemon(true);
	    server.start ();
	    // Make certain that the server thread is both running and
	    // waiting for a request before proceeding further.
	    try {
		server.wait();
	    } catch (InterruptedException ie) {
		throw new RuntimeException (ie);
	    }
	}
	return server;
    }

    /**
     * The thread explicitly used for ptrace calls.
     */
    static private Server server = newServer ();
    
    /**
     * Have the server thread execute a request.
     */
    public static void request (Execute op)
    {
	server.execute (op);
    }
    
    private Execute op;
    private Object serializeRequests = new Object ();
    private RuntimeException exception;

    /**
     * Process the request.
     */
    private void execute (Execute op)
    {
	// Requests are serialized - only one client can interact with
	// the server at any time.
	synchronized (serializeRequests) {
	    // Get the server's attention, pass it the request.
	    this.op = op;
	    this.exception = null;
	    synchronized (this) {
		notify();
		// Wait for the reply.
		try {
		    wait();
		} catch (InterruptedException ie) {
		    throw new RuntimeException (ie);
		}
	    }
	    if (exception != null)
		throw exception;
	}
    }

    /**
     * RUN - Main thread execution method.
     *
     * Loop and wait for notification events, signalling that the core
     * thread and relevant data are ready to have a ptrace call
     * executed and data returned.
     */
    public void run() 
    {
	synchronized (this) {
	    // Let newServer know that this task is running.
	    notify();
	    // Run for ever, serving each request as it comes in.
	    while (true) {
		try {
		    wait();
		    // Been notify()ed that the request is ready,
		    // execute it.
		    this.op.execute ();
		}
		catch (RuntimeException e) {
		    exception = e;
		}
		catch (InterruptedException e) {
		    exception = new RuntimeException (e);
		}
		// And ack back the requester.
		notify();
	    }
	}
    }
}
