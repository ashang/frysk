// This file is part of the program FRYSK.
//
// Copyright 2006 IBM Corp.
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
import inua.eio.ByteBuffer;
import frysk.sys.PtraceByteBuffer;

import inua.eio.ByteOrder;

abstract class IsaPowerPC
  implements Isa
{
  protected LinkedHashMap registerMap = new LinkedHashMap();

  // the illegal instruction for powerpc: 0x7d821008.
  // the default order is BIG_ENDIAN
  protected static byte[] BREAKPOINT_INSTRUCTION = { (byte)0x7d, (byte)0x82, 
                                                   (byte)0x10, (byte)0x08 };

  public Iterator RegisterIterator ()
  {
    return registerMap.values().iterator();
  }
  
  public String getRegisterNameByUnwindRegnum(long regnum)
  {
    /* FIXME: needs implementation.  */
    return null;
  }

  public Register getRegisterByName (String name)
  {
    return (Register)registerMap.get(name);
  }

  public long pc (Task task)
  {
    return getRegisterByName("nip").get(task);
  }

  abstract public int getWordSize ();
  
  public ByteOrder getByteOrder ()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  
  /**
   * Get the breakpoint instruction of the PowerPC platform.
   * 
   * @return bytes[] the breakpoint instruction
   */
  public final byte[] getBreakpointInstruction()
  {
    byte[] instruction = null;
    
    instruction = new byte[IsaPowerPC.BREAKPOINT_INSTRUCTION.length];
    
    System.arraycopy(IsaPowerPC.BREAKPOINT_INSTRUCTION, 0, 
                     instruction, 0, IsaPowerPC.BREAKPOINT_INSTRUCTION.length);
    
    return instruction;
  }
  
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In PowerPC, the PC register's value will 
   * remain unchanged. 
   * 
   */
  public final long getBreakpointAddress(Task task)
  {
    long pcValue = 0;

    pcValue = this.pc(task);
    
    return pcValue;
  }

  public Syscall[] getSyscallList ()
  {
    return LinuxPowerPCSyscall.syscallList;
  }

  public HashMap getUnknownSyscalls ()
  {
    return LinuxPowerPCSyscall.unknownSyscalls;
  }

  public Syscall syscallByName (String name)
  {
    Syscall syscall;

    syscall = Syscall.iterateSyscallByName (name, LinuxPowerPCSyscall.syscallList);
    if (syscall != null)
      return syscall;
    
    syscall = Syscall.iterateSyscallByName (name, LinuxPowerPCSyscall.socketSubcallList);
    if (syscall != null)
      return syscall;
    
    syscall = Syscall.iterateSyscallByName (name, LinuxPowerPCSyscall.ipcSubcallList);
    if (syscall != null)
      return syscall;

    return null;
  }

  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
    ByteBuffer[] result = new ByteBuffer[]
      { new PtraceByteBuffer(pid, PtraceByteBuffer.Area.USR) };
    result[0].order(getByteOrder());
    return result;
  }
 }
