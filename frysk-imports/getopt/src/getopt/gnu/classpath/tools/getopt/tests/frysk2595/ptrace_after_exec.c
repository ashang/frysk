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

#define _GNU_SOURCE

#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <errno.h>
#include <linux/unistd.h>
#include <pthread.h>
#include <linux.syscall.h>

#define __REENTRANT

_syscall2(int, tkill, int, tid, int, sig);

pid_t c_pid;

int 
main (int argc, char **argv) 
{
  switch (argc)
    {
    default:
      printf ("whats with the %d arguments\n", argc);
      exit (1);

    case 1: // First time run
      {
	c_pid = fork();
	switch (c_pid)
	  {
	  case -1:
	    {
	      perror("Error: could not fork child process");
	      exit(1);
	    }
	
	  case 0:
	    {
	      // Child
	      long ptrace_res = ptrace (PTRACE_TRACEME, 0, NULL, NULL);
	      if (ptrace_res == -1)
	      {
		perror("Error: could not start trace");
		exit(1);
	      }
	      
	      char* const args[] = {"/bin/true", NULL};
	      
	      execv(args[0], args);
	      
	      perror("Exec returned. Whoops!");
	      exit(1);
	    }

	  default:
	    {
	      // Parent
	      errno = 0;
	      int status;
	      pid_t wait_res = waitpid (c_pid, &status, __WALL); 
	      if (wait_res != c_pid)
		{
		  perror("waitpid didn't return the child pid");
		  exit(1);
		}
	      // Now exec ourselves with the pid as a parameter.
	      char *args[3];
	      args[0] = argv[0];
	      asprintf (&args[1], "%d", c_pid);
	      args[2] = NULL;
	      printf ("execing %s %s\n", args[0], args[1]);
	      execv (args[0], args);
	      perror ("execing with ptraced child\n");
	      exit (1);
	    }
	  }
      }

    case 2:// The exec of this process
      {
	c_pid = atol (argv[1]);
	printf ("after exec, pid is %d\n", c_pid);
	errno = 0;
	ptrace(PTRACE_PEEKUSER, c_pid, 0, 0);
	if (errno != 0)
	  {
	    perror("Ptrace after exec");
	    exit(1);
	  }
      }
    }
  tkill(c_pid, SIGKILL);
  return 0;
}
