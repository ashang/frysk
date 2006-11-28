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

import java.util.Observable;
import java.util.Observer;

import frysk.sys.SyscallNum;

/**
 * Check that registers and memory can be modified for all platform.
 * This test case runs an assembler program that will terminate successfully
 * if left to run untouched.  The test add a syscall observer and modifies 
 * a register which is used as a jump location when the test returns.
 * This causes an alternate code path which issues an exit syscall.
 * The test also modifies other registers as well as a data word in the
 * program being run.  The alternate code path in the program being run
 * will verify the new register values and will not issue the exit syscall
 * unless everything is correct.
 */

public class TestRegMemModify
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
    
    class I386TaskEventObserver
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
          if (syscallNum == 20)
            { 
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
          else if (syscallNum == 1)
            {
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
          // The task will receive SIGPWR when creating 
          // AttachedDaemonProcess object. It should be ignored.
          return Action.CONTINUE; // not reached
        }
    }
    
    I386TaskEventObserver taskEventObserver = new I386TaskEventObserver ();

    class I386ProcRemovedObserver
        implements Observer
    {
        volatile int count;
        public void update (Observable o, Object obj)
        {
          Proc process = (Proc) obj;
          if (isChildOfMine (process))
            {
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
            catch (TaskException e)
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
        (new I386ProcRemovedObserver ());
    }
  }
  
  // Timers, observers, counters, etc.. needed for the test.
  class TestX8664ModifyInternals {
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
    boolean X8664Isa;
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
    
    class X8664TaskEventObserver
      extends TaskObserverBase
      implements TaskObserver.Syscall, TaskObserver.Signaled
               
    {
      private long memDataAddress = 0L;
      private long branchRightAddress = 0L;
      
      public Action updateSyscallEnter (Task task)
      {
    syscallState = 1;
    SyscallEventInfo syscall;
    LinuxX8664 isa;
    try 
      {
        syscall = task.getSyscallEventInfo();
        isa = (LinuxX8664)task.getIsa();             
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
    if (syscallNum == SyscallNum.SYSgetpid)
      { 
        rsi = isa.getRegisterByName ("rsi").get (task);
        assertEquals ("rsi register", 22, rsi);
        rdx = isa.getRegisterByName ("rdx").get (task);
        assertEquals ("rdx register", 23, rdx);
        // r10 contains address of memory location we
        // are expected to write 8 to
        r10 = isa.getRegisterByName ("r10").get (task);
        memDataAddress = r10;
        int mem = task.memory.getInt (r10);
        assertEquals ("old mem value", 3, mem);
        
        rdi = isa.getRegisterByName ("rdi").get (task);
        assertEquals ("rdi register", 21, rdi);
        // r8 contains the address we want to jump to
        // when we return from the syscall
        r8 = isa.getRegisterByName ("r8").get (task);
        branchRightAddress = r8;
      }
    else if (syscallNum == SyscallNum.SYSexit) 
      {
        rdi = isa.getRegisterByName ("rdi").get (task);
        assertEquals ("exit code", 2, rdi);
        exitSyscall = true;
      }
    return Action.CONTINUE;
      }
      
      public Action updateSyscallExit (Task task)
      {
        syscallState = 0;
        
        SyscallEventInfo syscall;
        LinuxX8664 isa;
        try 
          {
            syscall = task.getSyscallEventInfo();
            isa = (LinuxX8664)task.getIsa();             
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
        if (syscallNum == SyscallNum.SYSgetpid)
          { 
            task.memory.putInt (this.memDataAddress, 8);
            
            int mem = task.memory.getInt (r10);
            assertEquals ("new mem value", 8, mem);
            
            isa.getRegisterByName ("r9").put (task, this.branchRightAddress);
            // set a number of the registers as expected
            isa.getRegisterByName ("rdi").put (task, 2);
            isa.getRegisterByName ("rsi").put (task, 3);
            isa.getRegisterByName ("rdx").put (task, 0xdeadbeefL);
            isa.getRegisterByName ("r8").put (task, 0xdeadbeefdeadbeefL);
          }
        return Action.CONTINUE;
      }
      public Action updateSignaled (Task task, int sig)
      {
    // The task will receive SIGPWR when AttachedDaemonProcess
    // object is created during the test. It's normal and the 
    // test shouldn't fail for this.
    return Action.CONTINUE; // not reached
      }
    }
    
    X8664TaskEventObserver taskEventObserver = new X8664TaskEventObserver ();

    class X8664ProcRemovedObserver
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
        if (isa instanceof LinuxX8664) {
          X8664Isa = true;
          task.requestAddSyscallObserver (taskEventObserver);
          task.requestAddSignaledObserver (taskEventObserver);
        }
        else {
          // If not ia32, stop immediately
          X8664Isa = false;
          Manager.eventLoop.requestStop ();
        }
      }
    });
      host.observableProcRemovedXXX.addObserver
    (new X8664ProcRemovedObserver ());
    }
  }

  class TestPPC64ModifyInternals
  {
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
    
    long gpr0;
    long gpr3;
    
    // Test the following registers' value
    long gpr4;
    long gpr5;
    
    long gpr6; 
    long gpr7;
    long gpr8;
    
    int syscallNum;
    boolean isPPC64Isa;
    boolean exitSyscall;
    
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)

    class PPC64TaskEventObserver
      extends TaskObserverBase
      implements TaskObserver.Syscall, TaskObserver.Signaled
    {
      public Action updateSyscallEnter (Task task)
      {
        syscallState = 1;
        SyscallEventInfo syscall;
        LinuxPPC64 isa;
        
        try 
          {
            syscall = task.getSyscallEventInfo();
            isa = (LinuxPPC64)task.getIsa();             
          }
        catch (TaskException e)
          {
            fail("Got task exception " + e);
            return Action.CONTINUE; // not reached
          }
        
        // The low-level assembler code performs an exit syscall
        // and sets up the registers with simple values.  We want
        // to verify that all the registers are as expected.
        syscallNum = syscall.number (task);
        if (syscallNum == SyscallNum.SYSgetpid)
        {
          // In the assembler program, we store 21 to gpr3, 22 to gpr4
          // and 23 to gpr5 before syscalling
          gpr3 = isa.getRegisterByName ("gpr3").get (task);
          assertEquals ("gpr3 register", 21, gpr3);
          
          gpr4 = isa.getRegisterByName ("gpr4").get (task);
          assertEquals ("rsi register", 22, gpr4);
          
          gpr5 = isa.getRegisterByName ("gpr5").get (task);
          assertEquals ("rdx register", 23, gpr5);
          
          gpr6 = isa.getRegisterByName ("gpr6").get (task);
          int mem = task.memory.getInt (gpr6);
          
          // gpr6 store the value 3 first and then we will modify it to 8
          assertEquals ("old mem value", 3, mem);
          task.memory.putInt (gpr6, 8);
          mem = task.memory.getInt (gpr6);
          assertEquals ("new mem value", 8, mem);
          
          // After this, the assembler program will be executed.
          // If all is ok, it will call 'exit' syscall to exit.
        }
        else if (syscallNum == SyscallNum.SYSexit)
        {
          gpr3 = isa.getRegisterByName ("gpr3").get (task);
          assertEquals ("exit code", 3, gpr3);
          exitSyscall = true;
        }
        return Action.CONTINUE;
      }
      
      public Action updateSyscallExit (Task task)
      {
        SyscallEventInfo syscall;
        LinuxPPC64 isa;
       
        syscallState = 0;
        try 
          {
            syscall = task.getSyscallEventInfo();
            isa = (LinuxPPC64)task.getIsa();             
          }
        catch (TaskException e)
          {
            fail("Got task exception " + e);
            return Action.CONTINUE; // not reached
          }
        
        syscallNum = syscall.number (task);
        if (syscallNum == SyscallNum.SYSgetpid)
        {
          // Set a number of the registers as expected.
          // Then check them in assembly
          isa.getRegisterByName ("gpr3").put (task, 3);
          isa.getRegisterByName ("gpr4").put (task, 4);
          isa.getRegisterByName ("gpr5").put (task, 5);
          isa.getRegisterByName ("gpr7").put (task, 7);
        }
        return Action.CONTINUE;
      }
      
      public Action updateSignaled (Task task, int sig)
      {
        // The task will receive SIGPWR when creating 
        // AttachedDaemonProcess object. It should be ignored.
        return Action.CONTINUE; // not reached
      }
    }
    
    PPC64TaskEventObserver taskEventObserver = new PPC64TaskEventObserver ();
  
    class PPC64ProcRemovedObserver
      implements Observer
    {
      volatile int count;
      
      public void update (Observable o, Object obj)
      {
        Proc process = (Proc) obj;
        if (isChildOfMine (process))
        {
          syscallState ^= 1;  // we won't return from exit syscall
          exited = true;
          Manager.eventLoop.requestStop ();
        }
      }
    }

    TestPPC64ModifyInternals ()
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
          
          if (isa instanceof LinuxPPC64)
          {
            isPPC64Isa = true;
            task.requestAddSyscallObserver (taskEventObserver);
            task.requestAddSignaledObserver (taskEventObserver);
          }
          else
          {
            //If not PPC64, stop immediately
            isPPC64Isa = false;
            Manager.eventLoop.requestStop ();
          }
        }
      });
      
      host.observableProcRemovedXXX.addObserver(
                                                new PPC64ProcRemovedObserver ());
    }
  }

  private void checkI386Modify ()
  {
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
  
  private void checkX8664Modify ()
  {
    if (MachineType.getMachineType() != MachineType.X8664)
      return;
    TestX8664ModifyInternals t = new TestX8664ModifyInternals ();
    // Create program making syscalls
    new AttachedDaemonProcess (new String[]
    {
      getExecPrefix () + "funit-x8664-modify"
    }).resume ();
    assertRunUntilStop ("run \"x86modify\" to exit");

    if (t.X8664Isa) {
      assertTrue ("proc exited", t.exited);
      assertTrue ("exit syscall found", t.exitSyscall);
    }
  }

  private void checkPPC64Modify ()
  {
    if (MachineType.getMachineType() != MachineType.PPC64)
      return;
    TestPPC64ModifyInternals t = new TestPPC64ModifyInternals ();
    
    // Call assembler program making syscalls
    new AttachedDaemonProcess (new String[]
    {
      getExecPrefix () + "funit-ppc64-modify"
    }).resume ();
    assertRunUntilStop ("run \"ppc64modify\" to exit");
    
    if (t.isPPC64Isa)
    {
      assertTrue ("proc exited", t.exited);
      assertTrue ("exit syscall found", t.exitSyscall);
    }
  }
  
  public void testRegMemModify()
  {
    if (MachineType.getMachineType() == MachineType.IA32)
      checkI386Modify();
    else if (MachineType.getMachineType() == MachineType.X8664)
      checkX8664Modify();
    else if (MachineType.getMachineType() == MachineType.PPC64)
      checkPPC64Modify();
    else
      throw new UnsupportedOperationException(MachineType.getMachineType() + 
                                              " is not supported now.");
  }
}
