// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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
/* Use processes to compute a Fibonacci number.  */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <unistd.h>

void
breakpoint_me(void)
{
}

int
main (int argc, char *argv[], char *envp[])
{
  int result = 0;
  int want;
  int n;
  if (argc != 2)
    {
      printf ("Usage: fib <number>\n");
      exit (1);
    }
  want = atol (argv[1]);
  if (want > 13)
    {
      printf ("<number> > 13 not allowed, limit due to size of wait.status.");
      exit (1);
    }

  n = want;
 compute_fib_n:
  /* Children jump back here with an updated N.  */
  breakpoint_me();
  switch (n)
    {
    case 0:
      result = 0;
      break;
    case 1:
      result = 1;
      break;
    default:
      {
	pid_t child[2];
	int i;
	for (i = 0; i < 2; i++)
	  {
	    child[i] = fork ();
	    switch (child[i])
	      {
	      case 0:
		/* Child; loop back round performing the same
		   computation but with ...  */
		n = n - 1 - i;
		goto compute_fib_n;
	      case -1:
		/* Ulgh.  */
		perror ("fork");
		exit (1);
	      }
	  }
	for (i = 0; i < 2; i++)
	  {
	    int status;
	    pid_t pid = waitpid (child[i], &status, 0);
	    if (pid < 0)
	      {
		perror ("waitpid");
		exit (1);
	      }
	    if (pid != child[i])
	      {
		printf ("waitpid (%ld) got back %ld\n", (long) child[i],
			(long) pid);
		exit (1);
	      }
	    if (!WIFEXITED (status))
	      {
		printf ("waitpid (%ld) got non exit staus 0x%x\n",
			(long) child[i], status);
		if (WIFSIGNALED(status))
		  {
		    printf("waitpid (%ld) got signal %d\n",
			   (long) child[i], WTERMSIG(status));
		    kill(getpid(), WTERMSIG(status));
		  }
		exit (1);
	      }
	    result += WEXITSTATUS (status);
	  }
      }
    }
  if (n == want)
    {
      printf ("fib (%d) = %d\n", n, result);
      exit (0);
    }
  else
    exit (result);
}
