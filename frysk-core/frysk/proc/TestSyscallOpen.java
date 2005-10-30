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
import inua.PrintWriter;

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
	extends TaskObserverBase
 	implements Observer, TaskObserver.Syscall
    {
	public Action updateSyscallEnter (Task task)
	{
	    fail ("not implemented");
	    return null;
	}
	public Action updateSyscallExit (Task task)
	{
	    fail ("not implemented");
	    return null;
	}
	public Action updateSyscallXXX (Task task)
	{
	    syscallTaskEventCount++;
	    inSyscall = !inSyscall;
	    I386Linux.SyscallEventInfo syscallEventInfo
		= new I386Linux.SyscallEventInfo ();
	    int syscallNum = syscallEventInfo.number (task);
	    if (inSyscall) {
		// syscall.printCall (writer, task, syscallEventInfo);
		// verify that open attempted for file a.file
		if (syscallNum == 5) { 
		    long addr = syscallEventInfo.arg (task, 1);
		    StringBuffer x = new StringBuffer ();
		    task.memory.get (addr, x);
		    String name = x.toString ();
		    if (name.indexOf (openName) >= 0) {
			testFileOpened = true;
			openingTestFile = true;
		    }
		}
	    }
	    else {
		// syscall.printReturn (writer, task, syscallEventInfo);
		// verify that open fails with ENOENT errno
		if (syscallNum == 5 && openingTestFile) {
		    openingTestFile = false;
		    int rc = (int)syscallEventInfo.returnCode (task);
		    if (rc == -2) // ENOENT
			expectedRcFound = true;
		}
	    }
	    return Action.CONTINUE;
	}
	public void update (Observable o, Object obj)
	{
	    TaskEvent e = (TaskEvent) obj;
            if (e instanceof TaskEvent.Trapped) {
	        stoppedTaskEventCount++;
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
	    if (!isChildOfMine (proc))
		return;
	    registerChild (proc.getId ().hashCode ());
            proc.observableTaskAdded.addObserver
                (new Observer () {
                        public void update (Observable o, Object obj)
                        {
                            task = (Task) obj;
 			    task.traceSyscall = true;
 			    task.requestAddSyscallObserver (taskEventObserver);
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
	    if (isChildOfMine (process)) {
 	        inSyscall = !inSyscall;  // we won't return from exit syscall
 	        exited = true;
		Manager.eventLoop.requestStop ();
	    }
	}
    }


    public void testSyscallOpen ()
    {
        Manager.host.observableProcAdded.addObserver (pco);
 	// Create program making syscalls
	Manager.host.requestCreateAttachedContinuedProc
	    (new String[] {
 		"./prog/syscall/syscalls"
 	    });

        Manager.host.observableProcRemoved.addObserver
	    (new ProcDestroyedObserver ());

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
    }
}
