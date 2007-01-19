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

public class IsaPPC64
  extends IsaPowerPC
{
  static class PPC64Register 
    extends Register
  {
    PPC64Register(String name, int wordOffset)
    {
      super (0, wordOffset * 8, 8, name);
    }
  }
  static class PPC6432BitRegister
    extends PPC64Register
  {
    PPC6432BitRegister(String name, int wordOffset)
    {
      super (name, wordOffset);
    }
    public int getLength()
    {
      return 4;
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

  private static PPC64Register[] fprs ()
  {
    PPC64Register[] fprs = new PPC64Register[32];
    for (int i = 0; i < fprs.length; i++)
      {
	// Please reference /usr/include/asm/ptrace.h, in which PT_FPR8
	// is 48, and every FP occupies 1 slot.
	fprs[i] = new PPC64Register ("fpr" + i, 48 + i);
      }
    return fprs;
  }

  private static final PPC64Register[] gpr = gprs();
 
  private static final PPC64Register nip = new PPC64Register("nip", 32);
  private static final PPC64Register msr = new PPC64Register("msr", 33);
  private static final PPC64Register orig_r3 = new PPC64Register("orig_r3", 34);
  private static final PPC64Register ctr = new PPC64Register("ctr", 35);
  private static final PPC64Register lnk = new PPC64Register("lnk", 36);
  private static final PPC64Register xer = new PPC6432BitRegister("xer", 37);
  private static final PPC64Register ccr = new PPC6432BitRegister("ccr", 38);
  private static final PPC64Register softe = new PPC64Register("softe", 39);
  private static final PPC64Register trap = new PPC64Register("trap", 40);
  private static final PPC64Register dar = new PPC64Register("dar", 41);
  private static final PPC64Register dsisr = new PPC64Register("dsisr", 42);
  private static final PPC64Register result = new PPC64Register("result", 43);

  private static final PPC64Register[] fpr = fprs();

  IsaPPC64()
  {
    for (int i = 0; i < gpr.length; i++) 
      {
	registerMap.put (gpr[i].getName(), gpr[i]);
      }

    registerMap.put(nip.getName(), nip);
    registerMap.put(msr.getName(), msr);
    registerMap.put(orig_r3.getName(), orig_r3);
    registerMap.put(ctr.getName(), ctr);
    registerMap.put(lnk.getName(), lnk);
    registerMap.put(xer.getName(), xer);
    registerMap.put(ccr.getName(), ccr);
    registerMap.put(softe.getName(), softe);
    registerMap.put(trap.getName(), trap);
    registerMap.put(dar.getName(), dar);
    registerMap.put(dsisr.getName(), dsisr);
    registerMap.put(result.getName(), result);

    for (int i = 0; i < fpr.length; i++) 
      {
	// FP Register Map needed here?
	registerMap.put (fpr[i].getName(), fpr[i]);
      }
  }
  
  public int getWordSize ()
  {
    return 8;
  }
}
