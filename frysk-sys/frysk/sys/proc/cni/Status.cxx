// This file is part of the program FRYSK.
//
// Copyright 2006, 2008, Red Hat Inc.
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

#include <stdlib.h>
#include <ctype.h>
#include <stdio.h>

#include <gcj/cni.h>

#include "frysk/sys/proc/cni/slurp.hxx"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/proc/Status.h"
#include "frysk/rsl/Log.h"
#include "frysk/rsl/cni/Log.hxx"
#include "java/lang/String.h"

static bool
scan(const char** p, jint* val, const char* prefix) {
  (*p) = strstr((*p), prefix);
  if ((*p) == NULL)
    return false;
  (*p) += strlen(prefix);
  char *endp;
  (*val) = strtol((*p), &endp, 10);
  if ((*p) == endp)
    return false;
  return true;
}

static frysk::sys::proc::Status*
scan(const char *p, frysk::sys::proc::Status* const status,
     frysk::rsl::Log* const fine) {
  // Clear everything
  status->state = '\0';
  status->stoppedState = false;
  status->uid = -1;
  status->gid = -1;

  // STATE (SUBSTATE)
  const char *state = "\nState:";
  p = strstr(p, state);
  if (p == NULL)
    return NULL;
  p += strlen(state);
  for (; (*p) != '\r' && (*p) != '\0'; p++) {
    char c = (*p);
    if (isspace(c))
      continue;
    if (strchr("RSDZTW", c) != NULL) {
      status->state = c;
      logf(fine, "state '%c'", c);
      const char *stopped = " (stopped)";
      status->stoppedState = strncmp(p + 1, stopped, strlen(stopped)) == 0;
      logf(fine, "stopped %s", status->stoppedState ? "true" : "false");
      break;
    }
  }
  if (state == '\0')
    return NULL;

  // UID
  if (!scan(&p, &status->uid, "\nUid:"))
    return NULL;
  logf(fine, "uid %d", (int) status->uid);

  // GID
  if (!scan(&p, &status->gid, "\nGid:"))
    return NULL;
  logf(fine, "gid %d", (int) status->gid);

  return status;
}

frysk::sys::proc::Status*
frysk::sys::proc::Status::scan(jint pid) {
  char buf[BUFSIZ];
  int bufLen = slurp(pid, "status", buf, sizeof buf);
  if (bufLen < 0)
    return NULL;
  return ::scan(buf, this, fine);
}

frysk::sys::proc::Status*
frysk::sys::proc::Status::scan(jbyteArray buf) {
  return ::scan((char*)elements(buf), this, fine);
}

