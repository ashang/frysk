// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteBuffer;
import frysk.isa.X8664Registers;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.Auxv;

class LinuxX8664 implements Isa {
    private static final Instruction X8664Breakpoint
	= new Instruction(new byte[] { (byte)0xcc }, false);
  
    /**
     * Get the breakpoint instruction for X8664.
     */
    public final Instruction getBreakpointInstruction() {
	return X8664Breakpoint;
    }

    /**
     * Returns the instruction at the given location in the memory
     * buffer, or null if there is no valid instruction at the given
     * location. FIXME - needs to be plugged into the InstructionParser
     * and cache the results.
     */
    public Instruction getInstruction(ByteBuffer bb, long addr) {
	bb.position(addr);
	return X8664InstructionParser.parse(bb);
    }

    /**
     * Get the true breakpoint address according to PC register after
     * hitting one breakpoint set in task. In X86-64, the length of
     * breakpoint instruction will be added to the PC register's
     * value. So the true breakpoint address is the PC register's
     * value minus the length of breakpoint.
     */
    public long getBreakpointAddress(Task task) {
	long pcValue = 0;

	pcValue = task.getPC();
	pcValue = pcValue - 1;
    
	return pcValue;
    }

    /**
     * Returns a non-empty list of addresses that can be used for out of
     * line stepping. Each address should point to a location at least
     * big enough for the largest instruction of this ISA.
     */
    public List getOutOfLineAddresses(Proc proc) {
	LinkedList addrs = new LinkedList();
	Auxv[] auxv = proc.getAuxv ();
	// Find the Auxv ENTRY data
	for (int i = 0; i < auxv.length; i++) {
	    if (auxv[i].type == inua.elf.AT.ENTRY)
		addrs.add(Long.valueOf(auxv[i].val));
	}
	return addrs;
    }

    /**
     * Reports whether or not the given Task just did a step of an
     * instruction.  This can be deduced by examining the single step
     * flag (BS bit 14) in the debug status register (DR6) on x86_64.
     * This resets the stepping flag.
     */
    public boolean isTaskStepped(Task task) {
	long value = task.getRegister(X8664Registers.DR6);
	boolean stepped = (value & 0x4000) != 0;
	task.setRegister(X8664Registers.DR6, value & ~0x4000);
	return stepped;
    }

    /**
     * Returns true if the last instruction executed by the given Task
     * was a trapping instruction that will be handled by the
     * kernel. This method should distinquish instructions that are
     * handled by the kernel (like syscall enter instructions) and those
     * that generate a trap signal. True is returned only when the
     * instruction shouldn't generate a signal. Called from the state
     * machine when a trap event has been detected that cannot be
     * attributed to entering a signal handler or a normal step
     * instruction notification.
     * 
     * On some kernels x86_64 doesn't generate spurious trap events (or
     * rather doesn't set the stepping flag) after returning from a
     * SYSCALL instruction.
     */
    public boolean hasExecutedSpuriousTrap(Task task) {
	long address = task.getPC();
	return (task.getMemory().getByte(address - 1) == (byte) 0x05
		&& task.getMemory().getByte(address - 2) == (byte) 0x0f);
    }

    /**
     * Returns true if the given Task is at an instruction that will
     * invoke the sig return system call.
     *
     * On x86_64 this is when the pc is at a 'syscall' instruction and
     * the rax register contains 0x0f.
     */
    public boolean isAtSyscallSigReturn(Task task) {
	long address = task.getPC();
	boolean result = (task.getMemory().getByte(address) == (byte) 0x0f
			  && task.getMemory().getByte(address + 1) == (byte) 0x05);
	if (result) {
	    long syscall_num = task.getRegister(X8664Registers.RAX);
	    result &= syscall_num == 0x0f;
	}
	return result;
    }

    private static LinuxX8664 isa;
    static LinuxX8664 isaSingleton () {
	if (isa == null)
	    isa = new LinuxX8664 ();
	return isa;
    }
}
