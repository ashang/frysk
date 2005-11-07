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

/* Decode a wait status notification using the WIFxxx macros,
   forwarding the decoded event, and its corresponding parameters, to
   the applicable observer.

   With one exception (WIFSTOPPED), the STATUS can be decoded using
   the standard WIFxxx macros described in wait(2).

   For WIFSTOPPED, there is an additional undocumented STOPEVENT field
   that when non-zero contains an event sub-category.  The kernel uses
   a call to ptrace_notify() to pack the status, and the packed format
   is ((STOPEVENT << 16) | ((STOPSIG & 0xff) << 8) | (IFSTOPPED)).

   In addition, for certain of the WIFSTOPPED sub-events, the kernel
   will save an additional undocumented auxilary value (exit code,
   ...) in the per-thread current->ptrace_message field, and the value
   can be fetched using PTRACE_GETEVENTMSG.  The details of the
   auxilary values are described below.  */

#define WSTOPEVENT(STATUS) (((STATUS) & 0xff0000) >> 16)

void
processStatus (int pid, int status,
	       frysk::sys::Wait$Observer* observer)
{
  if (0)
    ;
  else if (WIFEXITED (status))
    observer->terminated (pid, false, WEXITSTATUS (status),
			  WCOREDUMP (status));
  else if (WIFSIGNALED (status))
    observer->terminated (pid, true, WTERMSIG (status), WCOREDUMP (status));
  else if (WIFSTOPPED (status)) {
    switch (WSTOPEVENT (status)) {
    case PTRACE_EVENT_CLONE:
      try {
	// The event message contains the thread-ID of the new clone.
	jint clone = (jint) frysk::sys::Ptrace::getEventMsg (pid);
	observer->cloneEvent (pid, clone);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify observer.
	observer->disappeared (pid);
      }
      break;
    case PTRACE_EVENT_FORK:
      try {
	// The event message contains the process-ID of the new
	// process.
	jlong fork = frysk::sys::Ptrace::getEventMsg (pid);
	observer->forkEvent (pid, fork);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify observer.
	observer->disappeared (pid);
      }
      break;
    case PTRACE_EVENT_EXIT:
      try {
	// The event message contains the pending wait(2) status; need
	// to decode that.
	int exitStatus = frysk::sys::Ptrace::getEventMsg (pid);
	if (WIFEXITED (exitStatus)) {
	  observer->exitEvent (pid, false, WEXITSTATUS (exitStatus),
			       WCOREDUMP (exitStatus));
	}
	else if (WIFSIGNALED (exitStatus)) {
	  observer->exitEvent (pid, true, WTERMSIG (exitStatus),
			       WCOREDUMP (exitStatus));
	}
	else {
	  throwRuntimeException ("unknown exit event", "status", exitStatus);
	}
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify observer.
	observer->disappeared (pid);
      }
      break;
    case PTRACE_EVENT_EXEC:
      observer->execEvent (pid);
      break;
    case 0:
      int signum = WSTOPSIG (status);
      if (signum >= 0x80)
	// It's a syscall.  Pass -1 as a parameter to indicate we don't
	// know whether it is an entry or exit event.
	observer->syscallEvent (pid, -1);
      else
	observer->stopped (pid, signum);
      break;
    default:
      throwRuntimeException ("Unknown waitpid stopped event", "process", pid);
    }
  }
  else
    throwRuntimeException ("Unknown status", "process", pid);
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
