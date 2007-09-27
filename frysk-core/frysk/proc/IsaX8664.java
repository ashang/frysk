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
import java.util.Iterator;
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
import lib.unwind.RegisterAMD64;


public class IsaX8664 implements Isa
{
  /**
   * Offset into user struct from user.h. Determined with:
   *
   * #include <sys/types.h>
   * #include <sys/user.h>
   * #include <stdio.h>
   *
   * #define offsetof(TYPE, MEMBER) ((size_t) &((TYPE *)0)->MEMBER)
   * 
   * int
   * main (int argc, char **argv)
   * {
   *   printf("DBG_OFFSET = %d\n", offsetof(struct user, u_debugreg[0]));
   * }
   */
  private static final int DBG_OFFSET = 848;

  private static final Instruction X8664Breakpoint
    = new Instruction(new byte[] { (byte)0xcc }, false);
  
  static class X8664Register extends BankRegister
  {
    X8664Register(String name, int wordOffset)
    {
      super(0, wordOffset * 8, 8, name);
    }
  }
  static class X8664SegmentRegister extends X8664Register
  {
    X8664SegmentRegister (String name, int wordOffset)
    {
      super (name, wordOffset);
    }
    public int getLength()
    {
      return 4;
    }
  }

  static class X8664FPRegister 
    extends BankRegister 
  {
    X8664FPRegister(String name, int regNum) 
    {
      super(1, 32 + regNum * 16, 10, name);
    }
  }

  static class XMMRegister 
    extends BankRegister 
  {
    XMMRegister(String name, int regNum) 
    {
      super(1, 160 + regNum * 16, 16, name);
    }
  }

  static class FPConfigRegister
    extends BankRegister
  {
    FPConfigRegister(String name, int wordOffset, int length) 
    {
      super(1, wordOffset, length, name);
    }
  }

  /**
   * Debug registers come from the debug bank (USR area) starting at
   * DBG_OFFSET, are 8 bytes long and are named d0 till d7.
   */
  static class DBGRegister
    extends BankRegister
  {
    DBGRegister(int d)
    {
      super(2, DBG_OFFSET + d * 8, 8, "d" + d);
    }
  }

  private static final X8664Register[] regDefs
  = { new X8664Register("rax", 10),
      new X8664Register("rbx", 5),
      new X8664Register("rcx", 11),
      new X8664Register("rdx", 12),
      new X8664Register("rsi", 13),
      new X8664Register("rdi", 14),
      new X8664Register("rbp", 4),
      new X8664Register("rsp", 19),
      new X8664Register("r8", 9),
      new X8664Register("r9", 8),
      new X8664Register("r10", 7),
      new X8664Register("r11", 6),
      new X8664Register("r12", 3),
      new X8664Register("r13", 2),
      new X8664Register("r14", 1),
      new X8664Register("r15", 0),
      new X8664Register("rip", 16),
      new X8664Register("eflags", 18),
      new X8664SegmentRegister("cs", 17),
      new X8664SegmentRegister("ss", 20),
      new X8664SegmentRegister("ds", 23),
      new X8664SegmentRegister("es", 24),
      new X8664SegmentRegister("fs", 25),
      new X8664SegmentRegister("gs", 26),
      new X8664Register("orig_rax", 15),
      new X8664Register("fs_base", 21),
      new X8664Register("gs_base", 22) };


  private LinkedHashMap registerMap = new LinkedHashMap();

  IsaX8664()
  {
    for (int i = 0; i < regDefs.length; i++) {
      registerMap.put(regDefs[i].getName(), regDefs[i]);
    }
    registerMap.put("cwd", new FPConfigRegister("cwd", 0, 2));
    registerMap.put("swd", new FPConfigRegister("swd", 2, 2));
    registerMap.put("ftw", new FPConfigRegister("ftw", 4, 2));
    registerMap.put("fop", new FPConfigRegister("fop", 6, 2));
    registerMap.put("fprip", new FPConfigRegister("fprip", 8, 8));
    registerMap.put("rdp", new FPConfigRegister("rdp", 16, 8));
    registerMap.put("mxcsr", new FPConfigRegister("mxcsr", 24, 4));
    registerMap.put("mxcsr_mask", new FPConfigRegister("mxcsr_mask", 28, 4));
    for (int i = 0; i < 8; i++) 
      {
	String name = "st" + i;
        registerMap.put(name, new X8664FPRegister(name, i));
      }
    for (int i = 0; i < 16; i++) 
      {
	String name = "xmm" + i;
        registerMap.put(name, new XMMRegister(name, i));
      }
    for (int i = 0; i < 8; i++)
      {
	  BankRegister reg = new DBGRegister(i);
	registerMap.put(reg.getName(), reg);
      }
  }

  public Iterator RegisterIterator()
  {
    return registerMap.values().iterator();
  }

  public String getRegisterNameByUnwindRegnum(long regnum)
  {
    return RegisterAMD64.getUnwindRegister(regnum);
  }

  public BankRegister getRegisterByName(String name)
  {
    return (BankRegister)registerMap.get(name);
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
