// This file is part of the program FRYSK.

package frysk.proc;

import java.util.Iterator;
import java.util.LinkedHashMap;

import inua.eio.ByteOrder;

class IsaPPC64
  implements Isa
{
  static class PPC64Register 
    extends Register
  {
    PPC64Register(String name, int wordOffset)
    {
      super (0, wordOffset * 8, 8, name);
    }
  }
  
  private static PPC64Register[] gprs()
  {
    PPC64Register[] gprs = new PPC64Register[32];
    for (int i = 0; i < gprs.length; i++) 
      {
	gprs[i] = new PPC64Register ("gpr" + i, i);
      }
    return gprs;
  }

  private static final PPC64Register[] gpr = gprs();
 
  private static final PPC64Register nip = new PPC64Register("nip", 32);
  private static final PPC64Register msr = new PPC64Register("msr", 33);
  private static final PPC64Register orig_r3 = new PPC64Register("orig_r3", 34);
  private static final PPC64Register ctr = new PPC64Register("ctr", 35);
  private static final PPC64Register lnk = new PPC64Register("lnk", 36);
  private static final PPC64Register xer = new PPC64Register("xer", 37);
  private static final PPC64Register ccr = new PPC64Register("ccr", 38);
  private static final PPC64Register mq = new PPC64Register("mq", 39);
  private static final PPC64Register trap = new PPC64Register("trap", 40);
  private static final PPC64Register dar = new PPC64Register("dar", 41);
  private static final PPC64Register dsisr = new PPC64Register("dsisr", 42);
  private static final PPC64Register result = new PPC64Register("result", 43);

  private LinkedHashMap registerMap = new LinkedHashMap();

  // the illegal instruction for ppc64: 0x7d821008.
  // the default order is BIG_ENDIAN
  private static byte[] BREAKPOINT_INSTRUCTION = { (byte)0x7d, (byte)0x82, 
                                                   (byte)0x10, (byte)0x08 };
  
  IsaPPC64()
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
   * Get the breakpoint instruction of the PPC64 platform.
   * 
   * @return bytes[] the breakpoint instruction
   */
  public final byte[] getBreakpointInstruction()
  {
    byte[] instruction = null;
    
    if (null == IsaPPC64.BREAKPOINT_INSTRUCTION)
      return null;
    
    instruction = new byte[IsaPPC64.BREAKPOINT_INSTRUCTION.length];
    
    System.arraycopy(IsaPPC64.BREAKPOINT_INSTRUCTION, 0, 
                     instruction, 0, IsaPPC64.BREAKPOINT_INSTRUCTION.length);
    
    return instruction;
  }
  
  /**
   * Get the true breakpoint address according to PC register after hitting 
   * one breakpoint set in task. In PPC64, the PC register's value will 
   * remain unchanged. 
   * 
   */
  public final long getBreakpointAddress(Task task)
  {
    long pcValue = 0;
        
    if (null == task)
      return pcValue;

    pcValue = this.pc(task);
    
    return pcValue;
  }
}
