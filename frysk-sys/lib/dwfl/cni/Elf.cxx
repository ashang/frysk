// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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
#include <libelf.h>
#include <gelf.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawData.h>

#include "frysk/sys/FileDescriptor.h"
#include "lib/dwfl/ElfCommand.h"
#include "lib/dwfl/ElfException.h"
#include "lib/dwfl/ElfFileException.h"
#include "lib/dwfl/Elf.h"
#include "lib/dwfl/ElfEHeader.h"
#include "lib/dwfl/ElfPHeader.h"
#include "lib/dwfl/ElfArchiveHeader.h"
#include "lib/dwfl/ElfData.h"
#include "frysk/sys/cni/Errno.hxx"


static void throw_last_elf_error() __attribute__((noreturn));
static void throw_last_elf_error() {
  throw new lib::dwfl::ElfException(lib::dwfl::Elf::getLastErrorMsg());
}

gnu::gcj::RawData*
lib::dwfl::Elf::elfBegin (frysk::sys::FileDescriptor* fd,
			  lib::dwfl::ElfCommand* command)
{
  if(::elf_version(EV_CURRENT) == EV_NONE) 
    {
      fd->close();
      throw new lib::dwfl::ElfException(JvNewStringUTF("Elf library version out of date"));
    }
  errno = 0;	
  ::Elf* new_elf = ::elf_begin (fd->getFd(), (Elf_Cmd) (command->getValue()),
				NULL);
  if(errno != 0 || !new_elf) 
    {
      fd->close();
      throw new lib::dwfl::ElfException(JvNewStringUTF("Could not open Elf file"));
    }
  return (gnu::gcj::RawData*)new_elf;
}

jint
lib::dwfl::Elf::elf_next ()
{
  return (jint) ::elf_next((::Elf*) this->pointer);
}

void
lib::dwfl::Elf::elfEnd(gnu::gcj::RawData* pointer)
{
  ::elf_end((::Elf*) pointer);
}



void
lib::dwfl::Elf::elf_update(jint command) {
  if (::elf_update((::Elf*) this->pointer, (Elf_Cmd) command) < 0)
    ::throw_last_elf_error();
}

jint
lib::dwfl::Elf::elf_kind ()
{
  return ::elf_kind((::Elf*) this->pointer);
}

jlong
lib::dwfl::Elf::elf_getbase ()
{
  return ::elf_getbase((::Elf*) this->pointer);
}

jstring
lib::dwfl::Elf::elf_getident ()
{
  size_t length;
  char* ident = ::elf_getident((::Elf*) pointer, &length);
  return JvNewString((const jchar*) ident, length);
}

jstring
lib::dwfl::Elf::elf_get_last_error_msg ()
{
  const char *error = ::elf_errmsg(elf_errno());
  return JvNewStringLatin1(error, strlen(error));
}

jint 
lib::dwfl::Elf::elf_get_last_error_no ()
{
  return elf_errno();
}

lib::dwfl::ElfEHeader*
lib::dwfl::Elf::elf_getehdr() {
  GElf_Ehdr ehdr;
  if(::gelf_getehdr((::Elf*) this->pointer, &ehdr) == NULL) {
    throw_last_elf_error();
  }
  lib::dwfl::ElfEHeader *header = new lib::dwfl::ElfEHeader();
  jbyte *bytes = elements(header->ident);
  for(int i = 0; i < EI_NIDENT; i++)
    bytes[i] = (jbyte) ehdr.e_ident[i];
  header->type = (jint) ehdr.e_type;
  header->machine = (jint) ehdr.e_machine;
  header->version = (jint) ehdr.e_version;
  header->entry = (jlong) ehdr.e_entry;
  header->phoff = (jlong) ehdr.e_phoff;
  header->shoff = (jlong) ehdr.e_shoff;
  header->flags = (jint) ehdr.e_flags;
  header->ehsize = (jint) ehdr.e_ehsize;
  header->phentsize = (jint) ehdr.e_phentsize;
  header->phnum = (jint) ehdr.e_phnum;
  header->shentsize = (jint) ehdr.e_shentsize;
  header->shnum = (jint) ehdr.e_shnum;
  header->shstrndx = (jint) ehdr.e_shstrndx;
  return header;
}

