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

#include <unistd.h>
#include <signal.h>
#include <stdlib.h>
#include <linux/unistd.h>
#include <string.h>
#include <stdio.h>

int childPid;

_syscall2(int, tkill, pid_t, tid, int, sig);

void handler (int sig) {
   tkill (childPid, SIGUSR2);
}

void handlerChild (int sig) {
   /* Do nothing.  */
}

// Set up a pipe between a parent and child process.  The child
// process is supposed to be in a blocked read.  Parameters to
// main are:  <pid> - Pid of process running frysk
//            <sig> - Acknowledge signal to send to frysk
//            <restart> - Should read restart after signal (boolean)

int main (int argc, char **argv)
{
  int fd[2];
  int fd2[2];
  pid_t pid;
  pid_t fryskPid;
  int fryskSig;
  int restart;

  if (pipe (fd) < 0 || pipe (fd2) < 0) {
    abort ();
  }

  // Get the frysk manager pid and signal to use to
  // indicate everything is ready.
  fryskPid = (pid_t)atol (argv [1]);
  fryskSig = atoi (argv [2]);
  restart = atoi (argv[3]);

  pid = fork ();
  if (pid == (pid_t)0) {
    struct sigaction action;
    sigset_t a;
    /* Child process.  */
    close (fd[0]);
    close (fd2[0]);
    // Set up a signal handler for SIGUSR2.
    // Make certain that SIGUSR1 is not masked.
    memset (&action, 0, sizeof (action));
    sigemptyset (&action.sa_mask);
    sigaddset (&action.sa_mask, SIGUSR2);
    sigprocmask (SIG_UNBLOCK, &action.sa_mask, NULL);
    action.sa_handler = &handlerChild;
    sigaction (SIGUSR2, &action, NULL);
    sigemptyset (&a);
    // Signal to the parent process that child is ready.
    if (write (fd2[1], "a", 1) < 0){
      perror ("write");
      abort ();
    }
    // Wait until we get a signal and then allow program to finish.
    sigsuspend (&a);
    if (write (fd[1], "a", 1)){
      perror ("write");
      abort ();
    }
    close (fd[1]);
  }
  else {
    /* Parent process.  */
    childPid = pid;
    struct sigaction action;
    char buf[1];
    // First close other end of pipe.
    close (fd[1]);
    close (fd2[1]);
    // Set up a signal handler for SIGUSR1.
    // Make certain that SIGUSR1 is not masked.
    memset (&action, 0, sizeof (action));
    sigemptyset (&action.sa_mask);
    sigaddset (&action.sa_mask, SIGUSR1);
    sigprocmask (SIG_UNBLOCK, &action.sa_mask, NULL);
    action.sa_handler = &handler;
    if (restart)
      action.sa_flags = SA_RESTART;
    sigaction (SIGUSR1, &action, NULL);
    // Wait until child is ready.
    if (read (fd2[0], buf, 1)){
      perror ("read");
      abort ();
    }
    // Indicate to frysk that parent and child processes are ready.
    tkill (fryskPid, fryskSig);
    if (read (fd[0], buf, 1)){
      perror ("read");
      abort ();
    }
    close (fd[0]);
  }

  return 0;
}
