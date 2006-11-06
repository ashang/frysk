// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
import inua.eio.ByteBuffer;
import frysk.sys.Ptrace;
import frysk.sys.RegisterSetBuffer;

import lib.unwind.RegisterAMD64;

class IsaX8664 implements Isa
{
  static final int FPREGS_OFFSET = 28 * 8;
  static final int DBG_OFFSET = 32 * 8;

  private static final byte[] BREAKPOINT_INSTRUCTION = { (byte)0xcc };
  
  static class EMT64Register extends Register
  {
    EMT64Register(String name, int wordOffset)
    {
      super(0, wordOffset * 8, 8, name);
    }
  }
  static class EMT64SegmentRegister extends EMT64Register
  {
    EMT64SegmentRegister (String name, int wordOffset)
    {
      super (name, wordOffset);
    }
    public int getLength()
    {
      return 4;
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
    };

  static class X8664FPRegister 
    extends Register 
  {
    X8664FPRegister(String name, int regNum) 
    {
      super(1, 32 + regNum * 16, 10, name, fpViews);
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
      super(1, 160 + regNum * 16, 16, name, xmmViews);
    }
  }

  static class FPConfigRegister
    extends Register
  {
    FPConfigRegister(String name, int wordOffset, int length) 
    {
      super(1, wordOffset, length, name);
    }
  }

  private static final EMT64Register[] regDefs
  = { new EMT64Register("rax", 10),
      new EMT64Register("rbx", 5),
      new EMT64Register("rcx", 11),
      new EMT64Register("rdx", 12),
      new EMT64Register("rsi", 13),
      new EMT64Register("rdi", 14),
      new EMT64Register("rbp", 4),
      new EMT64Register("rsp", 19),
      new EMT64Register("r8", 9),
      new EMT64Register("r9", 8),
      new EMT64Register("r10", 7),
      new EMT64Register("r11", 6),
      new EMT64Register("r12", 3),
      new EMT64Register("r13", 2),
      new EMT64Register("r14", 1),
      new EMT64Register("r15", 0),
      new EMT64Register("rip", 16),
      new EMT64Register("eflags", 18),
      new EMT64SegmentRegister("cs", 17),
      new EMT64SegmentRegister("ss", 20),
      new EMT64SegmentRegister("ds", 23),
      new EMT64SegmentRegister("es", 24),
      new EMT64SegmentRegister("fs", 25),
      new EMT64SegmentRegister("gs", 26),
      new EMT64Register("orig_rax", 15),
      new EMT64Register("fs_base", 21),
      new EMT64Register("gs_base", 22) };


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
    
  }

  public Iterator RegisterIterator()
  {
    return registerMap.values().iterator();
  }

  public String getRegisterNameByUnwindRegnum(long regnum)
  {
    return RegisterAMD64.getUnwindRegister(regnum);
  }

  public Register getRegisterByName(String name)
  {
    return (Register)registerMap.get(name);
  }

  public long pc(Task task)
  {
    return getRegisterByName("rip").get(task);
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
   * Get the breakpoint instruction of the specific ISA.
   * 
   * @return bytes[] the instruction of the ISA or null if TRAP is not 
   *         initialized.
   */
  public final byte[] getBreakpointInstruction()
  {
    byte[] instruction = null;
    
    instruction = new byte[IsaX8664.BREAKPOINT_INSTRUCTION.length];
    
    System.arraycopy(IsaX8664.BREAKPOINT_INSTRUCTION, 0, 
                     instruction, 0, IsaX8664.BREAKPOINT_INSTRUCTION.length);
    
    return instruction;
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
    pcValue = pcValue - IsaX8664.BREAKPOINT_INSTRUCTION.length;
    
    return pcValue;
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
    ByteBuffer[] bankBuffers = new ByteBuffer[2];
    int[] bankNames =  { Ptrace.REGS, Ptrace.FPREGS };
    for (int i = 0; i < 2; i++) 
      {
	bankBuffers[i] = new RegisterSetBuffer(bankNames[i], pid);
	bankBuffers[i].order(getByteOrder());
      }
    return bankBuffers;
  }
}