void
lib::dwfl::Elf::elf_newehdr(jint wordSize) {
  ::Elf* elf = (::Elf*) this->pointer;
  int elfClass;
  switch (wordSize) {
  case 4:
    elfClass = ELFCLASS32;
    break;
  case 8:
    elfClass = ELFCLASS64;
    break;
  default:
    // This is a programmer error; and not an Elf file or format error.
    throwRuntimeException("Bad parameter to elf_newehdr", "word size",
			  wordSize);
  }
  if (::gelf_newehdr(elf, elfClass) < 0)
    throw_last_elf_error();
}

void
lib::dwfl::Elf::elf_updatehdr(ElfEHeader* phdr) {
  ::Elf* elf = (::Elf*) this->pointer;
  GElf_Ehdr hdr;

  if(::gelf_getehdr((::Elf*) this->pointer, &hdr) == NULL)
    throw_last_elf_error();

  jbyte *bytes = elements(phdr->ident);	
  for(int i = 0; i < EI_NIDENT; i++)
    hdr.e_ident[i] = (jbyte) bytes[i];
	
  hdr.e_type = (int) phdr->type;
  hdr.e_machine = (int) phdr->machine;
  hdr.e_version = (int) phdr->version;
  hdr.e_entry = (long) phdr->entry;
  hdr.e_phoff = (long) phdr->phoff;
  hdr.e_shoff = (long) phdr->shoff;
  hdr.e_flags = (int) phdr->flags;
  hdr.e_ehsize = (int) phdr->ehsize;
  hdr.e_phentsize = (int) phdr->phentsize;
  hdr.e_phnum = (int) phdr->phnum;
  hdr.e_shentsize = (int) phdr->shentsize;
  hdr.e_shnum = (int) phdr->shnum;
  hdr.e_shstrndx = (int) phdr->shstrndx;

  if (gelf_update_ehdr (elf,&hdr) == 0)
    throw_last_elf_error();
}

jint
lib::dwfl::Elf::elf_get_version()
{
  return EV_CURRENT;
}

void fillPHeader(lib::dwfl::ElfPHeader *header, GElf_Phdr *phdr){
  header->type = (jint) phdr->p_type;
  header->flags = (jint) phdr->p_flags;
  header->offset = (jlong) phdr->p_offset;
  header->vaddr = (jlong) phdr->p_vaddr;
  header->paddr = (jlong) phdr->p_paddr;
  header->filesz = (jlong) phdr->p_filesz;
  header->memsz = (jlong) phdr->p_memsz;
  header->align = (jlong) phdr->p_align;
}

lib::dwfl::ElfPHeader*
lib::dwfl::Elf::elf_getphdr (jint index)
{
  GElf_Phdr phdr;
  if(::gelf_getphdr((::Elf*) this->pointer, index, &phdr) == NULL)
    return NULL;
		
  lib::dwfl::ElfPHeader *header = new lib::dwfl::ElfPHeader(this);
	
  fillPHeader(header, &phdr);
	
  return header;
}

jint
lib::dwfl::Elf::elf_updatephdr(jint index, lib::dwfl::ElfPHeader *phdr)
{
  GElf_Phdr header;
  if (::gelf_getphdr((::Elf*) this->pointer, index, &header) == NULL)
    return -1;
  ::Elf* elf = (::Elf*) this->pointer;

  header.p_type = (jint) phdr->type;
  header.p_flags = (jint) phdr->flags;
  header.p_offset = (jlong) phdr->offset;
  header.p_vaddr = (jlong) phdr->vaddr;
  header.p_paddr = (jlong) phdr->paddr;
  header.p_filesz = (jlong) phdr->filesz;
  header.p_memsz = (jlong) phdr->memsz;
  header.p_align = (jlong) phdr->align;
	
  return gelf_update_phdr (elf, index, &header);
} 


jint
lib::dwfl::Elf::elf_newphdr (jlong cnt)
{
  return (jint) ::gelf_newphdr((::Elf*) this->pointer, (size_t) cnt);
}

