// This file is part of the program FRYSK.
//
// Copyright 2005,2007 Red Hat Inc.
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
#include <gelf.h>
#include <stdlib.h>
#include <inttypes.h>
#include <alloca.h>
#include <gcj/cni.h>

#include "lib/elf/ElfData.h"

#ifdef __cplusplus
extern "C"
{
#endif


void
lib::elf::ElfData::elf_data_finalize (){
//	free((Elf_Data*) this->pointer);
}


void
lib::elf::ElfData::elf_data_create_native ()
{
  this->pointer = (long) JvMalloc(sizeof(Elf_Data));  
  ((::Elf_Data *)this->pointer)->d_type = ELF_T_BYTE;
}

jbyte
lib::elf::ElfData::elf_data_get_byte (jlong offset)
{
  uint8_t* data = (uint8_t*) ((::Elf_Data*) this->pointer)->d_buf;	
  size_t size = ((Elf_Data*) this->pointer)->d_size;
  if (offset < 0)
    return -1;
  if ((size_t) offset > size)
    return -1;
  return (jbyte) data[offset];
}

extern jbyteArray internal_buffer;

void
lib::elf::ElfData::elf_data_set_buff (jlong size){

        jbyte *bytes = elements(internal_buffer);
	((Elf_Data*) this->pointer)->d_buf = bytes;
	((Elf_Data*) this->pointer)->d_size = size;
}


jint
lib::elf::ElfData::elf_data_get_type (){
	return (int) ((Elf_Data*) this->pointer)->d_type;
}

void
lib::elf::ElfData::elf_data_set_type (jint type){

	if (type == 0)
		((Elf_Data*) this->pointer)->d_type = ELF_T_BYTE;
}

jint
lib::elf::ElfData::elf_data_get_version (){
	return ((Elf_Data*) this->pointer)->d_version;
}

void
lib::elf::ElfData::elf_data_set_version (jint version){
	((Elf_Data*) this->pointer)->d_version = version;
}

jlong
lib::elf::ElfData::elf_data_get_size (){
	return ((Elf_Data*) this->pointer)->d_size;
}

void
lib::elf::ElfData::elf_data_set_size (jlong size){
	((Elf_Data*) this->pointer)->d_size = size;
}


jint
lib::elf::ElfData::elf_data_get_off (){
	return ((Elf_Data*) this->pointer)->d_off;
}

void
lib::elf::ElfData::elf_data_set_off (jint offset){
	((Elf_Data*) this->pointer)->d_off = offset;
}

jlong
lib::elf::ElfData::elf_data_get_align (){
	return ((Elf_Data*) this->pointer)->d_align;
}

void
lib::elf::ElfData::elf_data_set_align (jlong align){
	((Elf_Data*) this->pointer)->d_align = align;
}

jint
lib::elf::ElfData::elf_flagdata (jint command, jint flags){
	return ::elf_flagdata((Elf_Data*) this->pointer, (Elf_Cmd) command, flags);
}

jlong
lib::elf::ElfData::elf_xlatetom (jint encode){
	::Elf_Data *tmp = (Elf_Data*) alloca(sizeof(Elf_Data));
	return (jlong) ::gelf_xlatetom((::Elf*) this->parent, tmp, (Elf_Data*) this->pointer, (unsigned int) encode);

}

jlong
lib::elf::ElfData::elf_xlatetof (jint encode){
	::Elf_Data *tmp = (Elf_Data*) alloca(sizeof(Elf_Data));
	return (jlong) gelf_xlatetof((::Elf*) this->parent, tmp, (Elf_Data*) this->pointer, (unsigned int) encode);
}

#ifdef __cplusplus
}
#endif
