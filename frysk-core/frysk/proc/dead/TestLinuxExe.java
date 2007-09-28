// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.proc.dead;

import frysk.Config;
import inua.eio.ByteBuffer;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Host;
import frysk.testbed.TestLib;
import frysk.proc.ProcId;
import frysk.proc.Manager;
import frysk.proc.TaskObserver;

public class TestLinuxExe
    extends TestLib
	    
{

  Host exeHost = new LinuxExeHost(Manager.eventLoop, 
				Config.getPkgDataFile("test-exe-x86"));
  
  
  public void testLinuxTaskMemory ()
  {
	assertNotNull("Executable file Host is Null?",exeHost);
    
	Proc proc = exeHost.getProc(new ProcId(0));
	assertNotNull("Proc exists in exefile", proc);
	Task task = proc.getMainTask();
	assertNotNull("Task exists in proc",task);
	
	ByteBuffer buffer = task.getMemory();
	
	assertNotNull("Buffer was not allocated", buffer);
	
	buffer.position(0x08048000L);
	
	assertEquals("Peek a byte at 0x08048000", 0x7F, buffer.getUByte());
	assertEquals("Peek a byte at 0x08048001", 0x45, buffer.getUByte());

	buffer.position(0x080497dcL);
	assertEquals("Peek a byte at 0x080497dc", (byte) 0xFF, buffer.getUByte());
	assertEquals("Peek a byte at 0x080497dd", (byte) 0xFF, buffer.getUByte());
  }

  // Helper class for inserting a Code breakpoint observer.
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
      Manager.eventLoop.requestStop();
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }
}
