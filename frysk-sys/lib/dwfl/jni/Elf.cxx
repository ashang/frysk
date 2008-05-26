// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

#define ELF_POINTER ((::Elf*)GetPointer(env))

static void throw_last_elf_error(jnixx::env env) __attribute__((noreturn));
static void throw_last_elf_error(jnixx::env env) {
  const char *error = ::elf_errmsg(elf_errno());
  lib::dwfl::ElfException::ThrowNew(env, error);
}

jlong
lib::dwfl::Elf::elfBegin(jnixx::env env,
			 frysk::sys::FileDescriptor fd,
			 lib::dwfl::ElfCommand command) {
  if(::elf_version(EV_CURRENT) == EV_NONE) {
    fd.close(env);
    lib::dwfl::ElfException::ThrowNew(env, "Elf library version out of date");
  }
  ::Elf* new_elf = ::elf_begin(fd.getFd(env),
			       (Elf_Cmd) (command.getValue(env)),
			       NULL);
  if(new_elf == NULL) {
    char buf[128];
    snprintf(buf, sizeof buf,
	     "Could not open Elf file: fd=%d; error=\"%s\".",
	     (int)fd.getFd(env), elf_errmsg(elf_errno()));
    fd.close(env);
    lib::dwfl::ElfException::ThrowNew(env, buf);
  }
  return (jlong)new_elf;
}

jint
lib::dwfl::Elf::elf_next(jnixx::env env) {
  return (jint) ::elf_next(ELF_POINTER);
}

void
lib::dwfl::Elf::elfEnd(jnixx::env env, jlong pointer) {
  ::elf_end((::Elf*) pointer);
}



void
lib::dwfl::Elf::elf_update(jnixx::env env, jint command) {
  if (::elf_update(ELF_POINTER, (Elf_Cmd) command) < 0)
    ::throw_last_elf_error(env);
}

jint
lib::dwfl::Elf::elf_kind(jnixx::env env) {
  return ::elf_kind(ELF_POINTER);
}

jlong
lib::dwfl::Elf::elf_getbase(jnixx::env env) {
  return ::elf_getbase(ELF_POINTER);
}

String
lib::dwfl::Elf::elf_getident(jnixx::env env) {
  size_t length;
  char* ident = ::elf_getident(ELF_POINTER, &length);
  fprintf(stderr, "Was NewString, which is wrong; is this NUL terminated?");
  return String::NewStringUTF(env, ident);
}

String
lib::dwfl::Elf::elf_get_last_error_msg(jnixx::env env) {
  const char *error = ::elf_errmsg(elf_errno());
  return String::NewStringUTF(env, error);
}

jint 
lib::dwfl::Elf::elf_get_last_error_no(jnixx::env env) {
  return elf_errno();
}

lib::dwfl::ElfEHeader
lib::dwfl::Elf::elf_getehdr(jnixx::env env) {
  GElf_Ehdr ehdr;
  if(::gelf_getehdr(ELF_POINTER, &ehdr) == NULL) {
    throw_last_elf_error(env);
  }
  lib::dwfl::ElfEHeader header = lib::dwfl::ElfEHeader::New(env);
  jnixx::jbyteArray jbytes = header.GetIdent(env);
  jbyteArrayElements bytes = jbyteArrayElements(env, jbytes);
  for(int i = 0; i < EI_NIDENT; i++)
    bytes.elements()[i] = (jbyte) ehdr.e_ident[i];
  header.SetType(env, (jint) ehdr.e_type);
  header.SetMachine(env, (jint) ehdr.e_machine);
  header.SetVersion(env, (jint) ehdr.e_version);
  header.SetEntry(env, (jlong) ehdr.e_entry);
  header.SetPhoff(env, (jlong) ehdr.e_phoff);
  header.SetShoff(env, (jlong) ehdr.e_shoff);
  header.SetFlags(env, (jint) ehdr.e_flags);
  header.SetEhsize(env, (jint) ehdr.e_ehsize);
  header.SetPhentsize(env, (jint) ehdr.e_phentsize);
  header.SetPhnum(env, (jint) ehdr.e_phnum);
  header.SetShentsize(env, (jint) ehdr.e_shentsize);
  header.SetShnum(env, (jint) ehdr.e_shnum);
  header.SetShstrndx(env, (jint) ehdr.e_shstrndx);
  return header;
}

