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
#include <dirent.h>
#include <errno.h>
#include <stdlib.h>

#include <gcj/cni.h>
#include <gnu/gcj/RawData.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/ProcBuilder.h"
#include "frysk/sys/ProcessIdentifier.h"
#include "frysk/sys/ProcessIdentifierFactory.h"
#include "frysk/rsl/Log.h"
#include "frysk/rsl/cni/Log.hxx"

gnu::gcj::RawData*
frysk::sys::proc::ProcBuilder::open (jint pid)
{
  // Get the file name.
  const char *file;
  char tmp[FILENAME_MAX];
  if (pid > 0) {
    if (::snprintf (tmp, sizeof tmp, "/proc/%d/task", (int) pid)
	>= FILENAME_MAX)
      throwRuntimeException ("snprintf: buffer overflow");
    file = tmp;
  }
  else
    file = "/proc";
  return (gnu::gcj::RawData*)  opendir (file);
}

void
frysk::sys::proc::ProcBuilder::scan(gnu::gcj::RawData* rawData, jint pid,
				    frysk::rsl::Log* warning)
{
  DIR* proc = (DIR*) rawData;
  int bad = 1; // something non-ve or 0.

  while (true) {

    // Get the dirent.
    struct dirent *dirent = readdir (proc);
    if (dirent == NULL)
      break;

    // Scan the pid, skip if non-numeric.
    char* end = NULL;
    int id = strtol (dirent->d_name, &end, 10);
    if (end == dirent->d_name)
      continue;

    // Seems some kernels return a dirent containing bad (e.g., 0) or
    // even random entries; report them and then throw an error.
    if (bad <= 0) {
      logf(warning, "/proc/%d/task contained bad pid: %d; skipping %d",
	   (int)pid, bad, id);
    } else if (id <= 0) {
      bad = id;
      logf(warning, "/proc/%d/task contains bad pid: %d", (int)pid, id);
    } else {
      build(frysk::sys::ProcessIdentifierFactory::create(id));
    }
  }

  if (bad <= 0)
    throwRuntimeException("/proc/$$/task contains bad pid", "pid", bad);
}

void
frysk::sys::proc::ProcBuilder::close (gnu::gcj::RawData* rawData)
{
  closedir ((DIR*) rawData);
}
