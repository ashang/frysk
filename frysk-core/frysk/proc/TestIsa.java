// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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
import lib.dwfl.ElfEMachine;
import frysk.testbed.TestLib;
import frysk.testbed.TaskObserverBase;
import java.io.File;
import frysk.Config;
import frysk.testbed.FunitExecOffspring;

public class TestIsa
    extends TestLib
{

  class AttachedObserver
      extends TaskObserverBase
      implements TaskObserver.Attached
  {
    public Action updateAttached (Task task)
    {
	task.getIsa();
      
      assertTrue("task isa initialized", task.hasIsa());
      Manager.eventLoop.requestStop();
      return Action.CONTINUE;
    }
  }

  class DetachedObserver
      implements java.util.Observer
  {
    Task task;

    public DetachedObserver (Task t)
    {
      task = t;
    }

    public void update (Observable o, Object arg)
    {
      if (arg instanceof Task)
        {
          Task t = (Task) arg;

          if (t == task)
            {
              Manager.eventLoop.requestStop();
            }
        }
    }

  }

  public void testIsa ()
  {

    AckProcess ackProc = new DetachedAckProcess();

    final Task task = ackProc.findTaskUsingRefresh(true);

    assertFalse("Task isa not initialized", task.hasIsa());    

    TaskObserver.Attached attacher = new AttachedObserver();

    Task.taskStateDetached.addObserver(new DetachedObserver(task));

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("testIsa attach");
    task.requestDeleteAttachedObserver(attacher);
    assertRunUntilStop("testIsa detach");

    assertFalse("Task isa flushed", task.hasIsa());
  }

  public void testIsaSingleton ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    AckProcess ackProc2 = new DetachedAckProcess();

    Task firstMain = ackProc.findTaskUsingRefresh(true);
    Task secondMain = ackProc2.findTaskUsingRefresh(true);

    TaskObserver.Attached attacher = new AttachedObserver();

    firstMain.requestAddAttachedObserver(attacher);
    assertRunUntilStop("attach to first task");

    secondMain.requestAddAttachedObserver(attacher);
    assertRunUntilStop("attach to second task");

    assertNotNull("first task has Isa", firstMain.getIsa());
    assertNotNull("second task has Isa", secondMain.getIsa());
    
    assertSame(firstMain.getIsa(), secondMain.getIsa());
  }

  public void testAttachedCreateChild ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    Proc proc = ackProc.assertFindProcAndTasks();

    assertNotNull("child has an isa", proc.getMainTask().getIsa());

    ackProc.assertSendAddForkWaitForAcks();

    Proc child = (Proc) proc.getChildren().iterator().next();

    TaskObserver.Attached attacher = new AttachedObserver();

    child.getMainTask().requestAddAttachedObserver(attacher);

    assertRunUntilStop("attach to child process");

    assertNotNull("child has an isa", child.getMainTask().getIsa());
  }

  public void testAttachedCreateAttachedChild ()
  {
    AckProcess ackProc = new AttachedAckProcess();
    Proc proc = ackProc.assertFindProcAndTasks();

    class ForkedObserver
        extends TaskObserverBase
        implements TaskObserver.Forked
    {

      public Action updateForkedOffspring (Task parent, Task offspring)
      {
        offspring.requestAddForkedObserver(this);
        return Action.CONTINUE;
      }

      public Action updateForkedParent (Task parent, Task offspring)
      {
        return Action.CONTINUE;
      }

    }

    ForkedObserver forker = new ForkedObserver();
    proc.getMainTask().requestAddForkedObserver(forker);

    ackProc.assertSendAddForkWaitForAcks();

    Proc child = (Proc) proc.getChildren().iterator().next();

    assertNotNull("Child has an isa", child.getMainTask().getIsa());
  }
  
  public void testAttachedCreateAttachedClone()
  {
    AckProcess ackProc = new AttachedAckProcess();
    Proc proc = ackProc.assertFindProcAndTasks();

    class ClonedObserver
        extends TaskObserverBase
        implements TaskObserver.Cloned
    {

      public Action updateClonedOffspring (Task parent, Task offspring)
      {
        offspring.requestAddClonedObserver(this);
        return Action.CONTINUE;
      }

      public Action updateClonedParent (Task parent, Task offspring)
      {
        return Action.CONTINUE;
      }

    }

    ClonedObserver cloner = new ClonedObserver();
    proc.getMainTask().requestAddClonedObserver(cloner);

    ackProc.assertSendAddCloneWaitForAcks();

    Task clone = ackProc.findTaskUsingRefresh(false);

    assertNotNull("Clone has an isa", clone.getIsa());
  }

  public void testAttachDetachAttachAgainDetachAgainAttachAgainAgain ()
  {
    AckProcess ackProc = new DetachedAckProcess();

    Proc proc = ackProc.assertFindProcAndTasks();

    Task task = proc.getMainTask();

    AttachedObserver attacher = new AttachedObserver();

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("First attach");

    assertNotNull("Proc has an isa", proc.getMainTask().getIsa());

    Task.taskStateDetached.addObserver(new DetachedObserver(task));

    task.requestDeleteAttachedObserver(attacher);
    assertRunUntilStop("First Detach");

    assertFalse("Task doesn't have isa", proc.getMainTask().hasIsa());

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("Second attach");

    assertTrue("Task has isa", proc.getMainTask().hasIsa());

    task.requestDeleteAttachedObserver(attacher);
    assertRunUntilStop("Second detach");

    assertFalse("Task doesn't have isa", proc.getMainTask().hasIsa());

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("Third attach");

    assertTrue("Task has isa", proc.getMainTask().hasIsa());

  }
  
  public void test64To32To64 ()
  {
      File exec32 = Config.getPkgLib32File(null);
      File exec64 = Config.getPkgLib64File(null);
      if (unsupported ("32-on-64", exec32 == null && exec64 == null))
	  return;
      String[] invokeEcho = new String[] {
	  "/bin/echo", "hello"
      };
      String[] invoke64thenEcho
	  = FunitExecOffspring.getCommandLine(64, 0/*threads*/, null/*exe*/,
					      invokeEcho);
      String[] invoke32then64thenEcho
	  = FunitExecOffspring.getCommandLine(32, 0/*threads*/, null/*exe*/,
					      invoke64thenEcho);
      String[] invoke64then32then64thenEcho
	  = FunitExecOffspring.getCommandLine(32, 0/*threads*/, null/*exe*/,
					      invoke32then64thenEcho);
      FunitExecOffspring ackProc
	  = new FunitExecOffspring(invoke64then32then64thenEcho);

      Task task = ackProc.findTaskUsingRefresh(true);
      AttachedObserver attacher = new AttachedObserver();
      task.requestAddAttachedObserver(attacher);
      assertRunUntilStop("Attaching to main task");
      
      ackProc.assertRunExec("execing 64-bit");
      Isa isa64 = task.getIsa();
      assertNotNull("64 bit isa", isa64);
      
      ackProc.assertRunExec("64-bit execing 32-bit");
      assertNotNull("32 bit isa", task.getIsa());
      assertNotSame("32 bit and 64 bit isa", task.getIsa(), isa64);
      
      ackProc.assertRunExec("32-bit execing 64-bit");
      assertNotNull("64 bit isa", task.getIsa());
      assertSame("64 bit isa is a singleton", task.getIsa(), isa64);
  }

  public void testArbitraryISAInstantiation ()
  {
    IsaFactory factory = IsaFactory.getSingleton();
    assertSame("Arbitrary x86_64  ISA",factory.getIsaForCoreFile(ElfEMachine.EM_X86_64),LinuxX8664.isaSingleton());
    assertSame("Arbitrary i386  ISA",factory.getIsaForCoreFile(ElfEMachine.EM_386),LinuxIa32.isaSingleton());
    assertSame("Arbitrary PPC64 ISA",factory.getIsaForCoreFile(ElfEMachine.EM_PPC64),LinuxPPC64.isaSingleton());
    assertSame("Arbitrary PPC32 ISA",factory.getIsaForCoreFile(ElfEMachine.EM_PPC),LinuxPPC.isaSingleton());

  }
}
