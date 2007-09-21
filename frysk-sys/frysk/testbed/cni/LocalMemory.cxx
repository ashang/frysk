// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

jlong
frysk::testbed::LocalMemory::getByteDataAddr ()
{
  return (jlong) &byteData;
}
jlong
frysk::testbed::LocalMemory::getShortDataAddr ()
{
  return (jlong) &shortData;
}
jlong
frysk::testbed::LocalMemory::getIntDataAddr ()
{
  return (jlong) &intData;
}
jlong
frysk::testbed::LocalMemory::getLongDataAddr ()
{
  return (jlong) &longData;
}

jlong
frysk::testbed::LocalMemory::getDataAddr ()
{
  return (jlong) &byteData;
}

jbyteArray
frysk::testbed::LocalMemory::getBytes (jlong addr, jint length)
{
  uint8_t *start = (uint8_t*)addr;
  jbyteArray bytes = JvNewByteArray (length);
  memcpy (elements (bytes), start, bytes->length);
  return bytes;
}

/**
 * Function used by getCode*(), must be on a single line for __LINE__
 * to work correctly.
 */
jint frysk::testbed::LocalMemory::getCodeLine () { return __LINE__; }

jstring
frysk::testbed::LocalMemory::getCodeFile ()
{
  return JvNewStringUTF (__FILE__);
}

jlong
frysk::testbed::LocalMemory::getCodeAddr ()
{
#ifdef __powerpc64__
  return *((jlong*) frysk::testbed::LocalMemory::getCodeLine);
#else
  return (jlong) frysk::testbed::LocalMemory::getCodeLine;
#endif
}

void
frysk::testbed::LocalMemory::constructStack(frysk::testbed::LocalMemory$StackBuilder* builder)
{
  uint8_t addr[SIZE];
  uint8_t *start = (uint8_t*)frysk::testbed::LocalMemory::getDataAddr();
  // Copy the known data to the stack.
  memcpy (addr, start, SIZE);
  jbyteArray bytes = frysk::testbed::LocalMemory::getBytes((jlong)addr, SIZE);
  builder->stack((jlong)addr, bytes);
}
