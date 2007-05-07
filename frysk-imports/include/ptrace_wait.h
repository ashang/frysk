// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <assert.h>
#include <errno.h>
#include <sys/ptrace.h>
#include "linux.ptrace.h"
#include <unistd.h>
#include <string.h>

static int wifexited (int status) { return WIFEXITED (status); }
static int wifsignaled (int status) { return WIFSIGNALED (status); }
static int wifstopped (int status) { return WIFSTOPPED (status); }

static int wexitstatus (int status) { return WEXITSTATUS (status); }
static int wtermsig (int status) { return WTERMSIG (status); }
static int wstopsig (int status) { return WSTOPSIG (status); }

int
waitstatus (pid_t pid, const char *msg, int (*wif) (int), int reason)
{ 
  int rstatus = 0xdeadbeef;
  int rpid;
  int rerrno;

  // Make the call
  printf ("%d calling waitpid %d for << %s >>", getpid (), pid, msg);
  rpid = waitpid (pid, &rstatus, 0);
  rerrno = errno;

  // Dump the result
  if (rpid < 0) {
    printf (" fails (%s)", strerror (errno));
  }
  else {
    printf (" returns 0x%x ", rstatus);
    if (WIFEXITED (rstatus))
      printf ("WIFEXITED %d", WEXITSTATUS (rstatus));
    else if (WIFSIGNALED (rstatus))
      printf ("WIFSIGNALED %d", WTERMSIG (rstatus));
    else if (WIFSTOPPED (rstatus))
      printf ("WIFSTOPPED %d", WSTOPSIG (rstatus));
    else 
      abort ();
  }

  // validate
  if (rpid < 0) {
    if (wif != NULL || errno != reason) {
      printf ("\n");
      abort ();
    }
  }
  else {
    const char *wifname = NULL;
    int (*wstatus) (int) = NULL;
    if (wif == wifexited) { wifname = "WIFEXITED"; wstatus = wexitstatus; }
    if (wif == wifsignaled) { wifname = "WIFSIGNALED"; wstatus = wtermsig; }
    if (wif == wifstopped) { wifname = "WIFSTOPPED"; wstatus = wstopsig; }
    assert (wifname != NULL);
    assert (wstatus != NULL);
    
    if (!wif (rstatus)
	|| (reason >= 0 && wstatus (rstatus) != reason)) {
      printf (" -- %s %d expected\n", wifname, reason);
      abort ();
    }
  }

  printf (" -- ok\n");
  return rstatus;
}

void
ptracer (int op, pid_t pid, int sig)
{
  const char *what;
  switch (op) {
  case PTRACE_CONT: what = "CONT"; break;
  case PTRACE_ATTACH: what = "ATTACH"; break;
  case PTRACE_SETOPTIONS: what = "SETOPTIONS"; break;
  default: what = "<unknown>"; break;
  }
  printf ("%d calling ptrace %d (%s) %d %d\n", getpid (), op, what, pid, sig);
  errno = 0;
  ptrace (op, pid, NULL, sig);
  assert_perror (errno);
}

void
print_signal (int sig)
{
  printf ("%d received %s\n", getpid (), strsignal (sig));
}

void
wait_for_signals (sigset_t *mask)
{
  printf ("%d waiting for signals\n", getpid ());
  sigsuspend (mask);
}

void
send_signal (pid_t pid, int sig)
{
  printf ("%d signaling %d with %s\n", getpid (), pid, strsignal (sig));
  errno = 0;
  kill (pid, sig);
  assert_perror (errno);
}
