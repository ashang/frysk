// This file is part of the program FRYSK.

package frysk.proc;

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
}
