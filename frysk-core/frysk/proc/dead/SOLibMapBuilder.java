// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.proc.dead;

import lib.dwfl.Elf;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfCommand;
import java.io.File;

/**
 * Build a list of maps from the contents of the file linkmap table at
 * address specified.
 */

public abstract class SOLibMapBuilder {

  /**
   * Create a LinkmapBuilder; can only extend.
   */
  protected SOLibMapBuilder() {
  }
  
  /**
   * Scan the ELF file building up a list of memory maps.
   */
  public final void construct(File clientSolib, long base_addr, int wordSize) {

    Elf solib = openElf(clientSolib);
    ElfEHeader eHeader = solib.getEHeader();
    
    for(int z=0; z<eHeader.phnum; z++)
      {
	
	ElfPHeader pHeader = solib.getPHeader(z);
	if ((pHeader.type == ElfPHeader.PTYPE_LOAD) )
	  {
	    if (base_addr + pHeader.vaddr != 0) 
	    {
		boolean read = (pHeader.flags &  ElfPHeader.PHFLAG_READABLE) > 0 ? true:false;
		boolean write =  (pHeader.flags & ElfPHeader.PHFLAG_WRITABLE) > 0 ? true:false;
		boolean execute = (pHeader.flags & ElfPHeader.PHFLAG_EXECUTABLE) > 0 ? true:false;
		
		long mapBegin = base_addr + (pHeader.vaddr &~ (pHeader.align-1));
		long mapEnd = base_addr + ((pHeader.vaddr + pHeader.memsz) + pHeader.align -1) &~ (pHeader.align-1);

		// On 32 bit systems, if a segment has been relocated ie base_addr > 0 and base_addr + vaddr is
		// more than 0xffffffff then the address overlaps to 0++. As we are using a long, so it can store
		// 64 bit addresses on 64 bit systems, check wordsize == 4 and if so, limit size of address space
		//  to 32 bits.
		if (wordSize == 4)
		{
		    mapBegin &= 0x00000000ffffffffl;
		    mapEnd &= 0x00000000ffffffffl;
		}

		long aOffset = (pHeader.offset &- pHeader.align);
		buildMap(mapBegin, mapEnd, read, write, execute, 
			aOffset, clientSolib.getPath(),pHeader.align);
	    }
	  }
      }
    solib.close();
  }
  
  /**
   * Build an address map covering [addressLow,addressHigh) with
   * permissions {permR, permW, permX, permP }, device devMajor
   * devMinor, inode, and the pathname's offset/length within the
   * buf.
   *
   * !shared implies private, they are mutually exclusive.
   */
  
  abstract public void buildMap (long addrLow, long addrHigh, 
				 boolean permRead, boolean permWrite,
				 boolean permExecute, long offset, 
				 String name, long align);

    private Elf openElf(File name) {
	if ((!name.exists()) && (!name.canRead()) && (!name.isFile()))
	    throw new RuntimeException(name.getPath() + " is an invalid file");
	// Open up corefile corresponding directory.
	return new Elf(name, ElfCommand.ELF_C_READ);
    }
}
