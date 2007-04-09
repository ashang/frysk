// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

#include <alloca.h>
#include <errno.h>
#include <linux/unistd.h>
#include <setjmp.h>
#include <signal.h>
#include <stdio.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include "linux.ptrace.h"

#include <gcj/cni.h>

#include <java/util/logging/Logger.h>
#include <java/util/logging/Level.h>
#include "frysk/sys/Errno.h"
#include "frysk/sys/Errno$Esrch.h"
#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/PtraceServer.h"
#include "frysk/sys/Wait.h"
#include "frysk/sys/Sig.h"
#include "frysk/sys/cni/SignalSet.hxx"
#include "frysk/sys/SignalSet.h"
#include "frysk/sys/WaitBuilder.h"
#include "frysk/sys/SignalBuilder.h"

/* Unpack the WSTOPEVENT status field.

   With one exception (WIFSTOPPED), the STATUS can be decoded using
   the standard WIFxxx macros described in wait(2).

   For WIFSTOPPED, there is an additional undocumented STOPEVENT field
   that when non-zero contains an event sub-category.  The kernel uses
   a call to ptrace_notify() to pack the status, and the packed format
   is ((STOPEVENT << 16) | ((STOPSIG & 0xff) << 8) | (IFSTOPPED)).  */

#define WSTOPEVENT(STATUS) (((STATUS) & 0xff0000) >> 16)

/* Decode and log a waitpid result, but only when logging.  */

static void
log (java::util::logging::Logger *logger, pid_t pid, int status, int err)
{
  if (!(logger->isLoggable(java::util::logging::Level::FINEST)))
    return;
  if (pid > 0) {
    const char *wif_name = "<unknown>";
    int sig = -1;
    const char *sig_name = "<unknown>";
    if (WIFEXITED (status)) {
      wif_name = "WIFEXITED";
      sig = WEXITSTATUS (status);
      sig_name = "exit status";
    }
    if (WIFSTOPPED (status)) {
      switch WSTOPEVENT (status) {
      case PTRACE_EVENT_CLONE:
	wif_name = "WIFSTOPPED/CLONE";
	break;
      case PTRACE_EVENT_FORK:
	wif_name = "WIFSTOPPED/FORK";
	break;
      case PTRACE_EVENT_EXIT:
	wif_name = "WIFSTOPPED/EXIT";
	break;
      case PTRACE_EVENT_EXEC:
	wif_name = "WIFSTOPPED/EXEC";
	break;
      case 0:
	wif_name = "WIFSTOPPED";
	break;
      }
      sig = WSTOPSIG (status);
      sig_name = strsignal (sig);
    }
    if (WIFSIGNALED (status)) {
      wif_name = "WIFSIGNALED";
      sig = WTERMSIG (status);
      sig_name = strsignal (sig);
    }
    logFinest (&frysk::sys::Wait::class$, logger,
	       "frysk.sys.Wait pid %d status 0x%x %s %d (%s)\n",
	       pid, status, wif_name, sig, sig_name);
  }
  else
    logFinest (&frysk::sys::Wait::class$, logger,
	       "frysk.sys.Wait pid %d errno %d (%s)\n",
	       pid, err, strerror (err));
}

/* Decode a wait status notification using the WIFxxx macros,
   forwarding the decoded event, and its corresponding parameters, to
   the applicable observer.

   For certain of the WIFSTOPPED sub-events, the kernel will save an
   additional undocumented auxilary value (exit code, ...) in the
   per-thread current->ptrace_message field, and the value can be
   fetched using PTRACE_GETEVENTMSG.  The details of the auxilary
   values are described below.  */

