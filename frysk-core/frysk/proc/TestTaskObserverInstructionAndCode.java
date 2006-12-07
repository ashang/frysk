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

public class TestTaskObserverInstructionAndCode extends TestLib
{
  public void testInstructionAndCode()
  {
    if (brokenXXX(3676))
      return;

    // Create busy child.
    Child child = new AckDaemonProcess(true);
    Task task = child.findTaskUsingRefresh (true);

    // Watch for any unexpected terminations of the child process.
    TerminatedObserver to = new TerminatedObserver();
    task.requestAddTerminatedObserver(to);

    // Add a stepping observer.
    InstructionObserver instr = new InstructionObserver(task);
    task.requestAddInstructionObserver(instr);
    assertRunUntilStop("attach then block");

    // Make sure it triggered.
    assertTrue("added", instr.added);
    assertEquals("hit", 1, instr.hit);

    // Now add a Code observer on the address we just hit.
    long address = instr.lastAddress;
    CodeObserver code = new CodeObserver(task, address);
    task.requestAddCodeObserver(code, address);
    instr.setContinue(true);
    task.requestUnblock(instr);
    assertRunUntilStop("unblock and wait for hit");

    // Make sure it triggered.
    assertTrue("added", code.added);
    assertEquals("hit code", 1, code.hit);

    // Delete both observers.
    task.requestDeleteInstructionObserver(instr);
    task.requestDeleteCodeObserver(code, address);
    runPending();

    // Verify they were removed.
    assertTrue("deleted instr", instr.deleted);
    assertTrue("deleted code", code.deleted);
    assertEquals("hit code", 1, code.hit);

    // Add new observers
    instr = new InstructionObserver(task);
    code = new CodeObserver(task, address);
    task.requestAddCodeObserver(code, address);
    instr.setContinue(true);
    task.requestAddInstructionObserver(instr);
    // XXX - and here the inferior crashes - bug #3500 ?
    assertRunUntilStop("add both then wait for block");

    // Verify the code observer got hit.
    assertEquals("hit", 1, code.hit);
  }

  // Base class for this tests observers.
  // Keeps track of added, deleted and hit counts.
  static abstract class TestObserver implements TaskObserver
  {
    boolean added;
    boolean deleted;

    int hit;

    public void addedTo(Object o)
    {
      added = true;
    }

    public void deletedFrom(Object o)
    {
      deleted = true;
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }

  // Simple observer to alert when child process dies unexpectedly
  static class TerminatedObserver
    extends TestObserver
    implements TaskObserver.Terminated
  {

    // Shouldn't be triggered ever.
    public Action updateTerminated (Task task, boolean signal, int value)
    {
      String message = task + " terminated (signal: " + signal + "): " + value;
      System.err.println(message);
      throw new IllegalStateException(message);
    }
  }


  static class InstructionObserver
    extends TestObserver
    implements TaskObserver.Instruction
  {
    private final Task task;
    private boolean cont;

    long lastAddress;

    InstructionObserver(Task task)
    {
      this.task = task;
    }

    public Action updateExecuted(Task task)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);

      hit++;
      try
	{
	  lastAddress = task.getIsa().pc(task);
	}
      catch (TaskException te)
	{
	  throw new RuntimeException(te);
	}

      if (cont)
        return Action.CONTINUE;
      else
        {
          Manager.eventLoop.requestStop();
          return Action.BLOCK;
        }
    }

    public void setContinue(boolean cont)
    {
      this.cont = cont;
    }
  }

  static class CodeObserver
    extends TestObserver
    implements TaskObserver.Code
  {
    private final Task task;
    private final long address;

    public CodeObserver(Task task, long address)
    {
      this.task = task;
      this.address = address;
    }

    public Action updateHit (Task task, long address)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);
      if (address != this.address)
        throw new IllegalStateException("Wrong address, given " + address
                                        + " not equals expected "
                                        + this.address);

      hit++;
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
  }
}
