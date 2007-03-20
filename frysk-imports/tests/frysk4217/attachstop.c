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


static void
child_f (void)
{
  raise (SIGSTOP);
  abort ();
  /* NOTREACHED */
}

static pid_t child_pid;

static const char *handler_state;

static void
handler (int signo)
{
  kill (child_pid, SIGKILL);
  //  abort ();
  fprintf (stderr, "timeout: %s\n", handler_state);
  exit (1);
}

static unsigned char regs[0x10000];

static void attach (const char *state)
{
  int i;
  int status;

  handler_state = state;
  signal (SIGALRM, handler);
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
}

int main (void)
{
  int i;

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
      default:;
	/* PASSTHRU */
    }
  /* Parent.  */

  sleep (1);

  /* Buggy:
  kernel-xen-2.6.19-1.2898.2.3.fc7.i686
  kernel-2.6.20-1.2925.fc6.i586
  kernel-2.6.20-1.2928.rm1.fc6.x86_64
     Fixed:
  kernel-2.6.20-1.2935.rm1.fc6.x86_64
  kernel-2.6.20-1.2935.rm2.fc6.x86_64
  */
  attach ("first PTRACE_ATTACH");

  /* Buggy:
  kernel-2.6.20-1.2935.rm1.fc6.x86_64
     Fixed:
  kernel-2.6.20-1.2935.rm2.fc6.x86_64
  */
  errno = 0;
  ptrace (PTRACE_PEEKUSER, child_pid, NULL, NULL);
  //assert (errno == 0);
  if (errno != 0) error (1, errno, "PTRACE_PEEKUSER");
  i = ptrace (PTRACE_GETREGS, child_pid, NULL, regs);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_GETREGS");
  
  i = ptrace (PTRACE_DETACH, child_pid, NULL, NULL);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_DETACH");

  /* Buggy:
  kernel-2.6.20-1.2935.rm2.fc6.x86_64
  */
  attach ("second PTRACE_ATTACH");

  //  puts ("OK");
  kill (child_pid, SIGKILL);
  //return 0;
  exit (0);
}
