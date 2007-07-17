// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package lib.dwfl;

import java.util.ArrayList;
import java.util.Iterator;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

/**
 * Java Representation of the the Floating point notes secion
 * found in core files
 **/

public class ElfPrFPRegSet extends ElfNhdr.ElfNoteSectionEntry
{
  

  byte[] raw_registers;
  static ArrayList internalThreads = new ArrayList();

  public ElfPrFPRegSet()
  {  
  }

  private ElfPrFPRegSet(byte[] singleNoteData, Elf elf)
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

    raw_registers = new byte[(int)singleNoteData.length];
    noteBuffer.get(raw_registers,0,(int)singleNoteData.length);
  }		     
  
  /**
   * Sets the FP register buffer from buffer.
   *
   * 
   * @param byte[] buffer - the ptrace byte buffer
   * representing the block of memory the fp registers
   * reside. As fp registers are very arch specific
   * this is the only way we can represent in a genric way.
   * 
   */
  public void setFPRegisterBuffer(byte[] buffer) 
  {
    raw_registers = buffer;
  }

  /**
   * Returns the FP register info as a byte array
   *
   * @return -  buffer of fp registers
   */
  public byte[]  getFPRegisterBuffer()
  {
    return raw_registers;
  }

  public static ElfPrFPRegSet[] decode(ElfData noteData)
  {
    getNoteData(noteData);
    ElfPrFPRegSet threadList[] = new  ElfPrFPRegSet[internalThreads.size()];

    int count = 0;
    Iterator i = internalThreads.iterator();
    while (i.hasNext())
      {
	byte b[]  = (byte[]) i.next();
	threadList[count] = new ElfPrFPRegSet(b,noteData.getParent());
	count++;
      }
	
    internalThreads.clear();
    return threadList;
  }


  public native static long getNoteData(ElfData data);
  public native long getEntrySize();
  public native long fillMemRegion(byte[] buffer, long startAddress);
}
