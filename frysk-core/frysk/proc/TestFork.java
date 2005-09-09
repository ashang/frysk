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
 * Check that fork (new new) events are detected.
 *
 * This creates a program that, in turn, creates lots and lots of
 * sub-processes.  Check that the number of processes created and
 * destroyed matches what is expected.
 */

package frysk.proc;

public class TestFork
    extends TestLib
{
    // Need to add task observers to the process the moment it is
    public void testFork ()
    {
	int n = 10;

	ProcCounter procCounter = new ProcCounter ();
	new StopEventLoopOnProcDestroy ();

	Manager.host.requestCreateProc (null, "/dev/null", null,
					new String[] {
					    "./prog/fib/fork",
					    Integer.toString (n)
					});

	assertRunUntilStop ("run \"fork\" until exit");

	Fibonacci fib = new Fibonacci (n);

	assertEquals ("Proc's created matches fib.callCount",
		      fib.callCount, procCounter.numberAdded ());
	assertEquals ("Proc's destroyed matches fib.callCount",
		      fib.callCount, procCounter.numberRemoved ());
    }
}
