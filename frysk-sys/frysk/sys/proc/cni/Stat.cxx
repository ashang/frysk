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

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/Stat.h"

// Scan a jint, throw an error if there's a problem.
static jint
scanJint (char** p, int fd)
{
  char *pp;
  jint tmp = ::strtoul (*p, &pp, 0);
  if (*p == pp) {
    ::close (fd);
    throwRuntimeException ("strtoul");
  }
  return tmp;
}

// Scan a jlong, throw an error if there's a problem.
static jlong
scanJlong (char** p, int fd)
{
  char *pp;
  jlong tmp = ::strtoull (*p, &pp, 0);
  if (*p == pp) {
    ::close (fd);
    throwRuntimeException ("strtoul");
  }
  return tmp;
}

jboolean
frysk::sys::proc::Stat::refresh (jint procPid)
{
  // Get the file name.
  char fileName[FILENAME_MAX];
  if (::snprintf (fileName, sizeof fileName, "/proc/%d/stat", (int) procPid)
      >= FILENAME_MAX)
    throwRuntimeException ("snprintf: buffer overflow");
  
  // Open the stat file.
  errno = 0;
  int fd = ::open (fileName, O_RDONLY);
  if (errno != 0)
    return false;

  // Read in the entire stat file, NUL terminate the string.
  char buf[BUFSIZ];
  errno = 0;
  int bufLen = ::read (fd, buf, sizeof (buf));
  if (errno != 0) {
    ::close (fd);
    return false;
  }
  if (bufLen >= BUFSIZ - 1) {
    ::close (fd);
    throwRuntimeException ("read: buffer overflow");
  }
  buf[bufLen] = '\0';
  
  char* p = buf;

  pid = scanJint (&p, pid);

  // The "comm" needs special treatment, need to scan backwards for
  // ')' as the command itself could contain ')'.
  char* commStart = ::strchr (buf, '(');
  char* commEnd = ::strrchr (buf, ')');
  if (commStart == NULL || commEnd == NULL) {
    ::close (fd);
    throwRuntimeException ("botched comm field");
  }
  comm = JvNewStringLatin1 (commStart + 1, commEnd - commStart - 1);

  // Messy, its a character, need to first skip any white space.
  p = commEnd + 1;
  p += ::strspn (p, " ");
  state = *p++;

  ppid = scanJint (&p, fd);
  pgrp = scanJint (&p, fd);
  session = scanJint (&p, fd);
  ttyNr = scanJint (&p, fd);
  tpgid = scanJint (&p, fd);
  flags = scanJlong (&p, fd);
  minflt = scanJlong (&p, fd);
  cminflt = scanJlong (&p, fd);
  majflt = scanJlong (&p, fd);
  cmajflt = scanJlong (&p, fd);
  utime = scanJlong (&p, fd);
  stime = scanJlong (&p, fd);
  cutime = scanJlong (&p, fd);
  cstime = scanJlong (&p, fd);
  priority = scanJlong (&p, fd);
  nice = scanJint (&p, fd);
  irealvalue = scanJlong (&p, fd);
  starttime = scanJlong (&p, fd);
  vsize = scanJlong (&p, fd);
  rss = scanJlong (&p, fd);
  rlim = scanJlong (&p, fd);
  startcode = scanJlong (&p, fd);
  endcode = scanJlong (&p, fd);
  startstack = scanJlong (&p, fd);
  kstkesp = scanJlong (&p, fd);
  kstkeip = scanJlong (&p, fd);
  signal = scanJlong (&p, fd);
  blocked = scanJlong (&p, fd);
  sigignore = scanJlong (&p, fd);
  sigcatch = scanJlong (&p, fd);
  wchan = scanJlong (&p, fd);
  nswap = scanJlong (&p, fd);
  cnswap = scanJlong (&p, fd);
  exitSignal = scanJint (&p, fd);
  processor = scanJint (&p, fd);

  close (fd);
  return true;
}
