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

package lib.elf;

import java.util.ArrayList;
import java.math.BigInteger;


/**
 * Java Representation of the the PRSTATUS notes seciont
 * found in core files
 **/
public class ElfPrstatus extends ElfNhdr.ElfNoteSectionEntry
{
  
  private int pr_pid;
  private int pr_ppid;
  private int pr_pgrp;
  private int pr_sid;
  private long pr_sigpend;
  private long pr_sighold;
  private long pr_cursig;
  private ArrayList pr_reg = new ArrayList();
  private long raw_registers[];
  private int reg_length = 0;

  public ElfPrstatus()
  {  
  }
  
  /**
   * Sets the Prstatus pid value
   * 
   * @param pr_pid
   */
  public void setPrPid(int pid)
  {
    this.pr_pid = pid;
  }

  /**
   * Returns the Prstatus pid value
   * 
   * @return pr_pid
   */
  public int getPrPid()
  {
    return this.pr_pid;
  }
  
  /**
   * Sets the Prstatus parent pid value
   * 
   * @param pr_ppid
   */
  public void setPrPpid(int ppid)
  {
    this.pr_ppid = ppid;
  }

  /**
   * Returns the Prstatus parent pid value
   * 
   * @return pr_ppid
   */
  public int getPrPpid()
  {
    return this.pr_ppid;
  }
  
  /**
   * Sets the Prstatus group pid value
   * 
   * @param pr_pgrp
   */
  public void setPrPgrp(int pgrp)
  {
    this.pr_pgrp = pgrp;
  }

  /**
   * Returns the Prstatus group pid value
   * 
   * @return pr_pgrp
   */
  public int getPrPgrp()
  {
    return this.pr_pgrp;
  }
  
  /**
   * Sets the Prstatus session id value
   * 
   * @param pr_sid
   */
  public void setPrSid(int sid)
  {
    this.pr_sid = sid;
  }

  /**
   * Returns the Prstatus session id value
   * 
   * @return pr_sid
   */
  public int getPrSid()
  {
    return this.pr_sid;
  }

  /**
   * Sets the Prstatus signal pending id value
   * 
   * @param pr_sigpend
   */
  public void setPrSigPending(long sigpend)
  {
    this.pr_sigpend = sigpend;
  }

  /**
   * Returns the Prstatus signal pending id value
   * 
   * @return pr_sigpend
   */
  public long getPrSigPending()
  {
    return this.pr_sigpend;
  }

  /**
   * Sets the Prstatus signal hold id value
   * 
   * @param pr_sighold
   */
  public void setPrSigHold(long sighold)
  {
    this.pr_sighold = sighold;
  }

  /**
   * Returns the Prstatus signal hold id value
   * 
   * @return pr_sighold
   */
  public long getPrSigHold()
  {
    return this.pr_sighold;
  }

  /**
   * Sets the Prstatus current signal value
   * 
   * @param pr_cursig
   */
  public void setPrCurSig(long sigcur)
  {
    this.pr_cursig = sigcur;
  }

  /**
   * Returns the Prstatus current signal value
   * 
   * @return pr_cursig
   */
  public long getPrCurSigd()
  {
    return this.pr_cursig;
  }

  /**
   * Sets the GP register at index index to value
   * of BigInteger
   * 
   * @param int index
   * @param BigInteger value
   * 
   */
  public void setPrGPReg(int index, BigInteger value) 
  {
    pr_reg.add(index,value);
  }

  /**
   * Convert the array of BigIntegers to longs 
   *
   */
  protected void convertToLong()
  {
    raw_registers = new long[pr_reg.size()];
    reg_length = raw_registers.length; 
    for (int i=0; i<reg_length; i++)
      {
    	raw_registers[i]=((BigInteger)pr_reg.get(i)).longValue();
      }
  }

  public native long getEntrySize();
  public native long fillMemRegion(byte[] buffer, long startAddress);
}
