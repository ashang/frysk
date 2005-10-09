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

/* Create zombie or daemon process.  */

#include <sys/types.h>
#include <linux/unistd.h>
#include <sys/prctl.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

_syscall2(int, tkill, pid_t, tid, int, sig)



// Simple snooze for roughly SECONDS and then exit.

void
sigalrm (int sig)
{
  exit (0);
}

void
snooze (int seconds)
{
  signal (SIGALRM, sigalrm);
  alarm (seconds);
  sigset_t mask;
  sigemptyset (&mask);
  while (1) sigsuspend (&mask);
}


struct manager {
  pid_t pid;
  int sig;
} manager;

void
notify_manager ()
{
  tkill (manager.pid, manager.sig);
}

void
parent_died ()
{
  // Only notify the manager when the parent [correctly] switched to
  // process one.
  if (getppid () == 1)
    notify_manager ();
}

pid_t proc;
int seconds;

void
add_proc (int sig)
{
  if (proc == 0) {
    printf ("+");
    errno = 0;
    proc = fork ();
    switch (proc) {
    case 0:
      // Set up a handler that notifies the manager when the parent
      // exits.
      signal (SIGHUP, parent_died);
      prctl (PR_SET_PDEATHSIG, SIGHUP);
      // It's the responsibility of the running child to notify the
      // manager; the child does this when it is ready.
      notify_manager ();
      snooze (seconds);
    case -1:
      perror ("fork");
      exit (errno);
    default:
      printf ("%d\n", proc);
      return;
    }
  }
  else
    printf ("+\n");
}

void
del_proc (int sig)
{
  if (proc != 0) {
    printf ("-%d", proc);
    kill (proc, SIGKILL);
    // Now wait for the /proc/PID/task/PID directory to disappear
    // indicating that the task really has become a zombie.
    char *task;
    asprintf (&task, "/proc/%d/task/%d", proc, proc);
    while (access (task, R_OK) == 0);
    free (task);
    proc = 0;
    printf ("\n");
    notify_manager ();
  }
  else
    printf ("-\n");
}

int
main (int argc, char *argv[], char *envp[])
{
  if (argc != 4) {
    printf ("\
Usage:\n\t<pid> <signal> <seconds>\n\
Where:\n\
\t<pid> <signal>   Manager process to signal, and signal to use, once\n\
\t                 an operation has completed (e.g., running, thread\n\
\t                 created, thread exited).\n\
\t<seconds>        Number of seconds to sleep before exiting.");
    printf ("\
Operation:\n\
\tThe program notifies <pid>, using <signal> once it has started.\n\
\tThe program will then respond to the following signals:\n\
\t\tSIGUSR1: Create a child process.\n\
\t\tSIGUSR2: Delete a child process (the process isn't reaped).\n\
\tand after the request has been processed, notify the Manager Process\n\
\twith <signal>.\n\
\tIn addition, the child will signal the manager when it detects that\n\
\tits parent has changed (turning it into a daemon).\n");
      exit (1);
  }

  manager.pid = atol (argv[1]);
  manager.sig = atol (argv[2]);
  seconds = atol (argv[3]);

  setbuf (stdout, NULL);

  // Set up a signal handlers that will either add or delete one
  // process.  For each mask out the others signals.  Make certain
  // that the signals are not masked (don't ask, it appears that this
  // process, when started via vfork / fork / exec, inherits the
  // original processes mask).
  struct sigaction action;
  memset (&action, 0, sizeof (action));
  sigemptyset (&action.sa_mask);
  sigaddset (&action.sa_mask, SIGUSR1);
  sigaddset (&action.sa_mask, SIGUSR2);
  sigprocmask (SIG_UNBLOCK, &action.sa_mask, NULL);
  action.sa_handler = add_proc;
  sigaction (SIGUSR1, &action, NULL);
  action.sa_handler = del_proc;
  sigaction (SIGUSR2, &action, NULL);

  notify_manager ();
  snooze (seconds);
}
