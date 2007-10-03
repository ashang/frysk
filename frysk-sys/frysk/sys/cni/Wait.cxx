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

#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>
#include "linux.ptrace.h"

#include <gcj/cni.h>

#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/Ptrace.h"
#include "frysk/sys/Wait.h"
#include "frysk/sys/Wait$Observer.h"

#define WEVENTSTATUS(STATUS) (((STATUS) & 0xff0000) >> 16)

/* Process a wait status notifying any relevant observers.

   The event is determined by extracting bits from the STATUS.  For
   some events an additional event-message (set in the kernel by a
   call to "ptrace_notify" needs to be fetched).  The event is encoded
   in the STATUS as ((EVENT << 8) | WIF) << 8 | SIGNAL).  */

void
processStatus (int pid, int status,
	       frysk::sys::Wait$Observer* observer)
{
  if (0)
    ;
  else if (WIFEXITED (status))
    observer->exited (pid, WEXITSTATUS (status), WCOREDUMP (status));
  else if (WIFSIGNALED (status))
    observer->terminated (pid, WTERMSIG (status), WCOREDUMP (status));
  else if (WIFSTOPPED (status)) {
    switch (WEVENTSTATUS (status)) {
    case PTRACE_EVENT_CLONE:
      try {
	jint msg = (jint) frysk::sys::Ptrace::getEventMsg (pid);
	observer->cloneEvent (pid, msg);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify observer.
	observer->disappeared (pid);
      }
      break;
    case PTRACE_EVENT_FORK:
      try {
	jlong msg = frysk::sys::Ptrace::getEventMsg (pid);
	observer->forkEvent (pid, msg);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify observer.
	observer->disappeared (pid);
      }
      break;
    case PTRACE_EVENT_EXIT:
      observer->exitEvent (pid, frysk::sys::Ptrace::getEventMsg (pid));
      break;
    case PTRACE_EVENT_EXEC:
      observer->execEvent (pid);
      break;
    case 0:
      int signum = WSTOPSIG (status);
      if (signum >= 0x80)
	observer->syscallEvent (pid);
      else
	observer->stopped (pid, signum);
      break;
    default:
      throwRuntimeException ("Unknown waitpid stopped event");
    }
  }
  else
    throwRuntimeException ("Unknown status");
}

/* Keep polling the waitpid queue moving everything to the eventqueue
   until there's nothing left.  */

void
frysk::sys::Wait::waitAllNoHang (frysk::sys::Wait$Observer* observer)
{
  while (true) {
    // Keep fetching the wait status until there are none left.  If
    // there are no children ECHILD is returned which is ok.
    int status;
    errno = 0;
    int pid = ::waitpid (-1, &status, WNOHANG | __WALL);
    if (pid <= 0)
      switch (errno) {
      case 0:
      case ECHILD:
	return;
      default:
	throwErrno (errno, "waitpid", "process", -1);
      }
    // Process the result.
    processStatus (pid, status, observer);
  }
}

/* Do a blocking wait.  */

void
frysk::sys::Wait::waitAll (jint wpid, frysk::sys::Wait$Observer* observer)
{
  int status;
  errno = 0;
  int pid = ::waitpid (wpid, &status, __WALL);
  if (pid <= 0)
    throwErrno (errno, "waitpid", "process", wpid);
  // Process the result.
  processStatus (pid, status, observer);
}
