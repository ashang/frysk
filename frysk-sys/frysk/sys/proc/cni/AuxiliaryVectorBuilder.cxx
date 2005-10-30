// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

#include "frysk/sys/proc/cni/slurp.hxx"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/AuxiliaryVectorBuilder.h"

/**
 * Extract the value.
 *
 * XXX: As an unsigned!?
 */

static int64_t
get32 (char *b)
{
  return (*(uint32_t*)b);
}
static int64_t
get64 (char *b)
{
  return (*(uint64_t*)b);
}

// Verify the auxiliary vector is both correctly sized, and contains
// only reasonable entries.
static bool
verify (char* buf, int bufLen, int wordSize, int64_t (*get) (char*))
{
  if (bufLen % (wordSize * 2) != 0)
    return false;
  for (char* p = buf; p < buf + bufLen; p += wordSize * 2) {
    int64_t type = get (p);
    if (type > 1024 || type < 0)
      return false;
  }
  return true;
}

jboolean
frysk::sys::proc::AuxiliaryVectorBuilder::constructAuxv (jint pid)
{
  char buf[BUFSIZ];
  int bufLen = slurp (pid, "auxv", buf, sizeof buf);
  if (bufLen <= 0)
    return false;
  
  // Figure out the word-size of the auxv.  It is assumed that that
  // this process, and the auxv have at least a common byte order.
  int64_t (*get) (char* buf) = NULL;
  jint wordSize = 0;
  if (verify (buf, bufLen, 4, get32)) {
    if (verify (buf, bufLen, 8, get64)) {
      throwRuntimeException ("conflicting word sizes for auxv");
    }
    else {
      wordSize = 4;
      get = get32;
    }
  }
  else {
    if (verify (buf, bufLen, 8, get64)) {
      wordSize = 8;
      get = get64;
    }
    else {
      throwRuntimeException ("unknown word size for auxv");
    }
  }
  int length = bufLen / wordSize / 2;
  buildDimensions (wordSize, length);

  // Unpack the corresponding entries.
  for (int i = 0; i < length; i++) {
    char* p = buf + wordSize * i * 2;
    jint type = get (p + wordSize * 0);
    jlong value = get (p + wordSize * 1);
    buildAuxiliary (i, type, value);
  }

  return true;
}
