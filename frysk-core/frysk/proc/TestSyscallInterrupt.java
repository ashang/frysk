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

import java.util.Observer;
import java.util.Observable;
import frysk.sys.SyscallNum;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.sys.Fork;
import frysk.sys.Signal;
import java.util.Iterator;
import java.util.List;

/**
 * Check that we can attach to a process currently in a syscall and trace
 * syscall events properly.
 */

public class TestSyscallInterrupt
    extends TestLib
{
    // Class to create a detached child process that will be in the middle
    // of reading a pipe.

    protected class PipeReadChild
	extends Child
    {
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.exec (stdin, stdout, stderr, argv);
	}
	PipeReadChild (String[] argv)
	{
	    super (argv);
	}
	PipeReadChild (boolean restart)
	{
	    this (new String[] {
		      "./prog/syscall/syscallint",
		      Integer.toString (Pid.get ()),
		      Integer.toString (AckHandler.signal),
		      Integer.toString (restart ? 1 : 0)
		  });
	}
    }

    // Timers, observers, counters, etc.. needed for the test.
    class TestSyscallInterruptInternals {
	int pid;
	volatile int syscallEnter, syscallExit;
	volatile boolean inSyscall = false;
	volatile boolean exited;
	volatile int exitedTaskEventStatus;
	boolean openingTestFile;
	boolean testFileOpened;
	boolean expectedRcFound;
	int readEnter, readExit, sigusr1Count;
	
	// Need to add task observers to the process the moment it is
	// created, otherwize the creation of the very first task is
	// missed (giving a mismatch of task created and deleted
	// notifications.)
	
	class TaskEventObserver
	    extends TaskObserverBase
	    implements TaskObserver.Syscall
	{
	    public Action updateSyscallEnter (Task task)
	    {
		syscallEnter++;
		inSyscall = true;
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
		syscallExit++;
		inSyscall = false;
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
	}
	
        class StopEventObserver
            extends TaskObserverBase
            implements TaskObserver.Signaled
        {
            boolean startedLoop;
            public Action updateSignaled (Task task, int sig)
            {
		if (sig == Sig.USR1)
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
		if (task != null) {
		    Signal.tkill (task.id.id, Sig.USR1);
		}
	    }
	}

	TaskEventObserver taskEventObserver = new TaskEventObserver ();
	
	class ProcDestroyedObserver
	    implements Observer
	{
	    public void update (Observable o, Object obj)
	    {
		Proc process = (Proc) obj;
		if (isChildOfMine (process)) {
		    inSyscall = !inSyscall;  // we won't return from exit syscall
		    exited = true;
		    Manager.eventLoop.requestStop ();
		}
	    }
	}

	TestSyscallInterruptInternals (int pid)
	{
            this.pid = pid;
            host.requestRefreshXXX (true);
            Manager.eventLoop.runPending ();

            Proc p = host.getProc (new ProcId (pid));

            if (p != null) {
                List tasks = p.getTasks ();
                for (Iterator i = tasks.iterator (); i.hasNext (); ) {
                    Task t = (Task) i.next ();
                    if (t.getTaskId ().hashCode () == pid) {
                        t.traceSyscall = true;
                        t.requestAddSyscallObserver (taskEventObserver);
                        t.requestAddSignaledObserver (new StopEventObserver ());
                    }
                }
            }
            host.observableProcRemovedXXX.addObserver
                (new ProcDestroyedObserver ());
        }
    }

    public void testSyscallInterrupt ()
    {
	PipeReadChild prc = new PipeReadChild (false);

	TestSyscallInterruptInternals t 
	    = new TestSyscallInterruptInternals (prc.getPid ());

 	assertRunUntilStop ("run \"syscallint\" until exit");
	assertTrue ("process exited", t.exited);
	assertEquals ("read enter events", 1, t.readEnter);
	assertEquals ("read exit events", 1, t.readExit);
	assertEquals ("SIGUSR1 events", 1, t.sigusr1Count);
    }

    public void testSyscallInterruptRestart ()
    {
	PipeReadChild prc = new PipeReadChild (true);

	TestSyscallInterruptInternals t
	    = new TestSyscallInterruptInternals (prc.getPid ());

 	assertRunUntilStop ("run \"syscallint\" with restart until exit");
	assertTrue ("process exited", t.exited);
	assertEquals ("restart read enter events", 2, t.readEnter);
	assertEquals ("restart read exit events", 2, t.readExit);
	assertEquals ("restart sigusr1 events", 1, t.sigusr1Count);
    }
}
