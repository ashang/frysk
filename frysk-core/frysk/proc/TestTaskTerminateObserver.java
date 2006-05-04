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

import java.util.Iterator;

import frysk.sys.Pid;
import frysk.sys.Sig;

/**
 * Check the Task terminating and terminated observers.
 */

public class TestTaskTerminateObserver
    extends TestLib
{
    private static int INVALID = 128;

    /**
     * Save the Terminating, and Terminated values as they pass by.
     */
    class Terminate
	extends TaskObserverBase
	implements TaskObserver.Terminating, TaskObserver.Terminated
    {
	int terminating = INVALID;
	int terminated = INVALID;
	public Action updateTerminating (Task task, boolean signal,
					 int value)
	{
	    if (signal)
		terminating = -value;
	    else
		terminating = value;
	    return Action.CONTINUE;
	}
	public Action updateTerminated (Task task, boolean signal,
					int value)
	{
	    if (signal)
		terminated = -value;
	    else
		terminated = value;
	    return Action.CONTINUE;
	}
    }

    /**
     * When either Terminating, and Terminated is not INVALID, install
     * and verify corresponding observers.
     */
    public void check (int expected, int terminating, int terminated)
    {
	// Bail once it has exited.
	new StopEventLoopWhenChildProcRemoved ();

	// Start the program.
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-exit",
		Integer.toString (expected)
	    });
	// Set up an observer that watches for both Terminating and
	// Terminated events.
	Terminate terminate = new Terminate ();
	if (terminated != INVALID)
	    child.mainTask.requestAddTerminatedObserver (terminate);
	if (terminating != INVALID)
	    child.mainTask.requestAddTerminatingObserver (terminate);
	child.resume ();
	assertRunUntilStop ("run \"exit\" to exit");

	assertEquals ("terminating value", terminating, terminate.terminating);
	assertEquals ("terminated value", terminated, terminate.terminated);
    }

    /**
     * Check both the Terminating, and Terminated values.
     */
    public void terminate (int expected)
    {
	check (expected, expected, expected);
    }

    /**
     * Check the Terminating value.
     */
    public void terminating (int expected)
    {
	check (expected, expected, INVALID);
    }

    /**
     * Check the Terminated value.
     */
    public void terminated (int expected)
    {
	check (expected, INVALID, expected);
    }

    public void testTerminateExit0 () { terminate (0); }
    public void testTerminateExit47 () { terminate (47); }
    public void testTerminateKillINT () { terminate (-Sig.INT_); }
    public void testTerminateKillKILL () { terminate (-Sig.KILL_); }
    public void testTerminateKillHUP () { terminate (-Sig.HUP_); }

    public void testTerminatingExit0 () { terminating (0); }
    public void testTerminatingExit47 () { terminating (47); }
    public void testTerminatingKillINT () { terminating (-Sig.INT_); }
    public void testTerminatingKillKILL () { terminating (-Sig.KILL_); }
    public void testTerminatingKillHUP () { terminating (-Sig.HUP_); }

    public void testTerminatedExit0 () { terminated (0); }
    public void testTerminatedExit47 () { terminated (47); }
    public void testTerminatedKillINT () { terminated (-Sig.INT_); }
    public void testTerminatedKillKILL () { terminated (-Sig.KILL_); }
    public void testTerminatedKillHUP () { terminated (-Sig.HUP_); }

    class AttachCounter
    extends TaskObserverBase
    implements TaskObserver.Attached
{
    int count;
    public Action updateAttached (Task task)
    {
        count++;
        task.requestAddAttachedObserver (this);
        task.requestUnblock (this);
        return Action.BLOCK;
    }
}
    class TerminatingCounter
    extends TaskObserverBase
    implements TaskObserver.Terminating
{
    int count;
    public Action updateTerminating (Task task, boolean signal, int value)
    {
        count++;
        task.requestAddTerminatingObserver (this);
        task.requestUnblock (this);
        return Action.BLOCK;
    }
}

    
    /**
     * Check that a terminating thread T is tracked properly.
     */
   public void testTerm ()
    {
	final int timeout = 3;

	AttachCounter attachCounter = new AttachCounter ();
	TerminatingCounter terminatingCounter = new TerminatingCounter ();
	
	AckProcess childTerm = new DetachedAckProcess (ackSignal, new String[]
	    {
		getExecPrefix () + "funit-threadexit",
		Integer.toString (Pid.get ()),
		Integer.toString (ackSignal.hashCode ()),
		Integer.toString (timeout), // Seconds
	    });
	
	Proc proc = childTerm.findProcUsingRefresh (true);
 	assertNotNull ("Finding funit-threadexit", proc);
		
 	Task task = null;
        for (Iterator i = proc.getTasks ().iterator (); i.hasNext (); ) {
            task = (Task) i.next ();
            assertNotNull ("Finding funit-threadexit threads",
        	    task);
            if (task.getTid () == proc.getPid ()) {
		task.requestAddAttachedObserver (attachCounter);
		task.requestAddTerminatingObserver (terminatingCounter);
        	break;
	    }
        }
        Manager.eventLoop.runPolling (timeout * 2 * 1000);
 	assertTrue ("Number attached processes", attachCounter.count != 0);
	assertTrue ("Number terminating processes", terminatingCounter.count != 0);
    }
}
