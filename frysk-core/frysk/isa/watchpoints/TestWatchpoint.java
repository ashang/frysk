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

import java.util.ArrayList;
import java.util.Iterator;

import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.ElfSymbolBinding;
import lib.dwfl.ElfSymbolType;
import lib.dwfl.ElfSymbolVisibility;
import lib.dwfl.SymbolBuilder;
import frysk.config.Prefix;
import frysk.dwfl.DwflCache;
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
	long address = getGlobalSymbolAddress(task,"source");
	long debugControlRegister;
	WatchpointFunctions wp = WatchpointFunctionFactory.getWatchpointFunctions(task.getISA());
	long savedDebugControlRegister = wp.readControlRegister(task);
	for (int i=0; i<4; i++) {


	    wp.setWatchpoint(task, i, address, 4, false);
	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i).getAddress());
	    
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
		    wp.readWatchpoint(task, j).getAddress(),0);
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
	long address = getGlobalSymbolAddress(task,"source");
	long debugControlRegister;
	WatchpointFunctions wp = WatchpointFunctionFactory.getWatchpointFunctions(task.getISA());
	long savedDebugControlRegister = wp.readControlRegister(task);
	for (int i=0; i<4; i++) {


	    wp.setWatchpoint(task, i, address, 1, true);
	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i).getAddress());
	    
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
		    wp.readWatchpoint(task, j).getAddress(),0);
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
	long address = getGlobalSymbolAddress(task,"source");
	long debugControlRegister;	
	WatchpointFunctions wp = WatchpointFunctionFactory.getWatchpointFunctions(task.getISA());

	long savedDebugControlRegister = wp.readControlRegister(task);

	for (int i=0; i<4; i++) {
	    wp.setWatchpoint(task, i, address, 1, false);

	    // simple first test. Does register contain address?
	    assertEquals("Saved watchpoint and address are similar", 
		 address, wp.readWatchpoint(task, i).getAddress());
	    
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
	    assertEquals(i +"wp length bit 0", false, 
		    testBit(debugControlRegister, 18 + (i * 4)));
	    assertEquals(i + "wo length bit 1", false ,
		    testBit(debugControlRegister, 19 + (i * 4)));
	}
	
	for (int j=0; j < 4; j++) {	    
	    wp.deleteWatchpoint(task,j);
	    assertEquals("Deleted Watchpoint is 0", 
		    wp.readWatchpoint(task, j).getAddress(),0);
	}

	assertEquals("Debug control register is left as we originally found it", 
		savedDebugControlRegister,wp.readControlRegister(task));

    }

    public void testGetAllWatchpoints () {
	// Set maximum number of watchpoints, then test them
	// via getAll.
	if (unresolvedOnPPC(5991)) 
	    return;
	int lengthSet[]  = {1,1,2,4};
	int count = 0;
	Proc proc = giveMeABlockedProc();
	Task task = proc.getMainTask();
	long address = getGlobalSymbolAddress(task,"source");
	WatchpointFunctions wp = WatchpointFunctionFactory.getWatchpointFunctions(task.getISA());
	for (int i=0; i<wp.getWatchpointCount(); i++) 
	    wp.setWatchpoint(task, i, address, lengthSet[i], true);
	
	ArrayList watchpointSet = (ArrayList) wp.getAllWatchpoints(task);
	Iterator i = watchpointSet.iterator();
	while (i.hasNext()) {
	    Watchpoint watchpoint = ((Watchpoint) i.next());
	    assertNotNull("Check retrieved watchpoint is not null", watchpoint);
	    assertEquals("address = source var address for Watchpoint " + i,
		    address,watchpoint.getAddress());
	    assertEquals("length = " + lengthSet[count], lengthSet[count],
		    watchpoint.getRange());
	    assertEquals("register allocation = " + count, count,
		    watchpoint.getRegister());
	    assertEquals("writeOnly ", true,
		    watchpoint.isWriteOnly());
	    count++;
	}
	
	assertEquals("Returned count is correct", wp.getWatchpointCount(),count);
    }

    
    private boolean testBit(long register, int bitToTest) {
	return (register & (1L << bitToTest)) != 0;
    }
    
    private Proc giveMeABlockedProc () {
      DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
	      Prefix.pkgLibFile("funit-watchpoint"));
      assertNotNull(ackProc);
      return ackProc.getMainTask().getProc();
    }

    /**
     * Returns the address of a global label by quering the the Proc
     * main Task's Dwlf.
     */
    long getGlobalSymbolAddress(Task task, String label)  {
      Dwfl dwfl = DwflCache.getDwfl(task);
      Symbol sym = Symbol.get(dwfl, label);
      return sym.getAddress();
    }
    
    
    // Helper class since there there isn't a get symbol method in Dwfl,
    // so we need to wrap it all in a builder pattern.
    static class Symbol implements SymbolBuilder {
      private String name;
      private long address;

      private boolean found;

      private Symbol()  {
        // properties get set in public static get() method.
      }

      static Symbol get(Dwfl dwfl, String name)  {
        Symbol sym = new Symbol();
        sym.name = name;
        DwflModule[] modules = dwfl.getModules();
        for (int i = 0; i < modules.length && ! sym.found; i++)
  	modules[i].getSymbolByName(name, sym);
        
        if (sym.found)
  	return sym;
        else
  	return null;
      }

      String getName() {
        return name;
      }

      long getAddress() {
        return address;
      }

      public void symbol(String name, long value, long size, ElfSymbolType type,
	    ElfSymbolBinding bind, ElfSymbolVisibility visibility) {
	  if (name.equals(this.name)) {
	      this.address = value;
	      this.found = true;
	  }
	
      }
    } 
}    

