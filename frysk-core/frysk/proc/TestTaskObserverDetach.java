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

package frysk.proc;

import frysk.testbed.StatState;
import frysk.event.RequestStopEvent;
import frysk.sys.Signal;
import java.util.Observable;
import java.util.Observer;
import frysk.rsl.Log;
import frysk.testbed.SignalWaiter;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;
import frysk.testbed.TaskObserverBase;

/**
 * Check that random events, arriving mid-way through a detach, are
 * handled.
 */

public class TestTaskObserverDetach extends TestLib {
    private static final Log fine = Log.fine(TestTaskObserverDetach.class);

    /**
     * Co-ordinates a detach in the middle of some over random event.
     */
    abstract class Detach extends TaskObserverBase
	implements TaskObserver.Signaled
    {
	public void addedTo (Object o) {
	    Manager.eventLoop.requestStop ();
	}
	public void deletedFrom (Object o) {
	}
	public Action updateSignaled (Task task,
				      frysk.isa.signals.Signal signal) {
	    Manager.eventLoop.requestStop ();
	    return Action.CONTINUE;
	}
	/** Request that the target trigger the required event; return
	 * the expected acknowledge signals.  */
	abstract Signal[] requestEvent ();
	/** Ask the derived task to add an observer for the event that
	 * requestSignal triggers.  */
	abstract void addEventObserver (Task task);
	/** Is the event the signal?  If it isn't then the signal
	 * should be delivered before attempting the detach.  */
	abstract boolean eventIsSignal ();
	/** Remove the observer looking for the relevant event.  */
	abstract void deleteEventObserver (Task task);
	/** The process.  */
	SlaveOffspring child = SlaveOffspring.createDaemon ();
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
	    // Request that the event occure.
	    Signal[] eventAcks = requestEvent();
	    // When the event is the signal the event-loop doesn't
	    // need to be run, just need to wait for the signal to
	    // reach the thread.  However for other cases, the
	    // event-loop needs to run just long enough for the signal
	    // to be delivered to the process so that it will trigger
	    // the desired event.
	    if (!eventIsSignal ())
		assertRunUntilStop ("delivering signal");
	    
	    StatState.TRACED_OR_STOPPED.assertIs(task);
	    
	    // Set up an ack handler to catch the process
	    // acknowledging that it has completed the relevant task.
	    SignalWaiter ackHandler = new SignalWaiter (Manager.eventLoop,
							eventAcks,
							"eventAcks - detach");

	    // Remove all observers, this will cause the process to
	    // start transitioning to the detached state.
	    deleteEventObserver (task);
	    task.requestDeleteSignaledObserver (this);

	    // Run the event-loop out waiting for the final detach.
	    task.getProc().observableDetachedXXX.addObserver (new Observer ()
		{
		    Proc proc = task.getProc();
		    public void update (Observable obj, Object arg)
		    {
			proc.observableAttachedXXX.deleteObserver (this);
			Manager.eventLoop.requestStop ();
		    }
		});

	    fine.log(this, "waiting for detach");
	    ackHandler.assertRunUntilSignaled ();
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
	    Signal[] requestEvent () { return child.requestFork(); }
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
	    public Action updateForkedParent (Task parent, Task offspring)
	    {
		fail ("updateForkedParent");
		return null;
	    }
	    public Action updateForkedOffspring (Task parent, Task offspring)
	    {
		fail ("updateForkedOffspring");
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
	class DetachClone
	    extends Detach
	    implements TaskObserver.Cloned
	{
	    Signal[] requestEvent () { return child.requestClone(); }
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
	    public Action updateClonedParent (Task parent, Task offspring)
	    {
		fail ("cloned parent");
		return null;
	    }
	    public Action updateClonedOffspring (Task parent, Task offspring)
	    {
		fail ("cloned offspring");
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
	class DetachExec
	    extends Detach
	    implements TaskObserver.Execed
	{
	    Signal[] requestEvent () { return child.requestExec(); }
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
	class DetachSignal
	    extends Detach
	{
	    Signal[] requestEvent () { child.signal(Signal.TERM); return new Signal[0]; }
	    boolean eventIsSignal () { return true; }
	    void addEventObserver (Task task)
	    {
		Manager.eventLoop.add (new RequestStopEvent(Manager.eventLoop));
	    }
	    void deleteEventObserver (Task task)
	    {
	    }
	}
	new DetachSignal ().assertDetach ();
    }
}
