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
 * Check that clone (task create and delete) events are detected.
 *
 * This creates a program that, in turn, creates lots and lots of
 * tasks.  It then checks that the number of task create and delete
 * events matches the expected.
 */

package frysk.proc;

public class TestClone
    extends TestLib
{
    public void testClone ()
    {
	int fibCount = 10;

 	TaskCounter taskCounter = new TaskCounter ();
	new StopEventLoopOnProcDestroy ();

	Manager.host.requestCreateProc (null, "/dev/null", null,
					new String[] {
					    "./prog/fib/clone",
					    Integer.toString (fibCount)
					});
	
	assertRunUntilStop ("run \"clone\" to exit");

 	Fibonacci fib = new Fibonacci (fibCount);
	assertEquals ("Number of task created matches fib-call count",
		      fib.callCount, taskCounter.numberAdded ());
	assertEquals ("Number of tasks destroyed matches fib-call count",
		      fib.callCount, taskCounter.numberRemoved ());

    }
}
