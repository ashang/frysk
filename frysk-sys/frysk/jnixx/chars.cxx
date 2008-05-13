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

#include <malloc.h>

#include "jni.hxx"
#include "frysk/jnixx/chars.hxx"

using namespace java::lang;

char**
strings2chars(jnixx::env env, ::jnixx::array<String> strings) {
  jsize arrayLength = strings.GetLength(env);
  // compute the allocated size.
  size_t size = 0;
  size += sizeof(void*); // NULL
  for (int i = 0; i < arrayLength; i++) {
    size += sizeof(void*); // pointer
    String string = strings.GetElement(env, i);
    size += string.GetStringUTFLength(env); // chars
    size += 1; // NUL
    string.DeleteLocalRef(env);
  }
  // Create the array.
  char **elements = (char**) ::malloc(size);
  char **argv = elements;
  // Store strings after the array
  char *arg = (char*) (argv + arrayLength + 1);
  // Copy
  for (int i = 0; i < arrayLength; i++) {
    *argv++ = arg;
    String string = strings.GetElement(env, i);
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
      = ::jnixx::array<String>::New(env, length);
  for (int i = 0; i < length; i++) {
    String string = String::NewStringUTF(env, argv[i]);
    strings.SetElement(env, i, string);
    string.DeleteLocalRef(env);
  }
  return strings;
}