void
lib::dwfl::Elf::elf_newehdr(jnixx::env env, jint wordSize) {
  ::Elf* elf = ELF_POINTER;
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
    runtimeException(env, "Bad parameter to elf_newehdr (word size %d)",
		     wordSize);
  }
  if (::gelf_newehdr(elf, elfClass) < 0)
    throw_last_elf_error(env);
}

void
lib::dwfl::Elf::elf_updatehdr(jnixx::env env, ElfEHeader phdr) {
  ::Elf* elf = ELF_POINTER;
  GElf_Ehdr hdr;
  if(::gelf_getehdr(ELF_POINTER, &hdr) == NULL)
    throw_last_elf_error(env);
  jnixx::jbyteArray jbytes = phdr.GetIdent(env);
  jbyteArrayElements bytes = jbyteArrayElements(env, jbytes);
  for (int i = 0; i < EI_NIDENT; i++) {
    hdr.e_ident[i] = (jbyte) bytes.elements()[i];
  }
	
  hdr.e_type = (int) phdr.GetType(env);
  hdr.e_machine = (int) phdr.GetMachine(env);
  hdr.e_version = (int) phdr.GetVersion(env);
  hdr.e_entry = (long) phdr.GetEntry(env);
  hdr.e_phoff = (long) phdr.GetPhoff(env);
  hdr.e_shoff = (long) phdr.GetShoff(env);
  hdr.e_flags = (int) phdr.GetFlags(env);
  hdr.e_ehsize = (int) phdr.GetEhsize(env);
  hdr.e_phentsize = (int) phdr.GetPhentsize(env);
  hdr.e_phnum = (int) phdr.GetPhnum(env);
  hdr.e_shentsize = (int) phdr.GetShentsize(env);
  hdr.e_shnum = (int) phdr.GetShnum(env);
  hdr.e_shstrndx = (int) phdr.GetShstrndx(env);

  if (gelf_update_ehdr (elf,&hdr) == 0)
    throw_last_elf_error(env);
}

jint
lib::dwfl::Elf::elf_get_version(jnixx::env env) {
  return EV_CURRENT;
}

void fillPHeader(jnixx::env env, lib::dwfl::ElfPHeader header,
		 GElf_Phdr *phdr){
  header.SetType(env, (jint) phdr->p_type);
  header.SetFlags(env, (jint) phdr->p_flags);
  header.SetOffset(env, (jlong) phdr->p_offset);
  header.SetVaddr(env, (jlong) phdr->p_vaddr);
  header.SetPaddr(env, (jlong) phdr->p_paddr);
  header.SetFilesz(env, (jlong) phdr->p_filesz);
  header.SetMemsz(env, (jlong) phdr->p_memsz);
  header.SetAlign(env, (jlong) phdr->p_align);
}

lib::dwfl::ElfPHeader
lib::dwfl::Elf::elf_getphdr(jnixx::env env, jint index) {
  GElf_Phdr phdr;
  if(::gelf_getphdr(ELF_POINTER, index, &phdr) == NULL)
    return ElfPHeader(env, NULL);
  lib::dwfl::ElfPHeader header = lib::dwfl::ElfPHeader::New(env, *this);
  fillPHeader(env, header, &phdr);
  return header;
}

jint
lib::dwfl::Elf::elf_updatephdr(jnixx::env env, jint index,
			       lib::dwfl::ElfPHeader phdr) {
  GElf_Phdr header;
  if (::gelf_getphdr(ELF_POINTER, index, &header) == NULL)
    return -1;
  ::Elf* elf = ELF_POINTER;

  header.p_type = (jint) phdr.GetType(env);
  header.p_flags = (jint) phdr.GetFlags(env);
  header.p_offset = (jlong) phdr.GetOffset(env);
  header.p_vaddr = (jlong) phdr.GetVaddr(env);
  header.p_paddr = (jlong) phdr.GetPaddr(env);
  header.p_filesz = (jlong) phdr.GetFilesz(env);
  header.p_memsz = (jlong) phdr.GetMemsz(env);
  header.p_align = (jlong) phdr.GetAlign(env);
  return ::gelf_update_phdr (elf, index, &header);
} 


jint
lib::dwfl::Elf::elf_newphdr(jnixx::env env, jlong cnt) {
  return (jint) ::gelf_newphdr(ELF_POINTER, (size_t) cnt);
}

