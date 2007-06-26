// This file is part of the program FRYSK.
// 
// Copyright 2007 Oracle Corporation.
// Copyright 2007, Red Hat Inc.
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

import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;
import frysk.testbed.AttachedSelf;
import frysk.testbed.LocalMemory;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.Manager;

public class TestMemorySpaceByteBuffer
    extends TestCase
{
    public void tearDown()
    {
	TearDownProcess.tearDown();
    }
    private int pid;
    private MemorySpaceByteBuffer memorySpaceByteBuffer;

    public void setUp ()
    {
	pid = new AttachedSelf().hashCode();
	memorySpaceByteBuffer
	    = new MemorySpaceByteBuffer (pid, AddressSpace.TEXT);
    }

    public void testPeeks ()
    {
	long addr = LocalMemory.getFuncAddr();
	int length = LocalMemory.getFuncBytes().length;
	byte bytes[] = new byte[(int)length];
	memorySpaceByteBuffer.peek (addr, bytes, 0, length);
	byte[] origBytes = LocalMemory.getFuncBytes();
	for (int i = 0; i < length; i++) {
	    assertEquals ("byte at " + i, bytes[i], origBytes[i]);
	}
    }

    private class AsyncPeeks
	implements Runnable
    {
	private MemorySpaceByteBuffer buffer;
	private long addr;
	private long length;
	private byte[] bytes;
	private Exception e;
	AsyncPeeks (MemorySpaceByteBuffer buffer, long addr, long length)
	{
	    this.buffer = buffer;
	    this.addr = addr;
	    this.length = length;
	    this.bytes = new byte[(int)length];
	}
	public void run ()
	{
	    try {
		buffer.peek (addr, bytes, 0, length);
	    }
	    catch (Exception e) {
		this.e = e;
	    }
	    Manager.eventLoop.requestStop();
	}
	void call ()
	{
	    // Force the event loop to running on this thread.  Ugly, and is to
	    // to be removed when bug #4688 is resolved.
	    Manager.eventLoop.runPolling(1);
	    new Thread (this).start();
	    assertTrue("waiting for async peeks",
		       Manager.eventLoop.runPolling(getTimeoutMilliseconds()));
	    if (e != null)
		throw new RuntimeException (e);
	    byte[] origBytes = LocalMemory.getFuncBytes();
	    for (int i = 0; i < length; i++) {
		assertEquals ("byte at " + i, bytes[i], origBytes[i]);
	    }
	}
    }
    public void testAsyncPeeks ()
    {
	new AsyncPeeks(memorySpaceByteBuffer, LocalMemory.getFuncAddr(),
		       LocalMemory.getFuncBytes().length).call();
    }
}
