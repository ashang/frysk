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
#include <alloca.h>
#include <stdlib.h>
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

static void
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
	observer->disappeared (pid, err);
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
	observer->disappeared (pid, err);
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
	observer->disappeared (pid, err);
      }
      break;
    case PTRACE_EVENT_EXEC:
      observer->execEvent (pid);
      break;
    case 0:
      {
	int signum = WSTOPSIG (status);
	if (signum >= 0x80)
	  observer->syscallEvent (pid);
	else
	  observer->stopped (pid, signum);
      }
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
  struct WaitResult {
    pid_t pid;
    int status;
    WaitResult* next;
  };
  WaitResult* head = (WaitResult* ) malloc (sizeof (WaitResult));
  WaitResult* tail = head;

  // Drain the waitpid queue of all its events storing each in a list
  // on the stack.  The queue is fully drained _before_ it is
  // processed, that way there is no possibility of a continued thread
  // getting its next event back on the queue resulting in live lock.

  int myErrno = 0;
  int unset_status = 0;
  pid_t unset_status_pid = 0;
  int i = 0;
  while (true) {
    // Keep fetching the wait status until there are none left.  If
    // there are no children ECHILD is returned which is ok.
    errno = 0;
    // It's a long shot, but since the status int is pass-by-reference, it's
    // possible that if the kernel is munged up the waitpid could returrn
    // without having set the int.  This will check for that.  Assumes that
    // 0xaaaaaaaa is not a possible value for the status, which I don't know
    // for sure, but it seems unlikely.
    tail->status = 0xaaaaaaaa;
    tail->pid = ::waitpid (-1, &tail->status, WNOHANG | __WALL);
    if ((tail->pid > 0) && (unsigned int)(tail->status) == 0xaaaaaaaa) {
      unset_status = 1;
      unset_status_pid = tail->pid;
    }
    myErrno = errno;
    log (tail->pid, tail->status, errno);
    if (tail->pid <= 0)
      break;
    tail->next = (WaitResult*) malloc (sizeof (WaitResult));
    tail = tail->next;
    i++;
  }
  if (i > 2001)
    printf ("\tYo! There were %d simultaneous pending waitpid's!\n", i);
  if (unset_status)
    printf ("\tYo! waitpid failed to set status on pid %d!\n",
	    (int)unset_status_pid);
  // Check the reason for exiting.
  switch (myErrno) {
  case 0:
  case ECHILD:
    break;
  default:
    throwErrno (myErrno, "waitpid", "process", -1);
  }

  // Now unpack each, notifying the observer.
  /* We need to keep track of the status of the previous item in this queue
   * since some items are duplicated when waitpit() is called from a 
   * multithreaded parent - see #2774 */
  pid_t old_pid = -2;
  int old_status = 0;
  while (head != tail) {
    WaitResult * this_head;
    // Process the result - check for a duplicate entry
    if (old_pid != head->pid || old_status != head->status) 
      processStatus (head->pid, head->status, observer);
    old_pid = head->pid; old_status = head->status;
    this_head = head;
    head = head->next;
    free (this_head);
  }
  free (tail);
}

/* Do a blocking wait.  */

void
frysk::sys::Wait::waitAll (jint wpid, frysk::sys::Wait$Observer* observer)
{
  int status;
  errno = 0;
  pid_t pid = ::waitpid (wpid, &status, __WALL);
  int myErrno = errno;
  log (pid, status, errno);
  if (pid <= 0)
    throwErrno (myErrno, "waitpid", "process", wpid);
  // Process the result.
  processStatus (pid, status, observer);
}