jlong
lib::dwfl::Elf::elf_offscn(jnixx::env env, jlong offset) {
  return (jlong) gelf_offscn(ELF_POINTER, (Elf32_Off) offset);
}

jlong
lib::dwfl::Elf::elf_getscn(jnixx::env env, jlong index) {
  return (jlong) ::elf_getscn(ELF_POINTER, (size_t) index);
}

jlong
lib::dwfl::Elf::elf_nextscn(jnixx::env env, jlong section) {
  return (jlong) ::elf_nextscn(ELF_POINTER, (Elf_Scn*) section);
}

jlong
lib::dwfl::Elf::elf_newscn(jnixx::env env) {
  return (jlong) ::elf_newscn(ELF_POINTER);
}

jlong
lib::dwfl::Elf::elf_getshnum(jnixx::env env) {
  size_t count;
  /* XXX: What to do if this fails */
  ::elf_getshnum(ELF_POINTER, &count);
  return count;
}

jlong
lib::dwfl::Elf::elf_getshstrndx(jnixx::env env) {
  size_t index;
  /* XXX: What to do if this fails */
  ::elf_getshstrndx(ELF_POINTER, &index);
  return index;
}

jint
lib::dwfl::Elf::elf_flagelf(jnixx::env env, jint command, jint flags) {
  return ::elf_flagelf(ELF_POINTER, (Elf_Cmd) command, flags);
}

jint
lib::dwfl::Elf::elf_flagehdr(jnixx::env env, jint command, jint flags)
{
  return ::elf_flagehdr(ELF_POINTER, (Elf_Cmd) command, flags);
}

jint
lib::dwfl::Elf::elf_flagphdr(jnixx::env env, jint command, jint flags) {
  return ::elf_flagphdr(ELF_POINTER, (Elf_Cmd) command, flags);
}

String
lib::dwfl::Elf::elf_strptr(jnixx::env env, jlong index, jlong offset) {
  char* strptr = ::elf_strptr(ELF_POINTER, (size_t) index, (size_t) offset);
  return String::NewStringUTF(env, strptr);
}

lib::dwfl::ElfArchiveHeader
lib::dwfl::Elf::elf_getarhdr(jnixx::env env) {
  Elf_Arhdr *hdr = ::elf_getarhdr(ELF_POINTER);
	
  if(hdr == NULL)
    return lib::dwfl::ElfArchiveHeader(env, NULL);
		
  lib::dwfl::ElfArchiveHeader header
    = lib::dwfl::ElfArchiveHeader::New(env, *this);
	
  header.SetName(env, String::NewStringUTF(env, hdr->ar_name));
  header.SetDate(env, (jlong) hdr->ar_date);
  header.SetUid(env, (jint) hdr->ar_uid);
  header.SetGid(env, (jint) hdr->ar_gid);
  header.SetMode(env, (jint) hdr->ar_mode);
  header.SetSize(env, (jlong) hdr->ar_size);
  header.SetRawname(env, String::NewStringUTF(env, hdr->ar_rawname));
  return header;
}

jlong
lib::dwfl::Elf::elf_getaroff(jnixx::env env) {
  return ::elf_getaroff(ELF_POINTER);
}

jlong
lib::dwfl::Elf::elf_rand(jnixx::env env, jint offset) {
  return ::elf_rand(ELF_POINTER, (size_t) offset);
}

jlong
lib::dwfl::Elf::elf_getarsym(jnixx::env env, jlong ptr) {
  return (jlong) ::elf_getarsym(ELF_POINTER, (size_t*)(long) &ptr);
}

jint
lib::dwfl::Elf::elf_cntl(jnixx::env env, jint command) {
  return ::elf_cntl(ELF_POINTER, (Elf_Cmd) command);
}

lib::dwfl::ElfData
lib::dwfl::Elf::elf_get_raw_data(jnixx::env env, jlong offset, jlong size) {
  ::Elf_Data* chunk = elf_getdata_rawchunk(ELF_POINTER, offset, size,
                                           ELF_T_BYTE);
  lib::dwfl::ElfData data = lib::dwfl::ElfData::New(env, (jlong)chunk, *this);
  // chunk will be freed by libelf at elf_end.
  return data;
}

String
lib::dwfl::Elf::elf_rawfile(jnixx::env env, jlong ptr) {
  char* file = ::elf_rawfile(ELF_POINTER, (size_t*)(long) &ptr);
  return String::NewStringUTF(env, file);
}
