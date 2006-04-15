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
	extends AutoAddTaskObserverBase
	implements TaskObserver.Terminating, TaskObserver.Terminated
    {
	boolean terminatingP;
	boolean terminatedP;
	Terminate (int terminating, int terminated)
	{
	    terminatingP = (terminating != INVALID);
	    terminatedP = (terminated != INVALID);
	}
	void updateTaskAdded (Task task)
	{
	    if (terminatedP)
		task.requestAddTerminatedObserver (this);
	    if (terminatingP)
		task.requestAddTerminatingObserver (this);
	}
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
	// Set up an observer that watches for both Terminating and
	// Terminated events.
	final Terminate terminate = new Terminate (terminating, terminated);
	
	// Bail once it has exited.
	new StopEventLoopWhenChildProcRemoved ();

	// Start the program.
	new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-exit",
		Integer.toString (expected)
	    }).resume ();

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
    public void testTerminateKillINT () { terminate (-Sig._INT); }
    public void testTerminateKillKILL () { terminate (-Sig._KILL); }
    public void testTerminateKillHUP () { terminate (-Sig._HUP); }

    public void testTerminatingExit0 () { terminating (0); }
    public void testTerminatingExit47 () { terminating (47); }
    public void testTerminatingKillINT () { terminating (-Sig._INT); }
    public void testTerminatingKillKILL () { terminating (-Sig._KILL); }
    public void testTerminatingKillHUP () { terminating (-Sig._HUP); }

    public void testTerminatedExit0 () { terminated (0); }
    public void testTerminatedExit47 () { terminated (47); }
    public void testTerminatedKillINT () { terminated (-Sig._INT); }
    public void testTerminatedKillKILL () { terminated (-Sig._KILL); }
    public void testTerminatedKillHUP () { terminated (-Sig._HUP); }
}
