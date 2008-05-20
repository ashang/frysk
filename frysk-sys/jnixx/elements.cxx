// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

#include <malloc.h> // for realloc
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"
#include "jnixx/exceptions.hxx"

using namespace java::lang;

char**
strings2chars(jnixx::env env, ::jnixx::array<String> strings) {
  jsize arrayLength = strings.GetArrayLength(env);
  // compute the allocated size.
  size_t size = 0;
  size += sizeof(void*); // NULL
  for (int i = 0; i < arrayLength; i++) {
    size += sizeof(void*); // pointer
    String string = strings.GetObjectArrayElement(env, i);
    size += string.GetStringUTFLength(env); // chars
    size += 1; // NUL
    string.DeleteLocalRef(env);
  }
  // Create the array.
  char **elements = (char**) new char[size];
  char **argv = elements;
  // Store strings after the array
  char *arg = (char*) (argv + arrayLength + 1);
  // Copy
  for (int i = 0; i < arrayLength; i++) {
    *argv++ = arg;
    String string = strings.GetObjectArrayElement(env, i);
    int utfLength = string.GetStringUTFLength(env);
    string.GetStringUTFRegion(env, 0, string.GetStringLength(env), arg);
    arg += utfLength;
    string.DeleteLocalRef(env);
    *arg++ = '\0';
  }
  *argv = NULL;
  return elements;
}

::jnixx::array<String>
chars2strings(::jnixx::env env, char** argv) {
  int length = 0;
  for (char **p = argv; *p != NULL; p++) {
    length++;
  }
  ::jnixx::array<String> strings
      = ::jnixx::array<String>::NewObjectArray(env, length);
  for (int i = 0; i < length; i++) {
    String string = String::NewStringUTF(env, argv[i]);
    strings.SetObjectArrayElement(env, i, string);
    string.DeleteLocalRef(env);
  }
  return strings;
}

void
slurp(jnixx::env env, FileBytes& bytes, const char* file) {
  // Attempt to open the file.
  int fd = ::open(file, O_RDONLY);
  if (fd < 0) {
    errnoException(env, errno, "open", "file %s", file);
  }

  // Initially allocate space for two BUFSIZE reads (and an extra
  // char).  It appears that when reading /proc/<pid>/maps, and the
  // like, the reads are limited to at most a transfer of BUFSIZE
  // bytes (i.e., a short read does not indicate EOF).  Hence two
  // reads are needed to confirm EOF.  Allocating 2&BUFSIZE ensures
  // that there's always space for at least two reads.  Ref SW #3370
  jsize allocated = BUFSIZ * 2 + 1;
  bytes.elements = (jbyte*) ::malloc(allocated);
  if (bytes.elements == NULL) {
    errnoException(env, errno, "malloc");
  }

  bytes.length = 0;
  while (true) {
    // Attempt to fill the remaining buffer; less space for a
    // terminating NUL character.
    int size = ::read(fd, bytes.elements + bytes.length,
		      allocated - bytes.length - 1);
    if (size < 0) {
      ::close(fd);
      bytes.release();
      // Abandon the read with elements == NULL.
      return;
    } else if (size == 0) {
      break;
    }
    bytes.length += size;

    if (bytes.length + BUFSIZ >= allocated) {
      // Not enough space for the next ~BUFSIZ'd read; expand the
      // buffer.  Don't trust realloc with the pointer; will need to
      // free the old buffer if something goes wrong.
      allocated += BUFSIZ;
      jbyte *tmp = (jbyte*)::realloc(bytes.elements, allocated);
      if (tmp == NULL) {
	int err = errno;
	::close(fd);
	bytes.release();
	errnoException(env, err, "realloc");
      }
      bytes.elements = tmp;
    }
  }

  ::close(fd);

  // Null terminate the buffer.
  bytes.elements[bytes.length] = '\0';
  bytes.length++; // count the trailing NUL
}

FileBytes::FileBytes(jnixx::env env, const char* fmt, ...) {
  // Convert the string into a file.
  char file[FILENAME_MAX];
  va_list ap;
  va_start(ap, fmt);
  if (::vsnprintf(file, sizeof file, fmt, ap) >= FILENAME_MAX) {
    errnoException(env, errno, "vsnprintf");
  }
  va_end(ap);
  slurp(env, *this, file);
}

FileBytes::FileBytes(jnixx::env env, int pid, const char* name) {
  // Convert the string into a file.
  char file[FILENAME_MAX];
  if (::snprintf(file, sizeof file, "/proc/%d/%s", pid, name) >= FILENAME_MAX) {
    errnoException(env, errno, "snprintf");
  }
  slurp(env, *this, file);
}

FileBytes::FileBytes(jnixx::env env, int pid, int tid, const char* name) {
  // Convert the string into a file.
  char file[FILENAME_MAX];
  if (::snprintf(file, sizeof file, "/proc/%d/task/%d/%s", pid, tid, name)
      >= FILENAME_MAX) {
    errnoException(env, errno, "snprintf");
  }
  slurp(env, *this, file);
}

void
FileBytes::release() {
  if (elements != NULL) {
    ::free(elements);
    elements = NULL;
  }
}
