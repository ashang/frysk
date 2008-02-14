// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2008, Red Hat Inc.
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
#include "frysk/rsl/Log.h"
#include "frysk/rsl/cni/Log.hxx"
#include "frysk/sys/ProcessIdentifier.h"
#include "frysk/sys/ProcessIdentifierFactory.h"

static frysk::sys::proc::Stat*
scan(const char*p, frysk::sys::proc::Stat* const stat, frysk::rsl::Log* fine) {
  // The "comm" needs special treatment, need to scan backwards for
  // ')' as the command itself could contain ')'.
  char* commStart = ::strchr (p, '(');
  char* commEnd = ::strrchr (p, ')');
  if (commStart == NULL || commEnd == NULL)
    throwRuntimeException ("botched comm field");
  stat->comm = JvNewStringLatin1 (commStart + 1, commEnd - commStart - 1);

  stat->pid = frysk::sys::ProcessIdentifierFactory::create(scanJint(&p));
  log(fine, stat, "pid", stat->pid);

  // Messy, its a character, need to first skip any white space.
  p = commEnd + 1;
  p += ::strspn (p, " ");
  stat->state = *p++;
  logf(fine, stat, "state %c", (char) stat->state);

  stat->ppid = frysk::sys::ProcessIdentifierFactory::create(scanJint(&p));
  log(fine, stat, "ppid", stat->ppid);

  stat->pgrp = scanJint (&p);
  stat->session = scanJint (&p);
  stat->ttyNr = scanJint (&p);
  stat->tpgid = scanJint (&p);
  stat->flags = scanJlong (&p);
  stat->minflt = scanJlong (&p);
  stat->cminflt = scanJlong (&p);
  stat->majflt = scanJlong (&p);
  stat->cmajflt = scanJlong (&p);
  stat->utime = scanJlong (&p);
  stat->stime = scanJlong (&p);
  stat->cutime = scanJlong (&p);
  stat->cstime = scanJlong (&p);
  stat->priority = scanJlong (&p);
  stat->nice = scanJint (&p);
  stat->numThreads = scanJint(&p);
  stat->irealvalue = scanJlong (&p);
  stat->starttime = scanJlong (&p);
  stat->vsize = scanJlong (&p);
  stat->rss = scanJlong (&p);
  stat->rlim = scanJlong (&p);
  stat->startcode = scanJlong (&p);
  stat->endcode = scanJlong (&p);
  stat->startstack = scanJlong (&p);
  stat->kstkesp = scanJlong (&p);
  stat->kstkeip = scanJlong (&p);
  stat->signal = scanJlong (&p);
  stat->blocked = scanJlong (&p);
  stat->sigignore = scanJlong (&p);
  stat->sigcatch = scanJlong (&p);
  stat->wchan = scanJlong (&p);
  stat->nswap = scanJlong (&p);
  stat->cnswap = scanJlong (&p);
  stat->exitSignal = scanJint (&p);
  stat->processor = scanJint (&p);
  return stat;
}

frysk::sys::proc::Stat*
frysk::sys::proc::Stat::scan(jint procPid, jint threadTid) {
  char buf[BUFSIZ];
  int bufLen = slurp_thread (procPid, threadTid, "stat", buf, sizeof buf);
  if (bufLen < 0)
    return NULL;
  return ::scan(buf, this, fine);
}

frysk::sys::proc::Stat*
frysk::sys::proc::Stat::scan(jint procPid) {
  char buf[BUFSIZ];
  int bufLen = slurp (procPid, "stat", buf, sizeof buf);
  if (bufLen < 0)
    return NULL;
  return ::scan(buf, this, fine);
}

frysk::sys::proc::Stat*
frysk::sys::proc::Stat::scan(jbyteArray buf) {
  return ::scan((char*)elements(buf), this, fine);
}
