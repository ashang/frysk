// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.util.Observer;
import frysk.sys.Sig;
import java.util.Observable;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;

/**
 * Check that memory can be modified.
 * This test case has frysk run a C program that opens a file and records
 * a memory address inside.  A SIGSEGV is generated by the program so that 
 * frysk can know when the file is ready.  Frysk gets the address, modifies
 * the program's memory and finally continues the C program.  The C program
 * verifies that the memory has been changed as expected and either
 * exits cleanly or aborts.
 */

public class TestModify
   extends TestLib
{
    // Timers, observers, counters, etc.. needed for the test.
    class TestModifyInternals {
	volatile int stoppedTaskEventCount;
	volatile boolean exited;
	volatile int exitedTaskEventStatus;
	boolean openingTestFile;
	boolean testFileOpened;
	boolean expectedRcFound;
	String memAddrFileName = "memAddr.file";
	
	// Need to add task observers to the process the moment it is
	// created, otherwize the creation of the very first task is
	// missed (giving a mismatch of task created and deleted
	// notifications.)
	
	class TaskEventObserver
	    extends TaskObserverBase
	    implements TaskObserver.Signaled
	{
	    public Action updateSignaled (Task task, int sig)
	    {
		if (sig == Sig.SEGV_) {
		    ByteBuffer b;
		    long memAddr;
		    long addr;
		    
		    // At this point, the program has signalled us to let
		    // us know that a file exists with the int size and
		    // memory address to modify.
		    try {
			java.io.FileInputStream memAddrFile
			    = new java.io.FileInputStream (memAddrFileName);
			byte[] buf = new byte[16];
			int len = memAddrFile.read (buf);
			b = new ArrayByteBuffer (buf, 0, len);
			b.order (task.getIsa ().byteOrder);
			b.wordSize (task.getIsa ().wordSize);
			memAddrFile.close ();
			// Make sure file is deleted.
			java.io.File f = new java.io.File (memAddrFileName);
			f.delete ();
		    }
		    catch (Exception x) {
			throw new RuntimeException (x);
		    }
		    memAddr = b.getUWord ();
		    addr = memAddr;
		    String chString = "abcdefghijklmnopqrstuvwxyz";
		    // Modify byte values across a page boundary.
		    for (int i = 0; i < 4097; ++i)
			task.memory.putByte (addr + i, 
					     (byte) chString.charAt (i % 26));
		    // Modify short values across a page boundary.
		    addr = memAddr + 8000;
		    for (int i = 0; i < 100; ++i)
			task.memory.putShort (addr + i * 2, 
					      (short) (50 - i));
		    // Modify an unaligned short value.
		    addr = memAddr + 9999;
		    task.memory.putShort (addr, (short) 0xdeaf);
		    // Modify int values across a page boundary.
		    addr = memAddr + 12096;
		    for (int i = 0; i < 100; ++i)
			task.memory.putInt (addr + i * 4, 
					    (int) (50 - i));
		    // Modify an unaligned int value.
		    addr = memAddr + 14001;
		    task.memory.putInt (addr, (int) 0xabcdef01);
		    // Modify long values across a page boundary.
		    addr = memAddr + 16192;
		    for (int i = 0; i < 100; ++i)
			task.memory.putLong (addr + i * 8, 
					     (long) (50 - i));
		    // Modify an unaligned int value.
		    addr = memAddr + 17003;
		    task.memory.putLong (addr, (long) 0xabcdef0123456789L);
		}
		return Action.CONTINUE;
	    }
	}
	
	TaskEventObserver taskEventObserver = new TaskEventObserver ();
	
	class ProcRemovedObserver
	    implements Observer
	{
	    volatile int count;
	    public void update (Observable o, Object obj)
	    {
		Proc process = (Proc) obj;
		if (isChildOfMine (process)) {
		    Manager.eventLoop.requestStop ();
		}
	    }
	}
	
	class TaskTerminatedObserver
	    extends TaskObserverBase
	    implements TaskObserver.Terminated
	{
	    public Action updateTerminated (Task task, boolean signal, int value)
	    {
		if (!signal) {
		    exitedTaskEventStatus = value;
		    exited = true;
		}
		return Action.CONTINUE;
	    }
	}

	TestModifyInternals ()
	{
	    host.observableTaskAddedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Task task = (Task) obj;
			if (!isChildOfMine (task.proc))
			    return;
			killDuringTearDown (task.getTid ());
			task.requestAddTerminatedObserver (new TaskTerminatedObserver ());
			task.requestAddSignaledObserver (taskEventObserver);
		    }
		});
	    host.observableProcRemovedXXX.addObserver
		(new ProcRemovedObserver ());
	}
    }
	
    public void testModify ()
    {
	if (brokenXXX ())
	    return;
	TestModifyInternals t = new TestModifyInternals ();
	// Create program making syscalls
	new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-modify"
	    }).resume ();

	assertRunUntilStop ("run \"modify\" to exit");

	assertTrue ("proc exit confirmed", t.exited);
   }
}
