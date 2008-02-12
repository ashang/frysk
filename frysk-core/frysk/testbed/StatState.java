// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.testbed;

import frysk.proc.Manager;
import frysk.junit.TestCase;
import frysk.sys.proc.Stat;
import frysk.rsl.Log;
import frysk.event.TimerEvent;
import frysk.proc.Task;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

/**
 * Class for directly tracking <tt>/proc/$$/stat</tt>.
 */

public class StatState {
    private final static Log fine = Log.fine(StatState.class);
    private final static Log finest = Log.finest(StatState.class);

    private final char state;
    private StatState(char state) {
	this.state = state;
    }

    public String toString() {
	return "StatState." + state;
    }

    public static final StatState RUNNING = new StatState('R');
    public static final StatState SLEEPING = new StatState('S');
    public static final StatState DISK_WAIT = new StatState('D');
    public static final StatState ZOMBIE = new StatState('Z');
    public static final StatState TRACED_OR_STOPPED = new StatState('T');
    public static final StatState PAGING = new StatState('W');

    /**
     * Asserts that TID is in the specified state; or transitions to
     * that state within a short period of time.
     */
    public void assertIs(ProcessIdentifier tid) {
	fine.log(this, "assertInState", tid);
	Stat stat = new Stat();
	long startTime = System.currentTimeMillis();
	do {
	    stat.scan(tid);
	    finest.log(this, "assertInState tid", tid, "in", stat.state);
	    if (stat.state == state)
		break;
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException ie) {
		finest.log(this, "assertInState: ignoring interrupt");
	    }
	} while (System.currentTimeMillis()
		 < startTime + TestCase.getTimeoutMilliseconds());
	TestCase.assertEquals("Stat state for tid " + tid,
			      state, stat.state);
    }
    public void assertIs(Task task) {
	assertIs(ProcessIdentifierFactory.create(task.getTid()));
    }

    private static class Probe extends TimerEvent {
	private final Stat stat;
	private final StatState state;
	private final ProcessIdentifier pid;
	Probe(ProcessIdentifier pid, StatState state) {
	    super(0, 100); // Refresh every 100ms
	    this.stat = new Stat();
	    this.state = state;
	    this.pid = pid;
	}
	public void execute() {
	    stat.scan(pid);
	    finest.log(state, "assertRunToState tid", stat.tid, "in",
		       stat.state);
	    if (state.state == stat.state) {
		Manager.eventLoop.remove(this);
		Manager.eventLoop.requestStop();
	    }
	}
    }

    public void assertRunUntil(ProcessIdentifier tid) {
	fine.log(this, "assertRunToState tid", tid);
	Manager.eventLoop.add(new Probe(tid, this));
	long timeout = TestCase.getTimeoutMilliseconds();
	TestCase.assertTrue("run to state: " + state,
			    Manager.eventLoop.runPolling(timeout));
	
    }
    public void assertRunUntil(Task task) {
	assertRunUntil(ProcessIdentifierFactory.create(task.getTid()));
    }
}
