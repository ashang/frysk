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

import lib.dw.*;
import lib.elf.*;

import java.util.*;

public class TestTaskObserverCode extends TestLib
{
  public void testCode() throws Exception
  {
    // Create a child.
    Child child = new AckDaemonProcess();
    Task task = child.findTaskUsingRefresh (true);
    Proc proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    // Get address of "handler" function (which will handle all
    // signals of the funit-child so it is easy to trigger a hit).
    Elf elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
    Dwarf dwarf = new Dwarf(elf, DwarfCommand.READ, null);
    DwarfDie die = DwarfDie.getDecl(dwarf, "handler");
    ArrayList entryAddrs = die.getEntryBreakpoints();

    // We really expect just one entry point.
    assertEquals(entryAddrs.size(), 1);

    // Add a Code observer on the address.
    long address = ((Long) entryAddrs.get(0)).longValue();
    CodeObserver code = new CodeObserver(task, address);
    task.requestUnblock(attachedObserver);
    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("add breakpoint observer");

    assertFalse(code.hit);

    // Send the child an (arbitrary) signal
    child.signal(execSig);
    assertRunUntilStop("signal and wait for hit");

    assertTrue(code.hit);
  }

  static class CodeObserver
    implements TaskObserver.Code
  {
    private final Task task;
    private final long address;

    boolean hit;

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

      hit = true;

      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addedTo(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object o)
    {
      // Ignored
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }

  static class AttachedObserver
    implements TaskObserver.Attached
  {
    public Action updateAttached(Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed(Object observable, Throwable w)
    {
      fail(w.toString());
    }

    public void addedTo(Object observable)
    {
      // Ignored
    }

    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

}
