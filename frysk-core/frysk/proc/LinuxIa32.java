// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

import frysk.isa.IA32Registers;
import inua.eio.ByteBuffer;
import java.util.List;
import java.util.LinkedList;

class LinuxIa32 implements Isa {

    private static final Instruction IA32Breakpoint
	= new Instruction(new byte[] { (byte)0xcc }, false);
  
    public long pc(Task task) {
	return task.getRegister(IA32Registers.EIP);
    }

    public void setPC(Task task, long address) {
	task.setRegister(IA32Registers.EIP, address);
    }

    /**
     * Get the breakpoint instruction for IA32.
     */
    public final Instruction getBreakpointInstruction() {
	return IA32Breakpoint;
    }

    /**
     * Returns the instruction at the given location in the memory
     * buffer, or null if there is no valid instruction at the given
     * location. FIXME - needs to be plugged into the InstructionParser
     * and cache the results.
     */
    public Instruction getInstruction(ByteBuffer bb, long addr) {
	bb.position(addr);
	return IA32InstructionParser.parse(bb);
    }
  
    /**
     * Get the true breakpoint address according to PC register after hitting 
     * one breakpoint set in task. In X86, the length of breakpoint instruction
     * will be added to the PC register's value. So the true breakpoint address
     * is the PC register's value minus the length of breakpoint. 
     */
    public long getBreakpointAddress(Task task) {
	long pcValue;
    
	pcValue = this.pc(task);
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
	for (int i = 0; i < auxv.length; i++)
	    {
		if (auxv[i].type == inua.elf.AT.ENTRY)
		    addrs.add(Long.valueOf(auxv[i].val));
	    }
	return addrs;
    }

    /**
     * Reports whether or not the given Task just did a step of an
     * instruction.  This can be deduced by examining the single step
     * flag (BS bit 14) in the debug status register (DR6) on x86.
     * This resets the stepping flag.
     */
    public boolean isTaskStepped(Task task) {
	long value = task.getRegister(IA32Registers.D6);
	boolean stepped = (value & 0x4000) != 0;
	task.setRegister(IA32Registers.D6, value & ~0x4000);
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
     * <p>
     * ia32 generate spurious trap events on "int 0x80" instructions
     * that should trap into a kernel syscall.
     */
    public boolean hasExecutedSpuriousTrap(Task task) {
	long address = pc(task);
	return (task.getMemory().getByte(address - 1) == (byte) 0x80
		&& task.getMemory().getByte(address - 2) == (byte) 0xcd);
    }

    /**
     * Returns true if the given Task is at an instruction that will
     * invoke the sig return system call.
     *
     * On x86 this is when the pc is at a int 0x80 instruction and the
     * eax register contains 0x77.
     */
    public boolean isAtSyscallSigReturn(Task task) {
	long address = pc(task);
	boolean result = (task.getMemory().getByte(address) == (byte) 0xcd
			  && (task.getMemory().getByte(address + 1)
			      == (byte) 0x80));
	if (result) {
	    long syscallNum = task.getRegister(IA32Registers.EAX);
	    result &= syscallNum == 0x77;
	}
	return result;
    }

    private static LinuxIa32 isa;
    static LinuxIa32 isaSingleton () {
	if (isa == null)
	    isa = new LinuxIa32 ();
	return isa;
    }
}
