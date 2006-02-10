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

#include <sys/poll.h>
#include <setjmp.h>
#include <signal.h>
#include <alloca.h>
#include <errno.h>
#include <pthread.h>
#include <stdio.h>

#include <gcj/cni.h>
#include <gnu/gcj/RawDataManaged.h>

#include "frysk/sys/cni/Errno.hxx"
#include "frysk/sys/Tid.h"
#include "frysk/sys/Poll.h"
#include "frysk/sys/Poll$Fds.h"
#include "frysk/sys/Poll$SignalSet.h"
#include "frysk/sys/Poll$Observer.h"


// If there's a signal abort the wait() function using a longjmp (and
// return the signal).  Should the jmpbuf be per-thread?

struct poll_jmpbuf {
  bool p;
  sigjmp_buf buf;
};
__thread struct poll_jmpbuf poll_jmpbuf;

static void
handler (int signum)
{
  if (!poll_jmpbuf.p)
    throwRuntimeException ("frysk.sys.Poll: bad jmpbuf", "tid",
			   frysk::sys::Tid::get ());
  siglongjmp (poll_jmpbuf.buf, signum);
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
  int signum = sigsetjmp (poll_jmpbuf.buf, 1);
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

  // Unblock signals, and then wait for an event.  During the period
  // after the unmask, but before the poll call, signals could be
  // delivered but missed.  Avoid the problem by having the signal
  // handler longjmp back to above re-starting this code and forcing
  // the poll (even if it wasn't reached) to be canceled.
  poll_jmpbuf.p = true;
  pthread_sigmask (SIG_UNBLOCK, &mask, 0);
  errno = 0;
  int status = ::poll ((struct pollfd*)pollFds->fds, pollFds->numFds, timeout);
  if (status < 0)
    status = -errno; // Save the errno.
  pthread_sigmask (SIG_BLOCK, &mask, NULL);
  poll_jmpbuf.p = false;

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
