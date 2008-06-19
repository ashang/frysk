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

#include <libdwfl.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawData.h>

#include "lib/dwfl/Dwfl.h"
#include "lib/dwfl/DwarfDie.h"
#include "lib/dwfl/DwarfDieFactory.h"
#include "lib/dwfl/DwflModule.h"

#include "inua/eio/Buffer.h"
#include "inua/eio/ByteBuffer.h"

using namespace java::lang;

// Suck in elf_from_remote_memory from elfutils

extern "C"
{
  extern ::Elf *elf_from_remote_memory (GElf_Addr ehdr_vma,
					GElf_Addr *loadbasep,
					ssize_t (*read_memory) (void *arg,
								void *data,
								GElf_Addr address,
								size_t minread,
								size_t maxread),
					void *arg);
}
#define DWFL_POINTER ((::Dwfl *) pointer)
#define DWFL_CALLBACKS ((::Dwfl_Callbacks *) callbacks)

static ssize_t
read_proc_memory (void *arg, void *data, GElf_Addr address,
		  size_t minread, size_t maxread)
{
  inua::eio::ByteBuffer* memory = (inua::eio::ByteBuffer *) arg;
  jbyteArray bytes = JvNewByteArray(maxread);
  ssize_t nread = memory->safeGet((off64_t) address, bytes, 0, maxread);
  memcpy(data, elements(bytes), nread);
  if (nread > 0 && (size_t) nread < minread)
    nread = 0;
  return nread;
}

int
dwfl_frysk_proc_find_elf (Dwfl_Module *mod,
			  void **userdata,
			  const char *module_name, Dwarf_Addr base,
			  char **file_name, Elf **elfp)
{	
  // There is an edge case here that was tripped by a corefile
  // case. In that case the specified executable was defined as a
  // relative path (ie ../foo/bar). And that is perfectly valid path
  // name. However when the corefile created its maps it did not
  // convert that path to an absolute path, causing the test below to
  // fail and consider the file ../foo/bar to be an in memory elf
  // image.
  if (module_name[0] == '/') {
    // Pass back the file name and let dwfl take care of the rest.
    *file_name = strdup (module_name);
    return -1;
  } else {
      *elfp = elf_from_remote_memory (base, NULL, &read_proc_memory, *userdata);
      if (*elfp != NULL) {
	*file_name = ::strdup(module_name);
      }
      return -1;
    }
}

jlong
lib::dwfl::Dwfl::dwfl_userdata_begin(inua::eio::ByteBuffer *memory) {
  return (jlong)memory;
}

void
lib::dwfl::Dwfl::dwfl_userdata_end(jlong userdata) {
}

jlong
lib::dwfl::Dwfl::dwfl_callbacks_begin(jstring debugInfoPath) {
  char** path = (char**) JvMalloc(sizeof (char*));
  int len = JvGetStringUTFLength(debugInfoPath);
  *path = (char*)JvMalloc(len + 1);
  JvGetStringUTFRegion(debugInfoPath, 0, len, *path);
  (*path)[len] = '\0';
  jlong callbacks = (jlong) JvMalloc(sizeof(Dwfl_Callbacks));
  if (DWFL_CALLBACKS == 0) {
    return 0;
  }
  DWFL_CALLBACKS->find_elf = &::dwfl_frysk_proc_find_elf;
  DWFL_CALLBACKS->find_debuginfo = &::dwfl_standard_find_debuginfo;
  DWFL_CALLBACKS->debuginfo_path = path;
  return callbacks;
}

void
lib::dwfl::Dwfl::dwfl_callbacks_end(jlong callbacks) {
  JvFree(*DWFL_CALLBACKS->debuginfo_path);
  JvFree(DWFL_CALLBACKS->debuginfo_path);
  JvFree(DWFL_CALLBACKS);
}

jlong
lib::dwfl::Dwfl::dwfl_begin(jlong callbacks) {
  return (jlong) ::dwfl_begin(DWFL_CALLBACKS);
}

void
lib::dwfl::Dwfl::dwfl_end(jlong pointer){
  ::dwfl_end(DWFL_POINTER);
}

void
lib::dwfl::Dwfl::dwfl_report_begin(jlong pointer) {
  ::dwfl_report_begin(DWFL_POINTER);
}

void
lib::dwfl::Dwfl::dwfl_report_end(jlong pointer) {
  ::dwfl_report_end(DWFL_POINTER, NULL, NULL);
}


jlong
lib::dwfl::Dwfl::dwfl_report_module(jlong pointer, jstring moduleName,
				    jlong low, jlong high, jlong userdata) {
  jsize len = JvGetStringUTFLength(moduleName);
  char modName[len+1]; 
	
  JvGetStringUTFRegion(moduleName, 0, len, modName);
  modName[len] = '\0';
  
  Dwfl_Module* module = ::dwfl_report_module(DWFL_POINTER, modName,
					     (::Dwarf_Addr) low, 
					     (::Dwarf_Addr) high);
  if (userdata != 0) {
    // Get the address of the module's userdata, and store a global
    // reference to memory in there.  The global ref is detroyed along
    // with the Dwfl.
    void **userdatap;
    ::dwfl_module_info(module, &userdatap, NULL, NULL, NULL, NULL, NULL, NULL);
    *userdatap = (void*)userdata;
  }
  return (jlong) module;
}

extern "C" int moduleCounter(Dwfl_Module *, void **, const char *,
			     Dwarf_Addr, void *arg)
{
  int *iarg = (int *)arg;
  (*iarg)++;
  return DWARF_CB_OK;
}

typedef JArray<lib::dwfl::DwflModule *> DwflModuleArray;
struct ModuleAdderData
{
  lib::dwfl::Dwfl *dwfl;
  DwflModuleArray *moduleArray;
  int index;
};
