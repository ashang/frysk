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

package frysk.proc.live;

import java.util.Iterator;
import frysk.sys.Pid;
import frysk.testbed.TestLib;
import frysk.testbed.Offspring;
import frysk.testbed.SlaveOffspring;
import frysk.proc.Proc;
import frysk.proc.Host;
import java.util.Collection;
import java.util.HashSet;
import frysk.proc.HostRefreshBuilder;
import frysk.proc.Manager;

/**
 * Check the host's refresh mechanism.
 */
public class TestRefresh extends TestLib {

    private static final int PROCESSES = 1;
    private static final int DAEMONS = 2;
    private static final int NEW_PROCESSES = 4;
    private static final int EXITED_PROCESSES = 16;


    /**
     * Class to record a refresh; both saving the supplied updates,
     * and applying the updates to a local cache of the system.
     */
    private class HostState implements HostRefreshBuilder {
	private final Collection processes = new HashSet();
	private Collection newProcesses;
	private Collection exitedProcesses;
	private final Host host;
	HostState(Host host) {
	    this.host = host;
	}
	HostState assertRefresh(String why) {
	    host.requestRefresh(processes, this);
	    assertRunUntilStop(why);
	    return this;
	}
	public void construct(Collection newProcesses,
			      Collection exitedProcesses) {
	    this.newProcesses = newProcesses;
	    this.exitedProcesses = exitedProcesses;
	    processes.removeAll(exitedProcesses);
	    processes.addAll(newProcesses);
	    Manager.eventLoop.requestStop();
	}
	private void assertIn(String why, int pid, int what) {
	    assertIsMember(why + "(processes)", (what & PROCESSES) != 0,
			   processes, pid);
	    assertEquals(why + "(daemons)", (what & DAEMONS) != 0,
			 (findProc(processes, pid) != null
			  && (findProc(processes, pid)
			      .getParent().getPid() == 1)));
	    assertIsMember(why + "(new processes)", (what & NEW_PROCESSES) != 0,
			   newProcesses, pid);
	    assertIsMember(why + "(exited)", (what & EXITED_PROCESSES) != 0,
			   exitedProcesses, pid);
	}
	void assertIn(String why, Proc proc, int what) {
	    assertIn(why, proc.getPid(), what);
	}
	void assertIn(String why, Offspring offspring, int what) {
	    assertIn(why, offspring.getPid(), what);
	}
	Proc assertFindProc(String why, int pid) {
	    Proc proc = findProc(processes, pid);
	    assertNotNull(why, proc);
	    return proc;
	}
	Proc assertFindProc(String why, Offspring offspring) {
	    return assertFindProc(why, offspring.getPid());
	}
	Proc assertFindSelf() {
	    Proc self = findProc(processes, Pid.get());
	    assertNotNull ("self", self);
	    return self;
	}
	/**
	 * Find the Proc correspinding to Offspring in the set.
	 *
	 * This uses a brute-force search of the set so that it
	 * doesn't peturbe the state of the underlying system.
	 */
	private Proc findProc(Collection set, int pid) {
	    for (Iterator i = set.iterator(); i.hasNext(); ) {
		Proc p = (Proc) i.next();
		if (p.getPid() == pid)
		    return p;
	    }
	    return null;
	}
	private void assertIsMember(String why, boolean is,
				    Collection set, int pid) {
	    assertEquals(why, is, findProc(set, pid) != null);
	}
	
    }

    /**
     * Do several refreshes, check that the child is only added on the
     * first pass; and never removed.
     */
    public void testRepeatedHostRefresh() {
	// Create a daemon process, set things up to watch and verify
	// the child.
	Offspring offspring = SlaveOffspring.createDaemon ();
	HostState state = new HostState(host);

	for (int i = 0; i < 2; i++) {
	    state.assertRefresh("refresh " + i);
	    // Check that the offspring is in the process pool on both
	    // passes; but it only appears under new during the first
	    // pass.
	    state.assertIn("refresh " + i, offspring,
			   PROCESSES | DAEMONS
			   | (i == 0 ? NEW_PROCESSES : 0));
	}
    }

    /**
     * Check that a host refresh detects a process addition then
     * removal.
     */
    private void checkAdditionAndRemoval(boolean daemon) {
	HostState state = new HostState(host);
	state.assertRefresh("before creating process");
	
	// Create a sub-process; check that while there are some
	// processes this new one isn't known.
	SlaveOffspring child;
	if (daemon)
	    child = SlaveOffspring.createDaemon();
	else
	    child = SlaveOffspring.createChild();
	assertTrue("host.procPool non-empty", state.processes.size() > 0);
	state.assertIn("refresh before child created", child, 0);

	// Do a refresh, check that the process gets added.
	state.assertRefresh("after child created");
	state.assertIn("refresh after child created", child,
		       PROCESSES | NEW_PROCESSES
		       | (daemon ? DAEMONS : 0));

	// Delete the process.
	child.reap();

	// Check that a further refresh removes the process, generates
	// a removed event, and puts the proc into the removed state.
	state.assertRefresh("after child exited");
	state.assertIn("refresh after child exits", child,
		       EXITED_PROCESSES);
    }

