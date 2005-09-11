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
/* Create a rotating barrel of processes.  */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/wait.h>

static int created_processes;
static int joined_processes;

static pid_t
process_create (void)
{
  pid_t pid;
  pid = fork ();
  switch (pid) {
  case 0: /* Child.  */
    exit (0);
  case -1:
    perror ("fork");
    exit (1);
  default:
    created_processes++;
    return pid;
  }
}

static int
process_join (pid_t pid)
{
  int status;
  int retpid = waitpid (pid, &status, 0);
  if (status < 0) {
    perror ("waitpid");
    exit (1);
  }
  if (retpid != pid) {
    printf ("waitpid (%d) got back %d\n", (int) pid,
	    (int) retpid);
    exit (1);
  }
  if (!WIFEXITED (status)) {
    printf ("waitpid (%d) got non exit staus 0x%x\n", (int) pid,
	    status);
    exit (1);
  }
  joined_processes++;
  return WEXITSTATUS (status);
}

int
main (int argc, char *argv[], char *envp[])
{
  int c;
  int i;

  if (argc < 2 || argc > 3)
    {
      printf ("Usage: %s <number-processes> [ <join-delay> ]\n", argv[0]);
      printf ("\
This test program saturates the process create and join code.\n\
It creates and then joins <number-processes> (each does nothing but exit).\n\
A processes join being delayed until a further <join-delay> processes have\n\
been created (default 0).\n\
");
      exit (1);
    }

  int number_processes = atol (argv[1]);
  int length = 1;
  if (argc == 3)
    length = atol (argv[2]) + 1;

  if (number_processes < length) {
    printf ("Need at least <join-delay> processes.\n");
    exit (1);
  }

  pid_t *process = calloc (length, sizeof (pid_t));

  /* Prime the pump.  */
  for (i = 0; i < length; i++) {
    process[i] = process_create ();
  }
  number_processes -= length;

  /* Run the pipeline.  */
  for (i = 0; i < number_processes; i++) {
    process_join (process[i % length]);
    process[i % length] = process_create ();
  }

  /* Drain the pump.  */
  for (i = 0; i < length; i++) {
    process_join (process[(number_processes + i) % length]);
  }

  printf ("Created %d, Joined %d.\n", created_processes, joined_processes);
  return 0;
}
