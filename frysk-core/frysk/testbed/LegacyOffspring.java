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

package frysk.testbed;

import frysk.sys.ProcessIdentifier;
import frysk.proc.Manager;
import frysk.junit.TestCase;
import frysk.sys.Pid;
import frysk.sys.Signal;
import frysk.testbed.SignalWaiter;
import java.util.LinkedList;
import java.util.List;
import frysk.sys.Errno;
import frysk.sys.Wait;
import frysk.sys.UnhandledWaitBuilder;
import java.io.File;
import frysk.Config;

/**
 * XXX: Create a process running the funit-legacy program.  Some tests
 * require querks only implemented by that program.
 */
public class LegacyOffspring
    extends SynchronizedOffspring
{
    /**
     * Build the slave command that should be run.
     */
    static private String[] funitSlaveCommand (boolean busy,
					       String filenameArg,
					       String[] argv) {
	List command = new LinkedList();
	command.add(getExecutable().getAbsolutePath());
	command.add(busy ? "--wait=busy-loop" : "--wait=suspend");
	if (filenameArg != null)
	    command.add("--filename=" + TestLib.getExecPath (filenameArg));
	command.add(Integer.toString(TestCase.getTimeoutSeconds()));
	// Use getpid as this testsuite always runs the event loop
	// from the main thread (which has tid==pid).
	command.add(Integer.toString(Pid.get()));
	// Append any arguments.
	if (argv != null) {
	    for (int n = 0; n < argv.length; n++)
		command.add(argv[n]);
	}
	String[] args = new String[command.size()];
	command.toArray(args);
	return args;
    }

    public static final Signal CHILD_ACK = Signal.USR1;
    public static final Signal PARENT_ACK = Signal.USR2;

    private static final Signal[] SPAWN_ACK = new Signal[] { CHILD_ACK, PARENT_ACK };
    private static final Signal[] EXEC_ACK = new Signal[] { CHILD_ACK };

    private static final Signal ADD_CLONE_SIG = Signal.USR1;
    private static final Signal DEL_CLONE_SIG = Signal.USR2;
    private static final Signal ADD_FORK_SIG = Signal.HUP;
    private static final Signal DEL_FORK_SIG = Signal.INT;
    private static final Signal ZOMBIE_FORK_SIG = Signal.URG;
    private static final Signal EXEC_SIG = Signal.PWR;
    private static final Signal EXEC_CLONE_SIG = Signal.FPE;

    /** Create an ack process. */
    private LegacyOffspring (OffspringType type) {
	super(type, CHILD_ACK, funitSlaveCommand(false, null, null));
    }

    /**
     * Create an LegacyOffspring; if BUSY, the process will use a
     * busy-loop, instead of suspending, when waiting for signal
     * commands.
     */
    private LegacyOffspring (OffspringType type, boolean busy) {
	super(type, CHILD_ACK, funitSlaveCommand(busy, null, null));
    }

    /**
     * Tell TID to create a new offspring. Wait for the acknowledgment.
     */
    private void spawn (int tid, Signal sig, String why) {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, SPAWN_ACK, why);
	// XXX: Just trust that TID is part of this process.
	sig.tkill(tid);
	ack.assertRunUntilSignaled();
    }

    /** Add a Task; wait for acknowledgement. */
    public void assertSendAddCloneWaitForAcks () {
	spawn(getPid(), ADD_CLONE_SIG, "assertSendAddCloneWaitForAcks");
    }
    /** Request that a task be added.  */
    public Signal[] requestClone() {
	signal(ADD_CLONE_SIG);
	return SPAWN_ACK;
    }
    /** Add many Tasks; wait for acknowledgement.  */
    public LegacyOffspring assertSendAddClonesWaitForAcks(int count) {
	for (int i = 0; i < count; i++) {
	    assertSendAddCloneWaitForAcks();
	}
	return this;
    }

    /** Add a Task. */
    public void assertSendAddCloneWaitForAcks (int tid) {
	spawn(tid, ADD_CLONE_SIG, "addClone");
    }

    /** Delete a Task. */
    public void assertSendDelCloneWaitForAcks () {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, PARENT_ACK,
					    "assertSendDelCloneWaitForAcks");
	signal(DEL_CLONE_SIG);
	ack.assertRunUntilSignaled();
    }

    /** Add a child Proc; wait for acknowledgement */
    public void assertSendAddForkWaitForAcks () {
	spawn(getPid(), ADD_FORK_SIG, "assertSendAddForkWaitForAcks");
    }
    /** Request that a child Proc be added.  */
    public Signal[] requestFork() {
	signal(ADD_FORK_SIG);
	return SPAWN_ACK;
    }

    /** Add a child Proc. */
    public void assertSendAddForkWaitForAcks (int tid) {
	spawn(tid, ADD_FORK_SIG, "addFork");
    }

    /** Delete a child Proc. */
    public void assertSendDelForkWaitForAcks () {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, PARENT_ACK,
					    "assertSendDelForkWaitForAcks");
	signal(DEL_FORK_SIG);
	ack.assertRunUntilSignaled();
    }

    /** Terminate a fork Proc (creates zombie). */
    public void assertSendZombieForkWaitForAcks () {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, PARENT_ACK,
					    "assertSendZombieForkWaitForAcks");
	signal(ZOMBIE_FORK_SIG);
	ack.assertRunUntilSignaled();
    }

    /**
     * Kill the parent, expect an ack from the child (there had
     * better be a child).
     */
    public void assertSendFryParentWaitForAcks ()	{
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, CHILD_ACK,
					    "assertSendFryParentWaitForAcks");
	signal(Signal.KILL);
	ack.assertRunUntilSignaled();
    }

    /**
     * Request that the main task perform an exec; wait for the
     * acknowledge.
     */
    public void assertSendExecWaitForAcks () {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, EXEC_ACK,
					    "assertSendExecWaitForAcks");
	requestExec();
	ack.assertRunUntilSignaled();
    }
    /**
     * Request that the main task perform an exec.
     */
    public Signal[] requestExec() {
	signal(EXEC_SIG);
	return EXEC_ACK;
    }

    /**
     * Request that the cloned task perform an exec.
     */
    public void assertSendExecCloneWaitForAcks () {
	// First the main thread acks with .PARENT_ACK, and then the
	// execed process acks with .CHILD_ACK.
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop,
					    new Signal[] { PARENT_ACK, CHILD_ACK },
					    "assertSendExecCloneWaitForAcks");
	signal(EXEC_CLONE_SIG);
	ack.assertRunUntilSignaled();
    }

    /**
     * Reap the process.. Kill the process and then wait for and
     * consume all of that processes waitpid events.
     */
    public void reap () {
	kill();
	try {
	    while (true) {
		Wait.waitAll(getPid(), new UnhandledWaitBuilder () {
			protected void unhandled (String why) {
			    TestCase.fail ("killing child (" + why + ")");
			}
			public void terminated(ProcessIdentifier pid,
					       Signal signal, int value,
					       boolean coreDumped) {
			    // Termination with signal is ok.
			    TestCase.assertTrue("terminated with signal",
						signal != null);
			}
		    });
	    }
	} catch (Errno.Echild e) {
	    // No more waitpid events.
	}
    }

    /**
     * Create a slave-process that is a child of this process.
     */
    static public LegacyOffspring createChild() {
	return new LegacyOffspring(OffspringType.CHILD);
    }
    /**
     * Create a slave-process that is a child of this process.
     */
    static public LegacyOffspring createDaemon() {
	return new LegacyOffspring(OffspringType.DAEMON);
    }
    /**
     * Create a slave-process that is a child of this process.
     */
    static public LegacyOffspring createDaemon(boolean busy) {
	return new LegacyOffspring(OffspringType.DAEMON, busy);
    }
    /**
     * Create an attached slave-process that is a child of this
     * process.
     */
    static public LegacyOffspring createAttachedChild() {
	return new LegacyOffspring(OffspringType.ATTACHED_CHILD);
    }
    /**
     * Return the program executable that will be run.
     */
    static public File getExecutable() {
	return Config.getPkgLibFile("funit-legacy");
    }
}