    /**
     * Check a child creation/deletion.
     */
    public void testProcAdditionAndRemoval() {
	checkAdditionAndRemoval(false);
    }

    /**
     * Check a daemon creation/deletion.
     */
    public void testDaemonAdditionAndRemoval() {
	checkAdditionAndRemoval(true);
    }

    /**
     * Check that a parent child relationship is correct.
     */
    public void testParentChild() {
	// Create a sub process, refresh things so that it is known.
	Offspring child = SlaveOffspring.createChild();
	HostState state = new HostState(host).assertRefresh("find child");
	
	// Find child process.
	Proc childProc = state.assertFindProc("find child", child);
	// Find this process.
	Proc self = state.assertFindSelf();

	assertSame("this process and child's parent",
		   self, childProc.getParent ());
	assertEquals("self children count", 1,
		     self.getChildren().size());
	assertTrue("child is a child",
		   self.getChildren().contains(childProc));
    }

    /**
     * Check that a process that becomes a daemon gets its parent id
     * changed to 1.
     */
    public void testRefreshDaemon() {
	// Create the zombie maker, and then get it to create one
	// child.  Perform a refresh so that is all captured.
	SlaveOffspring zombie = SlaveOffspring.createDaemon();
	zombie.assertSendAddForkWaitForAcks();
	HostState state = new HostState(host).assertRefresh("find zombie");
	Proc zombieParent = state.assertFindProc("zombie", zombie);
	state.assertIn("zombie parent", zombieParent,
		       DAEMONS|PROCESSES|NEW_PROCESSES);
	assertEquals ("zombie maker has one child",
		      1, zombieParent.getChildren().size());
	Proc zombieChild = (Proc) zombieParent.getChildren().iterator().next();
	state.assertIn("zombie child", zombieChild,
		       PROCESSES|NEW_PROCESSES);
	assertSame ("zombie and zombie child's parent",
		    zombieChild.getParent(), zombieParent);

	// The init process.
	Proc initProc = state.assertFindProc("init", 1);

	// Blow away the parent, this turns the child into a daemon,
	// do a refresh and check that the child's parent changed to
	// process one.
	zombie.assertSendFryParentWaitForAcks();
	state.assertRefresh("find parent gone");
	state.assertIn("zombie parent", zombieParent,
		       EXITED_PROCESSES);
	state.assertIn("zombie child", zombieChild,
		       PROCESSES|DAEMONS);
	assertNotSame ("child's parent and zombie maker",
		       zombieChild.getParent(), zombieParent);
	assertSame ("child's parent and process 1",
		    zombieChild.getParent(), initProc);
	assertTrue ("process 1 includes child",
		    initProc.getChildren().contains(zombieChild));
	assertEquals ("count of children of dead zombie parent",
		      0, zombieParent.getChildren().size());
    }

    /**
     * Check that a refresh involving a zombie is ok.
     *
     * In /proc, a zombie has no child tasks.  Within PS, a zombie
     * appears as "defunct".
     */
    public void testRefreshZombie() {
	// Create the zombie maker, and then get it to create one
	// child.
	SlaveOffspring zombie = SlaveOffspring.createDaemon();
	zombie.assertSendAddForkWaitForAcks();
	HostState state = new HostState(host).assertRefresh("initial refresh");
	Proc zombieParent = state.assertFindProc("zombie", zombie);
	assertEquals("zombie maker child count", 1,
		     zombieParent.getChildren().size ());
	Proc zombieChild = (Proc) zombieParent.getChildren().iterator().next();
	assertEquals("zombie child process count",
		     0, zombieChild.getChildren().size ());

	// Turn the zombie-child into a true zombie, check things are
	// updated.
	zombie.assertSendZombieForkWaitForAcks();
	state.assertRefresh("finding the zombied process");
 	assertEquals("zombie maker child count",
 		      1, zombieParent.getChildren().size ());
	assertEquals("Zombie child process count",
		      0, zombieChild.getChildren().size ());
	assertSame("zombie and zombie's child's parent",
		   zombieParent, zombieChild.getParent ());
    }

    /**
     * A single threaded program performs an exec, check that it is
     * correctly tracked.
     */
    public void testUnattachedSingleExec() {
	SlaveOffspring child = SlaveOffspring.createDaemon ();
	HostState state = new HostState(host).assertRefresh("find child");	Proc proc = state.assertFindProc("child", child);
	
	child.assertSendExecWaitForAcks();

	state.assertRefresh("refresh exec-ed state");

	assertEquals ("proc's getCmdLine[0]",
		      proc.getPid () + ":" + proc.getPid (),
		      proc.getCmdLine ()[0]);
	assertEquals ("pid after exec", child.getPid (), proc.getPid ());
    }
}
