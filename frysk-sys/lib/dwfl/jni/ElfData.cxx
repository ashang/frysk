// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008 Red Hat Inc.
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

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

#define ELF_DATA_POINTER ((::Elf_Data*)GetPointer(env))

void
lib::dwfl::ElfData::elf_data_finalize(jnixx::env env) {
  //	free((Elf_Data*) this->pointer);
}


void
lib::dwfl::ElfData::elf_data_create_native(jnixx::env env) {
  Elf_Data* data = (Elf_Data*)::malloc(sizeof(Elf_Data));
  data->d_type = ELF_T_BYTE;
  SetPointer(env, (jlong) data);
}

jbyte
lib::dwfl::ElfData::elf_data_get_byte(jnixx::env env, jlong offset) {
  uint8_t* data = (uint8_t*) (ELF_DATA_POINTER)->d_buf;	
  size_t size = (ELF_DATA_POINTER)->d_size;
  if (offset < 0)
    return -1;
  if ((size_t) offset > size)
    return -1;
  return (jbyte) data[offset];
}

jnixx::jbyteArray
lib::dwfl::ElfData::getBytes(jnixx::env env) {
  uint8_t* data = (uint8_t*) (ELF_DATA_POINTER)->d_buf;	
  size_t size = (ELF_DATA_POINTER)->d_size;
  jnixx::jbyteArray ret = jnixx::jbyteArray::NewByteArray(env, (jsize) size);
  jbyteArrayElements bytes = jbyteArrayElements(env, ret);
  for (size_t i = 0; i < size; i++) {
    bytes.elements()[i] = (jbyte) data[i];
  }
  return ret;
}

extern jnixx::jbyteArray internal_buffer;

void
lib::dwfl::ElfData::elf_data_set_buff (jnixx::env env, jlong size) {
  fprintf(stderr, "accessing a global buffer\n");
  jbyteArrayElements bytes = jbyteArrayElements(env, internal_buffer);
  fprintf(stderr, "saving a pointer into the JNI\n");
  (ELF_DATA_POINTER)->d_buf = bytes.elements();
  (ELF_DATA_POINTER)->d_size = bytes.length();
}


jint
lib::dwfl::ElfData::elf_data_get_type(jnixx::env env) {
  return (int) (ELF_DATA_POINTER)->d_type;
}

void
lib::dwfl::ElfData::elf_data_set_type(jnixx::env env, jint type) {
  if (type == 0)
    (ELF_DATA_POINTER)->d_type = ELF_T_BYTE;
}

jint
lib::dwfl::ElfData::elf_data_get_version(jnixx::env env) {
  return (ELF_DATA_POINTER)->d_version;
}

void
lib::dwfl::ElfData::elf_data_set_version(jnixx::env env, jint version) {
  (ELF_DATA_POINTER)->d_version = version;
}

jlong
lib::dwfl::ElfData::elf_data_get_size(jnixx::env env) {
  return (ELF_DATA_POINTER)->d_size;
}

void
lib::dwfl::ElfData::elf_data_set_size(jnixx::env env, jlong size){
  (ELF_DATA_POINTER)->d_size = size;
}


jint
lib::dwfl::ElfData::elf_data_get_off(jnixx::env env){
  return (ELF_DATA_POINTER)->d_off;
}

void
lib::dwfl::ElfData::elf_data_set_off(jnixx::env env, jint offset){
  (ELF_DATA_POINTER)->d_off = offset;
}

jlong
lib::dwfl::ElfData::elf_data_get_align(jnixx::env env){
  return (ELF_DATA_POINTER)->d_align;
}

void
lib::dwfl::ElfData::elf_data_set_align(jnixx::env env, jlong align){
  (ELF_DATA_POINTER)->d_align = align;
}

jint
lib::dwfl::ElfData::elf_flagdata(jnixx::env env, jint command, jint flags){
  return ::elf_flagdata(ELF_DATA_POINTER, (Elf_Cmd) command, flags);
}

jlong
lib::dwfl::ElfData::elf_xlatetom(jnixx::env env, jint encode){
  ::Elf_Data *tmp = (Elf_Data*) alloca(sizeof(Elf_Data));
  return (jlong) ::gelf_xlatetom((::Elf*) GetParent(env).getPointer(env),
				 tmp, ELF_DATA_POINTER, (unsigned int) encode);

}

jlong
lib::dwfl::ElfData::elf_xlatetof(jnixx::env env, jint encode){
  ::Elf_Data *tmp = (Elf_Data*) alloca(sizeof(Elf_Data));
  return (jlong) gelf_xlatetof((::Elf*) GetParent(env).getPointer(env),
			       tmp, ELF_DATA_POINTER, (unsigned int) encode);
}
