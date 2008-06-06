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

#include <gcj/cni.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/testbed/LocalMemory.h"
#include "frysk/testbed/LocalMemory$StackBuilder.h"

using namespace frysk::testbed;

static jbyteArray
getBytes(void *addr, size_t length) {
  jbyteArray bytes = JvNewByteArray(length);
  memcpy(elements (bytes), addr, length);
  return bytes;
}

struct m {
  jbyte byteData;
  jshort shortData;
  jint intData;
  jlong longData;
} memory = { 43, 45, 42, 44 };

jlong
LocalMemory::getDataAddr() {
  return (jlong) &memory;
}

jbyteArray
LocalMemory::getDataBytes() {
  return getBytes(&memory, sizeof(memory));
}

/**
 * Function used by getCode*(), must be on a single line for __LINE__
 * to work correctly.
 */
extern "C" {
  static jint codeLine() { return __LINE__; }
}

jstring
LocalMemory::getCodeName() {
  return JvNewStringUTF("codeLine");
}

jint
LocalMemory::getCodeLine() {
  return codeLine();
}

jstring
LocalMemory::getCodeFile() {
  return JvNewStringUTF (__FILE__);
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
LocalMemory::getCodeAddr() {
  return (jlong)codeAddr();
}

jbyteArray
LocalMemory::getCodeBytes() {
  return getBytes(codeAddr(), sizeof(memory));
}

void
LocalMemory::constructStack(LocalMemory$StackBuilder* builder) {
  // Copy known data onto the stack.
  uint8_t addr[sizeof(memory)];
  memcpy(addr, &memory, sizeof(memory));
  jbyteArray bytes = getBytes(addr, sizeof(memory));
  builder->stack((jlong)addr, bytes);
}

jstring
LocalMemory::getModuleName() {
  // XXX: Should account for build-tree TestRunner vs install tree
  // funit, but not important as this CNI implementation will shortly
  // be deleted.
  return JvNewStringUTF("TestRunner");
}
