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

/**
 * Trace a process that exits with status 47.
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <assert.h>
#include <errno.h>
#include <sys/ptrace.h>
#include "../../include/linux.ptrace.h"
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
  printf ("%d signalling %d with %s\n", getpid (), pid, strsignal (sig));
  errno = 0;
  kill (pid, sig);
  assert_perror (errno);
}

int
main (int argc, char *argv[], char *envp[])
{
  // Stop any buffering
  setbuf (stdout, NULL);

  if (argc > 2) {
    printf ("%d argc %d %s %s\n", getpid (), argc, argv[1], argv[2]);
    if (argv[1][0] > '0') {
      argv[1][0]--;
      execl (argv[0], argv[0], argv[1], argv[2], NULL);
      abort ();
    }
    else {
      printf ("exiting %d with %s\n", getpid (), argv[2]);
      exit (atol (argv[2]));
    }
  }

  printf ("%d installing signal handler and mask\n", getpid ());
  signal (SIGUSR1, print_signal);
  sigset_t signal_mask;
  sigset_t old_mask;
  sigemptyset (&signal_mask);
  sigaddset (&signal_mask, SIGUSR1);
  sigprocmask (SIG_BLOCK, &signal_mask, &old_mask);

  printf ("%d forking\n", getpid ());
  pid_t ppid = getpid ();
  volatile pid_t pid;
  pid_t v = vfork ();
  switch (v) {

  case -1: // Oops
    assert_perror (errno);

  case 0: // child
    pid = fork ();
    switch (pid) {
    case -1:
      assert_perror (errno);
    case 0: // daemon
      errno = 0;
      send_signal (ppid, SIGUSR1);
      wait_for_signals (&old_mask);
      execl (argv[0], argv[0], "0", "47", NULL);
      assert_perror (errno);
    default:
      exit (0);
    }

  default: // parent
    // Wait for daemon to exit.
    waitstatus (v, "child for daemon exits", wifexited, 0);

    // Wait for for the daemon to report that it is ready.
    wait_for_signals (&old_mask);

    // Attach, wait, config and continue with signal.
    ptracer (PTRACE_ATTACH, pid, 0);
    waitstatus (pid, "daemon attached", wifstopped, SIGSTOP);
    ptracer (PTRACE_SETOPTIONS, pid, PTRACE_O_TRACEEXEC|PTRACE_O_TRACEEXIT);
    ptracer (PTRACE_CONT, pid, SIGUSR1);

    // Wait for the exec
    waitstatus (pid, "daemon stops at exec", wifstopped, 5);
    ptracer (PTRACE_CONT, pid, 0);

    // Wait for termination
    waitstatus (pid, "daemon stops at exit(47)", wifstopped, 5);
    ptracer (PTRACE_CONT, pid, 0);
    waitstatus (pid, "daemon does exit(47)", wifexited, 47);

    // And nothing else
    waitstatus (pid, "no children", NULL, ECHILD);
  }
  exit (0);
}
