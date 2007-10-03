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

class IsaEMT64 implements Isa
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

  IsaEMT64()
  {
    for (int i = 0; i < regDefs.length; i++) {
      registerMap.put(regDefs[i].name, regDefs[i]);
    }
  }

  public Iterator RegisterIterator()
  {
    return registerMap.values().iterator();
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
    
    instruction = new byte[IsaEMT64.BREAKPOINT_INSTRUCTION.length];
    
    System.arraycopy(IsaEMT64.BREAKPOINT_INSTRUCTION, 0, 
                     instruction, 0, IsaEMT64.BREAKPOINT_INSTRUCTION.length);
    
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
    pcValue = pcValue - IsaEMT64.BREAKPOINT_INSTRUCTION.length;
    
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
}