jlong
lib::dwfl::Elf::elf_offscn (jlong offset)
{
  return (jlong) gelf_offscn((::Elf*) this->pointer, (Elf32_Off) offset);
}

jlong
lib::dwfl::Elf::elf_getscn (jlong index)
{
  return (jlong) ::elf_getscn((::Elf*) this->pointer, (size_t) index);
}

jlong
lib::dwfl::Elf::elf_nextscn (jlong section)
{
  return (jlong) ::elf_nextscn((::Elf*) this->pointer, (Elf_Scn*) section);
}

jlong
lib::dwfl::Elf::elf_newscn ()
{
  return (jlong) ::elf_newscn((::Elf*) this->pointer);
}

jlong
lib::dwfl::Elf::elf_getshnum ()
{
  size_t count;
  /* XXX: What to do if this fails */
  ::elf_getshnum((::Elf*) this->pointer, &count);
  return count;
}

jlong
lib::dwfl::Elf::elf_getshstrndx ()
{
  size_t index;
  /* XXX: What to do if this fails */
  ::elf_getshstrndx((::Elf*) this->pointer, &index);
  return index;
}

jint
lib::dwfl::Elf::elf_flagelf (jint command, jint flags)
{
  return ::elf_flagelf((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::dwfl::Elf::elf_flagehdr (jint command, jint flags)
{
  return ::elf_flagehdr((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::dwfl::Elf::elf_flagphdr (jint command, jint flags)
{
  return ::elf_flagphdr((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jstring
lib::dwfl::Elf::elf_strptr (jlong index, jlong offset)
{
  char* strptr = ::elf_strptr((::Elf*) this->pointer, (size_t) index, (size_t) offset);
  return JvNewStringUTF(strptr);
}

lib::dwfl::ElfArchiveHeader*
lib::dwfl::Elf::elf_getarhdr ()
{
  Elf_Arhdr *hdr = ::elf_getarhdr((::Elf*) this->pointer);
	
  if(hdr == NULL)
    return NULL;
		
  lib::dwfl::ElfArchiveHeader *header = new lib::dwfl::ElfArchiveHeader(this);
	
  header->name = JvNewString((const jchar*)hdr->ar_name, strlen(hdr->ar_name));
  header->date = (jlong) hdr->ar_date;
  header->uid = (jint) hdr->ar_uid;
  header->gid = (jint) hdr->ar_gid;
  header->mode = (jint) hdr->ar_mode;
  header->size = (jlong) hdr->ar_size;
  header->rawname = JvNewString((const jchar*) hdr->ar_rawname, strlen(hdr->ar_rawname));
	
  return header;
}

jlong
lib::dwfl::Elf::elf_getaroff ()
{
  return ::elf_getaroff((::Elf*) this->pointer);
}

jlong
lib::dwfl::Elf::elf_rand (jint offset)
{
  return ::elf_rand((::Elf*) this->pointer, (size_t) offset);
}

jlong
lib::dwfl::Elf::elf_getarsym (jlong ptr)
{
  return (jlong) ::elf_getarsym((::Elf*) this->pointer, (size_t*)(long) &ptr);
}

jint
lib::dwfl::Elf::elf_cntl (jint command)
{
  return ::elf_cntl((::Elf*) this->pointer, (Elf_Cmd) command);
}

lib::dwfl::ElfData* lib::dwfl::Elf::elf_get_raw_data (jlong offset, jlong size)
{
  ::Elf_Data* chunk = elf_getdata_rawchunk((::Elf*) this->pointer, offset, size,
                                           ELF_T_BYTE);
  lib::dwfl::ElfData *data = new lib::dwfl::ElfData((jlong)chunk, this);
  // chunk will be freed by libelf at elf_end.
  return data;
}

jstring
lib::dwfl::Elf::elf_rawfile (jlong ptr)
{
  char* file = ::elf_rawfile((::Elf*) pointer, (size_t*)(long) &ptr);
  return JvNewString((const jchar*) file, strlen(file));
}
