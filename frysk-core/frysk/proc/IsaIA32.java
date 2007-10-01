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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import inua.eio.ByteOrder;
import inua.eio.ByteBuffer;
import frysk.sys.Ptrace.RegisterSet;
import frysk.sys.Ptrace.AddressSpace;
import frysk.proc.live.RegisterSetByteBuffer;
import frysk.proc.live.AddressSpaceByteBuffer;
import lib.dwfl.ElfEMachine;
import frysk.isa.IA32Registers;

public class IsaIA32 implements Isa
{
  private static final Instruction IA32Breakpoint
    = new Instruction(new byte[] { (byte)0xcc }, false);
  
  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
      ByteBuffer[] bankBuffers = new ByteBuffer[] {
	  new RegisterSetByteBuffer(pid, RegisterSet.REGS),
	  new RegisterSetByteBuffer(pid, RegisterSet.FPREGS),
	  new RegisterSetByteBuffer(pid, RegisterSet.FPXREGS),
	  new AddressSpaceByteBuffer(pid, AddressSpace.USR)
    };
    for (int i = 0; i < bankBuffers.length; i++) {
	  bankBuffers[i].order(getByteOrder());
    }
    return bankBuffers;
  }
  
    private final LinkedHashMap registerMap = new LinkedHashMap();
    private void add(BankRegister bankRegister) {
	registerMap.put(bankRegister.getName(), bankRegister);
    }

    IsaIA32() {
	add(new BankRegister (0, 24, 4,IA32Registers.EAX));
	add(new BankRegister (0, 0, 4, IA32Registers.EBX));
	add(new BankRegister (0, 4, 4, IA32Registers.ECX));
	add(new BankRegister (0, 8, 4, IA32Registers.EDX));
	add(new BankRegister (0, 12, 4, IA32Registers.ESI));
	add(new BankRegister (0, 16, 4, IA32Registers.EDI));
	add(new BankRegister (0, 20, 4, IA32Registers.EBP));
	add(new BankRegister (0, 52, 4, IA32Registers.CS));
	add(new BankRegister (0, 28, 4, IA32Registers.DS));
	add(new BankRegister (0, 32, 4, IA32Registers.ES));
	add(new BankRegister (0, 36, 4, IA32Registers.FS));
	add(new BankRegister (0, 40, 4, IA32Registers.GS));
	add(new BankRegister (0, 64, 4, IA32Registers.SS));
	add(new BankRegister (0, 44, 4, "orig_eax"));
	add(new BankRegister (0, 48, 4, IA32Registers.EIP));
	add(new BankRegister (0, 56, 4, IA32Registers.EFLAGS));
	add(new BankRegister (0, 60, 4, IA32Registers.ESP));
	add(new BankRegister (1, 0, 4, "cwd"));
	add(new BankRegister (1, 4, 4, "swd"));
	add(new BankRegister (1, 8, 4, "twd"));
	add(new BankRegister (1, 12, 4, IA32Registers.FIP));
	add(new BankRegister (1, 16, 4, IA32Registers.FCS));
	add(new BankRegister (1, 20, 4, "foo"));
	add(new BankRegister (1, 24, 4, "fos"));
	add(new BankRegister (1, 28, 10, IA32Registers.ST0));
	add(new BankRegister (1, 38, 10, IA32Registers.ST1));
	add(new BankRegister (1, 48, 10, IA32Registers.ST2));
	add(new BankRegister (1, 58, 10, IA32Registers.ST3));
	add(new BankRegister (1, 68, 10, IA32Registers.ST4));
	add(new BankRegister (1, 78, 10, IA32Registers.ST5));
	add(new BankRegister (1, 88, 10, IA32Registers.ST6));
	add(new BankRegister (1, 98, 10, IA32Registers.ST7));
	add(new BankRegister (2, 160, 16, IA32Registers.XMM0));
	add(new BankRegister (2, 176, 16, IA32Registers.XMM1));
	add(new BankRegister (2, 192, 16, IA32Registers.XMM2));
	add(new BankRegister (2, 208, 16, IA32Registers.XMM3));
	add(new BankRegister (2, 224, 16, IA32Registers.XMM4));
	add(new BankRegister (2, 240, 16, IA32Registers.XMM5));
	add(new BankRegister (2, 256, 16, IA32Registers.XMM6));
	add(new BankRegister (2, 272, 16, IA32Registers.XMM7));
	add(new BankRegister (3, 252, 4, "d0"));
	add(new BankRegister (3, 256, 4, "d1"));
	add(new BankRegister (3, 260, 4, "d2"));
	add(new BankRegister (3, 264, 4, "d3"));
	add(new BankRegister (3, 268, 4, "d4"));
	add(new BankRegister (3, 272, 4, "d5"));
	add(new BankRegister (3, 276, 4, "d6"));
	add(new BankRegister (3, 280, 4, "d7"));
    }
    
    public BankRegister getRegisterByName(String name)
  {
    return (BankRegister)registerMap.get(name);
  }

  public long pc(Task task)
  {
    return getRegisterByName("eip").get(task);
  }

  public void setPC(Task task, long address)
  {
    getRegisterByName("eip").put(task, address);
  }

  public int getWordSize()
  {
    return 4;
  }

  public ByteOrder getByteOrder()
  {
    return ByteOrder.LITTLE_ENDIAN;
  }
  
  /**
   * Get the breakpoint instruction for IA32.
   */
  public final Instruction getBreakpointInstruction()
  {
    return IA32Breakpoint;
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
    return IA32InstructionParser.parse(bb);
  }
  
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In X86, the length of breakpoint instruction
   * will be added to the PC register's value. So the true breakpoint address
   * is the PC register's value minus the length of breakpoint. 
   */
  public long getBreakpointAddress(Task task)
  {
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
   * flag (BS bit 14) in the debug status register (DR6) on x86.
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
   * <p>
   * ia32 generate spurious trap events on "int 0x80" instructions
   * that should trap into a kernel syscall.
   */
  public boolean hasExecutedSpuriousTrap(Task task)
  {
    long address = pc(task);
    return (task.getMemory().getByte(address - 1) == (byte) 0x80
	    && task.getMemory().getByte(address - 2) == (byte) 0xcd);
  }

  /**
   * Returns true if the given Task is at an instruction that will invoke
   * the sig return system call.
   *
   * On x86 this is when the pc is at a int 0x80 instruction and the
   * eax register contains 0x77.
   */
  public boolean isAtSyscallSigReturn(Task task)
  {
    long address = pc(task);
    boolean result = (task.getMemory().getByte(address) == (byte) 0xcd
		      && task.getMemory().getByte(address + 1) == (byte) 0x80);
    if (result)
      {
	BankRegister eax = getRegisterByName("eax");
	long syscall_num = eax.get(task);
	result &= syscall_num == 0x77;
      }
    return result;
  }

  public Syscall[] getSyscallList ()
  {
    return LinuxIa32Syscall.syscallList;
  }

  public HashMap getUnknownSyscalls ()
  {
    return LinuxIa32Syscall.unknownSyscalls;
  }

  public Syscall syscallByName (String name)
  {
    Syscall syscall;

    syscall = Syscall.iterateSyscallByName (name, LinuxIa32Syscall.syscallList);
    if (syscall != null)
      return syscall;
    
    syscall = Syscall.iterateSyscallByName (name, LinuxIa32Syscall.socketSubcallList);
    if (syscall != null)
      return syscall;
    
    syscall = Syscall.iterateSyscallByName (name, LinuxIa32Syscall.ipcSubcallList);
    if (syscall != null)
      return syscall;

    return null;
  }

  public int getElfMachineType()
  {
    return ElfEMachine.EM_386;
  }
}
