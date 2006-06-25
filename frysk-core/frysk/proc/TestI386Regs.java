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
 * Check that I386 registers can be accessed.
 */

public class TestI386Regs
    extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestI386RegsInternals {
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
		SyscallEventInfo syscall
		    = task.getIsa (). getSyscallEventInfo ();
		// The low-level assembler code performs an exit syscall
		// and sets up the registers with simple values.  We want
		// to verify that all the registers are as expected.
		syscallNum = syscall.number (task);
		if (syscallNum == 1) { 
		    LinuxIa32 isa = (LinuxIa32)task.getIsa ();
		    orig_eax = isa.orig_eax.get (task);
		    ebx = isa.ebx.get (task);
		    ecx = isa.ecx.get (task);
		    edx = isa.edx.get (task);
		    ebp = isa.ebp.get (task);
		    esi = isa.esi.get (task);
		    edi = isa.edi.get (task);
		    esp = isa.esp.get (task);
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
		stoppedTaskEventCount++;
		return Action.CONTINUE;
	    }
	}
	
	TaskEventObserver taskEventObserver = new TaskEventObserver ();
	
	class ProcDestroyedObserver
	    implements Observer
	{
	    volatile int count;
	    public void update (Observable o, Object obj)
	    {
		count++;
		Proc process = (Proc) obj;
		if (isChildOfMine (process)) {
		    syscallState ^= 1;  // we won't return from exit syscall
		    exited = true;
		    Manager.eventLoop.requestStop ();
		}
	    }
	}

	TestI386RegsInternals ()
	{
	    host.observableTaskAddedXXX.addObserver (new Observer () 
		{
		    public void update (Observable o, Object obj)
		    {
			Task task = (Task) obj;
			if (!isChildOfMine (task.proc))
			    return;
			killDuringTearDown (task.getTid ());
			if (task.getIsa () instanceof LinuxIa32) {
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
		(new ProcDestroyedObserver ());
	}
   }


    public void testI386Regs ()
    {
	if (brokenXXX ())
	    return;
	TestI386RegsInternals t = new TestI386RegsInternals ();
 	// Create program making an exit syscall");
	new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-ia32-regs"
	    }).resume ();
 	assertRunUntilStop ("run \"x86regs\" until exit");

	if (t.ia32Isa) {
	    assertEquals ("orig_eax register", 1, t.orig_eax);
	    assertEquals ("ebx register", 2, t.ebx);
	    assertEquals ("ecx register", 3, t.ecx);
	    assertEquals ("edx register", 4, t.edx);
	    assertEquals ("ebp register", 5, t.ebp);
	    assertEquals ("esi register", 6, t.esi);
	    assertEquals ("edi register", 7, t.edi);
	    assertEquals ("esp register", 8, t.esp);

            assertTrue ("exited", t.exited);
	}
    }
}
