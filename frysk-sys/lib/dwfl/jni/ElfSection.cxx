// This file is part of the program FRYSK.
//
// Copyright 2005, 2008, Red Hat Inc.
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
#include <gelf.h>
#include <stdio.h>

#include "jni.hxx"

#define ELF_SCN_POINTER ((::Elf_Scn*)GetPointer(env))

using namespace java::lang;

jlong
lib::dwfl::ElfSection::elf_ndxscn(jnixx::env env) {
  return ::elf_ndxscn(ELF_SCN_POINTER);
}

lib::dwfl::ElfSectionHeader
lib::dwfl::ElfSection::elf_getshdr(jnixx::env env){
  GElf_Shdr tmp;
  if(::gelf_getshdr(ELF_SCN_POINTER, &tmp) == NULL)
    return ElfSectionHeader(env, NULL);
		
  lib::dwfl::ElfSectionHeader header = lib::dwfl::ElfSectionHeader::New(env, GetParent(env));
  GElf_Ehdr *ehdr = (GElf_Ehdr *) alloca(sizeof(GElf_Ehdr));
  ehdr = gelf_getehdr((::Elf *) this->GetParent(env).getPointer(env), ehdr);
  char *str = ::elf_strptr((::Elf *) GetParent(env).getPointer(env),
			   ehdr->e_shstrndx, tmp.sh_name);
  if (str != NULL) {
    const char *name = ::elf_strptr((::Elf *) GetParent(env).getPointer(env),
				    ehdr->e_shstrndx, tmp.sh_name); 
    String jname = String::NewStringUTF(env, name);
    header.SetName(env, jname);
    jname.DeleteLocalRef(env);
  }
  header.SetType(env, (jint) tmp.sh_type);
  header.SetFlags(env, (jlong) tmp.sh_flags);
  header.SetAddr(env, (jlong) tmp.sh_addr);
  header.SetOffset(env, (jlong) tmp.sh_offset);
  header.SetSize(env, (jlong) tmp.sh_size);
  header.SetLink(env, (jint) tmp.sh_link);
  header.SetInfo(env, (jint) tmp.sh_info);
  header.SetAddralign(env, (jlong) tmp.sh_addralign);
  header.SetEntsize(env, (jlong) tmp.sh_entsize);
	
  return header;
}

jint
lib::dwfl::ElfSection::elf_updateshdr(jnixx::env env,
				      lib::dwfl::ElfSectionHeader section) {
  GElf_Shdr header;

  if(::gelf_getshdr(ELF_SCN_POINTER, &header) == NULL)
    return -1;
		
  header.sh_name = section.GetNameAsNum(env);
  header.sh_type = (jint) section.GetType(env);
  header.sh_flags = (jlong) section.GetFlags(env);
  header.sh_addr = (jlong) section.GetAddr(env);
  header.sh_offset = (jlong) section.GetOffset(env);
  header.sh_size = (jlong) section.GetSize(env);
  header.sh_link = (jint) section.GetLink(env);
  header.sh_info = (jint) section.GetInfo(env);
  header.sh_addralign = (jlong) section.GetAddralign(env);
  header.sh_entsize = (jlong) section.GetEntsize(env);
	
  return gelf_update_shdr(ELF_SCN_POINTER,&header);
}

jint
lib::dwfl::ElfSection::elf_flagscn(jnixx::env env, jint command, jint flags) {
  return ::elf_flagscn(ELF_SCN_POINTER, (Elf_Cmd) command, flags);
}

jint
lib::dwfl::ElfSection::elf_flagshdr(jnixx::env env, jint command, jint flags) {
  return ::elf_flagshdr(ELF_SCN_POINTER, (Elf_Cmd) command, flags);
}

jlong
lib::dwfl::ElfSection::elf_getdata(jnixx::env env){
  return (jlong) ::elf_getdata(ELF_SCN_POINTER, (Elf_Data*) NULL);
}

jlong
lib::dwfl::ElfSection::elf_rawdata(jnixx::env env) {
  return (jlong) ::elf_rawdata(ELF_SCN_POINTER, (Elf_Data*) NULL);
}

jlong
lib::dwfl::ElfSection::elf_newdata(jnixx::env env){
  return (jlong) ::elf_newdata(ELF_SCN_POINTER);
}
