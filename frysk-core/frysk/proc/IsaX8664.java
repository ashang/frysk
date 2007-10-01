// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteOrder;
import inua.eio.ByteBuffer;
import frysk.sys.Ptrace.RegisterSet;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.live.RegisterSetByteBuffer;
import frysk.proc.live.AddressSpaceByteBuffer;
import lib.dwfl.ElfEMachine;
import frysk.isa.X8664Registers;

public class IsaX8664 implements Isa
{
  private static final Instruction X8664Breakpoint
    = new Instruction(new byte[] { (byte)0xcc }, false);
  
    private final BankRegisterMap registerMap = new BankRegisterMap();

    IsaX8664()  {
	registerMap
	    .add(new BankRegister(0, 80, 8, X8664Registers.RAX))
	    .add(new BankRegister(0, 40, 8, X8664Registers.RBX))
	    .add(new BankRegister(0, 88, 8, X8664Registers.RCX))
	    .add(new BankRegister(0, 96, 8, X8664Registers.RDX))
	    .add(new BankRegister(0, 104, 8, X8664Registers.RSI))
	    .add(new BankRegister(0, 112, 8, X8664Registers.RDI))
	    .add(new BankRegister(0, 32, 8, X8664Registers.RBP))
	    .add(new BankRegister(0, 152, 8, X8664Registers.RSP))
	    .add(new BankRegister(0, 72, 8, X8664Registers.R8))
	    .add(new BankRegister(0, 64, 8, X8664Registers.R9))
	    .add(new BankRegister(0, 56, 8, X8664Registers.R10))
	    .add(new BankRegister(0, 48, 8, X8664Registers.R11))
	    .add(new BankRegister(0, 24, 8, X8664Registers.R12))
	    .add(new BankRegister(0, 16, 8, X8664Registers.R13))
	    .add(new BankRegister(0, 8, 8, X8664Registers.R14))
	    .add(new BankRegister(0, 0, 8, X8664Registers.R15))
	    .add(new BankRegister(0, 128, 8, X8664Registers.RIP))
	    .add(new BankRegister(0, 144, 8, "eflags"))
	    .add(new BankRegister(0, 136, 8, "cs"))
	    .add(new BankRegister(0, 160, 8, "ss"))
	    .add(new BankRegister(0, 184, 8, "ds"))
	    .add(new BankRegister(0, 192, 8, "es"))
	    .add(new BankRegister(0, 200, 8, "fs"))
	    .add(new BankRegister(0, 208, 8, "gs"))
	    .add(new BankRegister(0, 120, 8, "orig_rax"))
	    .add(new BankRegister(0, 168, 8, "fs_base"))
	    .add(new BankRegister(0, 176, 8, "gs_base"))
	    .add(new BankRegister(1, 0, 2, "cwd"))
	    .add(new BankRegister(1, 2, 2, "swd"))
	    .add(new BankRegister(1, 4, 2, "ftw"))
	    .add(new BankRegister(1, 6, 2, "fop"))
	    .add(new BankRegister(1, 8, 8, "fprip"))
	    .add(new BankRegister(1, 16, 8, "rdp"))
	    .add(new BankRegister(1, 24, 4, "mxcsr"))
	    .add(new BankRegister(1, 28, 4, "mxcsr_mask"))
	    .add(new BankRegister(1, 32, 10, "st0"))
	    .add(new BankRegister(1, 48, 10, "st1"))
	    .add(new BankRegister(1, 64, 10, "st2"))
	    .add(new BankRegister(1, 80, 10, "st3"))
	    .add(new BankRegister(1, 96, 10, "st4"))
	    .add(new BankRegister(1, 112, 10, "st5"))
	    .add(new BankRegister(1, 128, 10, "st6"))
	    .add(new BankRegister(1, 144, 10, "st7"))
	    .add(new BankRegister(1, 160, 16, "xmm0"))
	    .add(new BankRegister(1, 176, 16, "xmm1"))
	    .add(new BankRegister(1, 192, 16, "xmm2"))
	    .add(new BankRegister(1, 208, 16, "xmm3"))
	    .add(new BankRegister(1, 224, 16, "xmm4"))
	    .add(new BankRegister(1, 240, 16, "xmm5"))
	    .add(new BankRegister(1, 256, 16, "xmm6"))
	    .add(new BankRegister(1, 272, 16, "xmm7"))
	    .add(new BankRegister(1, 288, 16, "xmm8"))
	    .add(new BankRegister(1, 304, 16, "xmm9"))
	    .add(new BankRegister(1, 320, 16, "xmm10"))
	    .add(new BankRegister(1, 336, 16, "xmm11"))
	    .add(new BankRegister(1, 352, 16, "xmm12"))
	    .add(new BankRegister(1, 368, 16, "xmm13"))
	    .add(new BankRegister(1, 384, 16, "xmm14"))
	    .add(new BankRegister(1, 400, 16, "xmm15"))
	    .add(new BankRegister(2, 848, 8, "d0"))
	    .add(new BankRegister(2, 856, 8, "d1"))
	    .add(new BankRegister(2, 864, 8, "d2"))
	    .add(new BankRegister(2, 872, 8, "d3"))
	    .add(new BankRegister(2, 880, 8, "d4"))
	    .add(new BankRegister(2, 888, 8, "d5"))
	    .add(new BankRegister(2, 896, 8, "d6"))
	    .add(new BankRegister(2, 904, 8, "d7"))
	    ;
    }

