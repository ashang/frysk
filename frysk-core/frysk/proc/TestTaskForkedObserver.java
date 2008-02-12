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

import frysk.testbed.TestLib;
import frysk.testbed.ProcCounter;
import frysk.testbed.StopEventLoopWhenProcRemoved;
import frysk.testbed.Fibonacci;
import frysk.testbed.TaskObserverBase;
import frysk.testbed.DaemonBlockedAtEntry;

/**
 * Check that the observer TaskObserver.Forked works.
 */

public class TestTaskForkedObserver
    extends TestLib
{
    static int n = 10;

    /**
     * Test that the fork count from a sub-program that, in turn, creates lots and
     * lots of sub-processes matches the expected.
     */
    public void testTaskForkedObserver ()
    {
	ForkObserver forkObserver = new ForkObserver();
	ProcCounter procCounter
	    = setupForkTest(forkObserver, new String[]
		{
		    getExecPath ("funit-fib-fork"),
		    Integer.toString(n)
		});

	Fibonacci fib = new Fibonacci(n);

	assertEquals("number of child processes created (not counting first)",
		     fib.getCallCount() - 1,
		     procCounter.added.size());
	assertEquals("number of child processes destroyed (not counting first)",
		     fib.getCallCount() - 1,
		     procCounter.removed.size());
	assertEquals("number of times fork observer added",
		     fib.getCallCount(),
		     forkObserver.addedCount());
	assertEquals("number of forks (one less than number of processes)",
		     fib.getCallCount() - 1, forkObserver.count);
    }

    public void testTaskVforkObserver ()
    {
	if (unresolved(5466))
	    return;

	ForkObserver forkObserver = new ForkObserver();
	ProcCounter procCounter
	    = setupForkTest(forkObserver,
			    new String[] { getExecPath ("funit-vfork") });

	assertEquals("number of child processes created",
		     1, procCounter.added.size());
	assertEquals("number of child processes destroyed",
		     1, procCounter.removed.size());
	assertEquals("number of times fork observer added",
		     2, forkObserver.addedCount());
	assertEquals("number of forks (one less than number of processes)",
		     1, forkObserver.count);
    }

  public ProcCounter setupForkTest (ForkObserver forkObserver, String[] argv)
  {
    // Run a program that forks wildly.
    DaemonBlockedAtEntry child = new DaemonBlockedAtEntry(argv);
    int pid = child.getMainTask().getProc().getPid();
    ProcCounter procCounter = new ProcCounter(pid);

    new StopEventLoopWhenProcRemoved(child);
    child.getMainTask().requestAddForkedObserver(forkObserver);
    child.requestRemoveBlock();
    assertRunUntilStop("run \"fork\" until exit");

    return procCounter;

  }

    // Watch for any Task fork events, accumulating them as they
    // arrive.
    class ForkObserver
	extends TaskObserverBase
	implements TaskObserver.Forked
    {
	int count;

	public Action updateForkedParent (Task parent, Task offspring)
	{
	    count++;
	    parent.requestUnblock(this);
	    return Action.BLOCK;
	}

	public Action updateForkedOffspring (Task parent, Task offspring)
	{
	    // XXX: Is this legit? Like knowing that the request
	    // won't be processed until the event loop is run
	    // again so that there's no race condition.
	    offspring.requestAddForkedObserver(this);
	    offspring.requestUnblock(this);
	    return Action.BLOCK;
	}
    }
}
