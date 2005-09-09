// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

#include <sys/poll.h>
#include <setjmp.h>
#include <signal.h>
#include <alloca.h>
#include <errno.h>
#include <stdio.h>

#include <gcj/cni.h>
#include <gnu/gcj/RawDataManaged.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/Poll.h"
#include "frysk/sys/Poll$Fds.h"
#include "frysk/sys/Poll$SignalSet.h"
#include "frysk/sys/Poll$Observer.h"


// If there's a signal abort the wait() function using a longjmp (and
// return the signal).  Should the jmpbuf be per-thread?

sigjmp_buf poll_env;
static void
handler (int signum)
{
  siglongjmp (poll_env, signum);
}

gnu::gcj::RawDataManaged*
frysk::sys::Poll$SignalSet::get ()
{
  if (signalSet == NULL) {
    sigset_t* sigset = (sigset_t*) JvAllocBytes (sizeof (sigset_t));
    sigemptyset (sigset);
    signalSet = (gnu::gcj::RawDataManaged*) sigset;
  }
  return signalSet;
}

void
frysk::sys::Poll$SignalSet::add (jint signum)
{
  // Make certain that the signal is masked (this is process wide).
  sigset_t mask;
  sigemptyset (&mask);
  sigaddset (&mask, signum);
  sigprocmask (SIG_BLOCK, &mask, NULL);
  // Install the above signal handler (it long jumps back to the code
  // that enabled the signal).  To avoid potential recursion, all
  // signals are masked while the handler is running.
  struct sigaction sa;
  memset (&sa, 0, sizeof (sa));
  sa.sa_handler = handler;
  sigfillset (&sa.sa_mask);
  sigaction (signum, &sa, NULL);
  // Add the signal to the pselect list.
  sigset_t* sigset = (sigset_t*) get ();
  sigaddset (sigset, signum);
}

void
frysk::sys::Poll$SignalSet::empty ()
{
  // Note that this doesn't restore any signal handlers.
  sigset_t* sigset = (sigset_t*) get ();
  sigemptyset (sigset);
}

void
frysk::sys::Poll$Fds::init ()
{
  // Allocate a non-empty buffer, makes life easier.
  numFds = 0;
  fds = (gnu::gcj::RawDataManaged*) JvAllocBytes (sizeof (struct pollfd));
}

static void
addPollFd (gnu::gcj::RawDataManaged* &pollFds, jint &numPollFds,
	   int fd, short event)
{
  struct pollfd* ufds = (struct pollfd*) pollFds;
  // If the FD is alreay listed, just add the event.
  for (int i = 0; i < numPollFds; i++) {
    if (ufds[i].fd == fd) {
      ufds[i].events |= event;
      return;
    }
  }
  // Create space for, and then append a new poll fd.
  struct pollfd* newFds = (struct pollfd*) JvAllocBytes ((numPollFds + 1) * sizeof (struct pollfd));
  memcpy (newFds, ufds, numPollFds * sizeof (struct pollfd));
  newFds[numPollFds].fd = fd;
  newFds[numPollFds].events = event;
  pollFds = (gnu::gcj::RawDataManaged*) newFds;
  numPollFds++;
}

void
frysk::sys::Poll$Fds::addPollIn (jint fd)
{
  addPollFd (fds, numFds, fd, POLLIN);
}



void
frysk::sys::Poll::poll (frysk::sys::Poll$Fds* pollFds,
					     frysk::sys::Poll$Observer* pollObserver,
					     jlong timeout)
{
  // Set up a SIGSETJMP call that jumps back to here when any watched
  // signal is delivered.  This code then notifies the client of the
  // occurance of a signal event, removes the signal from the current
  // set (so that other potential signals can get through), and forces
  // things into a non-blocking poll (this method returns when zero or
  // more events, or a timeout has occured).

  sigset_t mask = *(sigset_t*) frysk::sys::Poll$SignalSet::get ();
  int signum = sigsetjmp (poll_env, 1);
  if (signum > 0) {
    // Remove the signal from the local copy of the signal-mask set,
    // doing this allows other signals to get through (otherwize this
    // code could be swamped by a single re-occuring signal).
    sigdelset (&mask, signum);
    // Notify the client of the signal.
    pollObserver->signal (signum);
    // Make the poll non-blocking.  Now that at least one event has
    // been detected, this method should not block.
    timeout = 0;
  }

  // Wait for an event.  If any sort of signal occures, long jump back
  // to the above which adds the signal to the event queue and then
  // sets things up again (minus that signal).  Wrapped in a SIGSETJMP
  // call which actives on any signal - avoids a race condition where
  // a signal can be missed while entering poll.  Even when there
  // isn't a poll, disable/enable masks - letting a signal pass
  // through.

  int status = 0;

  sigprocmask (SIG_UNBLOCK, &mask, 0);
  errno = 0;
  status = ::poll ((struct pollfd*)pollFds->fds, pollFds->numFds, timeout);
  if (status < 0)
    status = -errno; // Save the errno.
  sigprocmask (SIG_BLOCK, &mask, NULL);

  // Did something go wrong?
  if (status < 0) {
    switch (-status) {
    case EINTR:
      break;
    default:
      throwErrno (-status, "poll");
    }
  }

  // Did a connection fire, status when +ve, contains the number of
  // file descriptors that fired one or more events.
  struct pollfd* pollfd = (struct pollfd*) pollFds;
  while (status > 0) {
    if (pollfd->revents != 0) {
      if (pollfd->revents & POLLIN)
	pollObserver->pollIn (pollfd->fd);
      status--;
    }
    pollfd++;
  }
}
