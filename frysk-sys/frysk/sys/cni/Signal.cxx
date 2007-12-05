// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

#include <sys/types.h>
#include <signal.h>
#include <linux/unistd.h>
#include <errno.h>
#include <stdio.h>

#include <gcj/cni.h>

#include "frysk/sys/Signal.h"
#include "frysk/sys/cni/Errno.hxx"
#include <linux.syscall.h>
#include <unistd.h>

#define SIGVAL(sig, SIG)				\
  jint							\
  frysk::sys::Signal::sig() {				\
    return (jint)(SIG);					\
  }
SIGVAL(alrm,SIGALRM)
SIGVAL(bus,SIGBUS)
SIGVAL(chld,SIGCHLD)
SIGVAL(cont,SIGCONT)
SIGVAL(fpe,SIGFPE)
SIGVAL(hup,SIGHUP)
SIGVAL(ill,SIGILL)
SIGVAL(int_,SIGINT)
SIGVAL(io,SIGIO)
SIGVAL(kill,SIGKILL)
SIGVAL(none,0)
SIGVAL(prof,SIGPROF)
SIGVAL(pwr,SIGPWR)
SIGVAL(segv,SIGSEGV)
SIGVAL(stop,SIGSTOP)
SIGVAL(term,SIGTERM)
SIGVAL(trap,SIGTRAP)
SIGVAL(usr1,SIGUSR1)
SIGVAL(usr2,SIGUSR2)
SIGVAL(urg,SIGURG)
SIGVAL(winch,SIGWINCH)

void
frysk::sys::Signal::tkill(jint tid, jint signum) {
  errno = 0;
  if (::syscall (__NR_tkill, tid, signum) < 0)
    throwErrno (errno, "tkill", "task %d", (int)tid);
}

void
frysk::sys::Signal::kill(jint pid, jint signum) {
  errno = 0;
  if (::kill (pid, signum) < 0)
    throwErrno (errno, "kill", "process %d", (int)pid);
}

void
frysk::sys::Signal::drain (jint signum) {
//   sigset_t set;
//   sigpending (&set);
//   printf ("Before: %d (%s) pending? %s\n", signum, strsignal (signum),
// 	  sigismember (&set, signum) ? "YES" : "NO");
  struct sigaction oldAct = { };
  struct sigaction newAct = { };
  newAct.sa_handler = SIG_IGN;
  if (::sigaction (signum, &newAct, &oldAct))
    throwErrno (errno, "sigaction", "signal %d - %s", (int)signum,
		strsignal(signum));
  if (::sigaction (signum, &oldAct, NULL))
    throwErrno (errno, "sigaction", "signal %d - %s", (int)signum,
		strsignal(signum));
//   sigpending (&set);
//   printf ("After: %d (%s) pending? %s\n", signum, strsignal (signum),
// 	  sigismember (&set, signum) ? "YES" : "NO");
}
