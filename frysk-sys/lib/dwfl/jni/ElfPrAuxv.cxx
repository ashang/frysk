// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat Inc.
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
#include <stdio.h>
#include <stdint.h>

#include "libelf.h"
#include "elf.h"
#include "gelf.h"

#include "jni.hxx"

#include "jnixx/elements.hxx"

jnixx::jbyteArray
lib::dwfl::ElfPrAuxv::getNoteData(jnixx::env env, ElfData data) {
  void *elf_data = ((Elf_Data*)data.getPointer(env))->d_buf;
  GElf_Nhdr *nhdr = (GElf_Nhdr *)elf_data;
  long note_loc =0;
  long note_data_loc = 0;

  // Find auxv note data. If the first note header is not auxv
  // loop through, adding up header + align + data till we find the
  // next header. Continue until section end to find correct header.
  while ((nhdr->n_type != NT_AUXV) && (note_loc <= data.getSize(env)))
    {
      note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
      if (note_loc >= data.getSize(env))
	break;
      nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
    }

  // If loop through entire note section, and header not found, return
  // here with abnormal return code.
  if (nhdr->n_type != NT_AUXV)
    return jnixx::jbyteArray(env, NULL);

  // Find data at current header + alignment
  note_data_loc = (note_loc + sizeof(GElf_Nhdr) + ((nhdr->n_namesz +  0x03) & ~0x3));

  // Run some sanity checks, as we will be doing void pointer -> cast math.
  if ((nhdr->n_descsz > data.getSize(env))
      || (nhdr->n_descsz > (data.getSize(env)-note_data_loc))) {
    lib::dwfl::ElfException::ThrowNew(env, "note size and elf_data size mismatch");
  }
  jnixx::jbyteArray jbuf = jnixx::jbyteArray::NewByteArray(env, nhdr->n_descsz);
  jbyteArrayElements buf = jbyteArrayElements(env, jbuf);

  ::memcpy(buf.elements(), ((unsigned char  *)elf_data)+note_data_loc,
	   nhdr->n_descsz);
  
  return jbuf;
}
