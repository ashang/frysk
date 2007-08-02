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

/* On receipt of a signal, exec the program passed as an argument.  */

#define _GNU_SOURCE

#include <signal.h>
#include <ctype.h>
#include <pthread.h>
#include <limits.h>
#include <stdlib.h>

#include "funit-util.h"



static void
usage ()
{
  printf ("\
Usage: funit-exec [OPTION] PROGRAM ARGS ...\n\
On receipt of a signal, perform the operation:\n\
    SIGINT (cntrl-c)   The signaled thread execs PROGRAM with ARGS\n\
    SIGUSR1            Signal a non-main thread with SIGINT (it execs)\n\
    SIGABORT (cntrl-\\) Abort program.\n\
Where valid options are:\n\
    -e EXE           Instead of PROGRAM, exec EXE\n\
    -c COUNT         Create COUNT threads\n\
    -t TIMEOUT       Set an ALARM to TIMEOUT seconds; after which exit\n\
                     Default is to not exit\n\
    -s SIGNAL\n\
    -m MASTER        Once running, send SIGNAL to process MASTER\n\
                     Default is to send signal 0 to process 0\n\
    <program> ...    Program to exec.\n\
");
  exit (1);
}



char *exec_exe;
char **exec_argv;
char **exec_envp;

void
exec_handler (int sig)
{
  trace ("exec %s", exec_exe);
  execve (exec_exe, exec_argv, exec_envp);
  pfatal ("execve");
}

static volatile int random_thread;

void
random_handler (int sig)
{
  trace ("random %d", random_thread);
  if (tkill (random_thread, SIGINT) < 0)
    pfatal ("tkill");
}



// Simplistic thread that just blocks.

static pthread_barrier_t barrier;
static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;

void *
op_block (void *np)
{
  trace ("created");
  random_thread = gettid ();
  OK (pthread_detach, (pthread_self ()));
  pthread_barrier_wait (&barrier); // Can't check error status.
  OK (pthread_mutex_lock, (&lock));
  trace ("exiting");
  return NULL;
}

long
stringtolong (const char * string)
{
	char *end;
	long val = strtol (string, &end, 0);
	
	if (*end != '\0')
	{
		printf("Invalid integer argument %s\n", string);
		usage();
	}	
	return val; 
}

int
main (int argc, char *argv[], char *envp[])
{
  int i;
  int sig = 0;
  int pid = 0;
  int timeout = 0;
  int nr_threads = 0;
  int opt;

  while ((opt = getopt(argc, argv, "+m:s:t:e:c:")) != -1) {
    switch (opt) {
    case 'm': // master
      pid = atoi (optarg);
      break;
    case 's': // signal
      sig = atoi (optarg);
      break;
    case 't': // timeout
      timeout = atoi (optarg);
      break;
    case 'e': // executable
      exec_exe = optarg;
      break;
    case 'c': // clone count
      nr_threads = atoi (optarg);
      break;
    case '?':
      exit(1);
    }
  }

  if (optind == argc) {
    usage();
    exit(1);
  }

  if (exec_exe == NULL)
    exec_exe = argv[optind];
  exec_argv = argv + optind;
  exec_envp = envp;

  trace ("nr_threads = %d", nr_threads);
  trace ("master pid = %d", pid);
  trace ("master sig = %d", sig);
  trace ("timeout = %d", timeout);
  trace ("exec exe = %s", exec_exe);

  alarm(timeout);

  // Minimal stack.
  pthread_attr_t pthread_attr;
  OK (pthread_attr_init, (&pthread_attr));
  OK (pthread_attr_setstacksize, (&pthread_attr, PTHREAD_STACK_MIN));

  // Synchronization barrier; need both main and the new thread.
  OK (pthread_barrier_init, (&barrier, NULL, 2));

  // Lock a mutex so that nothing can advance.
  OK (pthread_mutex_lock, (&lock));

  // Make certain that all signal masks are correctly set.
  sigset_t mask;
  sigemptyset(&mask);
  sigaddset(&mask, SIGINT);
  sigaddset(&mask, SIGUSR1);
  sigprocmask(SIG_UNBLOCK, &mask, NULL);

  trace ("creating %d threads", nr_threads);
  for (i = 0; i < nr_threads; i++) {
    pthread_t p;
    OK (pthread_create, (&p, &pthread_attr, op_block, NULL));
    pthread_barrier_wait (&barrier); // Can't check error status.
  }

  sigset (SIGINT, exec_handler);
  sigset (SIGUSR1, random_handler);

  trace ("send sig %d to pid %d", sig, pid);
  tkill (pid, sig); // ack.

  while (1) {
    trace ("calling sigsuspend");
    sigset_t mask;
    sigemptyset(&mask);	
    sigsuspend(&mask);
  }

  return 0;
}
