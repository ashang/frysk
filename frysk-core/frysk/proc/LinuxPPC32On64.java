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

import java.math.BigInteger;
import inua.eio.ByteBuffer;

class LinuxPPC32On64
  extends LinuxPPC
{
  private static LinuxPPC32On64 isa;

  static LinuxPPC isaSingleton ()
  {
    if (isa == null)
      isa = new LinuxPPC32On64 ();
    return isa;
  }
  // The Isa object used to actually access registers in the target.
  private final IsaPPC64 isa64 = new IsaPPC64();

  /**
   * Get the buffers used to access registers in the different
   * banks.
   *
   * @return the <code>ByteBuffer</code>s used to access registers.
   */
  public ByteBuffer[] getRegisterBankBuffers(int pid) 
  {
    return isa64.getRegisterBankBuffers(pid);
  }
  
  // Map ppc32 registers to the ppc64 registers returned by ptrace on ppc64.
  private class IndirectRegister
    extends BankRegister 
  {
    String name;
      BankRegister ppc32Reg;
      BankRegister ppc64Reg;
    // Masks for cutting the 64 bit values down to 32. XXX Is this needed?
    long longMask;
    BigInteger bigIntMask;
    
    IndirectRegister(String name) 
    {
      super(0, 0, 0, name); // Dummy values, mostly
      this.name = name;

      ppc32Reg = getRegisterByNameSuper(name);
      ppc64Reg = isa64.getRegisterByName(name);

      if (getLength() != ppc64Reg.getLength()) 
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
      return ppc32Reg.getLength();
    }
    
    public long get(Task task) 
    {
      long rawVal = ppc64Reg.get(task);
      
      if (longMask != 0)
	return longMask & rawVal;
      else
	return rawVal;
    }
    
    public BigInteger getBigInteger(Task task) 
    {
      BigInteger rawVal = ppc64Reg.getBigInteger(task);

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
      ppc64Reg.put(task, realVal);
    }
    
    public void putBigInteger(Task task, BigInteger val) 
    {
      BigInteger realVal;
      
      if (bigIntMask != null) 
	realVal = bigIntMask.and(val);
      else
	realVal = val;
      ppc64Reg.putBigInteger(task, realVal);
    }
  }
  
    private BankRegisterMap registerMap = new BankRegisterMap();  
  
    /**
     * Default constructor
     */
    public LinuxPPC32On64() {
	for (int i = 0; i < 32; i++) {
	    BankRegister gpr = new IndirectRegister ("gpr" + i);
	    registerMap.add(gpr);
	}
	registerMap
	    .add (new IndirectRegister("nip"))
	    .add (new IndirectRegister("msr"))
	    .add (new IndirectRegister("orig_r3"))
	    .add (new IndirectRegister("ctr"))
	    .add (new IndirectRegister("lnk"))
	    .add (new IndirectRegister("xer"))
	    .add (new IndirectRegister("ccr"))
	    // No such register on ppc64.
	    // .add (new IndirectRegister("mq"))
	    .add (new IndirectRegister("trap"))
	    .add (new IndirectRegister("dar"))
	    .add (new IndirectRegister("dsisr"))
	    .add (new IndirectRegister("result"))
	    ;
	for (int i = 0; i < 32; i++) {
	    BankRegister fpr = new IndirectRegister ("fpr" + i);
	    registerMap.add(fpr);
	}
    }

    public BankRegister getRegisterByName(String name) {
	return registerMap.get(name);
    }
  private BankRegister getRegisterByNameSuper(String name)
  {
    return super.getRegisterByName(name);
  }
}
