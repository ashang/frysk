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
 * Check that registers and memory can be modified for I386.
 * This test case runs an assembler program that will terminate successfully
 * if left to run untouched.  The test add a syscall observer and modifies 
 * a register which is used as a jump location when the test returns.
 * This causes an alternate code path which issues an exit syscall.
 * The test also modifies other registers as well as a data word in the
 * program being run.  The alternate code path in the program being run
 * will verify the new register values and will not issue the exit syscall
 * unless everything is correct.
 */

public class TestI386Modify
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
    boolean exitSyscall;

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
		throw new RuntimeException ("Unexpected signaled event " +
					    ste.signal);
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
		    if (syscallNum == 20) { 
			I386Linux.Isa isa = (I386Linux.Isa)e.task.getIsa ();
			ebx = isa.ebx.get (e.task);
			assertEquals ("EBX is 22", 22, ebx);
			ecx = isa.ecx.get (e.task);
			assertEquals ("ECX is 23", 23, ecx);
			// edx contains address of memory location we
			// are expected to write 8 to
			edx = isa.edx.get (e.task);
			int mem = e.task.memory.getInt (edx);
			assertEquals ("Old mem value is 3", 3, mem);
			e.task.memory.putInt (edx, 8);
			mem = e.task.memory.getInt (edx);
			assertEquals ("New mem value is 8", 8, mem);
			ebp = isa.ebp.get (e.task);
			assertEquals ("ebp is 21", 21, ebp);
			// esi contains the address we want to jump to
			// when we return from the syscall
			esi = isa.esi.get (e.task);
			isa.edi.put (e.task, esi);
			// set a number of the registers as expected
			isa.ebx.put (e.task, 2);
			isa.ecx.put (e.task, 3);
			isa.edx.put (e.task, 4);
			isa.ebp.put (e.task, 5);
			isa.esi.put (e.task, 6);
                    }
		    else if (syscallNum == 1) {
			I386Linux.Isa isa = (I386Linux.Isa)e.task.getIsa ();
			ebx = isa.ebx.get (e.task);
			assertEquals ("Exit code 2", 2, ebx);
			exitSyscall = true;
		    }
		}
	    }
 	}
    }

    TaskEventObserver taskEventObserver = new TaskEventObserver ();
    class ProcDiscoveredObserver
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

    ProcDiscoveredObserver pdo = new ProcDiscoveredObserver ();

    class ProcRemovedObserver
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

    public void testI386Modify ()
    {
//         Manager.procDiscovered.addObserver (pdo);
//  	// Create program making syscalls
//  	    Manager.host.requestCreateProc ( new String[]
//            {
//  		"./prog/x86isa/x86modify"
//  	    });

//         Manager.procRemoved.addObserver (new ProcRemovedObserver ());
//  	   assertRunUntilStop ("run \x86modify\" to exit");

//         assertTrue ("Proc destruction confirmed", exited);
//         assertTrue ("Exit syscall found", exitSyscall);
// 	   assertEquals ("Manager has no tasks left", 0, Manager.taskPool.size ());
//         assertEquals ("Manager has no processes left", 0, 
// 		      Manager.host.procPool.size ());
    }
}
