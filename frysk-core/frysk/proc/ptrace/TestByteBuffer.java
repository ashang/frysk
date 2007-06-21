// This file is part of the program FRYSK.
// 
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

package frysk.proc.ptrace;

import inua.eio.ByteBuffer;
import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;
import frysk.testbed.AttachedSelf;
import frysk.testbed.LocalMemory;
import frysk.sys.Ptrace.RegisterSet;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.Manager;

public class TestByteBuffer
    extends TestCase
{
    public void tearDown()
    {
	TearDownProcess.tearDown();
    }
    private int pid;
    private ByteBuffer addressSpaceByteBuffer;
    private ByteBuffer memorySpaceByteBuffer;
    private ByteBuffer registerByteBuffer;

    public void setUp ()
    {
	pid = new AttachedSelf().hashCode();
	addressSpaceByteBuffer
	    = new AddressSpaceByteBuffer (pid, AddressSpace.TEXT);
	memorySpaceByteBuffer
	    = new MemorySpaceByteBuffer (pid, AddressSpace.TEXT);
	if (RegisterSet.REGS != null) {
	    registerByteBuffer
		= new RegisterSetByteBuffer (pid, RegisterSet.REGS);
	}
    }

    public void verifySlice(ByteBuffer buffer, long addr, long length)
    {
	ByteBuffer slice = buffer.slice (addr, length);
	byte bytes[] = new byte[ (int)length];
	buffer.get (addr, bytes, 0, (int)length);
	for (int i = 0; i < length; i++) {
	    assertEquals ("byte at " + i, bytes[i],
			  slice.get (i));
	}
    }
    public void testSliceAddressSpace()
    {
	verifySlice(addressSpaceByteBuffer, LocalMemory.getFuncAddr(),
		    LocalMemory.getFuncBytes().length);
    }
    public void testSliceMemorySpace()
    {
	verifySlice(memorySpaceByteBuffer, LocalMemory.getFuncAddr(),
		    LocalMemory.getFuncBytes().length);
    }
    public void testSliceRegisterSet()
    {
	if (registerByteBuffer == null) {
	    System.out.print("<<SKIP>>");
	    return;
	}
	verifySlice(registerByteBuffer, 4, 4);
    }

    private void verifyModify(ByteBuffer buffer, long addr)
    {
	// Read, modify, read.
	byte oldByte = buffer.get(addr);
	buffer.putByte(addr, (byte)~oldByte);
	assertEquals ("modified", (byte)~oldByte, buffer.get(addr));
    }
    public void testModifyRegisterSet()
    {
	if (RegisterSet.REGS == null) {
	    System.out.print("<<SKIP>>");
	    return;
	}
	verifyModify(registerByteBuffer, 0);
    }
    public void testModifyAddressSpace()
    {
	verifyModify(addressSpaceByteBuffer, LocalMemory.getFuncAddr());
    }
    public void testModifyMemorySpace()
    {
	verifyModify(memorySpaceByteBuffer, LocalMemory.getFuncAddr());
    }

    private class AsyncModify
	implements Runnable
    {
	private boolean ran;
	private byte oldByte;
	private byte newByte;
	private long addr;
	private ByteBuffer buffer;
	private Exception e;
	AsyncModify (ByteBuffer buffer, long addr)
	{
	    this.buffer = buffer;
	    this.addr = addr;
	}
	public void run ()
	{
	    try {
		oldByte = buffer.get(addr);
		buffer.putByte(addr, (byte)~oldByte);
		newByte = buffer.get(addr);
	    }
	    catch (Exception e) {
		this.e = e;
	    }
	    ran = true;
	    Manager.eventLoop.requestStop();
	}
	void call ()
	{
	    new Thread (this).start();
	    while (!ran)
		assertTrue ("waiting for async modify",
			    Manager.eventLoop.runPolling(getTimeoutMilliseconds()));
	    if (e != null)
		throw new RuntimeException (e);
	    assertEquals ("byte modified", (byte)~oldByte, newByte);
	}
    }
    public void testAsyncRegisterSet()
    {
	if (registerByteBuffer == null) {
	    System.out.print("<<SKIP>>");
	    return;
	}
	new AsyncModify(registerByteBuffer, 0).call();
    }
    public void testAsyncAddressSpace ()
    {
	new AsyncModify(addressSpaceByteBuffer, LocalMemory.getFuncAddr()).call();
    }
    public void testAsyncMemorySpace ()
    {
	new AsyncModify(memorySpaceByteBuffer, LocalMemory.getFuncAddr()).call();
    }
}