    public BankRegister getRegisterByName(String name) {
	return registerMap.get(name);
    }

  public long pc(Task task)
  {
    return getRegisterByName("rip").get(task);
  }

  public void setPC(Task task, long address)
  {
    getRegisterByName("rip").put(task, address);
  }

  public int getWordSize()
  {
    return 8;
  }

  public ByteOrder getByteOrder()
  {
    return ByteOrder.LITTLE_ENDIAN;
  }
  
  /**
   * Get the breakpoint instruction for X8664.
   */
  public final Instruction getBreakpointInstruction()
  {
    return X8664Breakpoint;
  }

  /**
   * Returns the instruction at the given location in the memory
   * buffer, or null if there is no valid instruction at the given
   * location. FIXME - needs to be plugged into the InstructionParser
   * and cache the results.
   */
  public Instruction getInstruction(ByteBuffer bb, long addr)
  {
    bb.position(addr);
    return X8664InstructionParser.parse(bb);
  }

  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In X86-64, the length of breakpoint instruction
   * will be added to the PC register's value. So the true breakpoint address
   * is the PC register's value minus the length of breakpoint. 
   */
  public long getBreakpointAddress(Task task)
  {
    long pcValue = 0;

    pcValue = this.pc(task);
    pcValue = pcValue - 1;
    
    return pcValue;
  }

  /**
   * Returns a non-empty list of addresses that can be used for out of
   * line stepping. Each address should point to a location at least
   * big enough for the largest instruction of this ISA.
   */
  public List getOutOfLineAddresses(Proc proc)
  {
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
   * flag (BS bit 14) in the debug status register (DR6) on x86_64.
   * This resets the stepping flag.
   */
  public boolean isTaskStepped(Task task)
  {
      BankRegister d6 = getRegisterByName("d6");
    long value = d6.get(task);
    boolean stepped = (value & 0x4000) != 0;
    d6.put(task, value & ~0x4000);
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
  public boolean hasExecutedSpuriousTrap(Task task)
  {
    long address = pc(task);
    return (task.getMemory().getByte(address - 1) == (byte) 0x05
            && task.getMemory().getByte(address - 2) == (byte) 0x0f);
  }

  /**
   * Returns true if the given Task is at an instruction that will invoke
   * the sig return system call.
   *
   * On x86_64 this is when the pc is at a 'syscall' instruction and the
   * rax register contains 0x0f.
   */
  public boolean isAtSyscallSigReturn(Task task)
  {
    long address = pc(task);
    boolean result = (task.getMemory().getByte(address) == (byte) 0x0f
		      && task.getMemory().getByte(address + 1) == (byte) 0x05);
    if (result)
      {
	  BankRegister rax = getRegisterByName("rax");
	long syscall_num = rax.get(task);
	result &= syscall_num == 0x0f;
      }
    return result;
  }

  public Syscall[] getSyscallList ()
  {
    return LinuxX8664Syscall.syscallList;
  }

  public HashMap getUnknownSyscalls ()
  {
    return LinuxX8664Syscall.unknownSyscalls;
  }

  public Syscall syscallByName (String name)
  {
    return Syscall.iterateSyscallByName (name, LinuxX8664Syscall.syscallList);
  }

  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
      ByteBuffer[] bankBuffers = new ByteBuffer[] {
	  new RegisterSetByteBuffer(pid, RegisterSet.REGS),
	  new RegisterSetByteBuffer(pid, RegisterSet.FPREGS),
	  new AddressSpaceByteBuffer(pid, AddressSpace.USR)
      };
      for (int i = 0; i < bankBuffers.length; i++) {
	  bankBuffers[i].order(getByteOrder());
      }
      return bankBuffers;
  }

  public int getElfMachineType()
  {
    return ElfEMachine.EM_X86_64;
  }
}
