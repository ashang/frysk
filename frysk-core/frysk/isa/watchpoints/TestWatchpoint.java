// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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


package frysk.isa.watchpoints;

import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;
public class TestWatchpoint extends TestLib {
  

    public void testWatchFourBytesBitPattern() {
	// Test Four byte bit pattern in a cumulative fasion
	// across all WP registers. Assume global exact and 
	// read/write bit is set. At end, delete all watchpoints
	// and ensure debug control register is as we found it.
	if (unresolvedOnPPC(5991)) 
	    return;
	Proc proc = giveMeABlockedProc();
	Task task = proc.getMainTask();
	long address = 0x1000;
	long debugControlRegister;
	Watchpoint wp = WatchpointFactory.getWatchpoint(task.getISA());
	long savedDebugControlRegister = wp.readControlRegister(task);
	for (int i=0; i<4; i++) {


	    wp.setWatchpoint(task, i, address, 4, false, false);
	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i));
	    
	    debugControlRegister = wp.readControlRegister(task);
	
	    // Test Debug Control Register Bit Pattern. Global Exact.
	    assertEquals(i + " wp local exact bit", false, 
		    testBit(debugControlRegister, 0 + (i*2)));
	    assertEquals(i + " wp global exact bit", true, 
		    testBit(debugControlRegister,  1 + (i*2)));

	    // Test Debug Control Register. Fire on Read and Write
	    assertEquals(i + "wp r/w bit 0", true, 
		    testBit(debugControlRegister, 16 + (i * 4)));
	    assertEquals(i + "wp r/w bit 1 ", true, 
		    testBit(debugControlRegister, 17 + (i * 4)));

	    // Test Debug Control Register. Test Length four bytes.
	    assertEquals(i +"wp length bit 0", true, 
		    testBit(debugControlRegister, 18 + (i * 4)));
	    assertEquals(i + "wo length bit 1", true ,
		    testBit(debugControlRegister, 19 + (i * 4)));
	}
	
	for (int j=0; j < 4; j++) {	    
	    wp.deleteWatchpoint(task,j);
	    assertEquals("Deleted Watchpoint is 0", 
		    wp.readWatchpoint(task, j),0);
	}

	assertEquals("Debug control register is left as we originally found it", 
		savedDebugControlRegister,wp.readControlRegister(task));
	
    }

    public void testWatchOneByteBitPattern() {
	// Test One byte bit pattern in a cumulative fasion
	// across all WP registers. Assume global exact and 
	// read/write bit is set. At end, delete all watchpoints
	// and ensure debug control register is as we found it.
	if (unresolvedOnPPC(5991)) 
	    return;
	Proc proc = giveMeABlockedProc();
	Task task = proc.getMainTask();
	long address = 0x1000;
	long debugControlRegister;
	Watchpoint wp = WatchpointFactory.getWatchpoint(task.getISA());
	long savedDebugControlRegister = wp.readControlRegister(task);
	for (int i=0; i<4; i++) {


	    wp.setWatchpoint(task, i, address, 1, true, false);
	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i));
	    
	    debugControlRegister = wp.readControlRegister(task);
	
	    // Test Debug Control Register Bit Pattern. Global Exact.
	    assertEquals(i + " wp local exact bit", false, 
		    testBit(debugControlRegister, 0 + (i*2)));
	    assertEquals(i + " wp global exact bit", true, 
		    testBit(debugControlRegister,  1 + (i*2)));

	    // Test Debug Control Register. Fire on Read and Write
	    assertEquals(i + "wp r/w bit 0", true, 
		    testBit(debugControlRegister, 16 + (i * 4)));
	    assertEquals(i + "wp r/w bit 1 ", false, 
		    testBit(debugControlRegister, 17 + (i * 4)));

	    // Test Debug Control Register. Test Length four bytes.
	    assertEquals(i +"wp length bit 0", false, 
		    testBit(debugControlRegister, 18 + (i * 4)));
	    assertEquals(i + "wo length bit 1", false,
		    testBit(debugControlRegister, 19 + (i * 4)));
	}
	
	for (int j=0; j < 4; j++) {	    
	    wp.deleteWatchpoint(task,j);
	    assertEquals("Deleted Watchpoint is 0", 
		    wp.readWatchpoint(task, j),0);
	}

	assertEquals("Debug control register is left as we originally found it", 
		savedDebugControlRegister,wp.readControlRegister(task));
	
    }


    public void testWatchTwoByteBitPattern() {
	// Test two byte bit pattern AND test local exact only.
	// Test bit pattern in a cumulative fasion
	// across all WP registers. Assume local exact and 
	// but read/write bit is set. At end, delete all watchpoints
	// and ensure debug control register is as we found it.
	if (unresolvedOnPPC(5991)) 
	    return;
	Proc proc = giveMeABlockedProc();
	Task task = proc.getMainTask();
	long address = 0x0;
	long debugControlRegister;	
	Watchpoint wp = WatchpointFactory.getWatchpoint(task.getISA());
	long savedDebugControlRegister = wp.readControlRegister(task);

	for (int i=0; i<4; i++) {
	    wp.setWatchpoint(task, i, address, 1, false, true);

	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i));
	    
	    debugControlRegister = wp.readControlRegister(task);
	
	    // Test Debug Control Register Bit Pattern. Local Exact.
	    assertEquals(i + " wp local exact bit", true, 
		    testBit(debugControlRegister, 0 + (i*2)));
	    assertEquals(i + " wp global exact bit", false, 
		    testBit(debugControlRegister,  1 + (i*2)));

	    // Test Debug Control Register. Fire on Read and Write
	    assertEquals(i + "wp r/w bit 0", true, 
		    testBit(debugControlRegister, 16 + (i * 4)));
	    assertEquals(i + "wp r/w bit 1 ", true, 
		    testBit(debugControlRegister, 17 + (i * 4)));

	    // Test Debug Control Register. Test Length four bytes.
	    assertEquals(i +"wp length bit 0", false, 
		    testBit(debugControlRegister, 18 + (i * 4)));
	    assertEquals(i + "wo length bit 1", false ,
		    testBit(debugControlRegister, 19 + (i * 4)));
	}
	
	for (int j=0; j < 4; j++) {	    
	    wp.deleteWatchpoint(task,j);
	    assertEquals("Deleted Watchpoint is 0", 
		    wp.readWatchpoint(task, j),0);
	}

	assertEquals("Debug control register is left as we originally found it", 
		savedDebugControlRegister,wp.readControlRegister(task));

    }

    private boolean testBit(long register, int bitToTest) {
	return (register & (1L << bitToTest)) != 0;
    }
    
    private Proc giveMeABlockedProc ()
    {
      String[] nocmds = {};
      DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(nocmds);
      assertNotNull(ackProc);
      return ackProc.getMainTask().getProc();
    }

}    

