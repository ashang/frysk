// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

import frysk.config.Config;
import inua.eio.ByteBuffer;
import java.util.HashSet;
import frysk.proc.FindProc;
import java.util.Collection;
import frysk.proc.HostRefreshBuilder;
import frysk.proc.Task;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.testbed.TestLib;

public class TestLinuxExe extends TestLib {
    public void testLinuxTaskMemory() {
	Proc proc
	    = LinuxExeFactory.createProc(Config.getPkgDataFile("test-exe-x86"),
					 new String[0]);
	assertNotNull("Proc exists in exefile", proc);
	assertNotNull("Executable file Host is Null?",proc.getHost());
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

    public void testRequestRefresh() {
	final Proc proc
	    = LinuxExeFactory.createProc(Config.getPkgDataFile("test-exe-x86"),
					 new String[0]);
	proc.getHost().requestRefresh(new HashSet(), new HostRefreshBuilder() {
		public void construct(Collection added, Collection removed) {
		    assertTrue("added contains proc", added.contains(proc));
		    assertFalse("removed contains proc", removed.contains(proc));
		    Manager.eventLoop.requestStop();
		}
	    });
	assertRunUntilStop("find proc");
    }

    public void testRequestProc() {
	final Proc proc
	    = LinuxExeFactory.createProc(Config.getPkgDataFile("test-exe-x86"),
					 new String[0]);
	proc.getHost().requestProc(proc.getPid(), new FindProc() {
		public void procFound(Proc found) {
		    assertEquals("found proc", proc, found);
		    Manager.eventLoop.requestStop();
		}
		public void procNotFound(int pid) {
		    fail("proc not found");
		}
	    });
	assertRunUntilStop("find proc");
    }
}
