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

#include <alloca.h>
#include <string.h>
#include <malloc.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>

#include <libdw.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;
using namespace java::util;

#define DWARF_POINTER ((::Dwarf *) GetPointer(env))

void
lib::dwfl::Dwarf::dwarf_begin_elf(jnixx::env env, jlong elf, jint command,
				  jlong section) {
  jlong pointer = (jlong)
    ::dwarf_begin_elf((::Elf*) elf, (::Dwarf_Cmd) command,
		      (::Elf_Scn*) section);
  SetPointer(env, pointer);
}

void
lib::dwfl::Dwarf::dwarf_begin(jnixx::env env, String file, jint command){
  jstringUTFChars fileName = jstringUTFChars(env, file);
  errno = 0;
  int fd = ::open(fileName.elements(), O_RDONLY);
  jlong pointer = (jlong) ::dwarf_begin(fd, (::Dwarf_Cmd) command);
  SetPointer(env, pointer);
}


LinkedList
lib::dwfl::Dwarf::get_cu_by_name(jnixx::env env, String name) {
  LinkedList list = LinkedList::New(env);
	
  Dwarf_Off offset = 0;
  Dwarf_Off old_offset;
  Dwarf_Die cudie_mem;
  size_t hsize;
	  
  while (dwarf_nextcu(DWARF_POINTER, old_offset = offset,
		      &offset, &hsize, NULL, NULL, NULL) == 0) {
		
      Dwarf_Die *cudie = dwarf_offdie(DWARF_POINTER, old_offset + hsize,
				      &cudie_mem);
      const char *die_name = dwarf_diename (cudie);
      String die_name_string = String::NewStringUTF(env, die_name);
		
      if (die_name_string.endsWith(env, name)) {

	Dwarf_Die *die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
	memcpy(die, cudie, sizeof(*die));
	lib::dwfl::DwarfDie cuDie = lib::dwfl::DwarfDieFactory::getFactory(env)
	  .makeDie(env, (jlong)die, lib::dwfl::DwflModule(env, NULL));
	cuDie.setManageDie(env, true);
	list.add(env, cuDie);
      }
      die_name_string.DeleteLocalRef(env);
    }
  return list;
}


jnixx::array<String>
lib::dwfl::Dwarf::get_source_files(jnixx::env env) {
  Dwarf_Off offset = 0;
  Dwarf_Off old_offset;
  Dwarf_Die cudie_mem;
  size_t hsize;
  Dwarf_Files **files;
  size_t *nfiles;
  size_t cu_cnt;
  
  // Allocate Dwarf_Files for each compile unit
  cu_cnt = 0;
  while (dwarf_nextcu(DWARF_POINTER, old_offset = offset, &offset, 
		      &hsize, NULL, NULL, NULL) == 0) {
    cu_cnt += 1;
  }
  files = (Dwarf_Files**)alloca (cu_cnt * sizeof (Dwarf_Files*));
  nfiles = (size_t*)alloca (cu_cnt * sizeof (size_t));

  // Fill Dwarf_Files
  cu_cnt = 0;
  offset = 0;
  while (dwarf_nextcu(DWARF_POINTER, old_offset = offset, &offset, 
		      &hsize, NULL, NULL, NULL) == 0) {
      size_t fcnt = 0;
      Dwarf_Die *cudie = dwarf_offdie (DWARF_POINTER, old_offset + hsize, &cudie_mem);
      if (dwarf_getsrcfiles (cudie, &files[cu_cnt], &fcnt) != 0)
	continue;
      nfiles[cu_cnt] = fcnt;
      cu_cnt += 1;
    }
  
  // Allocate JArray for each source file
  size_t entry_cnt = 0;
  for (size_t cu = 0 ; cu < cu_cnt; cu += 1)
    for (size_t f = 0; f < nfiles[cu]; f += 1)
      entry_cnt += 1;
  jnixx::array<String> jfiles
    = jnixx::array<String>::NewObjectArray(env, entry_cnt);

  // Fill JArray
  size_t j = 0;
  for (size_t cu = 0 ; cu < cu_cnt; cu += 1) {
    for (size_t f = 0; f < nfiles[cu]; f += 1) {
      const char *file = dwarf_filesrc (files[cu], f, NULL, NULL);
      String jfile = String::NewStringUTF(env, file);
      jfiles.SetObjectArrayElement(env, j, jfile);
      jfile.DeleteLocalRef(env);
      j += 1;
    }
  }
  return jfiles;
}

jint 
lib::dwfl::Dwarf::dwarf_end(jnixx::env env){
  return ::dwarf_end(DWARF_POINTER);
}
