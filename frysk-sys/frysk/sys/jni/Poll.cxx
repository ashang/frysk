// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

#include <malloc.h>
#include <string.h>
#include <sys/poll.h>
#include <setjmp.h>
#include <signal.h>
#include <alloca.h>
#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <linux.syscall.h>
#include <sys/syscall.h>

#include "jni.hxx"

#include "jnixx/exceptions.hxx"
#include "frysk/sys/jni/SignalSet.hxx"

using namespace java::lang;
using namespace frysk::sys;

// If there's a signal abort the wait() function using a longjmp (and
// return the signal).  Should the jmpbuf be per-thread?

struct poll_jmpbuf {
  pid_t tid;
  sigjmp_buf buf;
};
struct poll_jmpbuf poll_jmpbuf;

static void
handler(int signum, siginfo_t *siginfo, void *context) {
  // For what ever reason, the signal can come in on the wrong thread.
  // When that occures, re-direct it (explicitly) to the thread that
  // can handle the signal.
  pid_t me = ::syscall(SYS_gettid);
  if (poll_jmpbuf.tid == me) {
#if 0
    fprintf (stderr, "pid %d got signal %d (%s) from %d\n",
	     me, siginfo->si_signo, strsignal (siginfo->si_signo),
	     siginfo->si_pid);
#endif
    siglongjmp (poll_jmpbuf.buf, signum);
  }
  else
    // XXX: Want to edit this thread's mask so that from now on it
    // blocks this signal, don't know a way to do it though.
    ::syscall(SYS_tkill, poll_jmpbuf.tid, signum);
}

void
Poll::addSignalHandler(jnixx::env env, Signal sig) {
  int signum = sig.hashCode(env);
  // Make certain that the signal is masked (this is ment to be
  // process wide).
  sigset_t mask;
  sigemptyset(&mask);
  sigaddset(&mask, signum);
  // XXX: In a multi-threaded environment this call is not well
  // defined (although it does help reduce the number of signals
  // directed to the wrong thread).
  sigprocmask(SIG_BLOCK, &mask, NULL);
  // Install the above signal handler (it long jumps back to the code
  // that enabled the signal).  To avoid potential recursion, all
  // signals are masked while the handler is running.
  struct sigaction sa;
  memset(&sa, 0, sizeof (sa));
  sa.sa_sigaction = handler;
  sa.sa_flags = SA_SIGINFO;
  sigfillset(&sa.sa_mask);
  sigaction(signum, &sa, NULL);
}



jlong
Poll$Fds::malloc(jnixx::env env) {
  // Allocate a non-empty buffer, marked with a sentinel.
  struct pollfd* fds = (struct pollfd*) ::malloc (sizeof (struct pollfd));
  fds->fd = -1; // sentinel
  return (jlong)(long) fds;
}

void
Poll$Fds::free(jnixx::env env, jlong fds) {
  ::free((struct pollfd*)(long)fds);
}

static jlong
addPollFd(jlong pollFds, int fd, short event) {
  struct pollfd* ufds = (struct pollfd*) pollFds;
  // If the FD is alreay listed, just add the event; end up with a
  // count of fds.
  int numFds;
  for (numFds = 0; ufds[numFds].fd >= 0; numFds++) {
    if (ufds[numFds].fd == fd) {
      ufds[numFds].events |= event;
      return pollFds;
    }
  }
  // Create space for the new fd (and retain space for the sentinel).
  ufds = (struct pollfd*) ::realloc(ufds, (numFds + 2) * sizeof (struct pollfd));
  ufds[numFds + 0].fd = fd;
  ufds[numFds + 0].events = event;
  ufds[numFds + 1].fd = -1;
  return (jlong) (long) ufds;
}

jlong
Poll$Fds::addPollIn(jnixx::env env, jlong fds, jint fd) {
  return addPollFd(fds, fd, POLLIN);
}



void
Poll::poll(jnixx::env env, PollBuilder pollObserver, jlong timeout) {
  // Compute the current number of poll fds.
  struct pollfd* fds = (struct pollfd*)GetPollFds(env).GetFds(env);
  int numFds;
  for (numFds = 0; fds[numFds].fd >= 0; numFds++);

  // Set up a SIGSETJMP call that jumps back to here when any watched
  // signal is delivered.  The signals are accumulated in a sigset,
  // and removed from the current of signals being unmasked, and the
  // timer is set to zero forcing a non-blocking poll.

  sigset_t signals;
  sigemptyset(&signals);
  sigset_t mask = *getRawSet(env, GetSignalSet(env));
  int signum = sigsetjmp(poll_jmpbuf.buf, 1);
  if (signum > 0) {
    // Remove the signal from the local copy of the signal-mask set,
    // doing this allows other signals to get through (otherwize this
    // code could be swamped by a single re-occuring signal).
    sigdelset(&mask, signum);
    // Add it to those that have fired.
    sigaddset(&signals, signum);
    // Make the poll non-blocking.  Now that at least one event has
    // been detected, this method should not block instead returning
    // immediatly after the file descriptors have been polled.
    timeout = 0;
  }

  // Unblock signals, and then wait for an event.  There is a window
  // between the unmask and poll system calls during which a signal
  // could be delivered that doesn't interrupt the poll call.  Avoid
  // this race by having the signal handler longjmp back to the above
  // setjmp, re-starting this code, forcing the poll (even if it
  // wasn't reached) to be canceled.

  poll_jmpbuf.tid = ::syscall(SYS_gettid);
  errno = ::pthread_sigmask (SIG_UNBLOCK, &mask, 0);
  if (errno != 0)
    errnoException(env, errno, "pthread_sigmask.UNBLOCK");
  int status = ::poll (fds, numFds, timeout);
  if (status < 0)
    status = -errno; // Save the errno across the next system call.
  errno = ::pthread_sigmask (SIG_BLOCK, &mask, NULL);
  if (errno != 0)
    errnoException(env, errno, "pthread_sigmask.BLOCK");

  // Did something go wrong?
  if (status < 0) {
    switch (-status) {
    case EINTR:
      break;
    default:
      errnoException(env, -status, "poll");
    }
  }

  // Deliver any signals received during the poll; XXX: Is there a
  // more efficient way of doing this?

  for (int i = 1; i < 32; i++) {
    if (sigismember (&signals, i)) {
      // Find the signal object.
      Signal sig = Signal::valueOf(env, i);
      // Notify the client of the signal.
      pollObserver.signal(env, sig);
    }
  }

  // Did a file descriptor fire, status when +ve, contains the number
  // of file descriptors that fired one or more events.

  for (int i = 0; i < numFds; i++) {
    if (status <= 0)
      // bail early
      break;
    if (fds[i].revents != 0) {
      if (fds[i].revents & POLLIN)
	pollObserver.pollIn(env, fds[i].fd);
      status--;
    }
  }
}
