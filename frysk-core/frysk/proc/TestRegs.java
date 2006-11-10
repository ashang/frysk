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

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Observable;

import frysk.sys.SyscallNum;

/**
 * Check that registers can be accessed.
 */

public class TestRegs
  extends SyscallExaminer
{
  class TestI386RegsInternals extends SyscallExaminer.Tester
  {
    boolean ia32Isa;
    int syscallNum;
    long orig_eax;
    long ebx;
    long ecx;
    long edx;
    long ebp;
    long esp;
    long esi;
    long edi;
    int csLength;
    int ssLength;
    
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)
    
    class I386TaskEventObserver
      extends SyscallExaminer.Tester.TaskEventObserver
    {
      public Action updateSyscallEnter (Task task)
      {
        super.updateSyscallEnter(task);
        SyscallEventInfo syscall;
        LinuxIa32 isa;
        try 
          {
            syscall = task.getSyscallEventInfo ();
            isa = (LinuxIa32)task.getIsa ();
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
    	if (syscallNum == SyscallNum.SYSexit )
    	{
    	  orig_eax = isa.getRegisterByName ("orig_eax").get (task);
    	  ebx = isa.getRegisterByName ("ebx").get (task);
    	  ecx = isa.getRegisterByName ("ecx").get (task);
    	  edx = isa.getRegisterByName ("edx").get (task);
    	  ebp = isa.getRegisterByName ("ebp").get (task);
    	  esi = isa.getRegisterByName ("esi").get (task);
    	  edi = isa.getRegisterByName ("edi").get (task);
    	  esp = isa.getRegisterByName ("esp").get (task);
	  csLength = isa.getRegisterByName ("cs").getLength();
	  ssLength = isa.getRegisterByName ("ss").getLength();
    	}
    	return Action.CONTINUE;
    }
  }

    class I386RegsTestObserver 
      extends SyscallExaminer.TaskAddedObserver 
    {
      public void update(Observable o, Object obj)
      {
        super.update(o, obj);
        Task task = (Task)obj;
        
        if (!isChildOfMine(task.proc))
          return;
        
        I386TaskEventObserver taskEventObserver = new I386TaskEventObserver();
        Isa isa;
        
        try 
          {
            isa = task.getIsa();
          }
        catch (TaskException e) 
          {
            isa = null;
          }
        if (isa instanceof LinuxIa32) 
          {
            ia32Isa = true;
            task.requestAddSyscallObserver(taskEventObserver);
            task.requestAddSignaledObserver(taskEventObserver);
          }
        else
          {
            // If not ia32, stop immediately
            ia32Isa = false;
            Manager.eventLoop.requestStop();
          }
      }
    }

    TestI386RegsInternals ()
    {
      super();
      addTaskAddedObserver(new I386RegsTestObserver());
    }
  }
  
  class TestX8664RegsInternals extends SyscallExaminer.Tester
  {
    boolean EMT64Isa;
    int syscallNum;
    String[] regNames 
    = new String[] { "orig_rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    long[] regValues
    = new long[] 
    { 1, 2, 3, -4, 0xdeadbeefl, 0xfeeddeadbeefl,
      // 0xdeadbeefdeadbeef
      -0x2152411021524111l
    };
    int csLength;

    Hashtable longResults = new Hashtable();
    Hashtable bigResults = new Hashtable();
    
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)
    
    class X8664TaskEventObserver
      extends SyscallExaminer.Tester.TaskEventObserver
    {
    
    }

    class LongTaskEventObserver
      extends X8664TaskEventObserver
    {
      public Action updateSyscallEnter (Task task)
      {
        super.updateSyscallEnter(task);
        SyscallEventInfo syscall;
        LinuxX8664 isa;
        try 
          {
            syscall = task.getSyscallEventInfo ();
            isa = (LinuxX8664)task.getIsa ();
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
        if (syscallNum == SyscallNum.SYSexit)
	  { 
            for (int i = 0; i < regNames.length; i++)
              {
		longResults.put(regNames[i],
				new Long(isa.getRegisterByName(regNames[i]).get (task)));
              }
            for (int i = 0; i < regNames.length; i++) 
              {
		bigResults.put(regNames[i],
			       isa.getRegisterByName(regNames[i]).getBigInteger(task));
              }
	    csLength = isa.getRegisterByName("cs").getLength();
          }
        return Action.CONTINUE;
      }
    }

    class X8664RegsTestObserver 
      extends SyscallExaminer.TaskAddedObserver 
    {
      private X8664TaskEventObserver taskEventObserver;
      
      X8664RegsTestObserver(X8664TaskEventObserver teo)
      {
        super();
        taskEventObserver = teo;
      }
      
      public void update(Observable o, Object obj)
      {
        super.update(o, obj);
        Task task = (Task)obj;
        if (!isChildOfMine(task.proc))
          return;
        Isa isa;
        try 
          {
            isa = task.getIsa();
          }
        catch (TaskException e) 
          {
            isa = null;
          }
        if (isa instanceof LinuxX8664) 
          {
            EMT64Isa = true;
            task.requestAddSyscallObserver(taskEventObserver);
            task.requestAddSignaledObserver(taskEventObserver);
          }
        else
          {
            // If not X86_64, stop immediately
            EMT64Isa = false;
            Manager.eventLoop.requestStop();
          }
      }
    }

    TestX8664RegsInternals ()
    {
      super();
      addTaskAddedObserver(new X8664RegsTestObserver(new LongTaskEventObserver()));
    }
    
    void verify() 
    {
      for (int i = 0; i < regNames.length; i++) 
        {
          assertEquals(regNames[i] + " long value", 
                       regValues[i],
                       ((Long)longResults.get(regNames[i])).longValue());
        }
      
      for (int i = 0; i < regNames.length; i++) 
        {
          assertTrue(regNames[i] + " BigInteger value", 
                     BigInteger.valueOf(regValues[i])
                     .compareTo((BigInteger)bigResults.get(regNames[i])) == 0);
        }
      assertEquals ("cs length", 4, csLength);

      assertTrue ("exited", exited);
    }
  }

  class TestPPC64RegsInternals extends SyscallExaminer.Tester
  {
    long gpr0;
    long gpr3;
    long gpr4;
    long gpr5;
    double fpr0;
    double fpr1;
    double fpr2;
    int gpr0Length;
    int ccrLength;
    int xerLength;
    
    int syscallNum;

    static final int unknown = 0;
    static final int isaPPC32 = 1;
    static final int  isaPPC64 = 2;
    int isaType;
    
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)
    
    class PPC64TaskEventObserver
      extends SyscallExaminer.Tester.TaskEventObserver
    {
      public Action updateSyscallEnter (Task task)
      {
	Isa isaPPC64;
        SyscallEventInfo syscall;
        
        logger.entering("TestPPC64Regs.TaskEventObserver", 
            "updateSyscallEnter");
        super.updateSyscallEnter(task);
        
        try
          {
            syscall = task.getSyscallEventInfo ();
            isaPPC64 = task.getIsa ();
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
        
        if (syscallNum == SyscallNum.SYSexit)
        {
          long l;
          gpr0 = isaPPC64.getRegisterByName ("gpr0").get (task);
          gpr3 = isaPPC64.getRegisterByName ("gpr3").get (task);
          gpr4 = isaPPC64.getRegisterByName ("gpr4").get (task);
          gpr5 = isaPPC64.getRegisterByName ("gpr5").get (task);

          l = isaPPC64.getRegisterByName ("fpr0").get (task);
          fpr0 = Double.longBitsToDouble(l);
	  l = isaPPC64.getRegisterByName ("fpr1").get (task);
          fpr1 = Double.longBitsToDouble(l);
	  l = isaPPC64.getRegisterByName ("fpr2").get (task);
          fpr2 = Double.longBitsToDouble(l);

          gpr0Length = isaPPC64.getRegisterByName("gpr0").getLength();
	  ccrLength = isaPPC64.getRegisterByName("ccr").getLength();
	  xerLength = isaPPC64.getRegisterByName("xer").getLength();
        }
        
        return Action.CONTINUE;
      }
    }

    class PPC64RegsTestObserver 
      extends SyscallExaminer.TaskAddedObserver 
    {
      public void update(Observable o, Object obj)
      {
        super.update(o, obj);
        Task task = (Task)obj;
        
        if (!isChildOfMine(task.proc))
          return;
        
        Isa isa;
        PPC64TaskEventObserver taskEventObserver = new PPC64TaskEventObserver();
        
        try 
          {
            isa = task.getIsa();
          }
        catch (TaskException e) 
          {
            isa = null;
          }
        
        if ((isa instanceof LinuxPPC64) || isa instanceof LinuxPPC)
        {
          if (isa instanceof LinuxPPC64)
            isaType = TestPPC64RegsInternals.isaPPC64;
          else
            isaType = TestPPC64RegsInternals.isaPPC32;
          task.requestAddSyscallObserver(taskEventObserver);
          task.requestAddSignaledObserver(taskEventObserver);
        }
        else 
        {
          //If not PPC or PPC64, stop immediately
          isaType = TestPPC64RegsInternals.unknown;
          Manager.eventLoop.requestStop();
        }
      }
    }

    TestPPC64RegsInternals ()
    {
      super();
      addTaskAddedObserver(new PPC64RegsTestObserver());
    }
  }
  
  private void checkI386Regs ()
  {
    if (MachineType.getMachineType() != MachineType.IA32)
      return;
    
    TestI386RegsInternals t = new TestI386RegsInternals ();
    // Create program making an exit syscall");
    new AttachedDaemonProcess (new String[]
    {
      getExecPrefix () + "funit-ia32-regs"
    }).resume ();
    assertRunUntilStop ("run \"x86regs\" until exit");

    if (t.ia32Isa)
      {
        assertEquals ("orig_eax register", 1, t.orig_eax);
        assertEquals ("ebx register", 2, t.ebx);
        assertEquals ("ecx register", 3, t.ecx);
        assertEquals ("edx register", 4, t.edx);
        assertEquals ("ebp register", 5, t.ebp);
        assertEquals ("esi register", 6, t.esi);
        assertEquals ("edi register", 7, t.edi);
        assertEquals ("esp register", 8, t.esp);
	assertEquals ("cs length", 2, t.csLength);
	assertEquals ("ss length", 2, t.ssLength);  
        assertTrue ("exited", t.exited);
      }
  }
  
  private void checkX8664Regs ()
  {
    if (brokenXXX (3141))
        return;
    
    if (MachineType.getMachineType() != MachineType.X8664)
      return;
    
    TestX8664RegsInternals t = new TestX8664RegsInternals();
    // Create program making an exit syscall");
    AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
    {
      getExecPrefix () + "funit-x8664-regs"
    });
    
    logger.finest("About to resume funit-x8664-regs");
    child.resume();
    assertRunUntilStop ("run \"x86regs\" until exit");
    
    if (t.EMT64Isa) 
      {
        t.verify();
      }
  }
  
  private void checkPPC64Regs ()
  {
    if (MachineType.getMachineType() != MachineType.PPC64)
      return;
    
    TestPPC64RegsInternals t = new TestPPC64RegsInternals();
    // Create program making an exit syscall");
    AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
    {
      getExecPrefix () + "funit-ppc64-regs"
    });
    logger.finest("About to resume funit-ppc64-regs");
    child.resume();
    assertRunUntilStop ("run \"ppc64regs\" until exit");

    if (t.isaType == TestPPC64RegsInternals.isaPPC32 || 
	t.isaType == TestPPC64RegsInternals.isaPPC64)
      {
        assertEquals ("syscall", SyscallNum.SYSexit, t.syscallNum);
        assertEquals ("gpr0 register", 1, t.gpr0);
        assertEquals ("gpr3 register", 1, t.gpr3);
        assertEquals ("gpr4 register", 4, t.gpr4);
        if (t.isaType == TestPPC64RegsInternals.isaPPC64)
	  {
	    // Left shift 36bits from 0x1
	    assertEquals ("gpr5 register", 0x1000000000L, t.gpr5);
	    assertEquals ("gpr0 length", 8, t.gpr0Length);
	  }
	else
	  {
	    assertEquals ("gpr5 register", 0x0, t.gpr5);
	    assertEquals ("gpr0 length", 4, t.gpr0Length);
	  }
	assertEquals ("fpr0 register", (double)0.0, t.fpr0, 0);
	assertEquals ("fpr1 register", (double)1.0, t.fpr1, 0);
	assertEquals ("fpr2 register", (double)2.0, t.fpr2, 0);
	assertEquals ("ccr length", 4, t.ccrLength);
	assertEquals ("xer length", 4, t.xerLength);
        
        assertTrue ("exited", t.exited);
      }
  }

  public void testRegs()
  {
    String prefix = getExecPrefix();
    boolean doArch32 = prefix.indexOf("arch32") >= 0;
    
    if (MachineType.getMachineType() == MachineType.IA32
	|| (MachineType.getMachineType() == MachineType.X8664 && doArch32))
      checkI386Regs();
    else if (MachineType.getMachineType() == MachineType.X8664)
      checkX8664Regs();
    else if (MachineType.getMachineType() == MachineType.PPC64)
      checkPPC64Regs();
    else
      throw new UnsupportedOperationException(MachineType.getMachineType() + 
                                              " is not supported now.");
  }
}
