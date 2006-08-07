// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
 * Check that registers and memory can be modified for X8664.
 * This test case runs an assembler program that will terminate successfully
 * if left to run untouched.  The test add a syscall observer and modifies 
 * a register which is used as a jump location when the test returns.
 * This causes an alternate code path which issues an exit syscall.
 * The test also modifies other registers as well as a data word in the
 * program being run.  The alternate code path in the program being run
 * will verify the new register values and will not issue the exit syscall
 * unless everything is correct.
 */

public class TestX8664Modify
   extends TestLib
{
  // Timers, observers, counters, etc.. needed for the test.
  class TestX8664ModifyInternals {
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
    boolean EMT64Isa;
    int syscallNum;
    long orig_eax;
    long rax;
    long rdi;
    long rsi;
    long rdx;
    long r10;
    long r8;
    long r9;
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
	LinuxEMT64 isa;
	try 
	  {
	    syscall = task.getSyscallEventInfo();
	    isa = (LinuxEMT64)task.getIsa();		     
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
	  rsi = isa.getRegisterByName ("rsi").get (task);
	  assertEquals ("rsi register", 22, rsi);
	  rdx = isa.getRegisterByName ("rdx").get (task);
	  assertEquals ("rdx register", 23, rdx);
	  // r10 contains address of memory location we
	  // are expected to write 8 to
	  r10 = isa.getRegisterByName ("r10").get (task);
	  int mem = task.memory.getInt (r10);
	  assertEquals ("old mem value", 3, mem);
	  task.memory.putInt (r10, 8);
	  mem = task.memory.getInt (r10);
	  assertEquals ("new mem value", 8, mem);
	  rdi = isa.getRegisterByName ("rdi").get (task);
	  assertEquals ("rdi register", 21, rdi);
	  // r8 contains the address we want to jump to
	  // when we return from the syscall
	  r8 = isa.getRegisterByName ("r8").get (task);
	  isa.getRegisterByName ("r9").put (task, r8);
	  // set a number of the registers as expected
	  isa.getRegisterByName ("rdi").put (task, 2);
	  isa.getRegisterByName ("rsi").put (task, 3);
	  isa.getRegisterByName ("rdx").put (task, 4);
	  isa.getRegisterByName ("rcx").put (task, 5);
	  isa.getRegisterByName ("r8").put (task, 6);
	}
	else if (syscallNum == 1) {
	  rdi = isa.getRegisterByName ("rdi").get (task);
	  assertEquals ("exit code", 2, rdi);
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

    TestX8664ModifyInternals ()
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
	    catch (TaskException e)
	      {
		isa = null;
	      }
	    if (isa instanceof LinuxIa32) {
	      EMT64Isa = true;
	      task.requestAddSyscallObserver (taskEventObserver);
	      task.requestAddSignaledObserver (taskEventObserver);
	    }
	    else {
	      // If not ia32, stop immediately
	      EMT64Isa = false;
	      Manager.eventLoop.requestStop ();
	    }
	  }
	});
      host.observableProcRemovedXXX.addObserver
	(new ProcRemovedObserver ());
    }
  }
    
  public void testX8664Modify ()
  {
    if (MachineType.getMachineType() != MachineType.X8664)
      return;
    TestX8664ModifyInternals t = new TestX8664ModifyInternals ();
    // Create program making syscalls
    new AttachedDaemonProcess (new String[]
	{
	  getExecPrefix () + "funit-ia32-modify"
	}).resume ();
    assertRunUntilStop ("run \"x86modify\" to exit");

    if (t.EMT64Isa) {
      assertTrue ("proc exited", t.exited);
      assertTrue ("exit syscall found", t.exitSyscall);
    }
  }
}
