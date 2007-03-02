// This file is part of the program FRYSK.
//
// Copyright 2005, IBM Inc.
// Copyright 2007, Red Hat, Inc.
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

import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import lib.elf.ElfEHeader;

public class ElfPrpsinfo extends ElfNhdr.ElfNoteSectionEntry
{
  private char pr_state;
  private char pr_sname;
  private char pr_zomb;
  private char pr_nice;
  
  private long pr_flag;
  
  // on most platform, pr_uid is unsigned int. 
  // In java, "int" it sigend, so we have to use long type.
  private long pr_uid;
  private long pr_gid;
  
  private int pr_pid;
  private int pr_ppid;
  private int pr_pgrp;
  private int pr_sid;
  
  //XXX: the following two value must keep the same with the elfutils package.
  // -1 to allow for null terminator
  public static int ELF_PRPSINFO_FNAME_MAXLEN = 16 - 1;
  public static int ELF_PRPSINFO_ARGS_MAXLEN = 80 - 1;
  
  // filename of executable
  private String  pr_fname;
  
  // initial part of arg list
  private String pr_psargs;
  
  //private int pid;
  
  public ElfPrpsinfo()
  {
    
  }

  /** 
   * 
   * Extract note information from a section
   * containing note data
   *
   */
  public ElfPrpsinfo(ElfData noteData)
  {
    ByteOrder order = null;
    byte rawNoteData[] = getNoteData(noteData);

    if (rawNoteData.length <=0)
      return;

    ByteBuffer noteBuffer = new ArrayByteBuffer(rawNoteData);

    ElfEHeader header = noteData.getParent().getEHeader();
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
   
    pr_state = (char) noteBuffer.getByte();
    pr_sname = (char) noteBuffer.getByte();
    pr_zomb = (char) noteBuffer.getByte();
    pr_nice = (char) noteBuffer.getByte();
    pr_flag = noteBuffer.getWord();
    pr_uid = noteBuffer.getShort();
    pr_gid = noteBuffer.getShort();
    pr_pid = noteBuffer.getInt();
    pr_ppid = noteBuffer.getInt();
    pr_pgrp = noteBuffer.getInt();
    pr_sid = noteBuffer.getInt();

    StringBuffer noteStringBuffer = new StringBuffer();
    noteBuffer.get(noteBuffer.position(), 16, noteStringBuffer);
    noteBuffer.position(noteBuffer.position()+16);
    pr_fname = noteStringBuffer.toString();
    noteBuffer.get(noteBuffer.position(), 80, noteStringBuffer);
    pr_psargs = noteStringBuffer.toString();

  }

  public void setPrState(char state)
  {
    this.pr_state = state;
  }
  
  public char getPrState()
  {
    return this.pr_state;
  }
  
  public void setPrSname(char sname)
  {
    this.pr_sname = sname;
  }
  
  public char getPrSname()
  {
    return this.pr_sname;
  }
  
  public void setPrZomb(char zomb)
  {
    this.pr_zomb = zomb;
  }
  
  public char getPrZomb()
  {
    return this.pr_zomb;
  }
  
  public void setPrNice(char nice)
  {
    this.pr_nice = nice;
  }
  
  public char getPrNice()
  {
    return this.pr_nice;
  }
  
  public void setPrFlag(long flag)
  {
    this.pr_flag = flag;
  }
  
  public long getPrFlag()
  {
    return this.pr_flag;
  }
  
  public void setPrUid(long uid)
  {
    this.pr_uid = uid;
  }
  
  public long getPrUid()
  {
    return this.pr_uid;
  }
  
  public void setPrGid(long gid)
  {
    this.pr_gid = gid;
  }
  
  public long getPrGid()
  {
    return this.pr_gid;
  }
  
  public void setPrPid(int pid)
  { 
    this.pr_pid = pid;
  }
  public int getPrPid()
  {
    return this.pr_pid;
  }
  
  public void setPrPpid(int ppid)
  {
    this.pr_ppid = ppid;
  }

  public int getPrPpid()
  {
    return this.pr_ppid;
  }
  
  public void setPrPgrp(int pgrp)
  {
    this.pr_pgrp = pgrp;
  }
  
  public int getPrPgrp()
  {
    return this.pr_pgrp;
  }
  
  public void setPrSid(int sid)
  {
    this.pr_sid = sid;
  }

  public int getPrSid()
  {
    return this.pr_sid;
  }
  
  public void setPrFname(String fname)
  {
    if (fname == null)
      return;
    
    int length = fname.length();
    
    if (length <ELF_PRPSINFO_FNAME_MAXLEN)
      this.pr_fname = fname.substring(0,length);
    else
      this.pr_fname = fname.substring(0,ELF_PRPSINFO_FNAME_MAXLEN);
  }

  public String getPrFname()
  {
    return this.pr_fname;
  }

  public void setPrPsargs(String args)
  {
    if (args == null)
      return;

    int length = args.length();
    if (length < ELF_PRPSINFO_ARGS_MAXLEN)
      this.pr_psargs = args.substring(0,length);
    else
      this.pr_psargs = args.substring(0, ELF_PRPSINFO_ARGS_MAXLEN);
  }

  public String getPrPsargs()
  {
    return this.pr_psargs;
  }
 
 
  public native byte[] getNoteData(ElfData data);
  public native long getEntrySize();
  public native long fillMemRegion(byte[] buffer, long startAddress);
}
