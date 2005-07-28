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
/* Use threads to compute a Fibonacci number.  */

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <errno.h>

pthread_attr_t pthread_attr;

static void *
fib (void *arg)
{
  long n = (long) arg;
  switch (n)
    {
    case 0:
      return (void *) 0;
    case 1:
      return (void *) 1;
    default:
      {
	pthread_t thread[2];
	long result = 0;
	int i;
	for (i = 0; i < 2; i++)
	  {
	    while (1)
	      {
		int s = pthread_create (&thread[i], &pthread_attr,
					fib, (void *) (n - 1 - i));
		if (s == 0)
		  break;
		else if (s == EAGAIN)
		  continue;
		else
		  {
		    perror ("pthread_create");
		    exit (1);
		  }
	      }
	  }
	for (i = 0; i < 2; i++)
	  {
	    void *retval;
	    if (pthread_join (thread[i], &retval))
	      {
		perror ("ptread_join");
		exit (1);
	      }
	    result += (long) retval;
	  }
	return (void *) result;
      }
    }
}


int
main (int argc, char *argv[], char *envp[])
{
  void *n;
  if (argc != 2)
    {
      printf ("Usage: fib <number>\n");
      exit (1);
    }
  n = (void *) atol (argv[1]);
  pthread_attr_init (&pthread_attr);
  pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);
  printf ("fib (%ld) = %ld\n", (long) n, (long) fib (n));
  return 0;
}
