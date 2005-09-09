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
 * Check that a program can be run to completion.
 *
 * A scratch file is created.  The program "rm -f TMPFILE" is then
 * run.  That the tmp file has been removed is then checked.
 */

package frysk.proc;

public class TestRun
    extends TestLib
{
    public void testRun ()
    {
	TmpFile tmpFile = new TmpFile ();
	assertNotNull ("Temporary file created", tmpFile);

	// Add an observer that counts the number of proc create
	// events.
	ProcCounter procCounter = new ProcCounter ();

	// Once a proc destroyed has been seen stop the event loop.
	new StopEventLoopOnProcDestroy ();

	// Create a program that removes the above tempoary file, when
	// it exits the event loop will be shutdown.
	String[] command = new String[] {"rm", "-f", tmpFile.toString () };
	Manager.host.requestCreateProc (command);

	// Run the event loop, cap it at 5 seconds.
	assertRunUntilStop ("run \"rm\" to exit");

	assertEquals ("One process created",
		      1, procCounter.numberAdded ());
	assertEquals ("One process destroyed",
		      1, procCounter.numberRemoved ());
	assertFalse ("The file no longer exists",
		     tmpFile.stillExists ());
	assertEquals ("The MANAGER dropped the sole TASK",
		      0, Manager.host.taskPool.size ());
	assertEquals ("The MANAGER has no processes",
		      0, Manager.host.procPool.size ());
    }
}
