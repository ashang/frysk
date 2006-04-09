// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.proc;

import frysk.event.Event;
import frysk.sys.Sig;
import frysk.sys.proc.Stat;
import java.lang.Thread;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

/**
 * Check that random events, arriving mid-way through a detach, are
 * handled.
 */

public class TestTaskObserverDetach
    extends TestLib
{
    boolean skip = true;
    /**
     * Co-ordinates a detach in the middle of some over random event.
     */
    abstract class Detach
	extends TaskObserverBase
	implements TaskObserver.Signaled
    {
	public void addedTo (Object o)
	{
	    Manager.eventLoop.requestStop ();
	}
	public void deletedFrom (Object o)
	{
	}
	public Action updateSignaled (Task task, int signal)
	{
	    Manager.eventLoop.requestStop ();
	    return Action.CONTINUE;
	}
	/** The signal to send to the task so that it will perform an
	 * additiona request. */
	abstract Sig eventSignal ();
	/** The list of ack signals that the task will send back to
	 * this process indicating that the requested operation has
	 * been completed.  */
	abstract Sig[] eventAcks ();
	/** Ask the derived task to add an observer for the event that
	 * requestSignal triggers.  */
	abstract void addEventObserver (Task task);
	/** Is the event the signal?  If it isn't then the signal
	 * should be delivered before attempting the detach.  */
	abstract boolean eventIsSignal ();
	/** Remove the observer looking for the relevant event.  */
	abstract void deleteEventObserver (Task task);
	/** The process.  */
	AckProcess child = new AckDaemonProcess ();
	/** It's main task.  */
	final Task task = child.findTaskUsingRefresh (true);
	/**
	 * Set up a detach test.
	 */
	Detach ()
	{
	    // Install a signaled observer so that signal delivery can
	    // be seen (and possibly blocked).
	    task.requestAddSignaledObserver (this);
	    assertRunUntilStop ("adding signaled observer");
	    addEventObserver (task);
	    assertRunUntilStop ("adding observer of requested event");
	}
	/**
	 * Run the test asserting that the attach was successful.
	 */
	void assertDetach ()
	{
	    if (eventIsSignal ())
		// Send the signal to the task making that signal
		// delivery the pending event.
		child.signal (eventSignal ());
	    else {
		// Send the event's signal to the process, and then
		// allow the signal to be delivered, this allows the
		// inferior to run upto the point where the requested
		// action has been started.
		child.signal (eventSignal ());
		assertRunUntilStop ("delivering signal");
	    }
	    
	    // Poll until process goes into T state (indicating
	    // pending trace event); horrible race condition here.
	    // Can't use the event loop as that would detect, and soak
	    // up the event that is ment to be left sitting in the
	    // event loop.
	    Stat stat = new Stat ();
	    for (int i = 0; i < 100; i++) {
		assertTrue ("stat refresh", stat.refresh (task.getTid ()));
		if (stat.state == 'T')
		    break;
		try {
		    Thread.sleep (50); // milliseconds.
		}
		catch (Exception e) {
		    fail (e.toString ());
		}
	    }
	    assertEquals ("stat.state", 'T', stat.state);
	    
	    // Set up an ack handler to catch the process
	    // acknowledging that it has completed the relevant task.
	    AckHandler ackHandler = new AckHandler (eventAcks (), "eventAcks");

	    // Remove all observers, this will cause the process to
	    // start transitioning to the detached state.
	    deleteEventObserver (task);
	    task.requestDeleteSignaledObserver (this);

	    // Run the event-loop out waiting for the final detach.
	    task.proc.observableDetached.addObserver (new Observer ()
		{
		    Proc proc = task.proc;
		    public void update (Observable obj, Object arg)
		    {
			proc.observableAttached.deleteObserver (this);
			Manager.eventLoop.requestStop ();
		    }
		});

	    logger.log (Level.FINE, "{0} waiting for detach\n", this);
	    ackHandler.await ("attempting detach");
	}
    }
    /**
     * Check that a fork, arriving mid-way through a detach, is
     * handled.
     */
    public void testDetachFork ()
    {
	class DetachFork
	    extends Detach
	    implements TaskObserver.Forked
	{
	    Sig eventSignal () { return addForkSig; }
	    Sig[] eventAcks () { return spawnAck; }
	    boolean eventIsSignal () { return false; }
	    DetachFork ()
	    {
		super ();
	    }
	    void addEventObserver (Task task)
	    {
		task.requestAddForkedObserver (this);
	    }
	    void deleteEventObserver (Task task)
	    {
		task.requestDeleteForkedObserver (this);
	    }
	    public Action updateForked (Task task, Task fork)
	    {
		fail ("forked");
		return null;
	    }
	}
	new DetachFork ().assertDetach ();
    }
    /**
     * Check that a clone, arriving mid-way through a detach, is
     * handled.
     */
    public void testDetachClone ()
    {
	if (skip) return;
	class DetachClone
	    extends Detach
	    implements TaskObserver.Cloned
	{
	    Sig eventSignal () { return addCloneSig; }
	    Sig[] eventAcks () { return spawnAck; }
	    boolean eventIsSignal () { return false; }
	    DetachClone ()
	    {
		super ();
		       
	    }
	    void addEventObserver (Task task)
	    {
		task.requestAddClonedObserver (this);
	    }
	    void deleteEventObserver (Task task)
	    {
		task.requestDeleteClonedObserver (this);
	    }
	    public Action updateCloned (Task task, Task fork)
	    {
		fail ("cloned");
		return null;
	    }
	}
	new DetachClone ().assertDetach ();
    }
    /**
     * Check that a exec, arriving mid-way through a detach, is
     * handled.
     */
    public void testDetachExec ()
    {
	if (skip) return;
	class DetachExec
	    extends Detach
	    implements TaskObserver.Execed
	{
	    Sig eventSignal () { return execSig; }
	    Sig[] eventAcks () { return execAck; }
	    boolean eventIsSignal () { return false; }
	    DetachExec ()
	    {
		super ();
	    }
	    void addEventObserver (Task task)
	    {
		task.requestAddExecedObserver (this);
	    }
	    void deleteEventObserver (Task task)
	    {
		task.requestDeleteExecedObserver (this);
	    }
	    public Action updateExeced (Task task)
	    {
		fail ("execed");
		return null;
	    }
	}
	new DetachExec ().assertDetach ();
    }
    /**
     * Check that a signal, arriving mid-way through a detach, is
     * handled.
     */
    public void testDetachSignal ()
    {
	if (skip) return;
	class DetachSignal
	    extends Detach
	{
	    Sig eventSignal () { return Sig.TERM; }
	    Sig[] eventAcks () { return new Sig[0]; }
	    boolean eventIsSignal () { return true; }
	    void addEventObserver (Task task)
	    {
		Manager.eventLoop.add (new Event ()
		    {
			public void execute ()
			{
			    Manager.eventLoop.requestStop ();
			}
		    });
	    }
	    void deleteEventObserver (Task task)
	    {
	    }
	}
	new DetachSignal ().assertDetach ();
    }
}
