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

package frysk.proc.live;

import inua.eio.ByteBuffer;
import frysk.junit.TestCase;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.dummy.DummyProc;
import frysk.testbed.AttachedSelf;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.LocalMemory;
import frysk.sys.Ptrace.RegisterSet;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.Manager;

import java.util.Arrays;

public class TestByteBuffer
    extends TestCase
{
    // There are 2 sets of byte buffers to check, those that hold memory
    // and those that hold various register values.
    private ByteBuffer[] addressBuffers;
    private ByteBuffer[] registerBuffers;

    public void setUp () throws Exception
    {
      int pid;
      ByteBuffer addressSpaceByteBufferText;
      ByteBuffer addressSpaceByteBufferData;
      ByteBuffer memorySpaceByteBuffer;
      ByteBuffer usrByteBuffer;
      ByteBuffer registerByteBuffer;
      ByteBuffer fpregisterByteBuffer;
      ByteBuffer fpxregisterByteBuffer;

      // Watch for spawned processes, etc.
      super.setUp();

      pid = new AttachedSelf().hashCode();

      // Text and Data are the same, but can be accessed independently.
      addressSpaceByteBufferText
	= new AddressSpaceByteBuffer (pid, AddressSpace.TEXT);
      addressSpaceByteBufferData
	= new AddressSpaceByteBuffer (pid, AddressSpace.DATA);

      // Cheat with the proc, it is not actually used if no
      // breakpoints are set (testing with breakpoints set is done through
      // TestTaskObserverCode in the various BreakpointMemoryView tests).
      Proc proc = new DummyProc();
      BreakpointAddresses breakpoints = new BreakpointAddresses(proc);
      memorySpaceByteBuffer = new LogicalMemoryBuffer(pid,
						      AddressSpace.TEXT,
						      breakpoints);

      addressBuffers = new ByteBuffer[] { addressSpaceByteBufferText,
					  addressSpaceByteBufferData,
					  memorySpaceByteBuffer };

      // The USER area is seen as a register buffer.
      usrByteBuffer = new AddressSpaceByteBuffer(pid, AddressSpace.USR);

      // See how many other register sets there are.
      if (RegisterSet.REGS != null)
	{
	  registerByteBuffer
	    = new RegisterSetByteBuffer (pid, RegisterSet.REGS);
	  if (RegisterSet.FPREGS != null)
	    {
	      fpregisterByteBuffer
		= new RegisterSetByteBuffer (pid, RegisterSet.FPREGS);
	      if (RegisterSet.FPXREGS != null)
		{
		  fpxregisterByteBuffer
		    = new RegisterSetByteBuffer (pid, RegisterSet.FPXREGS);
		  registerBuffers = new ByteBuffer[] { usrByteBuffer,
						       registerByteBuffer,
						       fpregisterByteBuffer,
						       fpxregisterByteBuffer };
		}
	      else
		registerBuffers = new ByteBuffer[] { usrByteBuffer,
						     registerByteBuffer,
						     fpregisterByteBuffer };
	    }
	  else
	    registerBuffers = new ByteBuffer[] { usrByteBuffer,
						 registerByteBuffer };
	}
      else
	registerBuffers = new ByteBuffer[] { usrByteBuffer };
    }

    public void tearDown() throws Exception
    {
      addressBuffers = null;
      registerBuffers = null;

      // Clean up any left stuff processes/open files/etc.
      super.tearDown();
    }

    public void verifySlice(ByteBuffer buffer, long addr, int length)
    {
	ByteBuffer slice = buffer.slice (addr, length);
	byte bytes[] = new byte[length];
	buffer.get (addr, bytes, 0, length);
	for (int i = 0; i < length; i++) {
	    assertEquals ("byte at " + i, bytes[i],
			  slice.get (i));
	}
    }

    public void testSliceAddressBuffers()
    {
      for (int i = 0; i < addressBuffers.length; i++)
	verifySlice(addressBuffers[i], LocalMemory.getCodeAddr(),
		    LocalMemory.getCodeBytes().length);
    }

    public void testSliceRegisterBuffers()
    {
      for (int i = 0; i < registerBuffers.length; i++)
	verifySlice(registerBuffers[i], 4, 4);
    }

    private void verifyModify(ByteBuffer buffer, long addr)
    {
	// Read, modify, read.
	byte oldByte = buffer.get(addr);
	buffer.putByte(addr, (byte)~oldByte);
	assertEquals ("modified", (byte)~oldByte, buffer.get(addr));
    }

    public void testModifyRegisterBuffers()
    {
      for (int i = 0; i < registerBuffers.length; i++)
	verifyModify(registerBuffers[i], 0);
    }

    public void testModifyAddressBuffers()
    {
      for (int i = 0; i < addressBuffers.length; i++)
	verifyModify(addressBuffers[i], LocalMemory.getCodeAddr());
    }

    private void verifyAsyncModify(ByteBuffer buffer, long addr) {
	class AsyncModify implements Runnable {
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
	}
	// Force the event-loop onto the main thread; otherwize the
	// AsyncModify thread may get in first resulting in an
	// event-loop thread switch.
	Manager.eventLoop.runPending();
	AsyncModify asyncModify = new AsyncModify(buffer, addr);
	new Thread (asyncModify).start();
	long endTime = (System.currentTimeMillis()
			+ getTimeoutMilliseconds());
	while (!asyncModify.ran) {
	    assertTrue ("waiting for async modify",
			Manager.eventLoop.runPolling(getTimeoutMilliseconds()));
	    if (asyncModify.e != null)
		throw new RuntimeException (asyncModify.e);
	    if (endTime < System.currentTimeMillis())
		fail ("timeout expired");
	    assertEquals ("byte modified", (byte)~asyncModify.oldByte,
			  asyncModify.newByte);
	}
    }
    public void testAsyncRegisterBuffers() {
      for (int i = 0; i < registerBuffers.length; i++)
	verifyAsyncModify(registerBuffers[0], 0);
    }

    public void testAsyncAddressBuffers() {
      for (int i = 0; i < addressBuffers.length; i++)
	verifyAsyncModify(addressBuffers[i],
			  LocalMemory.getCodeAddr());
    }

    public void verifyPeeks(ByteBuffer buffer, long addr, byte[] origBytes)
    {
      byte bytes[] = new byte[origBytes.length];
      buffer.get(addr, bytes, 0, bytes.length);
      for (int i = 0; i < bytes.length; i++)
        assertEquals ("byte at " + i, bytes[i], origBytes[i]);
    }
  
    public void testAddressBufferPeeks()
    {
      long addr = LocalMemory.getCodeAddr();
      byte[] origBytes = LocalMemory.getCodeBytes();
      for (int i = 0; i < addressBuffers.length; i++)
	verifyPeeks(addressBuffers[i], addr, origBytes);
    }

    public void testRegisterBufferPeeks()
    {
      // Check that simple get loop is similar to bulk get.
      long addr = 4;
      byte[] origBytes = new byte[16];
      for (int i = 0; i < registerBuffers.length; i++)
	{
	  for (int j = 0; j < origBytes.length; j++)
	    origBytes[j] = registerBuffers[i].get(addr + j);
	  verifyPeeks(registerBuffers[i], addr, origBytes);
	}
    }
    private class AsyncPeeks
	implements Runnable
    {
	private ByteBuffer buffer;
	private long addr;
	private int length;
	private byte[] bytes;
	private Exception e;
	AsyncPeeks (ByteBuffer buffer, long addr, int length)
	{
	    this.buffer = buffer;
	    this.addr = addr;
	    this.length = length;
	    this.bytes = new byte[length];
	}
	public void run ()
	{
	    try {
		buffer.get (addr, bytes, 0, length);
	    }
	    catch (Exception e) {
		this.e = e;
	    }
	    Manager.eventLoop.requestStop();
	}
	void call (byte[] origBytes)
	{
	    // Force the event loop to running on this thread.  Ugly, and is to
	    // to be removed when bug #4688 is resolved.
	    Manager.eventLoop.runPolling(1);
	    new Thread (this).start();
	    assertTrue("waiting for async peeks",
		       Manager.eventLoop.runPolling(getTimeoutMilliseconds()));
	    if (e != null)
		throw new RuntimeException (e);
	    for (int i = 0; i < length; i++) {
		assertEquals ("byte at " + i, bytes[i], origBytes[i]);
	    }
	}
    }

    public void testAsyncPeeks ()
    {
      byte[] origBytes = LocalMemory.getCodeBytes();
      for (int i = 0; i < addressBuffers.length; i++)
	new AsyncPeeks(addressBuffers[i], LocalMemory.getCodeAddr(),
		       LocalMemory.getCodeBytes().length).call(origBytes);
    }

    public void testAsycnPeeksRegisters()
    {
      // Check position() and (async) get()
      int length = 8;
      byte[] origBytes = new byte[length];
      long address = 4;
      for (int i = 0; i < registerBuffers.length; i++)
	{
	  registerBuffers[i].position(address);
	  registerBuffers[i].get(origBytes);
	  new AsyncPeeks(registerBuffers[i], address,
			 length).call(origBytes);
	}
    }

  private void verifyBulkPut(ByteBuffer buffer, long addr, int len)
  {
    // Pasting the same bytes back over the old buffer in bulk
    // and read it back in.
    byte[] oldBytes = new byte[len];
    buffer.position(addr);
    buffer.get(oldBytes);
    buffer.position(addr);
    buffer.put(oldBytes);
    byte[] newBytes = new byte[len];
    buffer.position(addr);
    buffer.get(newBytes);
    assertTrue(Arrays.equals(oldBytes, newBytes));
  }

  public void testBulkPutRegisterBuffers()
  {
    for (int i = 0; i < registerBuffers.length; i++)
      verifyBulkPut(registerBuffers[i], 4, 4);
  }

  public void testBulkPutAddressBuffers()
  {
    for (int i = 0; i < addressBuffers.length; i++)
      verifyBulkPut(addressBuffers[i], LocalMemory.getCodeAddr(),
		    LocalMemory.getCodeBytes().length);
  }

  public void testMemoryBufferCapacity() 
  {
    Task task = new DaemonBlockedAtEntry("funit-slave").getMainTask();
    switch(task.getISA().wordSize()){
	case 4:
		assertEquals("Memory Buffer Capacity: ", 0xffffffffL,
		     task.getMemory().capacity());
		break;
	case 8:
		assertEquals("Memory Buffer Capacity: ", 0xffffffffffffffffL,
                     task.getMemory().capacity());
		break;
	default:
		fail("unknown word size");
	}
    }


}
