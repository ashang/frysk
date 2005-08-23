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

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/select.h>
#include <linux/unistd.h>
#include <limits.h>
#include <pthread.h>

pthread_mutex_t start;
pthread_mutex_t stop;

void *
hang (void *np)
{
  pthread_mutex_unlock (&start);
  pthread_mutex_lock (&stop);
}

int
main (int argc, char *argv[], char *envp[])
{
  if (argc != 5)
    {
      printf ("\
Usage: kill/suspend <pid> <signal> <suspend-seconds> <clones>\n\
After creating <clones> suspended threads, send <signal> to external\n\
thread <pid>, and then suspend for <suspend-seconds>.\n");
      exit (1);
    }
  int pid = atol (argv[1]);
  int sig = atol (argv[2]);
  int sec = atol (argv[3]);
  int thr = atol (argv[4]);

  // Don't need much stack.
  pthread_attr_t pthread_attr;
  pthread_attr_init (&pthread_attr);
  pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);

  pthread_mutex_init (&start, NULL);
  pthread_mutex_lock (&start);
  pthread_mutex_init (&stop, NULL);
  pthread_mutex_lock (&stop);

  // Start any threads, make certain they actually exist.
  int n;
  pthread_t *threads = NULL;
  for (n = 0; n < thr; n++) {
    threads = realloc (threads, sizeof (pthread_t) * (n + 1));
    pthread_create (&threads[n], &pthread_attr, hang, (void *) n);
    pthread_detach (threads[n]);
    pthread_mutex_lock (&start); // released by running SUB.
  }

  // Use tkill, instead of tkill (pid, sig) so that an exact task is
  // signalled.  Normal kill can send to any task and other tasks may
  // not be ready.
  if (syscall (__NR_tkill, pid, sig) < 0) {
    perror ("tkill");
    exit (errno);
  }

  struct timeval timeval;
  timeval.tv_sec = sec;
  timeval.tv_usec = 0;

  select (0, NULL, NULL, NULL, &timeval);

  exit (0);
}
