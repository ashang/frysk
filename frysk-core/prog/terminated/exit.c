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

/* Exits or kills itself.  */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>

int
main (int argc, char *argv[], char *envp[])
{
  if (argc != 2) {
    printf ("Usage: exit [ <status> | -<signal> ]\n\
Either <status> is passed to _exit(2), or <signal> passed to kill(2).\n");
    exit (1);
  }

  struct rlimit rlimit;
  rlimit.rlim_cur = 0;
  rlimit.rlim_max = 0;
  errno = 0;
  if (setrlimit (RLIMIT_CORE, &rlimit) < 0) {
    perror ("setrlimit");
    exit (1);
  }

  int val = atol (argv[1]);
  if (val >= 0) {
    _exit (val);
  }
  else {
    int sig = -val;
    if (sig != SIGKILL) { // Not allowed to change SIGKILL
      // Ensure that the signal is unmasked.
      sigset_t sigset;
      sigemptyset (&sigset);
      sigaddset (&sigset, sig);
      sigprocmask (SIG_UNBLOCK, &sigset, NULL);
      // Ensure that the signal is deliverable.
      if (signal (sig, SIG_DFL) == SIG_ERR) {
	perror ("signal");
	exit (1);
      }
    }
    if (kill (getpid (), sig) < 0) {
      perror ("kill");
      exit (1);
    }
  }

  return 0;
}
