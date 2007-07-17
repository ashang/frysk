// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import frysk.junit.TestCase;
import frysk.testbed.TearDownProcess;
import frysk.testbed.AttachedSelf;
import frysk.sys.Ptrace.AddressSpace;
import frysk.testbed.LocalMemory;

/**
 * Check the plumming of Ptrace.
 */

public class TestPtrace
    extends TestCase
{
    /**
     * Rip down everything related to PID.
     */
    public void tearDown ()
    {
	TearDownProcess.tearDown ();
    }
 
    public void testChildContinue ()
    {
	final int pid = Fork.ptrace(null, null, null,
				    new String[] { "/bin/true" });
	assertTrue ("pid", pid > 0);
	TearDownProcess.add (pid);
	
	// The initial stop.
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.TRAP_, signal);
		}
	    });

	Ptrace.singleStep(pid, 0);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.TRAP_, signal);
		}
	    });

	Ptrace.cont (pid, Sig.TERM_);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void terminated (int pid, boolean signal, int value,
					boolean coreDumped)
		{
		    assertEquals ("terminated pid", id, pid);
		    assertEquals ("terminated signal", true, signal);
		    assertEquals ("terminated value", Sig.TERM_, value);
		}
	    });
    }
	
    /**
     * Check attach (to oneself).
     */
    public void testAttachDetach ()
    {
	final int pid = new Daemon (new Execute ()
	    {
		public void execute ()
		{
		    Itimer.sleep (TestCase.getTimeoutSeconds());
		}
	    }).hashCode ();
	TearDownProcess.add (pid);
	assertTrue ("pid", pid > 0);

	Ptrace.attach(pid);
	Wait.waitAll (pid, new UnhandledWaitBuilder ()
	    {
		private final int id = pid;
		protected void unhandled (String why)
		{
		    fail (why);
		}
		public void stopped (int pid, int signal)
		{
		    assertEquals ("stopped pid", id, pid);
		    assertEquals ("stopped sig", Sig.STOP_, signal);
		}
	    });

	Ptrace.detach (pid, 0);
	Errno errno = null;
	try {
	    Wait.waitAll (pid, new UnhandledWaitBuilder ()
		{
		    protected void unhandled (String why)
		    {
			fail (why);
		    }
		});
	}
	catch (Errno e) {
	    errno = e;
	}
	assertEquals ("Errno", Errno.Echild.class, errno.getClass());
    }

    private void verifyBytes (String what, int pid,
			      AddressSpace space,
			      byte[] bytes, long addr)
    {
	for (int i = 0; i < bytes.length; i++) {
	    assertEquals (what + " " + i + " at " + addr + " in " + space,
			  bytes[i] & 0xff, // signed - ulgh
			  space.peek(pid, addr + i));
	}
    }

    private void verifyPeek (String what, AddressSpace space,
			     byte[] bytes, long addr)
    {
	verifyBytes (what, new AttachedSelf ().hashCode(),
		     space, bytes, addr);
    }
    public void testTextValPeek ()
    {
	verifyPeek ("TextVal", AddressSpace.TEXT,
		    LocalMemory.getValBytes (),
		    LocalMemory.getValAddr());
    }
    public void testDataValPeek ()
    {
	verifyPeek ("DataVal", AddressSpace.DATA,
		    LocalMemory.getValBytes (),
		    LocalMemory.getValAddr());
    }
    public void testTextFuncPeek ()
    {
	verifyPeek ("TextFunc", AddressSpace.TEXT,
		    LocalMemory.getFuncBytes (),
		    LocalMemory.getFuncAddr());
    }
    public void testDataFuncPeek ()
    {
	verifyPoke ("DataFunc", AddressSpace.DATA,
		    LocalMemory.getFuncBytes (),
		    LocalMemory.getFuncAddr());
    }

    public void verifyPoke (String what, AddressSpace space,
			    byte[] bytes, long addr)
    {
	int pid = new AttachedSelf ().hashCode();
	for (byte i = 4; i < 12; i++) {
	    space.poke(pid, addr + i, i);
	    bytes[i] = i;
	    verifyBytes (what, pid, space, bytes, addr);
	}
    }
    public void testTextValPoke ()
    {
	verifyPoke ("TextVal", AddressSpace.TEXT,
		    LocalMemory.getValBytes (),
		    LocalMemory.getValAddr ());
    }
    public void testDataValPoke ()
    {
	verifyPoke ("DataVal", AddressSpace.DATA,
		    LocalMemory.getValBytes (),
		    LocalMemory.getValAddr ());
    }
    public void testTextFuncPoke ()
    {
	verifyPoke ("TextFunc", AddressSpace.TEXT,
		    LocalMemory.getFuncBytes (),
		    LocalMemory.getFuncAddr ());
    }
    public void testDataFuncPoke ()
    {
	verifyPoke ("DataFunc", AddressSpace.DATA,
		    LocalMemory.getFuncBytes (),
		    LocalMemory.getFuncAddr ());
    }

    private void verifyPeekBytes (String why, AddressSpace space,
				  byte[] startBytes, long startAddr)
    {
	int pid = new AttachedSelf().hashCode();
	byte[] pidBytes = new byte[startBytes.length];
	byte[] myBytes = new byte[startBytes.length];
	for (int addr = 4; addr < 9; addr++) {
	    for (int length = 0; length < 9; length++) {
		for (int offset = 0; offset < 9; offset++) {
		    // Copy the bytes in
		    space.peek(pid, startAddr + addr,
			       length, pidBytes, offset);
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
    public void testTextValPeekBytes ()
    {
	verifyPeekBytes ("TextVal", AddressSpace.TEXT,
			 LocalMemory.getValBytes (),
			 LocalMemory.getValAddr ());
    }
    public void testDataValPeekBytes ()
    {
	verifyPeekBytes ("DataVal", AddressSpace.DATA,
			 LocalMemory.getValBytes (),
			 LocalMemory.getValAddr ());
    }
    public void testTextFuncPeekBytes ()
    {
	verifyPeekBytes ("TextFunc", AddressSpace.TEXT,
			 LocalMemory.getFuncBytes (),
			 LocalMemory.getFuncAddr ());
    }
    public void testDataFuncPeekBytes ()
    {
	verifyPeekBytes ("DataFunc", AddressSpace.DATA,
			 LocalMemory.getFuncBytes (),
			 LocalMemory.getFuncAddr ());
    }

    private void verifyOutOfBounds (String why, boolean expected,
				    long length, byte[] bytes, int offset)
    {
	int pid = new AttachedSelf().hashCode();
	boolean caught = false;
	try {
	    AddressSpace.DATA.peek (pid, LocalMemory.getFuncAddr (), length,
				    bytes, offset);
	}
	catch (ArrayIndexOutOfBoundsException e) {
	    caught = true;
	}
	assertEquals (why + " exception", expected, caught);
    }
    public void testLengthUnderBound ()
    {
	verifyOutOfBounds ("length under bound", true, -1, new byte[1], 1);
    }
    public void testOffsetUnderBound ()
    {
	verifyOutOfBounds ("offset under bound", true, 1, new byte[1], -1);
    }
    public void testLengthOverBound ()
    {
	verifyOutOfBounds ("length over bound", true, 1, new byte[0], 0);
    }
    public void testOffsetOverBound ()
    {
	verifyOutOfBounds ("offset over bound", true, 1, new byte[1], 2);
    }
    public void testLengthOnBound ()
    {
	verifyOutOfBounds ("length on bound", false, 1, new byte[1], 0);
    }
    public void testOffsetOnBound ()
    {
	verifyOutOfBounds ("length on bound", false, 0, new byte[1], 1);
    }
}
