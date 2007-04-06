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

// Here we fail on PTRACE_DETACH by ESRCH on kernel-2.6.20-1.3045.fc7.x86_64 .
// The test passes on kernel.org linux-2.6.20.4.x86_64 .

#include <unistd.h>
#include <assert.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <error.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <limits.h>
#include <string.h>


static void
child_f (void)
{
  for (;;)
    pause ();
  /* NOTREACHED */
  abort ();
  /* NOTREACHED */
}

static pid_t child_pid;

static void
handler (int signo)
{
  kill (child_pid, SIGKILL);
  //  abort ();
  fprintf (stderr, "timeout\n");
  exit (1);
}

int main (void)
{
  int i;
  int status;
  void (*handler_orig) (int signo);

  child_pid = fork();
  switch (child_pid)
    {
      case -1:
        perror ("fork()");
	exit (1);
	/* NOTREACHED */
      case 0:
        child_f ();
	/* NOTREACHED */
	abort ();
      default:;
	/* PASSTHRU */
    }
  /* Parent.  */

  sleep (1);

  handler_orig = signal (SIGALRM, handler);
  assert (handler_orig == SIG_DFL);
  alarm (1);

  i = ptrace (PTRACE_ATTACH, child_pid, NULL, NULL);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_ATTACH");
  i = waitpid (child_pid, &status, 0);
  //  assert (i == child_pid);
  if (i != child_pid) {
    if (-1 == i)  error (1, errno, "waitpid");
    else {
      fprintf (stderr, "waitpid pid mismatch\n");
      exit (1);
    }
  }
  //  assert (WIFSTOPPED (status) != 0);
  if (WIFSTOPPED (status) == 0) {
    fprintf (stderr, "WIFSTOPPED false\n");
    exit (1);
  }
  //  assert (WSTOPSIG (status) == SIGSTOP);
  if (WSTOPSIG (status) != SIGSTOP) {
    fprintf (stderr, "WSTOPSIG !- SIGSTOP\n");
    exit (1);
  }

  alarm (0);
  handler_orig = signal (SIGALRM, handler_orig);
  assert (handler_orig == handler);

  sleep (1);

  i = ptrace (PTRACE_CONT, child_pid, NULL, (void *) SIGSTOP);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_CONT SIGSTOP");

  sleep (1);

  // Here we fail on ESRCH on kernel-2.6.20-1.3045.fc7.x86_64 .
  i = ptrace (PTRACE_DETACH, child_pid, NULL, NULL);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_DETACH");

  // Process should be left running (not stopped) here.

  //  puts ("OK");
  kill (child_pid, SIGKILL);
  //return 0;
  exit (0);
}
