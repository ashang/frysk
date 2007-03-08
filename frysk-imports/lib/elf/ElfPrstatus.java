// This file is part of the program FRYSK.
//
// Copyright 2006,2007, Red Hat Inc.
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
import java.util.Iterator;
import java.math.BigInteger;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;



/**
 * Java Representation of the the PRSTATUS notes secion
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
  private long pr_info_si_signo;
  private long pr_info_si_code;
  private long pr_info_si_errno;

  private long pr_utime_sec;
  private long pr_utime_usec;
  private long pr_stime_sec;
  private long pr_stime_usec;
  private long pr_cutime_sec;
  private long pr_cutime_usec;
  private long pr_cstime_sec;
  private long pr_cstime_usec;

  private int pr_fpvalid;

  private ArrayList pr_reg = new ArrayList();
  private long raw_registers[];
  private byte raw_core_registers[];
  private int reg_length = 0;


  static ArrayList internalThreads = new ArrayList();

  public ElfPrstatus()
  {  
  }

  public ElfPrstatus(byte[] singleNoteData, Elf elf)
  {

    ByteOrder order = null;
    if (singleNoteData.length <=0)
      return;
    ByteBuffer noteBuffer = new ArrayByteBuffer(singleNoteData);

    ElfEHeader header = elf.getEHeader();
    switch (header.ident[5])
      {
      case ElfEHeader.PHEADER_ELFDATA2LSB: 
	order = ByteOrder.LITTLE_ENDIAN;
	break;
      case ElfEHeader.PHEADER_ELFDATA2MSB:
	order = ByteOrder.BIG_ENDIAN;
	break;
      default:
	return;
      }

    noteBuffer.order(order);
    
    switch (header.machine)
      {
      case ElfEMachine.EM_386:
      case ElfEMachine.EM_PPC:
	noteBuffer.wordSize(4);
	break;
      case ElfEMachine.EM_X86_64:
      case ElfEMachine.EM_PPC64:
	noteBuffer.wordSize(8);
	break;
      default:
	return;
      }
    //byte[] data = new byte[singleNoteData.length];
    //noteBuffer.get(data);

    pr_info_si_signo =  noteBuffer.getInt();
    pr_info_si_code =  noteBuffer.getInt();
    pr_info_si_errno = noteBuffer.getInt();
    pr_cursig = noteBuffer.getInt();
    pr_sigpend = noteBuffer.getUInt();
    pr_sighold = noteBuffer.getUInt();
    pr_pid =  noteBuffer.getInt();
    pr_ppid =  noteBuffer.getInt();
    pr_pgrp =  noteBuffer.getInt();
    pr_sid =  noteBuffer.getInt();
    pr_utime_sec =  noteBuffer.getInt();
    pr_utime_usec =  noteBuffer.getInt();
    pr_stime_sec =  noteBuffer.getInt();
    pr_stime_usec =  noteBuffer.getInt();
    pr_cutime_sec =  noteBuffer.getInt();
    pr_cutime_usec =  noteBuffer.getInt();
    pr_cstime_sec =  noteBuffer.getInt();
    pr_cstime_usec =  noteBuffer.getInt();
    
    raw_core_registers = new byte[(int) ((singleNoteData.length) - noteBuffer.position())];
    noteBuffer.get(raw_core_registers,0, (int) ((singleNoteData.length) - noteBuffer.position()));



  }

  public static ElfPrstatus[] decode(ElfData noteData)
  {
    getNoteData(noteData);
    ElfPrstatus threadList[] = new  ElfPrstatus[internalThreads.size()];

    int count = 0;
    Iterator i = internalThreads.iterator();
    while (i.hasNext())
      {
	byte b[]  = (byte[]) i.next();
	threadList[count] = new ElfPrstatus(b,noteData.getParent());
      }

    return threadList;
  }


  /** 
   * Returns the raw byte[] data 
   * representing the register data.
   */
  public byte[] getRawCoreRegisters()
  {
    return raw_core_registers;
  }

  public ArrayList getThreadData()
  {
    return internalThreads;
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
  public long getPrCurSig()
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

  public Iterator getPrGPRegIterator()
  {
    return pr_reg.iterator();
  }

  public void setPrInfoSiSigno(long pr_info_si_signo)
  {
    this.pr_info_si_signo = pr_info_si_signo;
  }

  public long getPrInfoSiSigno()
  {
    return this.pr_info_si_signo;
  }

  public void setPrInfoSiCode(long pr_info_si_code)
  {
    this.pr_info_si_code = pr_info_si_code;
  }

  public long getPrInfoSiCode()
  {
    return this.pr_info_si_code;
  }

  public void setPrInfoSiErrno(long pr_info_si_errno)
  {
    this.pr_info_si_errno = pr_info_si_errno;
  }

  public long getPrInfoSiErrno()
  {
    return this.pr_info_si_errno;
  }

  public void setPrFPValid(int pr_fpvalid)
  {
    this.pr_fpvalid = pr_fpvalid;
  }

  public long getPrFPValid()
  {
    return this.pr_fpvalid;
  }

  /**
   * Get the <code>Pr_Utime_Usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrUtimeUsec()
  {
    return pr_utime_usec;
  }

  /**
   * Set the <code>Pr_Utime_Usec</code> value.
   *
   * @param newPr_Utime_Usec The new Pr_Utime_Usec value.
   */
  public final void setPrUtimeUsec(final long newPrUtimeUsec) 
  {
    this.pr_utime_usec = newPrUtimeUsec;
  }
 
  /**
   * Get the <code>Pr_Utime_Usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrUtimeSec()
  {
    return pr_utime_sec;
  }

  /**
   * Set the <code>Pr_Utime_Usec</code> value.
   *
   * @param newPr_Utime_Usec The new Pr_Utime_Usec value.
   */
  public final void setPrUtimeSec(final long newPrUtimeSec) 
  {
    this.pr_utime_sec = newPrUtimeSec;
  }


  /**
   * Get the <code>pr_cutime_usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrCUtimeUsec()
  {
    return pr_cutime_usec;
  }

  /**
   * Set the <code>pr_cutime_usec</code> value.
   *
   * @param newPrCUtimeUsec The new PrCUtimeUsec value.
   */
  public final void setPrCUtimeUsec(final long newPrCUtimeUsec) 
  {
    this.pr_cutime_usec = newPrCUtimeUsec;
  }
 
  /**
   * Get the <code>pr_cutime_sec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrCUtimeSec()
  {
    return pr_cutime_sec;
  }

  /**
   * Set the <code>pr_cutime_sec</code> value.
   *
   * @param newPrUtimeUsec The new Pr_Utime_Usec value.
   */
  public final void setPrCUtimeSec(final long newPrCUtimeSec) 
  {
    this.pr_cutime_sec = newPrCUtimeSec;
  }

  /**
   * Get the <code>Pr_Stime_Usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrStimeUsec()
  {
    return pr_stime_usec;
  }

  /**
   * Set the <code>Pr_Stime_Usec</code> value.
   *
   * @param newPrStimeUsec The new Pr_Utime_Usec value.
   */
  public final void setPrStimeUsec(final long newPrStimeUsec) 
  {
    this.pr_stime_usec = newPrStimeUsec;
  }
 
  /**
   * Get the <code>Pr_Stime_Usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrStimeSec()
  {
    return pr_stime_sec;
  }

  /**
   * Set the <code>Pr_Utime_Usec</code> value.
   *
   * @param newPr_Utime_Usec The new Pr_Utime_Usec value.
   */
  public final void setPrStimeSec(final long newPrStimeSec) 
  {
    this.pr_stime_sec = newPrStimeSec;
  }

  /**
   * Get the <code>pr_sctime_usec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrCStimeUsec()
  {
    return pr_cstime_usec;
  }


  /**
   * Set the <code>pr_cstime_usec</code> value.
   *
   * @param newPrCStimeUsec The new Pr_CStime_Usec value.
   */
  public final void setPrCStimeUsec(final long newPrCStimeUsec) 
  {
    this.pr_cstime_usec = newPrCStimeUsec;
  }
 
  /**
   * Get the <code>>pr_cstime_sec</code> value.
   *
   * @return a <code>long</code> value
   */
  public final long getPrCStimeSec()
  {
    return pr_cstime_sec;
  }

  /**
   * Set the <code>pr_cstime_secc</code> value.
   *
   * @param newPr_CStime_sec The new Pr_CStime_sec value.
   */
  public final void setPrCStimeSec(final long newPrCStimeSec) 
  {
    this.pr_cstime_sec = newPrCStimeSec;
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

  public native static long getNoteData(ElfData data);
  public native long getEntrySize();
  public native long fillMemRegion(byte[] buffer, long startAddress);
}
