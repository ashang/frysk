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
#include <stdlib.h>
#include <gelf.h>

#include "lib/elf/ElfSection.h"
#include "lib/elf/ElfSectionHeader.h"
#include "lib/elf/Elf.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong
lib::elf::ElfSection::elf_ndxscn (){
	return ::elf_ndxscn((Elf_Scn*) this->pointer);
}

lib::elf::ElfSectionHeader*
lib::elf::ElfSection::elf_getshdr (){
	GElf_Shdr tmp;
	if(::gelf_getshdr((Elf_Scn*) this->pointer, &tmp) == NULL)
		return NULL;
		
	lib::elf::ElfSectionHeader *header = new lib::elf::ElfSectionHeader(this->parent);
	
	GElf_Ehdr *ehdr = (GElf_Ehdr *) alloca(sizeof(GElf_Ehdr));
	ehdr = gelf_getehdr((::Elf *) this->parent->getPointer(), ehdr);
	header->name = JvNewStringUTF(
						::elf_strptr((::Elf *) this->parent->getPointer(), ehdr->e_shstrndx, tmp.sh_name)
						); 
	
	header->type = (jint) tmp.sh_type;
	header->flags = (jlong) tmp.sh_flags;
	header->addr = (jlong) tmp.sh_addr;
	header->offset = (jlong) tmp.sh_offset;
	header->size = (jlong) tmp.sh_size;
	header->link = (jint) tmp.sh_link;
	header->info = (jint) tmp.sh_info;
	header->addralign = (jlong) tmp.sh_addralign;
	header->entsize = (jlong) tmp.sh_entsize;
	
	return header;
}

jint
lib::elf::ElfSection::elf_flagscn (jint command, jint flags){
	return ::elf_flagscn((Elf_Scn*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::elf::ElfSection::elf_flagshdr (jint command, jint flags){
	return ::elf_flagshdr((Elf_Scn*) this->pointer, (Elf_Cmd) command, flags);
}

jlong
lib::elf::ElfSection::elf_getdata (){
	return (jlong) ::elf_getdata((Elf_Scn*) this->pointer, (Elf_Data*) NULL);
}

jlong
lib::elf::ElfSection::elf_rawdata (){
	return (jlong) ::elf_rawdata((Elf_Scn*) this->pointer, (Elf_Data*) NULL);
}

jlong
lib::elf::ElfSection::elf_newdata (){
	return (jlong) ::elf_newdata((Elf_Scn*) this->pointer);
}

#ifdef __cplusplus
}
#endif
