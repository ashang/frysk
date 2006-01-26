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
/* Create a rotating barrel of threads.  */

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <errno.h>

pthread_attr_t pthread_attr;

static void *
nothing (void *arg)
{
  return NULL;
}

static int created_threads;
static int joined_threads;

static pthread_t
thread_create (void)
{
  pthread_t thread;
  if (pthread_create (&thread, &pthread_attr, nothing, NULL)) {
    perror ("pthread_create");
    exit (1);
  }
  created_threads++;
  return thread;
}

static void *
thread_join (pthread_t thread)
{
  void *retval;
  if (pthread_join (thread, &retval)) {
    perror ("ptread_join");
    exit (1);
  }
  joined_threads++;
  return retval;
}

int
main (int argc, char *argv[], char *envp[])
{
  int i;

  if (argc < 2 || argc > 3)
    {
      printf ("Usage: %s <number-threads> [ <join-delay> ]\n", argv[0]);
      printf ("\
This test program saturates the thread create and join code.\n\
It creates and then joins <number-threads> (each does nothing but exit).\n\
A thread's join being delayed until a further <join-delay> threads have\n\
been created (default 0).\n\
");
      exit (1);
    }

  int number_threads = atol (argv[1]);
  int length = 1;
  if (argc == 3)
    length = atol (argv[2]) + 1;

  if (number_threads < length) {
    printf ("Need at least <join-delay> threads.\n");
    exit (1);
  }

  pthread_attr_init (&pthread_attr);
  pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);

  pthread_t *thread = calloc (length, sizeof (pthread_t));

  /* Prime the pump.  */
  for (i = 0; i < length; i++) {
    thread[i] = thread_create ();
  }
  number_threads -= length;

  /* Run the pipeline.  */
  for (i = 0; i < number_threads; i++) {
    thread_join (thread[i % length]);
    thread[i % length] = thread_create ();
  }

  /* Drain the pump.  */
  for (i = 0; i < length; i++) {
    thread_join (thread[(number_threads + i) % length]);
  }

  printf ("Created %d, Joined %d.\n", created_threads, joined_threads);
  return 0;
}
