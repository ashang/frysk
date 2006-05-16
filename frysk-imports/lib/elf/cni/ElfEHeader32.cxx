// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
#include <gcj/cni.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <string.h>
#include <libelf.h>

#include "lib/elf/ElfEHeader.h"
#include "lib/elf/ElfEHeader32.h"

#ifdef __cplusplus
extern "C"
{
#endif

jbyte
lib::elf::ElfEHeader32::get_e_fileclass (){
	char* ident = (char*) ((Elf32_Ehdr*) this->getPointer())->e_ident;
	return (jbyte) ident[EI_CLASS];
}

jbyte
lib::elf::ElfEHeader32::get_e_dataencoding (){
	char* ident = (char*) ((Elf32_Ehdr*) this->getPointer())->e_ident;
	return (jbyte) ident[EI_DATA];
}

jbyte
lib::elf::ElfEHeader32::get_e_fileversion (){
	char* ident = (char*) ((Elf32_Ehdr*) this->getPointer())->e_ident;
	return (jbyte) ident[EI_VERSION];
}

jint
lib::elf::ElfEHeader32::get_e_type (){
	return ((Elf32_Ehdr*) this->getPointer())->e_type;
}

jint
lib::elf::ElfEHeader32::get_e_machine (){
	return ((Elf32_Ehdr*) this->getPointer())->e_machine;
}

jlong
lib::elf::ElfEHeader32::get_e_version (){
	return ((Elf32_Ehdr*) this->getPointer())->e_version;
}

jlong
lib::elf::ElfEHeader32::get_e_entry (){
	return ((Elf32_Ehdr*) this->getPointer())->e_entry;
}

jlong
lib::elf::ElfEHeader32::get_e_phoff (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phoff;
}

jlong
lib::elf::ElfEHeader32::get_e_shoff (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shoff;
}

jlong
lib::elf::ElfEHeader32::get_e_flags (){
	return ((Elf32_Ehdr*) this->getPointer())->e_flags;
}

jint
lib::elf::ElfEHeader32::get_e_ehsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_ehsize;
}

jint
lib::elf::ElfEHeader32::get_e_phentsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phentsize;
}

jint
lib::elf::ElfEHeader32::get_e_phnum (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phnum;
}

jint
lib::elf::ElfEHeader32::get_e_shentsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shentsize;
}

jint
lib::elf::ElfEHeader32::get_e_shnum (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shnum;
}

jint
lib::elf::ElfEHeader32::get_e_shstrndx (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shstrndx;
}

#ifdef __cplusplus
}
#endif
