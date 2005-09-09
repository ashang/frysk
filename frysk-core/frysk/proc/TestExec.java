// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/**
 * Test the exec event.
 *
 * The exec needs to completly replace the existing (possibly
 * multi-threaded) process with an entirely new one.
 */

package frysk.proc;

public class TestExec
    extends TestLib
{
    /**
     * A simple (single threaded) program performs an exec, check it
     * is correctly tracked.
     */
    public void testProcExec ()
    {
	ExecCounter execCounter = new ExecCounter ();
	new StopEventLoopOnProcDestroy ();

	ProcCounter procCounter = new ProcCounter ();

	// Create a temp file, the exec will remove.  That way it's
	// possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	Manager.host.requestCreateProc (null, "/dev/null", null,
					new String[] {
					    "./prog/syscall/exec",
					    "/bin/rm", tmpFile.toString (),
					});

	assertRunUntilStop ("run \"exec\" until exit");

	assertEquals ("Number of execs", 1, execCounter.numberExecs);
	assertTrue ("Tmp file was removed", !tmpFile.stillExists ());
    }

    /**
     * A multi-tasked program's non main task performs an exec, check
     * that it is correctly tracked.
     *
     * This case is messy, the exec blows away all but the exec task,
     * making the exec task the new main task.
     */
    public void testTaskExec ()
    {
	TaskCounter taskCounter = new TaskCounter ();
	ExecCounter execCounter = new ExecCounter ();
	ProcCounter procCounter = new ProcCounter ();
	new StopEventLoopOnProcDestroy ();

	// Create a temp file, the exec will remove.  That way it's
	// possible to confirm that the exec did work.
	TmpFile tmpFile = new TmpFile ();
	Manager.host.requestCreateProc (null, "/dev/null", null,
					new String[] {
					    "./prog/syscall/threadexec",
					    "/bin/rm", tmpFile.toString (),
					});

	assertRunUntilStop ("run \"threadexec\" to exit");

	assertEquals ("One task is expected to exec", 1,
		      execCounter.numberExecs);
	assertEquals ("Two tasks were created",
		      2, taskCounter.numberAdded ());
	assertEquals ("Only one task destroyed, the other disappearing"
		      + " in a puff of exec", 1, taskCounter.numberRemoved ());
	assertTrue ("Tmp file was removed", !tmpFile.stillExists ());

    }
}
