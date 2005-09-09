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
package frysk.proc;

import java.util.*;

/**
 * Check that syscall events are detected.
 *
 * This should be expanded later to support ISAs so that the syscall
 * parameters and return codes can be displayed.  That information must
 * be taken from registers specific to each platform.
 */

public class TestSyscall
    extends TestLib
{
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;

    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)

    class TaskEventObserver
 	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    TaskEvent e = (TaskEvent) obj;
            if (e instanceof TaskEvent.Trapped) {
	        stoppedTaskEventCount++;
	    }
            else if (e instanceof TaskEvent.Syscall) {
	        syscallTaskEventCount++;
	        syscallState ^= 1;
                TaskEvent.Syscall ste = (TaskEvent.Syscall)e;
	    }
 	}
    }

    TaskEventObserver taskEventObserver = new TaskEventObserver ();
    class ProcCreatedObserver
        implements Observer
    {
 	Task task;
        public void update (Observable o, Object obj)
        {
            Proc proc = (Proc) obj;
            proc.taskDiscovered.addObserver
                (new Observer () {
                        public void update (Observable o, Object obj)
                        {
                            task = (Task) obj;
 			    task.traceSyscall = true;
 			    task.syscallEvent.addObserver (taskEventObserver);
 			    task.stopEvent.addObserver (taskEventObserver);
                        }
                    }
                 );
        }
    }
                                                                                         
    ProcCreatedObserver pco = new ProcCreatedObserver ();

    class ProcDestroyedObserver
	implements Observer
    {
	volatile int count;
	public void update (Observable o, Object obj)
	{
	    Proc process = (Proc) obj;
	    if (process.parent == null) {
 	        syscallState ^= 1;  // we won't return from exit syscall
 	        exited = true;
		Manager.eventLoop.requestStop ();
	    }
	}
    }


    public void testSyscall ()
    {
        Manager.procDiscovered.addObserver (pco);
 	// Create program making syscalls
	Manager.host.requestCreateProc (new String[]
	    {
 		"./prog/syscall/syscalls"
 	    });

        Manager.procRemoved.addObserver (new ProcDestroyedObserver ());

 	assertRunUntilStop ("run \"syscalls\" until exit");

	assertEquals ("Single signalled task event", 1,
		      stoppedTaskEventCount);
	assertTrue ("At least 8 syscall events received",
		    syscallTaskEventCount >= 8);
	assertTrue ("Number of syscall events is even",
		    syscallState == 0);
	assertTrue ("Process exited", exited);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
