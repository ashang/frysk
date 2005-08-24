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

/* Create a multi-threaded detached program; see usage for
   details.  */

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/select.h>
#include <linux/unistd.h>
#include <limits.h>
#include <pthread.h>

_syscall2(int, tkill, pid_t, tid, int, sig);

pid_t pid;
int sig;

void
signal_parent ()
{
  // Use tkill, instead of kill so that an exact task is signalled.
  // Normal kill can send the signal to "any task" and "any [other]
  // task" may not be ready.
  if (tkill (pid, sig) < 0) {
    perror ("tkill");
    exit (errno);
  }
}

int nr_threads;
pthread_t *threads;
pthread_mutex_t *starts;
pthread_mutex_t *stops;
pid_t *tids;

_syscall0(pid_t,gettid);

void *
hang (void *np)
{
  int n = (int)np;
  tids[n] = gettid ();
  pthread_mutex_unlock (&starts[n]);
  pthread_mutex_lock (&stops[n]);
  tids[n] = 0;
}

// Create a new thread (if there is space).
void
add_thread (int sig)
{
  static int thread;
  if (tids[thread] == 0) {
    // Don't need much stack, trim it back.
    pthread_attr_t pthread_attr;
    pthread_attr_init (&pthread_attr);
    pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);
    // [re] initialize all the mutexes.
    pthread_mutex_init (&starts[thread], NULL);
    pthread_mutex_lock (&starts[thread]);
    pthread_mutex_init (&stops[thread], NULL);
    pthread_mutex_lock (&stops[thread]);
    // Create it then wait for it to ack its existance.
    pthread_create (&threads[thread], &pthread_attr, hang, (void *) thread);
    pthread_mutex_lock (&starts[thread]); // released by running SUB.
    printf ("+%d\n", tids[thread]);
    fflush (stdout);
    if (sig != 0)
      signal_parent ();
    thread = (thread + 1) % nr_threads;
  }
  else
    printf ("+\n");
}

// Stop a thread (if one is present).
void
del_thread ()
{
  static int thread;
  if (tids[thread] != 0) {
    printf ("-%d\n", tids[thread]);
    fflush (stdout);
    pthread_mutex_unlock (&stops[thread]);
    pthread_join (threads[thread], NULL);
    if (sig != 0)
      signal_parent ();
    thread = (thread + 1) % nr_threads;
  }
  else
    printf ("-\n");
}

void
sigalrm ()
{
  exit (0);
}

int
main (int argc, char *argv[], char *envp[])
{
  int n;

  if (argc != 5)
    {
      printf ("\nUsage:\n\n\
kill/suspend <pid> <signal> <suspend-seconds> <thread-count>\n");
      printf ("\nOperation:\n\n\
The program puts itself into the background (by forking and\n\
then having the parent exit); it then creates <thread-count>\n\
blocked threads ensuring that each has been started; sends the\n\
signal <signal> to <pid> indicating that all is ready.\n\
\n\
The program exits either after <suspend-seconds> or after all\n\
threads have exited.\n\
\n\
The PID of the main process and all threads are printed to\n\
standard output.\n");
      printf ("\nSignals:\n\n\
SIGUSR1: Cause one thread to be created (max <thread-count>).\n\
SIGUSR2: Cause one thread to exit\n\
\n\
Signal operations, once completed, are acknowledged by sending\n\
a further <signal> to <pid>.");
      exit (1);
    }

  pid = atol (argv[1]);
  sig = atol (argv[2]);
  int sec = atol (argv[3]);
  nr_threads = atol (argv[4]);

  // Over allocate space for all the thread structures.
  threads = calloc (nr_threads + 1, sizeof (pthread_t));
  tids = calloc (nr_threads + 1, sizeof (pid_t));
  starts = calloc (nr_threads + 1, sizeof (pthread_mutex_t));
  stops = calloc (nr_threads + 1, sizeof (pthread_mutex_t));

  // Go into the background.
  if (fork () > 0)
    exit (0);
  printf ("%d\n", getpid ());

  // Start any threads, synchronize with each to ensure that it is
  // running.
  for (n = 0; n < nr_threads; n++) {
    add_thread (0);
  }

  // Set up a signal handlers that will either add or release one
  // thread.
  signal (SIGUSR1, add_thread);
  signal (SIGUSR2, del_thread);

  // Signal that all is ready.
  signal_parent ();

  // Set up a timer so that in SEC seconds, the program is terminated.
  signal (SIGALRM, sigalrm);
  alarm (sec);

  while (1) sleep (sec * 2);
}
