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
import java.util.List;
import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.sys.SyscallNum;
import frysk.sys.Pid;
import frysk.sys.Fork;

/**
 * Check that syscall events are detected.
 *
 * This should be expanded later to support ISAs so that the syscall
 * parameters and return codes can be displayed.  That information must
 * be taken from registers specific to each platform.
 *
 * XXX: For reasons noted below, these tests are currently disabled.
 */

public class TestTaskSyscallObserver
    extends TestLib
{
    boolean skip = true; // XXX

    class SyscallObserver
	extends TaskObserverBase
	implements TaskObserver.Syscall
    {
	int enter = 0;
	int exit = 0;
	boolean inSyscall = false;
	public void addedTo (Object o)
	{
	    super.addedTo (o);
	    Manager.eventLoop.requestStop ();
	}
	public Action updateSyscallEnter (Task task)
	{
	    assertFalse ("inSyscall", inSyscall);
	    inSyscall = true;
	    enter++;
	    return Action.CONTINUE;
	}
	public Action updateSyscallExit (Task task)
	{
	    assertTrue ("inSyscall", inSyscall);
	    inSyscall = false;
	    exit++;
	    return Action.CONTINUE;
	}
    }
	
    /**
     * Test a system-call in a for loop.
     */
    public void testSyscallLoop ()
    {
	if (skip) {
	    System.out.print ("<<BROKEN>>"); // XXX
	    return;
	}

 	int count = 1000;
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscallloop",
 		Integer.toString (count),
  	    });

	// Add a syscall observer.  XXX: This doesn't work - system
	// call tracing doesn't get enabled enabled.
	SyscallObserver syscallObserver = new SyscallObserver ();
	child.mainTask.requestAddSyscallObserver (syscallObserver);
 	assertRunUntilStop ("add SyscallObserver");

	// XXX: This is wrong; the task isn't a child so this will
	// never work.  What about assertRunUntilTaskRemoved (...)?
	new StopEventLoopWhenChildProcRemoved ();
	child.resume ();
	assertRunUntilStop ("run until program exits");

 	assertTrue ("enough syscall enter events",
 		    syscallObserver.enter >= count);
 	assertTrue ("enough syscall enter exit",
 		    syscallObserver.exit >= count);
 	assertTrue ("inSyscall (last call doesn't exit)",
		    syscallObserver.inSyscall);
    }
    
    /**
     * Test system calls.
     *
     * XXX: How is this different to testSyscallLoop (other than the
     * program run).
     */
    public void testSyscalls ()
    {
	if (skip) {
	    System.out.print ("<<BROKEN>>"); // XXX
	    return;
	}

 	// Create program making syscalls
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscalls"
 	    });

	// Add a syscall observer.  XXX: This doesn't work - system
	// call tracing doesn't get enabled enabled.
	SyscallObserver syscallObserver = new SyscallObserver ();
	child.mainTask.requestAddSyscallObserver (syscallObserver);
 	assertRunUntilStop ("add SyscallObserver");

	// XXX: This is wrong; the task isn't a child so this will
	// never work.  What about assertRunUntilTaskRemoved (...)?
	new StopEventLoopWhenChildProcRemoved ();
	child.resume ();
	assertRunUntilStop ("run until program exits");

	assertTrue ("syscall events received >= 8",
		    syscallObserver.enter >= 8);
	assertFalse ("syscall events", syscallObserver.inSyscall);
    }

    /**
     * Need to add task observers to the process the moment it is
     * created, otherwize the creation of the very first task is
     * missed (giving a mismatch of task created and deleted
     * notifications.)
     */
	
    class SyscallOpenObserver
	extends SyscallObserver
    {
	boolean openingTestFile;
	boolean testFileOpened;
	boolean expectedRcFound;
	String openName = "a.file";
	public Action updateSyscallEnter (Task task)
	{
	    super.updateSyscallEnter (task);
	    SyscallEventInfo syscallEventInfo
		= task.getIsa ().getSyscallEventInfo ();
	    int syscallNum = syscallEventInfo.number (task);
	    if (syscallNum == SyscallNum.SYSopen) { 
		long addr = syscallEventInfo.arg (task, 1);
		StringBuffer x = new StringBuffer ();
		task.memory.get (addr, x);
		String name = x.toString ();
		if (name.indexOf (openName) >= 0) {
		    testFileOpened = true;
		    openingTestFile = true;
		}
	    }
	    return Action.CONTINUE;
	}
	public Action updateSyscallExit (Task task)
	{
	    super.updateSyscallExit (task);
	    SyscallEventInfo syscallEventInfo
		= task.getIsa ().getSyscallEventInfo ();
	    int syscallNum = syscallEventInfo.number (task);
	    if (syscallNum == SyscallNum.SYSopen && openingTestFile) {
		openingTestFile = false;
		int rc = (int)syscallEventInfo.returnCode (task);
		if (rc == -2) // ENOENT
		    expectedRcFound = true;
	    }
	    return Action.CONTINUE;
	}
    }

    /**
     * Check that a specific syscall event can be detected.  In this
     * case, an open syscall to a particular file.
     */
    public void testSyscallOpen ()
    {
	if (skip) {
	    System.out.print ("<<BROKEN>>"); // XXX
	    return;
	}

	SyscallOpenObserver syscallOpenObserver = new SyscallOpenObserver ();
	new StopEventLoopWhenChildProcRemoved ();

 	// Create program making syscalls
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscalls"
 	    });
	child.mainTask.requestAddSyscallObserver (syscallOpenObserver);

	child.resume ();
 	assertRunUntilStop ("run \"syscalls\" until exit");
	
	assertTrue ("syscall events received >= 8",
		    syscallOpenObserver.enter >= 8); 
	assertFalse ("in syscall", syscallOpenObserver.inSyscall);
	assertTrue ("attempt to open a.file",
		    syscallOpenObserver.testFileOpened);
	assertTrue ("open of a.file failed",
		    syscallOpenObserver.expectedRcFound);
    }

    /**
     * Class to create a detached child process that will be in the
     * middle of reading a pipe.
     */
    class PipeReadChild
	extends Child
    {
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.daemon (stdin, stdout, stderr, argv);
	}
	PipeReadChild (String[] argv)
	{
	    super (argv);
	}
	PipeReadChild (boolean restart)
	{
	    this (new String[] {
		      getExecPrefix () + "funit-syscallint",
		      Integer.toString (Pid.get ()),
		      Integer.toString (ackSignal.hashCode ()),
		      Integer.toString (restart ? 1 : 0)
		  });
	}
    }

    // Timers, observers, counters, etc.. needed for the test.
    class TestSyscallInterruptInternals {
	int readEnter, readExit, sigusr1Count;
	
	// Need to add task observers to the process the moment it is
	// created, otherwize the creation of the very first task is
	// missed (giving a mismatch of task created and deleted
	// notifications.)
	
	class SyscallInterruptObserver
	    extends SyscallObserver
            implements TaskObserver.Signaled
	{
	    public Action updateSyscallEnter (Task task)
	    {
		super.updateSyscallEnter (task);
		SyscallEventInfo syscallEventInfo
		    = task.getIsa ().getSyscallEventInfo ();
		int syscallNum = syscallEventInfo.number (task);
		// verify that read attempted
		if (syscallNum == SyscallNum.SYSread) { 
		    long numberOfBytes = syscallEventInfo.arg (task, 3);
		    if (numberOfBytes != 1)
			throw new RuntimeException ("bytes to read not 1");
		    if (readEnter == 0)
			Manager.eventLoop.add (new PausedReadTimerEvent (task, 500));
		    ++readEnter;
		}
		return Action.CONTINUE;
	    }
	    public Action updateSyscallExit (Task task)
	    {
		super.updateSyscallExit (task);
		SyscallEventInfo syscallEventInfo
		    = task.getIsa ().getSyscallEventInfo ();
		int syscallNum = syscallEventInfo.number (task);
		if (syscallNum == SyscallNum.SYSread) {
		    if (readEnter <= readExit)
			throw new RuntimeException ("Read exit before enter");
		    ++readExit;
		}
		return Action.CONTINUE;
	    }
            public Action updateSignaled (Task task, int sig)
            {
		if (sig == Sig._USR1)
		    sigusr1Count++;
                return Action.CONTINUE;
            }
        }

	class PausedReadTimerEvent
	    extends frysk.event.TimerEvent
	{
	    Task task;
	    long milliseconds;
	    PausedReadTimerEvent (Task task, long milliseconds)
	    {
		super (milliseconds);
		this.task = task;
		this.milliseconds = milliseconds;
	    }
	    public void execute ()
	    {
		// Make sure we didn't get a read exit up to now
		// as we are expecting to interrupt a blocked read.
		if (readExit > 0)
		    throw new RuntimeException ("read exited without signal");
		// We want to signal the process so it will interrupt
		// the read.
		Signal.tkill (task.getTid (), Sig.USR1);
	    }
	}

	SyscallInterruptObserver syscallObserver = new SyscallInterruptObserver ();
	
	TestSyscallInterruptInternals (int pid)
	{
            host.requestRefreshXXX (true);
            Manager.eventLoop.runPending ();

            Proc p = host.getProc (new ProcId (pid));

            if (p != null) {
                List tasks = p.getTasks ();
                for (Iterator i = tasks.iterator (); i.hasNext (); ) {
                    Task t = (Task) i.next ();
                    if (t.getTaskId ().hashCode () == pid) {
                        t.traceSyscall = true;
                        t.requestAddSyscallObserver (syscallObserver);
                        t.requestAddSignaledObserver (syscallObserver);
                    }
                }
            }
        }
    }

    /**
     * Check that we can attach to a process currently in a syscall
     * and trace syscall events properly.
     */
    public void testSyscallInterrupt ()
    {
	if (skip) {
	    System.out.print ("<<BROKEN>>"); // XXX
	    return;
	}
	PipeReadChild prc = new PipeReadChild (false);

	TestSyscallInterruptInternals t 
	    = new TestSyscallInterruptInternals (prc.getPid ());
	new StopEventLoopWhenChildProcRemoved ();

 	assertRunUntilStop ("run \"syscallint\" until exit");
	assertEquals ("read enter events", 1, t.readEnter);
	assertEquals ("read exit events", 1, t.readExit);
	assertEquals ("SIGUSR1 events", 1, t.sigusr1Count);
    }

    /**
     * Check that we can attach to a process currently in a syscall
     * and trace syscall events properly.
     */
    public void testSyscallInterruptRestart ()
    {
	if (skip) {
	    System.out.print ("<<BROKEN>>"); // XXX
	    return;
	}
	PipeReadChild prc = new PipeReadChild (true);

	TestSyscallInterruptInternals t
	    = new TestSyscallInterruptInternals (prc.getPid ());
	new StopEventLoopWhenChildProcRemoved ();

 	assertRunUntilStop ("run \"syscallint\" with restart until exit");
	assertEquals ("restart read enter events", 2, t.readEnter);
	assertEquals ("restart read exit events", 2, t.readExit);
	assertEquals ("restart sigusr1 events", 1, t.sigusr1Count);
    }
}
