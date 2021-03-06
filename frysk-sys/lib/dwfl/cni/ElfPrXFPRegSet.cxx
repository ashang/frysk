// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat, Inc.
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

#include <stdlib.h>
#include <unistd.h>
#include <gcj/cni.h>
#include <stdio.h>
#include <stdint.h>

#include "elf.h"
#include "libelf.h"
#include "gelf.h"

#include "lib/dwfl/ElfPrXFPRegSet.h"
#include "lib/dwfl/ElfData.h"

#include <java/util/ArrayList.h>

using namespace std;
using namespace java::lang;
using namespace java::util;

#define NT_PRFPXREG     20              /* Contains copy of fprxregset struct*/
#define NT_PRXFPREG     0x46e62b7f      /* Contains copy of user_fxsr_struct*/


#ifdef __cplusplus
extern "C"
{
#endif

// Return the size of this entry, in the notes.
jlong
lib::dwfl::ElfPrXFPRegSet::getEntrySize()
{
  return this->getXFPRegisterBuffer()->length;
}

// This is called when the notes section is filled. 
// Fill the passed buffer with your own data, 
// starting at startAddress

jlong 
lib::dwfl::ElfPrXFPRegSet::fillMemRegion(jbyteArray buffer, jlong startAddress)
{
  jbyte *bs = elements(buffer);
  jbyte *buff = elements(this->getXFPRegisterBuffer ());
  
  memcpy(bs + startAddress, buff, this->raw_registers->length);
  
  return this->raw_registers->length;
}

jlong lib::dwfl::ElfPrXFPRegSet::getNoteData(ElfData *data)
{
  void *elf_data = ((Elf_Data*)data->getPointer())->d_buf;
  GElf_Nhdr *nhdr = (GElf_Nhdr *)elf_data;
  long note_loc =0;
  long note_data_loc = 0;


  // Can have more that on Prstatus note per core file. Collect all of them.
  while (note_loc < data->getSize())
    {
      // Find Prstatus note data. If the first note header is not prstatus
      // loop through, adding up header + align + data till we find the
      // next header. Continue until section end to find correct header.
      while ((nhdr->n_type != NT_PRXFPREG) && (note_loc <= data->getSize()))      
	{
	  note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
	  if (note_loc >= data->getSize())
	    break;
	  nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
	}
      
      // If loop through entire note section, and header not found, return
      // here with abnormal return code.
      if (nhdr->n_type != NT_PRXFPREG)
	return -1;
      
      // Find data at current header + alignment
      note_data_loc = (note_loc + sizeof(GElf_Nhdr) + ((nhdr->n_namesz +  0x03) & ~0x3));
      
      jbyteArray jbuf = JvNewByteArray(nhdr->n_descsz);
      memcpy(elements(jbuf),((unsigned char  *)elf_data)+note_data_loc,  nhdr->n_descsz);
      
      internalThreads->add(jbuf);

      // Move pointer along, now we have processed the first thread
      note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
      nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
    }
  
  return 0;
}
#ifdef __cplusplus
}
#endif
