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
import util.PrintWriter;

/**
 * Check that a specific syscall event can be detected.  In this case,
 * an open syscall to a particular file.
 */

public class TestSyscallOpen
    extends TestLib
{
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile boolean inSyscall = false;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
    boolean openingTestFile;
    boolean testFileOpened;
    boolean expectedRcFound;
    String openName = "a.file";
    PrintWriter writer = new PrintWriter (System.out);

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
	        inSyscall = !inSyscall;
                TaskEvent.Syscall ste = (TaskEvent.Syscall)e;
		I386Linux.SyscallEventInfo syscallEventInfo
		    = new I386Linux.SyscallEventInfo ();
		int syscallNum = syscallEventInfo.number (e.task);
		Syscall syscall = Syscall.syscallByNum (syscallNum);
		if (inSyscall) {
		    char[] ch = new char[21];
		    long arg1, arg2;
		    int index = 0;
		    // syscall.printCall (writer, e.task, syscallEventInfo);
		    // verify that open attempted for file a.file
		    if (syscallNum == 5) { 
			long addr = syscallEventInfo.arg (e.task, 1);
                	StringBuffer x = new StringBuffer ();
                    	e.task.memory.get (addr, x);
			String name = x.toString ();
			if (name.indexOf (openName) >= 0) {
			    testFileOpened = true;
			    openingTestFile = true;
			}
                    }
		}
		else {
		    // syscall.printReturn (writer, e.task, syscallEventInfo);
		    // verify that open fails with ENOENT errno
		    if (syscallNum == 5 && openingTestFile) {
		    	openingTestFile = false;
			int rc = (int)syscallEventInfo.returnCode (e.task);
			if (rc == -2) // ENOENT
			    expectedRcFound = true;
		    }
		}
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
	public void update (Observable o, Object obj)
	{
	    Proc process = (Proc) obj;
	    if (process.parent == null) {
 	        inSyscall = !inSyscall;  // we won't return from exit syscall
 	        exited = true;
		Manager.eventLoop.requestStop ();
	    }
	}
    }


    public void testSyscallOpen ()
    {
        Manager.procDiscovered.addObserver (pco);
 	// Create program making syscalls
	Manager.host.requestCreateProc (new String[]
	    {
 		"./prog/syscall/syscalls"
 	    });

        Manager.procRemoved.addObserver (new ProcDestroyedObserver ());

 	assertRunUntilStop ("run \"syscalls\" until exit");
	
	assertEquals ("One signal task event received", 1,
		      stoppedTaskEventCount);
	assertTrue ("At least 8 syscall events received",
		    syscallTaskEventCount >= 8); 
	assertFalse ("Syscall state is initial state", inSyscall);
	assertTrue ("Attempt to open a.file", testFileOpened);
	assertTrue ("Open of a.file failed", expectedRcFound);
	assertTrue ("Process exited", exited);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
