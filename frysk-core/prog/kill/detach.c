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
#include <string.h>


// Simple sleep for roughly SECONDS and then exit.

static void
sigalrm ()
{
  exit (0);
}

void
snooze (int seconds)
{
  signal (SIGALRM, sigalrm);
  alarm (seconds);
  sigset_t mask;
  sigemptyset (&mask);
  while (1) sigsuspend (&mask);
}


// Very primative message passing mechanism.  Implemented using either
// mutex or flags.  The lock, by default is held.

int polling;  // Use polling (i.e., non-blocking flags) 

struct msg {
  pthread_mutex_t mutex;
  volatile int flag;
};

static void
init (struct msg *l)
{
  if (polling)
    l->flag = 0;
  else {
    pthread_mutex_init (&l->mutex, NULL);
    pthread_mutex_lock (&l->mutex);
  }
}

static void
recv (struct msg *l)
{
  if (polling)
    while (!l->flag);
  else
    pthread_mutex_lock (&l->mutex);
}

static void
send (struct msg *l)
{
  if (polling)
    l->flag = 1;
  else
    pthread_mutex_unlock (&l->mutex);
}


_syscall2(int, tkill, pid_t, tid, int, sig);

int bg;  // Run in the background?

struct manager {
  pid_t pid;
  int sig;
} manager;

void
signal_manager ()
{
  // Use tkill, instead of kill so that an exact task is signalled.
  // Normal kill can send the signal to "any task" and "any [other]
  // task" may not be ready.
  if (tkill (manager.pid, manager.sig) < 0) {
    perror ("tkill");
    exit (errno);
  }
}

int nr_threads;
struct thread {
  pthread_t pthread;
  struct msg start;
  struct msg stop;
  pid_t tid;
} *threads;

_syscall0(pid_t,gettid);

void *
hang (void *np)
{
  struct thread *thread = np;
  thread->tid = gettid ();
  send (&thread->start);
  recv (&thread->stop);
  thread->tid = 0;
}

// Create a new thread (if there is space).
void
add_thread (int sig)
{
  static int thread_nr;
  struct thread *thread = threads + thread_nr;
  if (thread->tid == 0) {
    printf ("+");
    // Don't need much stack, trim it back.
    pthread_attr_t pthread_attr;
    pthread_attr_init (&pthread_attr);
    pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);
    // [re] initialize all the mutexes.
    init (&thread->start);
    init (&thread->stop);
    // Create it then wait for it to ack its existance.
    pthread_create (&thread->pthread, &pthread_attr, hang, (void *) thread);
    // Wait until start lock is released by the running / created
    // thread.
    recv (&thread->start);
    printf ("%d\n", thread->tid);
    if (sig != 0)
      signal_manager ();
    thread_nr = (thread_nr + 1) % nr_threads;
  }
  else
    printf ("+\n");
}

// Stop a thread (if one is present).
void
del_thread (int sig)
{
  static int thread_nr;
  struct thread *thread = threads + thread_nr;
  if (thread->tid != 0) {
    printf ("-%d", thread->tid);
    send (&thread->stop);
    pthread_join (thread->pthread, NULL);
    printf ("\n");
    if (sig != 0)
      signal_manager ();
    thread_nr = (thread_nr + 1) % nr_threads;
  }
  else
    printf ("-\n");
}

int
main (int argc, char *argv[], char *envp[])
{
  int n;

  if (argc != 7)
    {
      printf ("\
Usage:\n\t<pid> <signal> <seconds> <nr-threads> <bg> <polling>\n\
Where:\n\
\t<pid> <signal>   Manager process to signal, and signal to use, once\n\
\t                 an operation has completed (e.g., running, thread\n\
\t                 created, thread exited).\n\
\t<seconds>        Number of seconds that the program should run.\n\
\t<nr-threads>     Number of threads to initially create.\n\
\t<bg>             Put the program into the background.\n\
\t<polling>        Use polling (i.e., busy-loops), rather than blocking,\n\
\t                 when synchronizing threads.\n");
      printf ("\
Operation:\n\
\tThe program creates <nr-threads> and then notifies the Manager\n\
\tProcess that it is ready using <signal>.  After <seconds> the program\n\
\texits.  The program will respond to the following signals:\n\
\t\tSIGUSR1: Create a new thread <thread-count>).\n\
\t\tSIGUSR2: Delete a thread.\n\
\tand after the request has been processed, notify the Manager Process\n\
\twith <signal>.\n");
      exit (1);
    }

  manager.pid = atol (argv[1]);
  manager.sig = atol (argv[2]);
  int sec = atol (argv[3]);
  nr_threads = atol (argv[4]);
  bg = atol (argv[5]);
  polling = atol (argv[6]);

  // Over allocate space for all the thread structures.
  threads = calloc (nr_threads + 1, sizeof (struct thread));

  // Go into the background.
  if (bg) {
    if (fork () > 0)
      exit (0);
  }

  // Disable buffering; tell the world the pid.
  setbuf (stdout, NULL);
  printf ("%d\n", getpid ());

  // Start any threads, synchronize with each to ensure that it is
  // running.
  for (n = 0; n < nr_threads; n++) {
    add_thread (0);
  }

  // Set up a signal handlers that will either add or release one
  // thread.  For each mask out the others signals.  Make certain that
  // the signals are not masked (don't ask, it appears that this
  // process, when started via vfork / fork / exec, inherits the
  // original processes mask).
  struct sigaction action;
  memset (&action, 0, sizeof (action));
  sigemptyset (&action.sa_mask);
  sigaddset (&action.sa_mask, SIGUSR1);
  sigaddset (&action.sa_mask, SIGUSR2);
  sigprocmask (SIG_UNBLOCK, &action.sa_mask, NULL);
  action.sa_handler = add_thread;
  sigaction (SIGUSR1, &action, NULL);
  action.sa_handler = del_thread;
  sigaction (SIGUSR2, &action, NULL);

  // Signal that all is ready.
  signal_manager ();

  // Set up a timer so that in SEC seconds, the program is terminated.
  snooze (sec);
}
