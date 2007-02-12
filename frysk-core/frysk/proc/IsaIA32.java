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
import inua.eio.ByteOrder;
import lib.elf.ElfEMachine;
import lib.unwind.RegisterX86;
import inua.eio.ByteBuffer;
import frysk.sys.Ptrace;
import frysk.sys.PtraceByteBuffer;

import frysk.sys.RegisterSetBuffer;

public class IsaIA32 implements Isa
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
  private static final int DBG_OFFSET = 252;

  private static final byte[] BREAKPOINT_INSTRUCTION = { (byte)0xcc };
  
  private ByteBuffer[] bankBuffers;
  
  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
    bankBuffers = new ByteBuffer[4];
    int[] bankNames =  { Ptrace.REGS, Ptrace.FPREGS, Ptrace.FPXREGS };
    for (int i = 0; i < 3; i++) 
      {
	bankBuffers[i] = new RegisterSetBuffer(bankNames[i], pid);
	bankBuffers[i].order(getByteOrder());
      }

    // Debug registers come from the USR area.
    bankBuffers[3] = new PtraceByteBuffer(pid, PtraceByteBuffer.Area.USR);
    bankBuffers[3].order(getByteOrder());

    return bankBuffers;
  }
  
  static class IA32Register 
    extends Register
  {
    IA32Register(String name, int wordOffset)
    {
      super(0, wordOffset * 4, 4, name);
    }
  }

  static class IA32SegmentRegister 
    extends IA32Register
  {
    IA32SegmentRegister(String name, int wordOffset)
    {
      super(name, wordOffset);
    }
    public int getLength()
    {
      return 2;
    }
  }
  
  // The floating point registers can also be mmx registers. The
  // normal x87 view of the registers is as a long float, because they
  // are always saved to memory in that format.
  static private RegisterView[] fpViews = 
    {
      new RegisterView(80, 80, RegisterView.LONGFLOAT),
      new RegisterView(64, 32, RegisterView.FLOAT),
      new RegisterView(64, 64, RegisterView.INTEGER),
      new RegisterView(64, 32, RegisterView.INTEGER),
      new RegisterView(64, 16, RegisterView.INTEGER),
      new RegisterView(64, 8, RegisterView.INTEGER)
    } ;

  static class I387ConfigRegister
    extends Register
  {
    I387ConfigRegister(String name, int wordOffset) 
    {
      super(1, wordOffset * 4, 4, name);
    }
  }
    
  static class IA32FPRegister 
    extends FPRegister
  {
    IA32FPRegister(String name, int regNum) 
    {
      super(1, 7*4 + regNum * 10, 10, name, fpViews);
    }
    
  }

    static private RegisterView[] xmmViews =
    {
      new RegisterView(128, 64, RegisterView.DOUBLE),
      new RegisterView(128, 32, RegisterView.FLOAT),
      new RegisterView(128, 64, RegisterView.INTEGER),
      new RegisterView(128, 32, RegisterView.INTEGER),
      new RegisterView(128, 16, RegisterView.INTEGER),
      new RegisterView(128, 8, RegisterView.INTEGER)
    };

  static class XMMRegister 
    extends Register 
  {
    XMMRegister(String name, int regNum) 
    {
      super(2, 160 + regNum * 16, 16, name, xmmViews);
    }
  }

  /**
   * Debug registers come from the debug bank (USR area) starting at
   * DBG_OFFSET, are 4 bytes long and are named d0 till d7.
   */
  static class DBGRegister
    extends Register
  {
    DBGRegister(int d)
    {
      super(3, DBG_OFFSET + d * 4, 4, "d" + d);
    }
  }

  private static final IA32Register[] 
  regDefs = { new IA32Register("eax", 6),
	      new IA32Register("ebx", 0),
	      new IA32Register("ecx", 1),
	      new IA32Register("edx", 2),
	      new IA32Register("esi", 3),
	      new IA32Register("edi", 4),
	      new IA32Register("ebp", 5),
	      new IA32SegmentRegister("cs", 13),
	      new IA32SegmentRegister("ds", 7),
	      new IA32SegmentRegister("es", 8),
	      new IA32SegmentRegister("fs", 9),
	      new IA32SegmentRegister("gs", 10),
	      new IA32SegmentRegister("ss", 16),
	      new IA32Register("orig_eax", 11),
	      new IA32Register("eip", 12),
	      new IA32Register("efl", 14),
	      new IA32Register("esp", 15) };

  private LinkedHashMap registerMap = new LinkedHashMap();

  IsaIA32()
  {
    for (int i = 0; i < regDefs.length; i++) 
      {
        registerMap.put(regDefs[i].getName(), regDefs[i]);
      }
    String[] i387CntlNames = {"cwd", "swd", "twd", "fip", "fcs", "foo", "fos"};
    for (int i = 0; i < i387CntlNames.length; i++) 
      registerMap.put(i387CntlNames[i],
		      new I387ConfigRegister(i387CntlNames[i], i));
    for (int i = 0; i < 8; i++) 
      {
        String name = "st" + i;
        registerMap.put(name, new IA32FPRegister(name, i));
      }
    for (int i = 0; i < 8; i++) 
      {
	String name = "xmm" + i;
	registerMap.put(name, new XMMRegister(name, i));
      }
    for (int i = 0; i < 8; i++) 
      {
	Register reg = new DBGRegister(i);
	registerMap.put(reg.getName(), reg);
      }
  }
    
  public Iterator RegisterIterator()
  {
    return registerMap.values().iterator();
  }

    public String getRegisterNameByUnwindRegnum(long regnum)
  {
    return RegisterX86.getUnwindRegister(regnum);
  }

    public Register getRegisterByName(String name)
  {
    return (Register)registerMap.get(name);
  }

  public long pc(Task task)
  {
    return getRegisterByName("eip").get(task);
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
   * Get the breakpoint instruction.
   * 
   * @return bytes[] the instruction of the ISA or null if TRAP is not 
   *         initialized.
   */
  public final byte[] getBreakpointInstruction()
  {
    byte[] instruction = null;
    
    instruction = new byte[IsaIA32.BREAKPOINT_INSTRUCTION.length];
    
    System.arraycopy(IsaIA32.BREAKPOINT_INSTRUCTION, 0, 
                     instruction, 0, IsaIA32.BREAKPOINT_INSTRUCTION.length);
    
    return instruction;
  }
  
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In X86, the length of breakpoint instruction
   * will be added to the PC register's value. So the true breakpoint address
   * is the PC register's value minus the length of breakpoint. 
   */
  public long getBreakpointAddress(Task task)
  {
    long pcValue = 0;
    
    pcValue = this.pc(task);
    pcValue = pcValue - IsaIA32.BREAKPOINT_INSTRUCTION.length;
    
    return pcValue;
  }

  /**
   * Reports whether or not the given Task just did a step of an
   * instruction.  This can be deduced by examining the single step
   * flag (BS bit 14) in the debug status register (DR6) on x86.
   * This resets the stepping flag.
   */
  public boolean isTaskStepped(Task task)
  {
    Register d6 = getRegisterByName("d6");
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
	Register eax = getRegisterByName("eax");
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
