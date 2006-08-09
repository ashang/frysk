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

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <stdlib.h>

#include <gcj/cni.h>

#include "frysk/sys/proc/cni/slurp.hxx"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/Stat.h"

jboolean
frysk::sys::proc::Stat::refresh (jint procPid)
{
  char buf[BUFSIZ];
  int bufLen = slurp (procPid, "stat", buf, sizeof buf);
  if (bufLen < 0)
    return false;
  
  const char* p = buf;

  pid = scanJint (&p);

  // The "comm" needs special treatment, need to scan backwards for
  // ')' as the command itself could contain ')'.
  char* commStart = ::strchr (buf, '(');
  char* commEnd = ::strrchr (buf, ')');
  if (commStart == NULL || commEnd == NULL)
    throwRuntimeException ("botched comm field");
  comm = JvNewStringLatin1 (commStart + 1, commEnd - commStart - 1);

  // Messy, its a character, need to first skip any white space.
  p = commEnd + 1;
  p += ::strspn (p, " ");
  state = *p++;

  ppid = scanJint (&p);
  pgrp = scanJint (&p);
  session = scanJint (&p);
  ttyNr = scanJint (&p);
  tpgid = scanJint (&p);
  flags = scanJlong (&p);
  minflt = scanJlong (&p);
  cminflt = scanJlong (&p);
  majflt = scanJlong (&p);
  cmajflt = scanJlong (&p);
  utime = scanJlong (&p);
  stime = scanJlong (&p);
  cutime = scanJlong (&p);
  cstime = scanJlong (&p);
  priority = scanJlong (&p);
  nice = scanJint (&p);
  zero = scanJint(&p);
  irealvalue = scanJlong (&p);
  starttime = scanJlong (&p);
  vsize = scanJlong (&p);
  rss = scanJlong (&p);
  rlim = scanJlong (&p);
  startcode = scanJlong (&p);
  endcode = scanJlong (&p);
  startstack = scanJlong (&p);
  kstkesp = scanJlong (&p);
  kstkeip = scanJlong (&p);
  signal = scanJlong (&p);
  blocked = scanJlong (&p);
  sigignore = scanJlong (&p);
  sigcatch = scanJlong (&p);
  wchan = scanJlong (&p);
  nswap = scanJlong (&p);
  cnswap = scanJlong (&p);
  exitSignal = scanJint (&p);
  processor = scanJint (&p);
  return true;
}
