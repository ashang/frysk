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

import frysk.isa.registers.X8664Registers;
import frysk.proc.Task;

class X8664Watchpoint extends Watchpoint {

    // Architecture Watchpoint Count. Number of usable
    // Address-Breakpoint Registers (DR0-DR3)
    public X8664Watchpoint () {
	noOfWatchpoints = 4;
    }
   
    /**
     * Builds and sets a hardware watchpoint on a task.
     *
     * @param task - task to set a watchpoint on.
     * @param index - watchpoint number to write. 4 on
     * x8664
     * @param addr - linear virtual address to watch.
     * @param range - length of range to watch. Normally
     * 1,24, or 8 bytes.
     * @param writeOnly - When true, only trigger when address is
     * written. False, trigger when address is read or written to.
     * @param localOnly - set local extant only.
     */
    public void setWatchpoint(Task task, int index, 
	       long addr, int range,
	       boolean writeOnly, 
	       boolean localOnly) {

	// turn bit off (b = bit no): l &= ~(1L << b)
	// turn bit on ( b= bit no):  l |= (1L << b);
	if ((range == 1) || (range == 2) || (range == 4) || (range == 8)) {
	    
	    // Set the Debug register with the linear address.
	    task.setRegister(X8664Registers.DEBUG_REGS_GROUP.getRegisters()[index],
			     addr);
	    // Get the Debug Control Register
	    long debugControl = task.getRegister(X8664Registers.DEBUG_CONTROL);
	    
	    // First eight bits of register define the global/local
	    // status of each of the four DR registers. Two bits per
	    // register
	    
	    // Calculate "Global Exact Breakpoint #index Enabled" bit to set
	    int bitToSet = index * 2;

	    if (localOnly) {
		// Set "Local Exact Breakpoint #index Enabled" to 1
		debugControl |= (1L << bitToSet);		
		// Set Global Exact Breakpoint to 0
		debugControl &= ~(1L << bitToSet+1);		
	    } else {
		// Set "Local Exact Breakpoint #index Enabled" to 0
		debugControl &= ~(1L << bitToSet);		
		// Set Global Exact Breakpoint to 1
		debugControl |= (1L << bitToSet+1);
	    }	    
	    // Dending on the WP register to set, the next
	    // 4 bits are offset 4 * WP Count. On x8664 
	    // the control bits for DR0 start at bit 16,
	    // DR1 at 20 and so on. 

	    // The next four bits are as follows:

	    // Set watchpoint to read/write detection
	    // bits 11 = read/write. bits 01 = write only.
	    int typeOfWpTrap = 16 + (index *4);
	    
	    if (writeOnly) {
		debugControl |= (1L << typeOfWpTrap);
		debugControl &= ~(1L << typeOfWpTrap+1);		
	    } else {
		debugControl |= (1L << typeOfWpTrap);
		debugControl |= (1L << typeOfWpTrap+1);
	    }
	    // Set watch point length
	    // 00 = 1 byte, 01 = 2 bytes, 11 = 4 bytes
	    // 10 = 8 bytes
	    int length = typeOfWpTrap + 2;
	    switch (range) {
	    case 1:
		debugControl &= ~(1L << length);
		debugControl &= ~(1L << length+1);
		break;
	    case 2:
		debugControl &= ~(1L << length);
		debugControl |= (1L << length+1);
		break;
	    case 4:
		debugControl |=(1L << length);
		debugControl |= (1L << length+1);
		break;
	    case 8:
		debugControl |= (1L << length);
		debugControl &= ~(1L << length+1);
		break;
	    }

	    task.setRegister(X8664Registers.DEBUG_CONTROL, debugControl);
	}
	else
	    throw new RuntimeException("Invalid size for watchpoint " +
				       "range. Has to be 1, 2, 4 or 8");
    }

    /**
     * Reads a watchpoint. Takes a task, and an index.
     *
     * @param task - task to read a watchpoint from.
     * @param index - watchpoint number to read.
     *
     * @return long - value of register for watchpoint.
     */
    public long readWatchpoint(Task task, int index) {
	return task.getRegister(X8664Registers.DEBUG_REGS_GROUP.getRegisters()[index]);
    }	

    /**
     * Deletes a watchpoint. Takes a task, and an index.
     *
     * @param task - task on which to delete a watchpoint.
     * @param index - watchpoint number to delete.
     *
     * @return long - value of register for wp
     */
    public final void deleteWatchpoint(Task task, int index) {
	    // Set the Debug register with the linear address.
	    task.setRegister(X8664Registers.DEBUG_REGS_GROUP.getRegisters()[index],
			     0x0L);
	    // Get the Debug Control Register
	    long debugControl = task.getRegister(X8664Registers.DEBUG_CONTROL);
	    
	    // First eight bits of register define the global/local
	    // status of each of the four DR registers. Two bits per
	    // register
	    
	    // Calculate "Global Exact Breakpoint #index Enabled" bit to set
	    int bitToSet = index * 2;

	    // Clear all register bits to 0
	    debugControl &= ~(1L << bitToSet);
	    debugControl &= ~(1L << bitToSet+1);

	    int typeOfWpTrap = 16 + (index *4);
	    debugControl &= ~(1L << typeOfWpTrap);
	    debugControl &= ~(1L << typeOfWpTrap+1);

	    int length = typeOfWpTrap + 2;
	    debugControl &= ~(1L << length);
	    debugControl &= ~(1L << length+1);

	    task.setRegister(X8664Registers.DEBUG_CONTROL, debugControl);
    }

    

    /**
     * Reads the Debug control register.
     *
     * @param task - task to read the debug control
     * register from.
     */
    public long readControlRegister(Task task) {
	return task.getRegister(X8664Registers.DEBUG_CONTROL);
    }


}
