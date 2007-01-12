// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

public class TestTaskObserverInstruction extends TestLib
{
  public void testInstruction() throws TaskException
  {
    // We want a busy child, because we are going to follow its steps.
    Child child = new AckDaemonProcess(true);
    Task task = child.findTaskUsingRefresh (true);

    InstructionObserver instr1 = new InstructionObserver();

    task.requestAddInstructionObserver(instr1);
    assertRunUntilStop("attach then block");

    assertFalse("deleted", instr1.deleted);
    assertTrue("added", instr1.added);
    assertEquals("hit", 1, instr1.hit);

    assertFalse("!isa.isTaskStepped()", task.getIsa().isTaskStepped(task));

    task.requestUnblock(instr1);
    assertRunUntilStop("unblock self and hit");
    
    assertFalse("deleted", instr1.deleted);
    assertTrue("added", instr1.added);
    assertEquals("hit", 2, instr1.hit);

    assertTrue("isa.isTaskStepped()", task.getIsa().isTaskStepped(task));

    InstructionObserver instr2 = new InstructionObserver();

    task.requestAddInstructionObserver(instr2);
    assertRunUntilStop("attach while blocked");

    assertFalse("deleted 1", instr1.deleted);
    assertTrue("added 1", instr1.added);
    assertEquals("hit 1", 2, instr1.hit);

    assertFalse("deleted 2", instr2.deleted);
    assertTrue("added 2", instr2.added);
    assertEquals("hit 2", 1, instr2.hit);

    task.requestUnblock(instr1);
    task.requestUnblock(instr2);
    assertRunUntilStop("unblock both");

    assertFalse("deleted both 1", instr1.deleted);
    assertTrue("added both 1", instr1.added);
    assertEquals("hit both 1", 3, instr1.hit);

    assertFalse("deleted both 2", instr2.deleted);
    assertTrue("added both 2", instr2.added);
    assertEquals("hit both 2", 2, instr2.hit);

    task.requestDeleteInstructionObserver(instr1);
    task.requestUnblock(instr2);
    assertRunUntilStop("delete and unblock");

    assertTrue("deleted delete and unblock 1", instr1.deleted);
    assertTrue("added delete and unblock 1", instr1.added);
    assertEquals("hit delete and unblock 1", 3, instr1.hit);

    assertFalse("deleted delete and unblock 2", instr2.deleted);
    assertTrue("added delete and unblock 2", instr2.added);
    assertEquals("hit delete and unblock 2", 3, instr2.hit);

    task.requestDeleteInstructionObserver(instr2);
    task.requestAddInstructionObserver(instr1);
    assertRunUntilStop("delete and add");

    assertTrue("added delete and add 1", instr1.added);
    assertEquals("hit delete and add 1", 4, instr1.hit);

    assertTrue("deleted delete and add 2", instr2.deleted);
    assertEquals("hit delete and add 2", 3, instr2.hit);
  }

  static class InstructionObserver implements TaskObserver.Instruction
  {
    boolean added;
    boolean deleted;
    
    int hit;

    public Action updateExecuted(Task task)
    {
      hit++;
      Manager.eventLoop.requestStop ();
      return Action.BLOCK;
    }

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
}
