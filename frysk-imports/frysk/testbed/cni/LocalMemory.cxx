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

#include <gcj/cni.h>

#include "frysk/testbed/LocalMemory.h"

jlong
frysk::testbed::LocalMemory::getByteValAddr ()
{
  return (jlong) &byteVal;
}
jlong
frysk::testbed::LocalMemory::getShortValAddr ()
{
  return (jlong) &shortVal;
}
jlong
frysk::testbed::LocalMemory::getIntValAddr ()
{
  return (jlong) &intVal;
}
jlong
frysk::testbed::LocalMemory::getLongValAddr ()
{
  return (jlong) &longVal;
}

/**
 * Function used by getFunc*(), must be on a single line for __LINE__
 * to work correctly.
 */
jint frysk::testbed::LocalMemory::getFuncLine () { return __LINE__; }

jstring
frysk::testbed::LocalMemory::getFuncFile ()
{
  return JvNewStringUTF (__FILE__);
}

jlong
frysk::testbed::LocalMemory::getFuncAddr ()
{
#ifdef __powerpc64__
  return *((jlong*) frysk::testbed::LocalMemory::getFuncLine);
#else
  return (jlong) frysk::testbed::LocalMemory::getFuncLine;
#endif
}

jbyteArray
frysk::testbed::LocalMemory::getFuncBytes ()
{
  char *addr = (char*)frysk::testbed::LocalMemory::getFuncAddr ();
  jbyteArray bytes = JvNewByteArray (4);
  memcpy (elements (bytes), addr, 4);
  return bytes;
}
