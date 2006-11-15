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

/*	
 * frysk-imports/tests/frysk3381
 */

#define _GNU_SOURCE
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <pthread.h>
#include <alloca.h>
#include <signal.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ptrace.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <linux/ptrace.h>

#define NR_CHILDREN 3

int
main (int ac, char * av[])
{
  int i;
  pid_t dpids[NR_CHILDREN];

  for (i = 0; i < NR_CHILDREN; i++) {
    pid_t pid = fork ();
    switch (pid) {
    case -1:
      perror ("fork");
      exit (1);
    case 0: // child
      {
	while (1) {
	  usleep (250000);
	}
      }
      break;
    default: // Parent
      dpids[i] = pid;
      break;
    }
  }

  for (i = 0; i < NR_CHILDREN; i++) {
    int status;
    
    if (ptrace (PTRACE_ATTACH, dpids[i], NULL, NULL) < 0) {
      perror ("ptrace -- for attach");
      exit (1);
    }
    if (waitpid (dpids[i], &status,  __WALL) < 0) {
      perror ("waitpid -- for attach");
      exit (1);
    }
  }

  for (i = 0; i < NR_CHILDREN; i++) {
    if (kill (dpids[i], SIGKILL)) 
      perror ("kill SIGKILL");
  }

  sleep (2);

  {
    int nr_resps;

    for (nr_resps = 0; 0 < waitpid (-1, NULL, WNOHANG); nr_resps++) {}
    exit ((0 == nr_resps) ? 1 : 0);
  }
}
