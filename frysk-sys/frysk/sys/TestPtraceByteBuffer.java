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

package frysk.sys;

import junit.framework.TestCase;
import inua.eio.*;
import java.util.Arrays;

public class TestPtraceByteBuffer extends TestCase
{
    private int pid;
  

    protected void setUp()
    {
	pid = TestLib.forkIt();
	Ptrace.attach(pid);
	TestLib.waitIt(pid);
    }

    protected void tearDown()
    {
	TestLib.tearDown (pid);
    }

    /**
     * Tests that a variable in the traced child can be manipulated.
     */
    public void testTextVariable()
    {
	// XXX - Note that we currently don't have any way to determine
	// what in what real endian mode the TEXT Area is. So we fake
	// being in either big or little endian mode for now to see if
	// both work. We just poke values in the child space instead of
	// having the child setup the variables itsefl. This does also
	// mean we cannot make direct references to our own variables for
	// comparison since we won't know whether they are stored as
	// little or big endian.

	long addr;
	ByteBuffer buffer;
	buffer = new PtraceByteBuffer(pid, PtraceByteBuffer.Area.TEXT,
				      0xffffffffl);

	// We start in big endian mode.
	byte[] newBytes = new byte[]
	    { (byte) 0, (byte) 0, (byte) 0, (byte) 52 };
	addr = TestLib.getIntValAddr();
	buffer.position(addr);
	buffer.putByte(newBytes[0]);
	buffer.putByte(newBytes[1]);
	buffer.putByte(newBytes[2]);
	buffer.putByte(newBytes[3]);

	addr = TestLib.getByteValAddr();
	buffer.putByte(addr, (byte) 53);

	newBytes = new byte[]
	    { (byte) 0, (byte) 0, (byte) 0, (byte) 0,
	      (byte) 0, (byte) 0, (byte) 0, (byte) 54 };
	addr = TestLib.getLongValAddr();
	buffer.position(addr);
	buffer.putByte(newBytes[0]);
	buffer.putByte(newBytes[1]);
	buffer.putByte(newBytes[2]);
	buffer.putByte(newBytes[3]);
	buffer.putByte(newBytes[4]);
	buffer.putByte(newBytes[5]);
	buffer.putByte(newBytes[6]);
	buffer.putByte(newBytes[7]);

	// Int
	addr = TestLib.getIntValAddr();
	int intVal = buffer.getInt(addr);
	assertEquals("Forked child int value", 52, intVal);

	buffer.putInt(addr, 2);
	intVal = buffer.getInt(addr);
	assertEquals("Forked child int value after poke", 2, intVal);

	assertEquals("Our own int value", 42, TestLib.intVal);

	// Byte
	addr = TestLib.getByteValAddr();
	byte byteVal = buffer.getByte(addr);
	assertEquals("Forked child byte value", 53, byteVal);

	buffer.putByte(addr, (byte) 3);
	byteVal = buffer.getByte(addr);
	assertEquals("Forked child long value after poke", 3, byteVal);

	assertEquals("Our own byte value", 43, TestLib.byteVal);

	// Long
	addr = TestLib.getLongValAddr();
	long longVal = buffer.getLong(addr);
	assertEquals("Forked child long value", 54, longVal);

	buffer.putLong(addr, 4);
	longVal = buffer.getLong(addr);
	assertEquals("Forked child long value after poke", 4, longVal);

	assertEquals("Our own long value", 44, TestLib.longVal);

	// Pretend we have little endian values now
	newBytes = new byte[]
	    { (byte) 62, (byte) 0, (byte) 0, (byte) 0 };
	addr = TestLib.getIntValAddr();
	buffer.position(addr);
	buffer.putByte(newBytes[0]);
	buffer.putByte(newBytes[1]);
	buffer.putByte(newBytes[2]);
	buffer.putByte(newBytes[3]);

	addr = TestLib.getByteValAddr();
	buffer.putByte(addr, (byte) 63);

	newBytes = new byte[]
	    { (byte) 64, (byte) 0, (byte) 0, (byte) 0,
	      (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
	addr = TestLib.getLongValAddr();
	buffer.position(addr);
	buffer.putByte(newBytes[0]);
	buffer.putByte(newBytes[1]);
	buffer.putByte(newBytes[2]);
	buffer.putByte(newBytes[3]);
	buffer.putByte(newBytes[4]);
	buffer.putByte(newBytes[5]);
	buffer.putByte(newBytes[6]);
	buffer.putByte(newBytes[7]);

	buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);

	// Int
	addr = TestLib.getIntValAddr();
	intVal = buffer.getInt(addr);
	assertEquals("Forked child int value", 62, intVal);

	buffer.putInt(addr, 2);
	intVal = buffer.getInt(addr);
	assertEquals("Forked child int value after poke", 2, intVal);

	assertEquals("Our own int value", 42, TestLib.intVal);

	// Byte
	addr = TestLib.getByteValAddr();
	byteVal = buffer.getByte(addr);
	assertEquals("Forked child byte value", 63, byteVal);

	buffer.putByte(addr, (byte) 3);
	byteVal = buffer.getByte(addr);
	assertEquals("Forked child long value after poke", 3, byteVal);

	assertEquals("Our own byte value", 43, TestLib.byteVal);

	// Long
	addr = TestLib.getLongValAddr();
	longVal = buffer.getLong(addr);
	assertEquals("Forked child long value", 64, longVal);

	buffer.putLong(addr, 4);
	longVal = buffer.getLong(addr);
	assertEquals("Forked child long value after poke", 4, longVal);

	assertEquals("Our own long value", 44, TestLib.longVal);
    }

    /**
     * Tests that function code in the traced child can be manipulated.
     */
    public void testDataFunction()
    {
	PtraceByteBuffer buffer;
	buffer = new PtraceByteBuffer(pid, PtraceByteBuffer.Area.DATA,
				      0xffffffffl);

	long addr = TestLib.getFuncAddr();
	byte[] bytes = TestLib.getFuncBytes();
	byte[] childBytes = new byte[4];
	buffer.get(addr, childBytes, 0, 4);
	assertTrue("Child function address word",
		   Arrays.equals(bytes, childBytes));

	// int 0x80, int3
	byte[] newBytes = new byte[]
	    { (byte) 0xcd, (byte) 0x80, (byte) 0xcc, (byte) 0x00 };
	buffer.position(addr);
	buffer.putByte(newBytes[0]);
	buffer.putByte(newBytes[1]);
	buffer.putByte(newBytes[2]);
	buffer.putByte(newBytes[3]);
	bytes = TestLib.getFuncBytes();
	assertTrue("Our own function bytes after poke",
		   ! Arrays.equals(bytes, newBytes));
	buffer.get(addr, childBytes, 0, 4);
	assertTrue("Forked function word after poke",
		   Arrays.equals(newBytes, childBytes));
    }
  
}
