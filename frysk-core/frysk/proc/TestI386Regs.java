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
 * Check that I386 registers can be accessed.
 *
 */

public class TestI386Regs
    extends TestLib
{
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
    boolean openingTestFile;
    boolean testFileOpened;
    boolean expectedRcFound;
    String openName = "a.file";
    int syscallNum;
    long orig_eax;
    long ebx;
    long ecx;
    long edx;
    long ebp;
    long esp;
    long esi;
    long edi;

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
            if (e instanceof TaskEvent.Signaled) {
	        stoppedTaskEventCount++;
               TaskEvent.Signaled ste = (TaskEvent.Signaled)e;
	    }
            else if (e instanceof TaskEvent.Syscall) {
	        syscallState ^= 1;
                TaskEvent.Syscall ste = (TaskEvent.Syscall)e;
		I386Linux.SyscallEventInfo syscall = new I386Linux.SyscallEventInfo ();
		// The low-level assembler code performs an exit syscall
		// and sets up the registers with simple values.  We
		// want to verify that all the registers are as expected.
		if (syscallState == 1) {
		    char[] ch = new char[21];
		    long arg1, arg2;
		    int index = 0;
		    // verify that exit syscall occurs
		    syscallNum = syscall.number (e.task);
		    if (syscallNum == 1) { 
			I386Linux.Isa isa = (I386Linux.Isa)e.task.getIsa ();
			orig_eax = isa.orig_eax.get (e.task);
			ebx = isa.ebx.get (e.task);
			ecx = isa.ecx.get (e.task);
			edx = isa.edx.get (e.task);
			ebp = isa.ebp.get (e.task);
			esi = isa.esi.get (e.task);
			edi = isa.edi.get (e.task);
			esp = isa.esp.get (e.task);
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
	volatile int count;
	public void update (Observable o, Object obj)
	{
	    count++;
	    Proc process = (Proc) obj;
	    if (process.parent == null) {
 	        syscallState ^= 1;  // we won't return from exit syscall
 	        exited = true;
		Manager.eventLoop.requestStop ();
	    }
	}
    }


    public void testI386Regs ()
    {
        Manager.procDiscovered.addObserver (pco);
 	// Create program making an exit syscall");
	Manager.host.requestCreateProc (new String[]
	    {
 		"./prog/x86isa/x86regs"
 	    });

        Manager.procRemoved.addObserver (new ProcDestroyedObserver ());

 	assertRunUntilStop ("run \"x86regs\" until exit");

	assertEquals ("orig_eax = 1", 1, orig_eax);
	assertEquals ("ebx = 2", 2, ebx);
	assertEquals ("ecx = 3", 3, ecx);
	assertEquals ("edx = 4", 4, edx);
	assertEquals ("ebp = 5", 5, ebp);
	assertEquals ("esi = 6", 6, esi);
	assertEquals ("edi = 7", 7, edi);
	assertEquals ("esp = 8", 8, esp);

        assertTrue ("Exited", exited);
	assertEquals ("No tasks left", 0, Manager.host.taskPool.size ());
	assertEquals ("No processes left", 0, Manager.host.procPool.size ());
    }
}
