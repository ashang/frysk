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

import java.util.Hashtable;
import java.util.Observable;
import java.math.BigInteger;

/**
 * Check that x86_64 registers can be accessed.
 */

public class TestX8664Regs
  extends SyscallExaminer
{
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
    Hashtable longResults = new Hashtable();
    Hashtable bigResults = new Hashtable();
    
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)
	
    class TaskEventObserver
      extends SyscallExaminer.Tester.TaskEventObserver
    {
    
    }

    class LongTaskEventObserver
      extends TaskEventObserver
    {
      public Action updateSyscallEnter (Task task)
      {
	super.updateSyscallEnter(task);
	SyscallEventInfo syscall;
	LinuxEMT64 isa;
	try 
	  {
	    syscall = task.getSyscallEventInfo ();
	    isa = (LinuxEMT64)task.getIsa ();
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
	if (syscallNum == 1) 
	  { 
	    for (int i = 0; i < regNames.length; i++)
	      {
		longResults.put(regNames[i], 
				new Long(isa.getRegisterByName(regNames[i])
					 .get (task)));
	      }
	    for (int i = 0; i < regNames.length; i++) 
	      {
		bigResults.put(regNames[i],
			       isa.getRegisterByName(regNames[i])
			       .getBigInteger(task));
	      }
	    
	  }
	return Action.CONTINUE;
      }
    }

    class RegsTestObserver 
      extends SyscallExaminer.TaskAddedObserver 
    {
      private TaskEventObserver taskEventObserver;
      
      RegsTestObserver(TaskEventObserver teo)
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
	if (isa instanceof LinuxEMT64) 
	  {
	    EMT64Isa = true;
	    task.requestAddSyscallObserver(taskEventObserver);
	    task.requestAddSignaledObserver(taskEventObserver);
	  }
	else
	  {
	    // If not ia32, stop immediately
	    EMT64Isa = false;
	    Manager.eventLoop.requestStop();
	  }
      }
    }

    TestX8664RegsInternals ()
    {
      super();
      addTaskAddedObserver(new RegsTestObserver(new LongTaskEventObserver()));
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
      assertTrue ("exited", exited);
    }
  }

  public void testX8664Regs ()
  {
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
}
