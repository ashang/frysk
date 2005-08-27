// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

/* Create zombie process.  */

#include <sys/types.h>
#include <linux/unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>


// Simple sleep for roughly SECONDS and then exit.

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


_syscall2(int, tkill, pid_t, tid, int, sig);

struct manager {
  pid_t pid;
  int sig;
} manager;

void
notify_manager ()
{
  tkill (manager.pid, manager.sig);
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
      // The running child notifies the manager, once that is done
      // snooze.
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
\twith <signal>.\n");
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