static void
processStatus (int pid, int status,
	       frysk::sys::WaitBuilder* builder)
{
  if (0)
    ;
  else if (WIFEXITED (status))
    builder->terminated (pid, false, WEXITSTATUS (status), false);
  else if (WIFSIGNALED (status))
    builder->terminated (pid, true, WTERMSIG (status), WCOREDUMP (status));
  else if (WIFSTOPPED (status)) {
    switch (WSTOPEVENT (status)) {
    case PTRACE_EVENT_CLONE:
      try {
	// The event message contains the thread-ID of the new clone.
	jint clone = (jint) frysk::sys::PtraceServer::getEventMsg (pid);
	builder->cloneEvent (pid, clone);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify builder.
	builder->disappeared (pid, err);
      }
      break;
    case PTRACE_EVENT_FORK:
      try {
	// The event message contains the process-ID of the new
	// process.
	jlong fork = frysk::sys::PtraceServer::getEventMsg (pid);
	builder->forkEvent (pid, fork);
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify builder.
	builder->disappeared (pid, err);
      }
      break;
    case PTRACE_EVENT_EXIT:
      try {
	// The event message contains the pending wait(2) status; need
	// to decode that.
	int exitStatus = frysk::sys::PtraceServer::getEventMsg (pid);
	if (WIFEXITED (exitStatus)) {
	  builder->exitEvent (pid, false, WEXITSTATUS (exitStatus),
			       false);
	}
	else if (WIFSIGNALED (exitStatus)) {
	  builder->exitEvent (pid, true, WTERMSIG (exitStatus),
			       WCOREDUMP (exitStatus));
	}
	else {
	  throwRuntimeException ("unknown exit event", "status", exitStatus);
	}
      } catch (frysk::sys::Errno$Esrch *err) {
	// The PID disappeared after the WAIT message was created but
	// before the getEventMsg could be extracted (most likely due
	// to a KILL -9).  Notify builder.
	builder->disappeared (pid, err);
      }
      break;
    case PTRACE_EVENT_EXEC:
      builder->execEvent (pid);
      break;
    case 0:
      {
	int signum = WSTOPSIG (status);
	if (signum >= 0x80)
	  builder->syscallEvent (pid);
	else
	  builder->stopped (pid, signum);
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
frysk::sys::Wait::waitAllNoHang (frysk::sys::WaitBuilder* builder)
{
  java::util::logging::Logger *logger = getLogger ();
  struct WaitResult {
    pid_t pid;
    int status;
    WaitResult* next;
  };
  WaitResult* head = (WaitResult* ) alloca (sizeof (WaitResult));
  WaitResult* tail = head;

  // Drain the waitpid queue of all its events storing each in a list
  // on the stack.  The queue is fully drained _before_ it is
  // processed, that way there is no possibility of a continued thread
  // getting its next event back on the queue resulting in live lock.

  int myErrno = 0;
  int i = 0;
  while (true) {
    // Keep fetching the wait status until there are none left.  If
    // there are no children ECHILD is returned which is ok.
    errno = 0;
    tail->pid = ::waitpid (-1, &tail->status, WNOHANG | __WALL);
    myErrno = errno;
    log (logger, tail->pid, tail->status, errno);
    if (tail->pid <= 0)
      break;
    tail->next = (WaitResult*) alloca (sizeof (WaitResult));
    tail = tail->next;
    i++;
  }
  if (i > 2001)
    printf ("\tYo! There were %d simultaneous pending waitpid's!\n", i);
  // Check the reason for exiting.
  switch (myErrno) {
  case 0:
  case ECHILD:
    break;
  default:
    throwErrno (myErrno, "waitpid", "process", -1);
  }

  // Now unpack each, notifying the builder.
  /* We need to keep track of the status of the previous item in this queue
   * since some items are duplicated when waitpit() is called from a 
   * multithreaded parent - see #2774 */
  pid_t old_pid = -2;
  int old_status = 0;
  while (head != tail) {
    // Process the result - check for a duplicate entry
    if (old_pid != head->pid || old_status != head->status)
      processStatus (head->pid, head->status, builder);
    old_pid = head->pid; old_status = head->status;
    head = head->next;
  }
}

/* Do a blocking wait.  */

void
frysk::sys::Wait::waitAll (jint wpid, frysk::sys::WaitBuilder* builder)
{
  int status;
  errno = 0;
  pid_t pid = ::waitpid (wpid, &status, __WALL);
  int myErrno = errno;
  log (getLogger(), pid, status, errno);
  if (pid <= 0)
    throwErrno (myErrno, "waitpid", "process", wpid);
  // Process the result.
  processStatus (pid, status, builder);
}

/** Drain wait events.  */

void frysk::sys::Wait::drain (jint wpid)
{
  while (1) {
    int status;
    errno = 0;
    pid_t pid = ::waitpid (wpid, &status, __WALL);
    int err = errno;
    log (getLogger (), pid, status, err);
    if (err == ESRCH || err == ECHILD)
      break;
    if (pid <= 0)
      throwErrno (err, "waitpid", "process", wpid);
  }
}

void frysk::sys::Wait::drainNoHang (jint wpid)
{
  while (1) {
    int status;
    errno = 0;
    pid_t pid = ::waitpid (wpid, &status, __WALL| WNOHANG);
    int err = errno;
    log (getLogger (), pid, status, err);
    if (err == ESRCH || err == ECHILD)
      break;
    if (pid <= 0)
      throwErrno (err, "waitpid", "process", wpid);
  }
}

// A linked list of received events.  This is stored on the stack
// using alloca.
struct event {
  pid_t pid;
  int status;
  event* next;
};

// If there's a signal abort the wait() function using a longjmp
// (return the signal).  Store which signal was received in SIGNALS.
// Only bail when STATUS == -1 implying that the waitpid call hasn't
// yet run.

struct wait_jmpbuf {
  pid_t tid;
  int status;
  sigset_t signals;
  sigjmp_buf buf;
};
static struct wait_jmpbuf wait_jmpbuf;

static void
waitInterrupt (int signum)
{
  // For what ever reason, the signal can come in on the wrong thread.
  // When that occures, re-direct it (explicitly) to the thread that
  // can handle the signal.
  pid_t me = ::syscall (__NR_gettid);
  if (wait_jmpbuf.tid == me) {
    sigaddset (&wait_jmpbuf.signals, signum);
    if (wait_jmpbuf.status == -1) {
      // waitpid hasn't returned a meaningful result, abort call.
      siglongjmp (wait_jmpbuf.buf, signum);
    }
  }
  else {
    // XXX: Want to edit this thread's mask so that from now on it
    // blocks this signal, don't know a way to do it though.
    ::syscall (__NR_tkill, wait_jmpbuf.tid, signum);
  }
}

void
frysk::sys::Wait::signalEmpty ()
{
  // Static CNI methods do not trigger a class to be initialized, work
  // around it.
  if (signalSet == NULL)
    signalSet = new frysk::sys::SignalSet ();
  // Note that this doesn't restore any signal handlers.
  signalSet->empty ();
  // Disable and mask SIGALRM
  signal (SIGALRM, SIG_IGN);
  sigset_t mask;
  sigemptyset (&mask);
  sigaddset (&mask, SIGALRM);
  sigprocmask (SIG_BLOCK, &mask, NULL);
}


void
frysk::sys::Wait::signalAdd (frysk::sys::Sig* sig)
{
  java::util::logging::Logger *logger = frysk::sys::Wait::getLogger ();
  // Get the hash code.
  int signum = sig->hashCode ();
  logFinest (&frysk::sys::Wait::class$, logger,
	     "adding %d (%s)\n", signum, strsignal (signum));
  // Add it to the signal set.
  signalSet->add (sig);
  // Make certain that the signal is masked (this is ment to be
  // process wide).  XXX: In a multi-threaded environment this call is
  // not well defined (although it does help reduce the number of
  // signals directed to the wrong thread).
  sigset_t mask;
  sigemptyset (&mask);
  sigaddset (&mask, signum);
  sigprocmask (SIG_BLOCK, &mask, NULL);
  // Install the above signal handler (it long jumps back to the code
  // that enabled the signal).  The handler's mask is set to F..F to
  // ensure that the handler can't be pre-empted with another signal.
  struct sigaction sa;
  memset (&sa, 0, sizeof (sa));
  sa.sa_handler = waitInterrupt;
  sigfillset (&sa.sa_mask);
  sigaction (signum, &sa, NULL);
}

void
frysk::sys::Wait::waitAll (jlong millisecondTimeout,
			   frysk::sys::WaitBuilder* waitBuilder,
			   frysk::sys::SignalBuilder* signalBuilder)
{
  java::util::logging::Logger *logger = frysk::sys::Wait::getLogger ();
  // Zero the existing timeout, and drain any pending SIGALRM
  logFinest (&frysk::sys::Wait::class$, logger,
	     "flush old timeout & SIGALRM\n");
  struct itimerval timeout;
  memset (&timeout, 0, sizeof (timeout));
  setitimer (ITIMER_REAL, &timeout, NULL);
  signal (SIGALRM, SIG_IGN);

  // Set up a new timeout and it's handler.
  logFinest (&frysk::sys::Wait::class$, logger,
	     "install new timeout & SIGALRM\n");
  if (millisecondTimeout > 0) {
    struct sigaction alarm_action;
    memset (&alarm_action, 0, sizeof (alarm_action));
    alarm_action.sa_handler = waitInterrupt;
    sigfillset (&alarm_action.sa_mask);
    sigaction (SIGALRM, &alarm_action, NULL);
    timeout.it_value.tv_sec = millisecondTimeout / 1000;
    timeout.it_value.tv_usec = (millisecondTimeout % 1000) * 1000;
    setitimer (ITIMER_REAL, &timeout, NULL);
  }

  // Get the signal mask of all allowed signals; clear the set of
  // received signals.  Need to include SIGALRM.
  sigset_t mask = *getRawSet (signalSet);
  sigaddset (&mask, SIGALRM);

  // Set the STATUS to something that waitpid() will not return.  When
  // a waitpid() system call completes successfully STATUS gets
  // changed, and waitInterrupt() can use that to detect the success.
  wait_jmpbuf.status = -1;
  sigemptyset (&wait_jmpbuf.signals);
 
  // Establish a jump buf so that, when a signal is delivered while
  // entering the blocking waitpid call, the waitpid call sequence can
  // be interrupted and control be returned to here.
  bool block = (millisecondTimeout != 0);
  wait_jmpbuf.tid = ::syscall (__NR_gettid);
  int signum = sigsetjmp (wait_jmpbuf.buf, 1);
  if (signum > 0) {
    // Interrupted by SIGNUM, disable further blocking.  When multiple
    // signals are pending, each will cause a longjmp back to here.
    sigdelset (&mask, signum);
    block = false;
  }

  // Unmask signals; from this point on, things can be interrupted.
  errno = ::pthread_sigmask (SIG_UNBLOCK, &mask, 0);
  if (errno != 0)
    throwErrno (errno, "pthread_sigmask.UNBLOCK");

  // A signal delivered here will see that STATUS==-1, indicating that
  // the waitpid() call has not completed successfully, will long-jump
  // back to the above sigsetjmp causing the waitpid() call to be
  // aborted.

  int pid = 0;
  if (waitBuilder != NULL) {
    pid = ::waitpid (-1, &wait_jmpbuf.status,
			__WALL | (block ? 0 : WNOHANG));
    if (pid < 0 && errno == ECHILD && block)
      // No children; block anyway.
      pid = ::select (0, NULL, NULL, NULL, NULL);
  }
  else if (block) {
    pid = ::select (0, NULL, NULL, NULL, NULL);
  }
  if (pid < 0)
    pid = -errno;

  // A signal delivered here that sees STATUS!=-1, indicating that
  // waitpid() has completed and is in the process of returning the
  // PID, will return normally allowing this code to resume and ensure
  // that the waitpid event is not lost.

  // A signal delivered here that sees STATUS==-1, indicating that
  // either the waitpid() call failed (no result) or select() was
  // called, will long-jump back to the above sigsetjmp and cause any
  // error status to be abandoned.

  errno = ::pthread_sigmask (SIG_BLOCK, &mask, NULL);
  if (errno != 0)
    throwErrno (errno, "pthread_sigmask.BLOCK");

  // Made it all the way through a [blocking] waitpid call without
  // being restarted.
  log (logger, pid, wait_jmpbuf.status, -pid);

  // Create a linked list of the waitpid events that are received;
  // keep it on the stack to avoid malloc() overhead.
  struct event* firstEvent = NULL;
  if (pid > 0) {
    // Save the first waitpid status.
    firstEvent = (struct event*) alloca (sizeof (struct event));
    firstEvent->pid = pid;
    firstEvent->status = wait_jmpbuf.status;
    firstEvent->next = NULL;
    struct event* lastEvent = firstEvent;
    // Do more waitpid calls with blocking disabled so that all
    // pending waitpid events are collected.
    while (true) {
      int status;
      pid = ::waitpid (-1, &status, __WALL|WNOHANG);
      log (logger, pid, status, errno);
      if (pid <= 0)
	break;
      if (pid == lastEvent->pid && status == lastEvent->status) {
	// When an attached child process terminats, it generates two
	// identical waitpid events: one for the parent thread; and one
	// for the attached thread.  When the parent and attached
	// threads are different but in the same process, both events
	// are seen.  Discard the duplicate.
	continue;
	logFinest (&frysk::sys::Wait::class$, logger,
		   "discarding duplicate terminated event for pid %d\n", pid);
      }
      // Append the event.
      lastEvent->next = (struct event*) alloca (sizeof (struct event));
      lastEvent = lastEvent->next;
      lastEvent->pid = pid;
      lastEvent->status = status;
      lastEvent->next = NULL;
    }
  }

  // Deliver any signals received during the waitpid; XXX: Is there a
  // more efficient way of doing this?
  for (int i = 1; i < 32; i++) {
    if (i != SIGALRM && sigismember (&wait_jmpbuf.signals, i)) {
      // Find the signal object.
      frysk::sys::Sig* sig = frysk::sys::Sig::valueOf (i);
      // Notify the client of the signal.
      signalBuilder->signal (sig);
    }
  }

  // Deliver all pending waitpid() events.
  struct event *curr;
  for (curr = firstEvent; curr != NULL; curr = curr->next) {
    processStatus (curr->pid, curr->status, waitBuilder);
  }
}
