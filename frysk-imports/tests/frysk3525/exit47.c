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
  printf ("waitpid %d for << %s >>", pid, msg);
  rpid = waitpid (pid, &rstatus, 0);
  rerrno = errno;

  // Dump the result
  if (rpid < 0) {
    printf ("fails (%s)", strerror (errno));
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
      exit (1);
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
      exit (1);
    }
  }

  printf (" -- ok\n");
  return rstatus;
}

int
main (int argc, char *argv[], char *envp[])
{
  setbuf (stdout, NULL);

  if (argc > 2) {
    printf ("%d argc %d %s %s\n", getpid (), argc, argv[1], argv[2]);
    if (argv[1][0] > '0') {
      argv[1][0]--;
      execl (argv[0], argv[0], argv[1], argv[2], NULL);
      exit (1);
    }
    else {
      printf ("exiting %d with %s\n", getpid (), argv[2]);
      exit (atol (argv[2]));
    }
  }

  pid_t pid = fork ();
  switch (pid) {

  case -1: // Oops
    assert_perror (errno);

  case 0: // child
    errno = 0;
    ptrace (PTRACE_TRACEME, 0, NULL, NULL);
    assert_perror (errno);
    execl (argv[0], argv[0], "1", "47", NULL);
    exit (1);

  default: // parent
    // Wait for for the daemon to exec
    waitstatus (pid, "child stops at exec", wifstopped, -1);
    errno = 0;
    ptrace (PTRACE_SETOPTIONS, pid, 0, PTRACE_O_TRACEEXEC|PTRACE_O_TRACEEXIT);

    // Run to the next exec.
    errno = 0;
    ptrace (PTRACE_CONT, pid, NULL, NULL);
    assert_perror (errno);
    waitstatus (pid, "child stops at next exec", wifstopped, -1);

    // Run to the next exec.
    errno = 0;
    ptrace (PTRACE_CONT, pid, NULL, NULL);
    assert_perror (errno);
    waitstatus (pid, "child stops exiting", wifstopped, 5);
    ptrace (PTRACE_CONT, pid, NULL, NULL);
    waitstatus (pid, "child exit(47)", wifexited, 47);

    // And nothing else
    waitstatus (pid, "no children", NULL, ECHILD);
  }
  exit (0);
}
