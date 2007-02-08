// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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


#define _GNU_SOURCE
#include <string.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <signal.h>
#include <sys/wait.h>
#include <errno.h>

static int
child ()
{
  pid_t pid = fork ();
  switch (pid) {
  case 0: //child
    exit (0);
  case -1:
    perror ("fork");
    exit (1);
  default:
    return pid;
  }
}

static void
sigchld_handler (int sig)
{
  printf ("SIGCHLD received\n");
  exit (0);
}

int
main ()
{
  int err;

  printf ("\n\
\n\
This program tests to see if any pending SIGCHLD signal is removed\n\
after all pending WAITPID events have been drained.\n\
\n\
A SIGCHLD handler is installed, and then SIGCHLD is blocked.  A\n\
number of children are created and they exit generating both\n\
WAITPID events and SIGCHLD events.  The status of pending SIGCHLD\n\
is then checked.\n\
");

  printf ("\n-> Install the SIGCHLD handler.\n");
  struct sigaction act = { };
  act.sa_handler = sigchld_handler;
  errno = 0;
  sigaction (SIGCHLD, &act, NULL);
  err = errno;
  perror ("sigaction");

  printf ("\n-> Mask SIGCHLD, to keep things pending.\n");
  sigset_t set;
  sigemptyset (&set);
  sigaddset (&set, SIGCHLD);
  errno = 0;
  sigprocmask (SIG_BLOCK, &set, NULL);
  err = errno;
  perror ("sigprocmask");

  printf ("\n-> Create a children.\n");
  child ();

  printf ("\n-> Reap all exit status.\n");
  int status;
  do {
    errno = 0;
    waitpid (-1, &status, 0);
    err = errno;
    perror ("waitpid");
  } while (err == 0);

  printf ("\n-> Check for pending SIGCHLD, exit on that.\n");
  errno = 0;
  sigpending (&set);
  err = errno;
  perror ("sigpending");
  int pending = sigismember (&set, SIGCHLD);
  printf ("%d (%s) pending? %s\n", SIGCHLD, strsignal (SIGCHLD),
	  pending ? "YES" : "NO");
  if (!pending)
    exit (1);

  printf ("\n-> Unmask the SIGCHLD so it is delivered\n");
  sigprocmask (SIG_UNBLOCK, &set, NULL);
  exit (1);
}
