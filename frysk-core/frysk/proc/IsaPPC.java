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

import inua.eio.ByteOrder;

class IsaPPC
  implements Isa
{
  static class PPCRegister
    extends Register
  {
    PPCRegister (String name, int wordOffset)
    {
      super (0, wordOffset * 4, 4, name);
    }
  }
  
  private static PPCRegister[] gprs ()
  {
    PPCRegister[] gprs = new PPCRegister[32];
    for (int i = 0; i < gprs.length; i++) 
      {
	gprs[i] = new PPCRegister ("gpr" + i, i);
      }
    return gprs;
  }

  private static final PPCRegister[] gpr = gprs();
 
  private static final PPCRegister nip = new PPCRegister("nip", 32);
  private static final PPCRegister msr = new PPCRegister("msr", 33);
  private static final PPCRegister orig_r3 = new PPCRegister("orig_r3", 34);
  private static final PPCRegister ctr = new PPCRegister("ctr", 35);
  private static final PPCRegister lnk = new PPCRegister("lnk", 36);
  private static final PPCRegister xer = new PPCRegister("xer", 37);
  private static final PPCRegister ccr = new PPCRegister("ccr", 38);
  private static final PPCRegister mq = new PPCRegister("mq", 39);
  private static final PPCRegister trap = new PPCRegister("trap", 40);
  private static final PPCRegister dar = new PPCRegister("dar", 41);
  private static final PPCRegister dsisr = new PPCRegister("dsisr", 42);
  private static final PPCRegister result = new PPCRegister("result", 43);

  private LinkedHashMap registerMap = new LinkedHashMap ();

  IsaPPC ()
  {
    for (int i = 0; i < gpr.length; i++) 
      {
	registerMap.put (gpr[i].name, gpr[i]);
      }

    registerMap.put(nip.name, nip);
    registerMap.put(msr.name, msr);
    registerMap.put(orig_r3.name, orig_r3);
    registerMap.put(ctr.name, ctr);
    registerMap.put(lnk.name, lnk);
    registerMap.put(xer.name, xer);
    registerMap.put(ccr.name, ccr);
    registerMap.put(mq.name, mq);
    registerMap.put(trap.name, trap);
    registerMap.put(dar.name, dar);
    registerMap.put(dsisr.name, dsisr);
    registerMap.put(result.name, result);
  }
    
  public Iterator RegisterIterator ()
  {
    return registerMap.values().iterator();
  }

  public Register getRegisterByName (String name)
  {
    return (Register)registerMap.get(name);
  }

  public long pc (Task task)
  {
    return getRegisterByName("nip").get(task);
  }

  public int getWordSize ()
  {
    return 4;
  }
  
  public ByteOrder getByteOrder ()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  
  /**
   * Not support now.
   * 
   * @return bytes[] the instruction of the ISA.
   */
  public byte[] getBreakpointInstruction()
  {
    throw new RuntimeException("unsupported architecture: " + this);
  }
  
  /**
   * Not support now.
   */
  public long getBreakpointAddress(Task task)
  {
    throw new RuntimeException("unsupported architecture: " + this);
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
}
