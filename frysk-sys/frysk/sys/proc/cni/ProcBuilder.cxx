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
#include <java/lang/RuntimeException.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/ProcBuilder.h"
#include "frysk/sys/ProcessIdentifier.h"
#include "frysk/sys/ProcessIdentifierFactory.h"
#include "frysk/rsl/Log.h"
#include "frysk/rsl/cni/Log.hxx"

void
frysk::sys::proc::ProcBuilder::construct(jint pid, frysk::rsl::Log* warning) {
  // Open the directory
  DIR *proc;
  {
    const char *dir;
    char tmp[FILENAME_MAX];
    if (pid > 0) {
      if (::snprintf(tmp, sizeof tmp, "/proc/%d/task", (int) pid)
	  >= FILENAME_MAX)
	throwRuntimeException("snprintf: buffer overflow");
      dir = tmp;
    } else {
      dir = "/proc";
    }
    proc = ::opendir(dir);
    if (proc == NULL) {
      // If access isn't possible then there are no entries.
      return;
    }
  }

  // Scan the directory tree.
  while (true) {
    // Get the dirent.
    struct dirent *dirent = readdir(proc);
    if (dirent == NULL) {
      break;
    }
    // Scan the pid, skip if non-numeric.
    char* end = NULL;
    int id = strtol(dirent->d_name, &end, 10);
    if (end == dirent->d_name) {
      continue;
    }
    // Seems some kernels return a dirent containing bad (e.g., 0) or
    // even random entries; report them and then throw an error.
    if (id <= 0) {
      logf(warning, "/proc/%d/task contained bad pid: %d", (int)pid, id);
      break;
    }

    try {
      build(frysk::sys::ProcessIdentifierFactory::create(id));
    } catch (java::lang::RuntimeException *e) {
      ::closedir(proc);
      throw e;
    }
  }

  // Open the directory.
  ::closedir (proc);
}
