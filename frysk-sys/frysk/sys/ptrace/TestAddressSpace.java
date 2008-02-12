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

package frysk.sys.ptrace;

import frysk.sys.ProcessIdentifier;
import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;
import frysk.testbed.ForkFactory;
import frysk.testbed.LocalMemory;
import frysk.testbed.LocalMemory.StackBuilder;

/**
 * Trace a process.
 */

public class TestAddressSpace extends TestCase {
    /**
     * Rip down everything related to PID.
     */
    public void tearDown() {
	TearDownProcess.tearDown ();
    }

    private void verifyBytes(String what, ProcessIdentifier pid,
			     AddressSpace space,
			     byte[] bytes, long addr) {
	for (int i = 0; i < bytes.length; i++) {
	    assertEquals(what + " " + i + " at " + addr + " in " + space,
			 bytes[i] & 0xff, // signed - ulgh
			 space.peek(pid, addr + i));
	}
    }

    private void verifyPeek(String what, AddressSpace space,
			    byte[] bytes, long addr) {
	verifyBytes(what, ForkFactory.attachedDaemon(), space, bytes, addr);
    }
    public void testTextValPeek() {
	verifyPeek("TextVal", AddressSpace.TEXT,
		   LocalMemory.getDataBytes(),
		   LocalMemory.getDataAddr());
    }
    public void testDataValPeek() {
	verifyPeek("DataVal", AddressSpace.DATA,
		   LocalMemory.getDataBytes(),
		   LocalMemory.getDataAddr());
    }
    public void testTextFuncPeek() {
	verifyPeek("TextFunc", AddressSpace.TEXT,
		   LocalMemory.getCodeBytes(),
		   LocalMemory.getCodeAddr());
    }
    public void testDataFuncPeek() {
	verifyPeek("DataFunc", AddressSpace.DATA,
		   LocalMemory.getCodeBytes(),
		   LocalMemory.getCodeAddr());
    }
    public void testDataStackPeek() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPeek("DataStack", AddressSpace.DATA,
			       bytes, addr);
		}
	    });
    }
    public void testTextStackPeek() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPeek("DataStack", AddressSpace.TEXT,
			       bytes, addr);
		}
	    });
    }

    public void verifyPoke(String what, AddressSpace space,
			   byte[] bytes, long addr) {
	ProcessIdentifier pid = ForkFactory.attachedDaemon();
	for (byte i = 4; i < 12; i++) {
	    space.poke(pid, addr + i, i);
	    bytes[i] = i;
	    verifyBytes(what, pid, space, bytes, addr);
	}
    }
    public void testTextValPoke() {
	verifyPoke("TextVal", AddressSpace.TEXT,
		   LocalMemory.getDataBytes(),
		   LocalMemory.getDataAddr());
    }
    public void testDataValPoke() {
	verifyPoke("DataVal", AddressSpace.DATA,
		   LocalMemory.getDataBytes(),
		   LocalMemory.getDataAddr());
    }
    public void testTextFuncPoke() {
	verifyPoke("TextFunc", AddressSpace.TEXT,
		   LocalMemory.getCodeBytes(),
		   LocalMemory.getCodeAddr());
    }
    public void testDataFuncPoke() {
	verifyPoke("DataFunc", AddressSpace.DATA,
		   LocalMemory.getCodeBytes(),
		   LocalMemory.getCodeAddr());
    }
    public void testDataStackPoke() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPoke("DataStack", AddressSpace.DATA,
			       bytes, addr);
		}
	    });
    }
    public void testTextStackPoke() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPoke("DataStack", AddressSpace.TEXT,
			       bytes, addr);
		}
	    });
    }

    private void verifyPeekBytes(String why, AddressSpace space,
				 byte[] startBytes, long startAddr) {
	ProcessIdentifier pid = ForkFactory.attachedDaemon();
	byte[] pidBytes = new byte[startBytes.length];
	byte[] myBytes = new byte[startBytes.length];
	for (int addr = 4; addr < 9; addr++) {
	    for (int length = 0; length < 9; length++) {
		for (int offset = 0; offset < 9; offset++) {
		    // Copy the bytes in
		    space.peek(pid, startAddr + addr,
			       pidBytes, offset, length);
		    // Mimic the copy using local data.
		    for (int i = 0; i < length; i++)
			myBytes[offset + i] = startBytes[addr + i];
		    // Verify
		    for (int i = 0; i < myBytes.length; i++)
			assertEquals (why
				      + " addr=" + addr
				      + " length=" + length
				      + " offset=" + offset
				      + " i=" + i,
				      myBytes[i], pidBytes[i]);
		}
	    }
	}
    }
    public void testTextValPeekBytes() {
	verifyPeekBytes ("TextVal", AddressSpace.TEXT,
			 LocalMemory.getDataBytes(),
			 LocalMemory.getDataAddr());
    }
    public void testDataValPeekBytes() {
	verifyPeekBytes ("DataVal", AddressSpace.DATA,
			 LocalMemory.getDataBytes(),
			 LocalMemory.getDataAddr());
    }
    public void testTextFuncPeekBytes() {
	verifyPeekBytes("TextFunc", AddressSpace.TEXT,
			LocalMemory.getCodeBytes(),
			LocalMemory.getCodeAddr());
    }
    public void testDataFuncPeekBytes() {
	verifyPeekBytes("DataFunc", AddressSpace.DATA,
			LocalMemory.getCodeBytes(),
			LocalMemory.getCodeAddr());
    }
    public void testDataStackPeekBytes() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPeekBytes("DataStack", AddressSpace.DATA,
				    bytes, addr);
		}
	    });
    }
    public void testTextStackPeekBytes() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPeekBytes("DataStack", AddressSpace.TEXT,
				    bytes, addr);
		}
	    });
    }

    private void verifyPokeBytes(String why, AddressSpace space,
				 byte[] startBytes, long startAddr) {
	ProcessIdentifier pid = ForkFactory.attachedDaemon();
	byte[] newBytes = new byte[startBytes.length];
	byte[] myBytes = new byte[startBytes.length];
	byte[] pidBytes = new byte[startBytes.length];
	for (int i = 0; i < pidBytes.length; i++) {
	    myBytes[i] = startBytes[i];
	}
	for (int addr = 4; addr < 9; addr++) {
	    for (int length = 0; length < 9; length++) {
		for (int offset = 0; offset < 9; offset++) {
		    // Create some randomish data.
		    for (int i = 0; i < newBytes.length; i++)
			newBytes[i] = (byte)(addr + length + offset + i);
		    // Copy the sub buffer out.
		    space.poke(pid, startAddr + addr,
			       newBytes, offset, length);
		    // Fake the copy using local data.
		    for (int i = 0; i < length; i++)
			myBytes[addr + i] = newBytes[offset + i];
		    // Verify
		    space.peek(pid, startAddr, pidBytes, 0, startBytes.length);
		    for (int i = 0; i < myBytes.length; i++)
			assertEquals (why
				      + " addr=" + addr
				      + " length=" + length
				      + " offset=" + offset
				      + " byte=" + i,
				      myBytes[i], pidBytes[i]);
		}
	    }
	}
    }
    public void testTextValPokeBytes() {
	verifyPokeBytes("TextVal", AddressSpace.TEXT,
			LocalMemory.getDataBytes(),
			LocalMemory.getDataAddr());
    }
    public void testDataValPokeBytes() {
	verifyPokeBytes("DataVal", AddressSpace.DATA,
			LocalMemory.getDataBytes(),
			LocalMemory.getDataAddr());
    }
    public void testTextFuncPokeBytes() {
	verifyPokeBytes("TextFunc", AddressSpace.TEXT,
			LocalMemory.getCodeBytes(),
			LocalMemory.getCodeAddr());
    }
    public void testDataFuncPokeBytes() {
	verifyPokeBytes("DataFunc", AddressSpace.DATA,
			LocalMemory.getCodeBytes(),
			LocalMemory.getCodeAddr());
    }
    public void testDataStackPokeBytes() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPokeBytes("DataStack", AddressSpace.DATA,
				    bytes, addr);
		}
	    });
    }
    public void testTextStackPokeBytes() {
	LocalMemory.constructStack(new StackBuilder() {
		public void stack(long addr, byte[] bytes) {
		    verifyPokeBytes("DataStack", AddressSpace.TEXT,
				    bytes, addr);
		}
	    });
    }

    private void verifyOutOfBounds(String why, boolean expected,
				   int length, byte[] bytes, int offset) {
	ProcessIdentifier pid = ForkFactory.attachedDaemon();
	boolean caught = false;
	try {
	    AddressSpace.DATA.peek(pid, LocalMemory.getCodeAddr(),
				   bytes, offset, length);
	} catch (ArrayIndexOutOfBoundsException e) {
	    caught = true;
	}
	assertEquals(why + " exception", expected, caught);
    }
    public void testLengthUnderBound()
    {
	verifyOutOfBounds ("length under bound", true, -1, new byte[1], 1);
    }
    public void testOffsetUnderBound()
    {
	verifyOutOfBounds ("offset under bound", true, 1, new byte[1], -1);
    }
    public void testLengthOverBound()
    {
	verifyOutOfBounds ("length over bound", true, 1, new byte[0], 0);
    }
    public void testOffsetOverBound()
    {
	verifyOutOfBounds ("offset over bound", true, 1, new byte[1], 2);
    }
    public void testLengthOnBound()
    {
	verifyOutOfBounds ("length on bound", false, 1, new byte[1], 0);
    }
    public void testOffsetOnBound()
    {
	verifyOutOfBounds ("length on bound", false, 0, new byte[1], 1);
    }
}
