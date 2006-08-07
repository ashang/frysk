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

import java.util.Observer;
import java.util.Observable;

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
    // Timers, observers, counters, etc.. needed for the test.
    class TestI386ModifyInternals {
	volatile int stoppedTaskEventCount;
	volatile int syscallTaskEventCount;
	volatile int syscallState;
	volatile boolean exited;
	volatile int exitedTaskEventStatus;
	boolean openingTestFile;
	boolean testFileOpened;
	boolean expectedRcFound;
	boolean ia32Isa;
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
	    extends TaskObserverBase
	    implements TaskObserver.Syscall, TaskObserver.Signaled
		       
	{
	    public Action updateSyscallEnter (Task task)
	    {
		syscallState = 1;
		SyscallEventInfo syscall;
		LinuxIa32 isa;
		try 
		{
		     syscall = task.getSyscallEventInfo();
		     isa = (LinuxIa32)task.getIsa();		     
		}
		catch (TaskException e)
		{
		     fail("got task exception " + e);
		     return Action.CONTINUE; // not reached
		}
		// The low-level assembler code performs an exit syscall
		// and sets up the registers with simple values.  We want
		// to verify that all the registers are as expected.
		syscallNum = syscall.number (task);
		if (syscallNum == 20) { 
		    ebx = isa.getRegisterByName ("ebx").get (task);
		    assertEquals ("ebx register", 22, ebx);
		    ecx = isa.getRegisterByName ("ecx").get (task);
		    assertEquals ("ecx register", 23, ecx);
		    // edx contains address of memory location we
		    // are expected to write 8 to
		    edx = isa.getRegisterByName ("edx").get (task);
		    int mem = task.memory.getInt (edx);
		    assertEquals ("old mem value", 3, mem);
		    task.memory.putInt (edx, 8);
		    mem = task.memory.getInt (edx);
		    assertEquals ("new mem value", 8, mem);
		    ebp = isa.getRegisterByName ("ebp").get (task);
		    assertEquals ("ebp register", 21, ebp);
		    // esi contains the address we want to jump to
		    // when we return from the syscall
		    esi = isa.getRegisterByName ("esi").get (task);
		    isa.getRegisterByName ("edi").put (task, esi);
		    // set a number of the registers as expected
		    isa.getRegisterByName ("ebx").put (task, 2);
		    isa.getRegisterByName ("ecx").put (task, 3);
		    isa.getRegisterByName ("edx").put (task, 4);
		    isa.getRegisterByName ("ebp").put (task, 5);
		    isa.getRegisterByName ("esi").put (task, 6);
		}
		else if (syscallNum == 1) {
		    ebx = isa.getRegisterByName ("ebx").get (task);
		    assertEquals ("exit code", 2, ebx);
		    exitSyscall = true;
		}
		return Action.CONTINUE;
	    }
	    public Action updateSyscallExit (Task task)
	    {
		syscallState = 0;
		return Action.CONTINUE;
	    }
	    public Action updateSignaled (Task task, int sig)
	    {
		fail ("unexpected signal " + sig);
		return Action.CONTINUE; // not reached
	    }
	}
	
	TaskEventObserver taskEventObserver = new TaskEventObserver ();

	class ProcRemovedObserver
	    implements Observer
	{
	    volatile int count;
	    public void update (Observable o, Object obj)
	    {
		Proc process = (Proc) obj;
		if (isChildOfMine (process)) {
		    syscallState ^= 1;  // we won't return from exit syscall
		    exited = true;
		    Manager.eventLoop.requestStop ();
		}
	    }
	}

	TestI386ModifyInternals ()
	{
	    host.observableTaskAddedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Task task = (Task) obj;
			if (!isChildOfMine (task.proc))
			    return;
			killDuringTearDown (task.getTid ());
			Isa isa;
			try
			{
			     isa = task.getIsa();
			}
			catch (Task.TaskException e)
			{
			     isa = null;
			}
			if (isa instanceof LinuxIa32) {
			    ia32Isa = true;
			    task.requestAddSyscallObserver (taskEventObserver);
			    task.requestAddSignaledObserver (taskEventObserver);
			}
			else {
			    // If not ia32, stop immediately
			    ia32Isa = false;
			    Manager.eventLoop.requestStop ();
			}
		    }
		});
	    host.observableProcRemovedXXX.addObserver
		(new ProcRemovedObserver ());
	}
    }
    
    public void testI386Modify ()
    {
	if (brokenXXX (2933))
	    return;
	TestI386ModifyInternals t = new TestI386ModifyInternals ();
	// Create program making syscalls
	new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-ia32-modify"
	    }).resume ();
	assertRunUntilStop ("run \"x86modify\" to exit");

	if (t.ia32Isa) {
	    assertTrue ("proc exited", t.exited);
	    assertTrue ("exit syscall found", t.exitSyscall);
	}
   }
}
