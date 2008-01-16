// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.proc.live;

import frysk.isa.ISA;
import frysk.testbed.TestLib;
import frysk.testbed.TaskObserverBase;
import frysk.testbed.ExecOffspring;
import frysk.testbed.SlaveOffspring;
import frysk.testbed.ExecCommand;
import frysk.testbed.StatState;
import frysk.proc.TaskObserver;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Action;
import frysk.proc.Manager;

public class TestRuntimeIsa extends TestLib {

    private static void assertHasIsaEquals(String reason, Task task,
					   boolean hasIsa) {
	assertEquals("Has ISA (" + reason + ")", hasIsa,
		     ((LinuxPtraceTask)task).hasIsa());
    }

    static class AttachedObserver
	extends TaskObserverBase
	implements TaskObserver.Attached
    {
	public Action updateAttached(Task task) {
	    task.getISA();
	    assertHasIsaEquals("just attached", task, true);
	    Manager.eventLoop.requestStop();
	    return Action.CONTINUE;
	}
    }

  public void testIsa() {
      SlaveOffspring ackProc = SlaveOffspring.createChild();
      Task task = ackProc.findTaskUsingRefresh(true);
      assertHasIsaEquals("before attach", task, false);
      TaskObserver.Attached attacher = new AttachedObserver();
      task.requestAddAttachedObserver(attacher);
      assertRunUntilStop("testIsa attach");
      task.requestDeleteAttachedObserver(attacher);
      StatState.SLEEPING.assertRunUntil(task.getTid());
      assertHasIsaEquals("after detach", task, false);
  }

  public void testIsaSingleton ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    SlaveOffspring ackProc2 = SlaveOffspring.createChild();

    Task firstMain = ackProc.findTaskUsingRefresh(true);
    Task secondMain = ackProc2.findTaskUsingRefresh(true);

    TaskObserver.Attached attacher = new AttachedObserver();

    firstMain.requestAddAttachedObserver(attacher);
    assertRunUntilStop("attach to first task");

    secondMain.requestAddAttachedObserver(attacher);
    assertRunUntilStop("attach to second task");

    assertNotNull("first task has Isa", firstMain.getISA());
    assertNotNull("second task has Isa", secondMain.getISA());
    
    assertSame(firstMain.getISA(), secondMain.getISA());
  }

  public void testAttachedCreateChild ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
    Proc proc = ackProc.assertFindProcAndTasks();

    assertNotNull("child has an isa", proc.getMainTask().getISA());

    ackProc.assertSendAddForkWaitForAcks();

    Proc child = (Proc) proc.getChildren().iterator().next();

    TaskObserver.Attached attacher = new AttachedObserver();

    child.getMainTask().requestAddAttachedObserver(attacher);

    assertRunUntilStop("attach to child process");

    assertNotNull("child has an isa", child.getMainTask().getISA());
  }

  public void testAttachedCreateAttachedChild ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
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

    assertNotNull("Child has an isa", child.getMainTask().getISA());
  }
  
  public void testAttachedCreateAttachedClone()
  {
    SlaveOffspring ackProc = SlaveOffspring.createAttachedChild();
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

    assertNotNull("Clone has an isa", clone.getISA());
  }

  public void testAttachDetachAttachAgainDetachAgainAttachAgainAgain ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();

    Proc proc = ackProc.assertFindProcAndTasks();

    Task task = proc.getMainTask();

    AttachedObserver attacher = new AttachedObserver();

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("First attach");

    assertNotNull("Proc has an isa", proc.getMainTask().getISA());

    task.requestDeleteAttachedObserver(attacher);
    StatState.SLEEPING.assertRunUntil(task.getTid());

    assertHasIsaEquals("after 1st detach", proc.getMainTask(), false);

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("Second attach");

    assertHasIsaEquals("second attach", proc.getMainTask(), true);

    task.requestDeleteAttachedObserver(attacher);
    StatState.SLEEPING.assertRunUntil(task.getTid());

    assertHasIsaEquals("after 2nd detach", proc.getMainTask(), false);

    task.requestAddAttachedObserver(attacher);
    assertRunUntilStop("Third attach");

    assertHasIsaEquals("third attach", proc.getMainTask(), true);
  }
  
  public void test64To32To64 () {
      if (missing32or64())
	  return;
      ExecCommand invoke64
	  = new ExecCommand(ExecCommand.Executable.BIT64);
      ExecCommand invoke32then64
	  = new ExecCommand(ExecCommand.Executable.BIT32, invoke64);
      ExecCommand invoke64then32then64
	  = new ExecCommand(ExecCommand.Executable.BIT64, invoke32then64);
      ExecOffspring ackProc
	  = new ExecOffspring(new ExecCommand(ExecCommand.Executable.DEFAULT,
					      invoke64then32then64));

      Task task = ackProc.findTaskUsingRefresh(true);
      AttachedObserver attacher = new AttachedObserver();
      task.requestAddAttachedObserver(attacher);
      assertRunUntilStop("Attaching to main task");
      
      ackProc.assertRunExec("execing 64-bit");
      ISA isa64 = task.getISA();
      assertNotNull("64 bit isa", isa64);
      
      ackProc.assertRunExec("64-bit execing 32-bit");
      assertNotNull("32 bit isa", task.getISA());
      assertNotSame("32 bit and 64 bit isa", task.getISA(), isa64);
      
      ackProc.assertRunExec("32-bit execing 64-bit");
      assertNotNull("64 bit isa", task.getISA());
      assertSame("64 bit isa is a singleton", task.getISA(), isa64);
  }
}
