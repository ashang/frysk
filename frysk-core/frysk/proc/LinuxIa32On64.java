// This file is part of the program FRYSK.
//
// Copyright 2006 Red Hat Inc.
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.math.BigInteger;
import inua.eio.ByteBuffer;

/**
 * Class for IA32 processes running on 64 bit machines.
 *
 * For the purposes of ptrace, Linux treats a 32 bit process like a 64
 * bit process, with the same user area and register layout that a 32
 * bit process would have; the 32 bit IA32 registers are stored in the
 * corresponding slots in the 64 bit structures. In order to debug a
 * 32 bit process we use a LinuxIa32 class with new overriding methods
 * that access registers,  but otherwise things "just work;" for
 * example, reading and writing memory is fine because the process has
 * the 32 bit memory map, memory will be read / written in 32 bit
 * chunks because the Isa work length is 32 bits, but the
 * PtraceByteBuffer class knows to access memory in 64 bit chunks.
 */
class LinuxIa32On64
extends LinuxIa32
{
  private static LinuxIa32On64 isa;

  /**
   * Returns the Isa singleton object. Note that the return type is
   * not LinuxIa32On64 because that would cause a conflict with the
   * isaSingleton method in the superclass.
   *
   * @return the Isa singleton object.
   */
  static LinuxIa32 isaSingleton()
  {
    if (isa == null)
      isa = new LinuxIa32On64();
    return isa;
  }
  // The Isa object used to actually access registers in the target.
  private final IsaX8664 isa64 = new IsaX8664();

  /**
   * Get the buffers used to access registers in the different
   * banks. This Isa has just one register bank -- the USR area
   * of the x8664 -- even though Ia32 has 3.
   *
   * @return the <code>ByteBuffer</code>s used to access registers.
   */
  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
    return isa64.getRegisterBankBuffers(pid);
  }
  
  // Map i386 registers to the x86_64 registers returned by ptrace on x86_64.
  private class IndirectRegister
    extends Register 
  {
    String ia32Name;
    String x8664Name;
    Register ia32Reg;
    Register x8664Reg;
    // Masks for cutting the 64 bit values down to 32. XXX Is this needed?
    long longMask;
    BigInteger bigIntMask;
    
    IndirectRegister(String ia32Name, String x8664Name) 
    {
      super(0, 0, 0, ia32Name);	// Dummy values, mostly
      this.ia32Name = ia32Name;
      this.x8664Name = x8664Name;
      ia32Reg = getRegisterByNameSuper(ia32Name);
      x8664Reg = isa64.getRegisterByName(x8664Name);

      if (getLength() != x8664Reg.getLength()) 
	{
	  longMask = (1L << (getLength() * 8)) - 1L;
	  bigIntMask = BigInteger.ONE.shiftLeft(getLength() * 8)
	    .subtract(BigInteger.ONE); 
	}
      else 
	{
	  longMask = 0;
	  bigIntMask = null;
	}
    }
    
    public int getLength()
    {
      return ia32Reg.getLength();
    }
    

    public RegisterView[] getViews() 
    {
      return ia32Reg.getViews();
    }
    

    public long get(Task task) 
    {
      long rawVal = x8664Reg.get(task);
      
      if (longMask != 0)
	return longMask & rawVal;
      else
	return rawVal;
    }
    
    public BigInteger getBigInteger(Task task) 
    {
      BigInteger rawVal = x8664Reg.getBigInteger(task);

      if (bigIntMask != null)
	return bigIntMask.and(rawVal);
      else
	return rawVal;
    }
    
    public void put(Task task, long val) 
    {
      long realVal;
      
      if (longMask != 0)
	realVal = longMask & val;
      else
	realVal = val;
      x8664Reg.put(task, realVal);
    }
    
    public void putBigInteger(Task task, BigInteger val) 
    {
      BigInteger realVal;
      
      if (bigIntMask != null) 
	realVal = bigIntMask.and(val);
      else
	realVal = val;
      x8664Reg.putBigInteger(task, realVal);
    }
  }

  private class ConstantRegister extends Register 
  {
    String ia32Name;
    final long value;
    final BigInteger bigValue;
    Register ia32Reg;
    
    ConstantRegister(String name, long value) 
    {    
      super(0, 0, 0, name);	// Dummy values, mostly
      ia32Name = name;
      this.value = value;
      bigValue = BigInteger.valueOf(value);
      ia32Reg = getRegisterByNameSuper(ia32Name);
    }

    public int getLength()
    {
      return ia32Reg.getLength();
    }
    

    public RegisterView[] getViews() 
    {
      return ia32Reg.getViews();
    }
    

    public long get(Task task) 
    {
      return value;
    }
    
    public BigInteger getBigInteger(Task task) 
    {
      return bigValue;
    }
    
    public void put(Task task, long val) 
    {
    }
    
    public void putBigInteger(Task task, BigInteger val) 
    {
    }
  }
  
  private LinkedHashMap registerMap = new LinkedHashMap();  
  
  /**
   * Default constructor
   */
  public LinuxIa32On64() 
  {
    super();
    // TODO: floating point and debug registers
    final Register[] regDefs = new Register[] 
      { new IndirectRegister("eax", "rax"),
	new IndirectRegister("ebx", "rbx"),
	new IndirectRegister("ecx", "rcx"),
	new IndirectRegister("edx", "rdx"),
	new IndirectRegister("esi", "rsi"),
	new IndirectRegister("edi", "rdi"),
	new IndirectRegister("ebp", "rbp"),
	new IndirectRegister("cs", "cs"),
	new IndirectRegister("ds", "ds"),
	new IndirectRegister("es", "es"),
	new IndirectRegister("fs", "fs"),
	new IndirectRegister("gs", "gs"),
	new IndirectRegister("ss", "gs"),
	new IndirectRegister("orig_eax", "orig_rax"),
	new IndirectRegister("eip", "rip"),
	new IndirectRegister("efl","eflags"),
	new IndirectRegister("esp", "rsp"),
	new IndirectRegister("cwd", "cwd"),
	new IndirectRegister("swd", "swd"),
	new IndirectRegister("twd", "ftw"),
	new IndirectRegister("fip", "fprip"),
	new ConstantRegister("fcs", 0),
	new IndirectRegister("foo", "rdp"),
	new ConstantRegister("fos", 0)
      };
    for (int i = 0; i < regDefs.length; i++)
      registerMap.put(regDefs[i].getName(), regDefs[i]);
    for (int i = 0; i < 8; i++) 
      {
	String fpName = "st" + i;
	registerMap.put(fpName, new IndirectRegister(fpName, fpName));
      }
    for (int i = 0; i < 8; i++) 
      {
	String fpName = "xmm" + i;
	registerMap.put(fpName, new IndirectRegister(fpName, fpName));
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

  private Register getRegisterByNameSuper(String name)
  {
    return super.getRegisterByName(name);
  }

}

  
