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

package frysk.proc;

import java.lang.Thread;
import java.util.Observer;
import java.util.Observable;
import frysk.event.Event;
import frysk.sys.Sig;
import frysk.sys.proc.Stat;

/**
 * Check that random events, arriving mid-way through a detach, are
 * handled.
 */

public class TestTaskObserverDetach
    extends TestLib
{
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
	abstract void requestAdd (Task task);
	abstract void requestDelete (Task task);
	abstract int signal ();
	/**
	 * Cause an unexpected event to occure while a task is
	 * detaching.
	 */
	Detach (boolean skipSignal)
	{
	    // Create a process, attach to it.
	    AckProcess child = new AckDaemonProcess ();
	    final Task task = child.findTaskUsingRefresh (true);

	    task.requestAddSignaledObserver (this);
	    assertRunUntilStop ("adding signaled observer");

	    requestAdd (task);
	    assertRunUntilStop ("adding observer of unexpected event");
	    
	    // Send the signal to the task which will, in response,
	    // perform a requested operation (fork, clone, exec, ...).
	    child.signal (signal ());

	    if (skipSignal)
		assertRunUntilStop ("delivering signal");
	    
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
	    
	    // Remove all observers, this will result in a detach.
	    requestDelete (task);
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
	    assertRunUntilStop ("attempting detach");
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
	    DetachFork ()
	    {
		super (true);
	    }
	    void requestAdd (Task task)
	    {
		task.requestAddForkedObserver (this);
	    }
	    void requestDelete (Task task)
	    {
		task.requestDeleteForkedObserver (this);
	    }
	    public Action updateForked (Task task, Task fork)
	    {
		fail ("forked");
		return null;
	    }
	    int signal ()
	    {
		return AckProcess.addForkSig;
	    }
	}
	new DetachFork ();
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
	    DetachClone ()
	    {
		super (true);
	    }
	    void requestAdd (Task task)
	    {
		task.requestAddClonedObserver (this);
	    }
	    void requestDelete (Task task)
	    {
		task.requestDeleteClonedObserver (this);
	    }
	    public Action updateCloned (Task task, Task fork)
	    {
		fail ("cloned");
		return null;
	    }
	    int signal ()
	    {
		return AckProcess.addCloneSig;
	    }
	}
	new DetachClone ();
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
	    DetachExec ()
	    {
		super (true);
	    }
	    void requestAdd (Task task)
	    {
		task.requestAddExecedObserver (this);
	    }
	    void requestDelete (Task task)
	    {
		task.requestDeleteExecedObserver (this);
	    }
	    public Action updateExeced (Task task)
	    {
		fail ("execed");
		return null;
	    }
	    int signal ()
	    {
		return AckProcess.execSig;
	    }
	}
	new DetachExec ();
    }
    /**
     * Check that a signal, arriving mid-way through a detach, is
     * handled.
     */
    public void testDetachSignal ()
    {
	new Detach (false)
	{
	    void requestAdd (Task task)
	    {
		Manager.eventLoop.add (new Event ()
		    {
			public void execute ()
			{
			    Manager.eventLoop.requestStop ();
			}
		    });
	    }
	    void requestDelete (Task task)
	    {
	    }
	    int signal ()
	    {
		return Sig.TERM;
	    }
	};
    }
}
