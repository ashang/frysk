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
#include <string.h>

#include "jni.hxx"

#include "jnixx/exceptions.hxx"
#include "jnixx/elements.hxx"
#include "jnixx/scan.hxx"
#include "jnixx/logging.hxx"

using namespace java::lang;
using namespace frysk::sys::proc;

static void
scan(jnixx::env env, const char*p, Stat& stat, frysk::rsl::Log fine) {
  // The "comm" needs special treatment, need to scan backwards for
  // ')' as the command itself could contain ')'.
  char* commStart = ::strchr(p, '(');
  char* commEnd = ::strrchr(p, ')');
  if (commStart == NULL || commEnd == NULL)
    runtimeException(env, "botched comm field");
  int commLength = commEnd - (commStart + 1);
  char comm[commLength + 1];
  strncpy(comm, commStart+1, commLength);
  comm[commLength] = '\0';
  logf(env, fine, "comm %s", comm);
  stat.SetComm(env, String::NewStringUTF(env, comm));

  jint pid = scanJint(env, &p);
  logf(env, fine, "pid %d", pid);
  stat.SetPid(env, frysk::sys::ProcessIdentifierFactory::create(env, pid));

  // Messy, its a character, need to first skip any white space.
  p = commEnd + 1;
  p += ::strspn (p, " ");
  char state = *p++;
  logf(env, fine, "state %c", state);
  stat.SetState(env, state);

  jint ppid = scanJint(env, &p);
  logf(env, fine, "ppid %d", ppid);
  stat.SetPpid(env, frysk::sys::ProcessIdentifierFactory::create(env, ppid));

  stat.SetPgrp(env, scanJint(env, &p));
  stat.SetSession(env, scanJint(env, &p));
  stat.SetTtyNr(env, scanJint(env, &p));
  stat.SetTpgid(env, scanJint(env, &p));
  stat.SetFlags(env, scanJlong(env, &p));
  stat.SetMinflt(env, scanJlong(env, &p));
  stat.SetCminflt(env, scanJlong(env, &p));
  stat.SetMajflt(env, scanJlong(env, &p));
  stat.SetCmajflt(env, scanJlong(env, &p));
  stat.SetUtime(env, scanJlong(env, &p));
  stat.SetStime(env, scanJlong(env, &p));
  stat.SetCutime(env, scanJlong(env, &p));
  stat.SetCstime(env, scanJlong(env, &p));
  stat.SetPriority(env, scanJlong(env, &p));
  stat.SetNice(env, scanJint(env, &p));
  stat.SetNumThreads(env, scanJint(env, &p));
  stat.SetIrealvalue(env, scanJlong(env, &p));
  stat.SetStarttime(env, scanJlong(env, &p));
  stat.SetVsize(env, scanJlong(env, &p));
  stat.SetRss(env, scanJlong(env, &p));
  stat.SetRlim(env, scanJlong(env, &p));
  stat.SetStartcode(env, scanJlong(env, &p));
  stat.SetEndcode(env, scanJlong(env, &p));
  stat.SetStartstack(env, scanJlong(env, &p));
  stat.SetKstkesp(env, scanJlong(env, &p));
  stat.SetKstkeip(env, scanJlong(env, &p));
  stat.SetSignal(env, scanJlong(env, &p));
  stat.SetBlocked(env, scanJlong(env, &p));
  stat.SetSigignore(env, scanJlong(env, &p));
  stat.SetSigcatch(env, scanJlong(env, &p));
  stat.SetWchan(env, scanJlong(env, &p));
  stat.SetNswap(env, scanJlong(env, &p));
  stat.SetCnswap(env, scanJlong(env, &p));
  stat.SetExitSignal(env, scanJint(env, &p));
  stat.SetProcessor(env, scanJint(env, &p));
}

frysk::sys::proc::Stat
frysk::sys::proc::Stat::scan(jnixx::env env, jint procPid, jint threadTid) {
  return scan(env, threadTid);
}

frysk::sys::proc::Stat
frysk::sys::proc::Stat::scan(jnixx::env env, jint procPid) {
  FileBytes bytes = FileBytes(env, procPid, "stat");
  if (bytes.elements() == NULL)
    return Stat(env, NULL);
  ::scan(env, (const char*) bytes.elements(), *this, GetFine(env));
  bytes.release();
  return *this;
}

frysk::sys::proc::Stat
frysk::sys::proc::Stat::scan(jnixx::env env, jnixx::jbyteArray buf) {
  ArrayBytes bytes = ArrayBytes(env, buf);
  ::scan(env, (const char*) bytes.elements(), *this, GetFine(env));
  bytes.release();
  return *this;
}
