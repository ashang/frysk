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
  return NULL;
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
    pthread_create (&threads[n], &pthread_attr, hang, NULL);
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
