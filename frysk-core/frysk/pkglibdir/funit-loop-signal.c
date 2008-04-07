// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

#include <stdio.h>
#include <signal.h>
#include <unistd.h>

// Simple test prog that sets an alarm, spins in foo () till ALRM
// signal arrives and twiddles a bit and breaks the loop.
// Used to check a backtrace correctly shows the signal interrupted
// function (and its parent).

// Gets set to 1 by signal handler (foo checks the value).
volatile int i;

// Random counter variable to give foo () something to do.
int count;

// The signal handler.
void
handler (int sig)
{
  i = 1; // _signal_handler_
}

int
foo (int x)
{
  i = 0;           // _foo_entry_
  count = 0;

  // Spin till signal sets i to one.
  while (i == 0)
    count += x;

  i = 0; // _foo_exit_

  return x - i;
}

int
main (int argc, char *argv[], char *envp[])
{
  // Setup signal handler.
  struct sigaction sa;
  sa.sa_handler = handler;
  sigemptyset(&sa.sa_mask);
  sa.sa_flags = 0;
  sigaction(SIGALRM, &sa, NULL) ;

  // Trigger signal after 1 second.
  alarm(1);

  // Spin till signal
  return foo (1); // zero

  // Something with sigaltstack
}
