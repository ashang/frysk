// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include "jni.hxx"
#include "jnixx/exceptions.hxx"

using namespace java::lang;
using namespace frysk::testbed;

static ::jnixx::jbyteArray
getBytes(::jnixx::env env, void *addr, size_t length) {
  ::jnixx::jbyteArray bytes = ::jnixx::jbyteArray::NewByteArray(env, length);
  bytes.SetByteArrayRegion(env, 0, length, (jbyte*) addr);
  return bytes;
}

struct m {
  jbyte byteData;
  jshort shortData;
  jint intData;
  jlong longData;
} memory = { 43, 45, 42, 44 };

jlong
LocalMemory::getDataAddr(::jnixx::env) {
  return (jlong) &memory;
}

::jnixx::jbyteArray
LocalMemory::getDataBytes(::jnixx::env env) {
  return getBytes(env, &memory, sizeof(memory));
}

/**
 * Function used by getCode*(), must be on a single line for __LINE__
 * to work correctly.
 */
extern "C" {
  static jint codeLine() { return __LINE__; }
}

String
LocalMemory::getCodeName(jnixx::env env) {
  return String::NewStringUTF(env, "codeLine");
}

jint
LocalMemory::getCodeLine(::jnixx::env) {
  return codeLine();
}

String
LocalMemory::getCodeFile(::jnixx::env env) {
  return String::NewStringUTF(env, __FILE__);
}

static void*
codeAddr() {
#ifdef __powerpc64__
  return *((void**) codeLine);
#else
  return (void*)&codeLine;
#endif
}

jlong
LocalMemory::getCodeAddr(::jnixx::env) {
  return (jlong)codeAddr();
}

::jnixx::jbyteArray
LocalMemory::getCodeBytes(::jnixx::env env) {
  return getBytes(env, codeAddr(), sizeof(memory));
}

void
LocalMemory::constructStack(::jnixx::env env,
					    LocalMemory$StackBuilder builder) {
  // Copy known data onto the stack.
  uint8_t addr[sizeof(memory)];
  memcpy(addr, &memory, sizeof(memory));
  ::jnixx::jbyteArray bytes = getBytes(env, addr, sizeof(memory));
  builder.stack(env, (jlong)addr, bytes);
}

String
LocalMemory::getModuleName(::jnixx::env env) {
  return String::NewStringUTF(env, "libfrysk-sys-jni.so");
}
