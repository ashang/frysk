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
 * Create a multi-threaded program, attach to it, try to
 * simultaneously detach and terminate it.
 */

#define _GNU_SOURCE
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <assert.h>
#include <errno.h>
#include <sys/ptrace.h>
#include <linux.ptrace.h>
#include <linux.syscall.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include <dirent.h>

_syscall2(int, tkill, int, tid, int, sig);

#define OK(FUNC,ARGS) { if (FUNC ARGS) { perror (#FUNC); exit (1); } }

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
  rpid = waitpid (pid, &rstatus, __WALL);
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
  else if (wif != NULL) {
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
  return rpid;
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

/**
 * A little thread that just hangs.
 */
pthread_mutex_t hung_mutex = PTHREAD_MUTEX_INITIALIZER;
void *
hung_thread (void *arg)
{
  pthread_mutex_lock (&hung_mutex);
  return NULL;
}

int
main (int argc, char *argv[], char *envp[])
{
  int i;

  // set up an alarm, if that expires the test should just die and
  // fail.
  alarm (100);

  // Stop any buffering
  setbuf (stdout, NULL);

  printf ("%d installing signal handler and mask\n", getpid ());
  signal (SIGUSR1, print_signal);
  sigset_t signal_mask;
  sigset_t old_mask;
  sigemptyset (&signal_mask);
  sigaddset (&signal_mask, SIGUSR1);
  sigprocmask (SIG_BLOCK, &signal_mask, &old_mask);

  printf ("%d forking daemon\n", getpid ());
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
      pthread_mutex_lock (&hung_mutex);
#define NR_TASKS 1
      for (i = 0; i < NR_TASKS; i++) {
	pthread_t t;
	OK (pthread_create, (&t, NULL, hung_thread, NULL));
      }
      // OK (pthread_create, (&rhs, NULL, hung_thread, NULL));
      send_signal (ppid, SIGUSR1);
      wait_for_signals (&old_mask);
    default:
      exit (0);
    }

  default: // parent
    // Wait for the child to exit, creating a daemon below that.
    waitstatus (v, "child exit creating daemon", wifexited, 0);

    // Wait for for the daemon to report that it is ready.
    wait_for_signals (&old_mask);

    // Find the two tasks
    pid_t tasks[NR_TASKS + 1];
    tasks[0] = pid;
    {
      char *p;
      asprintf (&p, "/proc/%d/task", pid);
      if (p == NULL) {
	perror ("printf");
	exit (1);
      }
      printf ("%d tasks in %s\n", getpid (), p);
      DIR *task_dir = opendir (p);
      if (task_dir == NULL) {
	perror ("opendir");
	exit (1);
      }
      struct dirent *d;
      i = 1;
      for (d = readdir (task_dir); d != NULL; d = readdir (task_dir)) {
	if (!isdigit (d->d_name[0])) {
	  printf ("%d skip name %s/%s\n", getpid (), p, d->d_name);
	  continue;
	}
	pid_t task = atoi (d->d_name);
	if (task == pid) {
	  printf ("%d skip pid %s/%d\n", getpid (), p, task);
	  continue;
	}
	tasks[i++] = task;
	printf ("%d add %s/%d\n", getpid (), p, task);
      }
      closedir (task_dir);
    }

    // Attach and wait for the other two threads
    for (i = 0; i < NR_TASKS; i++) {
      ptracer (PTRACE_ATTACH, tasks[i], 0);
      waitstatus (tasks[i], "daemon other attached", wifstopped, SIGSTOP);
    }

    // Now resume the first and third tasks
    // ptracer (PTRACE_CONT, tasks[2], 0);
    ptracer (PTRACE_CONT, tasks[0], 0);

    // Rip the heart out of the task
    tkill (pid, SIGKILL);

    // Start trying to detach in reverse order!
    for (i = NR_TASKS; i >= 0; i--) {
      printf ("%d clobbering %d\n", getpid (), tasks[i]);
      if (ptrace (PTRACE_DETACH, tasks[i], 0, 0) < 0)
	perror ("ptrace");
      if (tkill (tasks[i], SIGCONT) < 0)
	perror ("tkill SIGCONT");
      if (tkill (tasks[i], SIGKILL) < 0)
	perror ("tkill SIGKILL");
    }

    while (1) {
      int rpid = waitstatus (-1, "wait for anything", NULL, 0);
      if (rpid < 0)
	break;
      printf ("%d re-clobbering %d\n", getpid (), rpid);
      if (ptrace (PTRACE_DETACH, rpid, 0, 0) < 0)
	perror ("ptrace");
      if (tkill (rpid, SIGCONT) < 0)
	perror ("tkill SIGCONT");
      if (tkill (rpid, SIGKILL) < 0)
	perror ("tkill SIGKILL");
    }
  }

  exit (0);
}
