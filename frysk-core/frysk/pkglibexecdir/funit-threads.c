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

/* Create a multi-threaded program that maintaints a N constantly
   cloning threads.  */

#define _GNU_SOURCE

#include <signal.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/select.h>
#include <limits.h>
#include <pthread.h>
#include <string.h>

#include "util.h"



static void
usage ()
{
  printf ("\
Usage:\n\
    [ --clone | --block | --loop ] <pid> <signal> <seconds> <nr-threads>\n\
Where:\n\
    --clone          Each thread will repeatedly clone itself (default).\n\
    --block          Each thread will block.\n\
    --loop           Each thread will execute an infinite loop.\n\
    <pid> <signal>   Manager process to signal, and signal to use, once\n\
                     an operation has completed (e.g., running, thread\n\
                     created, thread exited).\n\
    <seconds>        Number of seconds that the program should run.\n\
    <nr-threads>     Number of threads that should be created\n\
");
  exit (1);
}



pthread_attr_t pthread_attr;

volatile static int halt;

volatile static long long count;
static void
update_counter ()
{
  static pthread_mutex_t count_mutex = PTHREAD_MUTEX_INITIALIZER;
  OK (pthread_mutex_lock, (&count_mutex));
  count++;
  OK (pthread_mutex_unlock, (&count_mutex));
}

typedef void *(op_t)(void *);

void *
op_clone (void *np)
{
  pthread_t tmp;
  pthread_t *thread = (pthread_t*)np;
  if (*thread != 0)
    OK (pthread_join, (*thread, np));
  *thread = pthread_self ();
  update_counter ();
  if (halt)
    return NULL;
  OK (pthread_create, (&tmp, &pthread_attr, op_clone, np));
  return NULL;
}

void *
op_loop (void *np)
{
  pthread_t *thread = (pthread_t*)np;
  *thread = pthread_self ();
  while (!halt)
    update_counter ();
  return NULL;
}

void *
op_block (void *np)
{
  static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
  pthread_t *thread = (pthread_t*)np;
  *thread = pthread_self ();
  update_counter ();
  OK (pthread_detach, (*thread));
  OK (pthread_mutex_lock, (&lock));
  return NULL;
}

int
main (int argc, char *argv[])
{
  int i;
  int n;
  int sig;
  int pid;
  int sec;
  pthread_t *threads;

  int argi = 1;
  op_t *op = op_clone;
  while (argi < argc) {
    if (strcmp (argv[argi], "--clone") == 0)
      op = op_clone;
    else if (strcmp (argv[argi], "--block") == 0)
      op = op_block;
    else if (strcmp (argv[argi], "--loop") == 0)
      op = op_loop;
    else
      break;
    argi++;
  }

  if (argi + 4 > argc)
    usage ();

  pid = atol (argv[argi++]);
  sig = atol (argv[argi++]);
  sec = atol (argv[argi++]);
  n = atol (argv[argi++]);

  // Minimal stack.
  OK (pthread_attr_init, (&pthread_attr));
  OK (pthread_attr_setstacksize, (&pthread_attr, PTHREAD_STACK_MIN));

  printf ("%d lines\n", n);
  threads = calloc (n, sizeof (pthread_t));
  for (i = 0; i < n; i++) {
    pthread_t p;
    OK (pthread_create, (&p, &pthread_attr, op, &threads[i]));
  }

  printf ("kill pid %d sig %d\n", pid, sig);
  kill (pid, sig); // ack.
  printf ("%d sec\n", sec);
  sleep (sec);
  halt = 1;
  
  for (i = 0; i < n; i++) {
    void *result;
    // Might work - race between this and clone.
    pthread_join (threads[i], &result);
  }

  printf ("count %lld\n", count);
  return 0;
}
