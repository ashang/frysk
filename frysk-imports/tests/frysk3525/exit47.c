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

#include "ptrace_wait.h"

int
main (int argc, char *argv[], char *envp[])
{
  // Stop any buffering
  setbuf (stdout, NULL);

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
      exit (47);
    default:
      exit (0);
    }

  default: // parent
    // Wait for the child to exit, creating a daemon below that.
    waitstatus (v, "child exit creating daemon", wifexited, 0);

    // Wait for for the daemon to report that it is ready.
    wait_for_signals (&old_mask);

    // Attach, wait, config and continue; so that the daemon can run
    // up-to and block, enabling signal delivery.
    ptracer (PTRACE_ATTACH, pid, 0);
    waitstatus (pid, "daemon attached", wifstopped, SIGSTOP);
    ptracer (PTRACE_CONT, pid, 0);

    // Send the daemon an alert, the daemon will respond by exiting.
    // Can't do this during the attach as the daemon may not be ready
    // for the signal - leading to this process seeing an extra
    // stop-with-sigusr1.
    send_signal (pid, SIGUSR1);
    waitstatus (pid, "daemon receives SIGUSR1", wifstopped, SIGUSR1);
    ptracer (PTRACE_CONT, pid, SIGUSR1);

    // Wait for termination
    waitstatus (pid, "daemon does exit(47)", wifexited, 47);

    // And nothing else
    waitstatus (pid, "no children", NULL, ECHILD);
  }
  exit (0);
}
