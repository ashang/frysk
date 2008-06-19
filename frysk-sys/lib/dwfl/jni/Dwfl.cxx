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
#include <string.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

// Suck in elf_from_remote_memory from elfutils

extern "C" {
  extern ::Elf *elf_from_remote_memory(GElf_Addr ehdr_vma,
				       GElf_Addr *loadbasep,
				       ssize_t (*read_memory) (void *arg,
							       void *data,
							       GElf_Addr address,
							       size_t minread,
							       size_t maxread),
				       void *arg);
}

// Grub the pointer out of the object; replace this with ...
#define DWFL_POINTER_FIXME ((::Dwfl *)GetPointer(env))

// Assume the method was parameterised with POINTER.
#define DWFL_POINTER ((::Dwfl *)pointer)



// Our data associated with the dwfl (well actually the Dwfl_Module).
// Need to make memory available so that, in the case of the vdso, it
// can be slurped from memory.

static ssize_t
read_proc_memory(void *userdata, void *data, GElf_Addr address,
		 size_t minread, size_t maxread) {
  // Get the current thread's ENV; can't save it in dwfl_userdata
  // since can't determine, ahead of time, which thread will call this
  // code.
  ::jnixx::env env = Object::_env_();
  ::inua::eio::ByteBuffer memory
      = ::inua::eio::ByteBuffer(env, (jobject)userdata);
  jnixx::jbyteArray bytes = jnixx::jbyteArray::NewByteArray(env, maxread);
  ssize_t nread = memory.safeGet(env, (off64_t) address, bytes, 0, maxread);
  jbyteArrayElements bytesp = jbyteArrayElements(env, bytes);
  memcpy(data, bytesp.elements(), nread);
  if (nread > 0 && (size_t) nread < minread)
    nread = 0;
  bytes.DeleteLocalRef(env);
  return nread;
}

static int
dwfl_frysk_proc_find_elf(Dwfl_Module *mod,
			 void **userdata,
			 const char *module_name, Dwarf_Addr base,
			 char **file_name, Elf **elfp) {	
  // There is an edge case here that was tripped by a corefile case.
  // In that case the specified executable was defined as a relative
  // path (ie ../foo/bar). And that is perfectly valid path name.
  // However when the corefile created its maps it did not convert
  // that path to an absolute path, causing the test below to fail and
  // consider the file ../foo/bar to be an in memory elf image.
  if (module_name[0] == '/') {
    int fd = ::open64(module_name, O_RDONLY);
    if (fd >= 0) {
      *file_name = ::strdup(module_name);
      if (*file_name == NULL) {
	::close(fd);
	return ENOMEM;
      }
    }
    return fd;
  } else {
    // dwfl passes in the address of the Dwfl_Module user pointer
    // contained within.  That pointer has been previously stuffed
    // with our "userdata".
    *elfp = elf_from_remote_memory (base, NULL, &read_proc_memory, *userdata);
    return 0;
  }
}

jlong
lib::dwfl::Dwfl::dwfl_userdata_begin(jnixx::env env,
				     inua::eio::ByteBuffer memory) {
  return (jlong) env.NewGlobalRef(memory._object);
}

void
lib::dwfl::Dwfl::dwfl_userdata_end(jnixx::env env, jlong userdata) {
  env.DeleteGlobalRef((jobject)userdata);
}



#define DWFL_CALLBACKS ((::Dwfl_Callbacks*)callbacks)

jlong
lib::dwfl::Dwfl::dwfl_callbacks_begin(jnixx::env env, String jdebugInfoPath) {
  jstringUTFChars debugInfoPath = jstringUTFChars(env, jdebugInfoPath);
  char** path = (char**) ::malloc(sizeof (char*));
  if (path == NULL) {
    return 0;
  }
  *path = ::strdup(debugInfoPath.elements());
  jlong callbacks = (jlong) ::malloc(sizeof(Dwfl_Callbacks));
  ::memset(DWFL_CALLBACKS, 0, sizeof(Dwfl_Callbacks));
  DWFL_CALLBACKS->find_elf = &::dwfl_frysk_proc_find_elf;
  DWFL_CALLBACKS->find_debuginfo = &::dwfl_standard_find_debuginfo;
  DWFL_CALLBACKS->debuginfo_path = path;
  return callbacks;
}

void
lib::dwfl::Dwfl::dwfl_callbacks_end(jnixx::env env, jlong callbacks) {
  ::free(*DWFL_CALLBACKS->debuginfo_path);
  ::free(DWFL_CALLBACKS->debuginfo_path);
  ::free(DWFL_CALLBACKS);
}

jlong
lib::dwfl::Dwfl::dwfl_begin(jnixx::env env, jlong callbacks) {
  return (jlong) ::dwfl_begin(DWFL_CALLBACKS);
}

void
lib::dwfl::Dwfl::dwfl_end(jnixx::env env, jlong pointer) {
  ::dwfl_end(DWFL_POINTER);
}

void
lib::dwfl::Dwfl::dwfl_report_begin(jnixx::env env, jlong pointer) {
  ::dwfl_report_begin(DWFL_POINTER);
}

void
lib::dwfl::Dwfl::dwfl_report_end(jnixx::env env, jlong pointer) {
  ::dwfl_report_end(DWFL_POINTER, NULL, NULL);
}


jlong
lib::dwfl::Dwfl::dwfl_report_module(jnixx::env env, jlong pointer,
				    String jmoduleName, jlong low, jlong high,
				    jlong userdata) {
  jstringUTFChars moduleName = jstringUTFChars(env, jmoduleName);
  Dwfl_Module *module = ::dwfl_report_module(DWFL_POINTER,
					     moduleName.elements(),
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
  return (jlong)module;
}
