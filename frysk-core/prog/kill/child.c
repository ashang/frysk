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

/* A little program that, in response to various signals, will grow
   and/or shrink a process / task tree.  */

#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/select.h>
#include <linux/unistd.h>
#include <limits.h>
#include <pthread.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/prctl.h>

_syscall0(pid_t,gettid);
_syscall2(int, tkill, pid_t, tid, int, sig);


static void usage ()
{
  printf ("\
Usage:\n\
\t<sleep> [ <pid> ]\n\
Where:\n\
\t<sleep>          Number of seconds that the program should sleep before\n\
\t                 exiting.\n\
\t<pid>            Manager process to send task acknowledgments to.\n\
Operation:\n\
\tEach task, once started, sends <pid> a SIGUSR1 notification, and then\n\
\twaits <sleep> seconds for signal commands.  Valid signal commands are:\n\
\t\tSIGUSR1: Add a clone\n\
\t\tSIGUSR2: Delete a fork\n\
\t\tSIGHUP: Add a fork\n\
\t\tSIGINT: Delete a fork\n\
\t\tSIGURG: Terminate a fork, do not reap\n\
\t\tSIGALRM: Exit.\n\
\t\tSIGPWR: Re-exec this program\n\
\t\tSIGPIPE: (internal) Parent exited event.\n\
\t\tSIGCHLD: (internal) Child exited event.\n\
\tFor any operation, the parent also acks by sending a SIGUSR2\n\
");
}

pid_t manager_tid;
static void
notify_manager (int sig)
{
  if (manager_tid > 0) {
    // Use tkill, instead of kill so that an exact task is signalled.
    // Normal kill can send the signal to "any task" and "any [other]
    // task" may not be ready.
    printf ("%d:child.tkill %d %d\n", gettid (), manager_tid, sig);
    if (tkill (manager_tid, sig) < 0) {
      perror ("tkill");
      exit (errno);
    }
  }
}


#define MAX_PIDS 100
struct tiddle {
  pid_t pid;
  pid_t tid;
  int sig;
  int nr_threads;
  pthread_t threads[MAX_PIDS];
  int nr_forks;
  pid_t forks[MAX_PIDS];
};
struct tiddle tids[MAX_PIDS];
pthread_mutex_t tids_mutex;

struct tiddle *find_tiddle ()
{
  pthread_mutex_lock (&tids_mutex);
  int i;
  pid_t tid = gettid ();
  for (i = 0; i < MAX_PIDS; i++) {
    if (tids[i].tid == tid) {
      pthread_mutex_unlock (&tids_mutex);
      return &tids[i];
    }
  }
  printf ("tid %d not found\n", (int) tid);
  abort ();
}

struct tiddle *new_tiddle ()
{
  pthread_mutex_lock (&tids_mutex);
  int i;
  pid_t pid = getpid ();
  pid_t tid = gettid ();
  for (i = 0; i < MAX_PIDS; i++) {
    if (tids[i].tid == 0) {
      tids[i].tid = tid;
      tids[i].pid = pid;
      pthread_mutex_unlock (&tids_mutex);
      return &tids[i];
    }
  }
  printf ("no space for %d\n", (int) tid);
  abort ();
}

static void
handler (int sig)
{
  printf ("%d!%d (%s)\n", gettid (), sig, strsignal (sig));
  struct tiddle *tiddle = find_tiddle ();
  tiddle->sig = sig;
}

int main_pid;
char **main_argv;
char **main_envp;

void *
server (void *np)
{
  struct tiddle *tiddle = NULL;
  // Action for a SIGCHLD: >0 -> waitpid and ack; <0 -> ack; ==0 ->
  // discard.
  int sigchld_pid = 0;
 start:
  // Find this tasks entry.
  tiddle = new_tiddle ();
  // If a child processes' main thread finds that it's parent exits,
  // get a SIGPIPE.
  if (main_pid != tiddle->pid) {
    if (tiddle->pid == tiddle->tid) {
      prctl (PR_SET_PDEATHSIG, SIGPIPE);
    }
  }
  // Signal that this task is ready.
  notify_manager (SIGUSR1);
  printf ("+%d\n", (int) tiddle->tid);
  // handle any signals.
  while (1) {
    // Block waiting on a signal.
    sigset_t mask;
    sigemptyset (&mask);
    sigsuspend (&mask);
    // Now check the signal.
    int sig = tiddle->sig;
    tiddle->sig = 0;
    switch (sig) {
    case SIGUSR1: // add clone
      {
	// Create the new task.
	pthread_attr_t pthread_attr;
	pthread_attr_init (&pthread_attr);
	// pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);
	pthread_create (&tiddle->threads[tiddle->nr_threads],
			&pthread_attr, server, (void *) NULL);
	tiddle->nr_threads = (tiddle->nr_threads + 1) % MAX_PIDS;
	notify_manager (SIGUSR2);
      }
      break;
    case SIGUSR2: // delete clone
      {
	int i;
	for (i = 0; i < MAX_PIDS; i++) {
	  if (tiddle->threads[i] != 0) {
	    void **arg;
	    pthread_t thread = tiddle->threads[i];
	    tiddle->threads[i] = 0;
	    pthread_cancel (thread);
	    pthread_join (thread, arg);
	    break;
	  }
	}
	notify_manager (SIGUSR2);
      }
      break;
    case SIGHUP: // add fork
      {
	pid_t pid = fork ();
	switch (pid) {
	case -1:
	  perror ("fork");
	  exit (1);
	case 0: // child
	  goto start;
	  break;
	default: // parent
	  tiddle->forks[tiddle->nr_forks] = pid;
	  tiddle->nr_forks = (tiddle->nr_forks + 1) % MAX_PIDS;
	  notify_manager (SIGUSR2);
	  break;
	}
      }
      break;
    case SIGINT: // delete fork (see also SIGCHLD)
      {
	int i;
	for (i = 0; i < MAX_PIDS; i++) {
	  if (tiddle->forks[i] != 0) {
	    sigchld_pid = tiddle->forks[i];
	    tiddle->forks[i] = 0;
	    kill (sigchld_pid, SIGKILL);
	  }
	}
      }
      break;
    case SIGURG: // zombie fork (two parts), see also SIGCHLD)
      {
	int i;
	for (i = 0; i < MAX_PIDS; i++) {
	  if (tiddle->forks[i] != 0) {
	    int pid = tiddle->forks[i];
	    tiddle->forks[i] = 0;
	    sigchld_pid = -1;
	    tkill (pid, SIGKILL);
	    printf ("-%d\n", pid);
	  }
	}
      }
      break;
    case SIGCHLD:
      {
	if (sigchld_pid > 0) {
	  int status;
	  int pid = waitpid (sigchld_pid, &status, 0);
	  printf ("-%d\n", pid);
	  sigchld_pid = 0;
	  notify_manager (SIGUSR2);
	}
	else if (sigchld_pid < 0)
	  notify_manager (SIGUSR2);
      }
      break;
    case SIGPIPE: // parent exit
      // Only notify the manager when the parent [correctly] switched
      // to process one.
      if (getppid () == 1)
	notify_manager (SIGUSR2);
      break;
    case SIGALRM: // exit all
      {
	printf ("-%d\n", (int) tiddle->pid);
      }
      exit (0);
    case SIGPWR:
      {
	execve (main_argv[0], main_argv, main_envp);
	// Any execve return is an error.
	perror ("execve");
	exit (errno);
      }
      break;
    default:
      abort ();
    }
  }
}

int
main (int argc, char *argv[], char *envp[])
{
  int n;

  if (argc < 2 || argc > 3)
    {
      usage ();
      exit (1);
    }

  int sec = atol (argv[1]);
  if (argc > 2)
    manager_tid = atol (argv[2]);
  main_pid = getpid ();
  main_argv = argv;
  main_envp = envp;

  // Disable buffering; tell the world the pid.
  setbuf (stdout, NULL);
  printf ("%d\n", getpid ());

  // Set up a signal handlers that will either add or release one
  // thread.  For each mask out the others signals.  Make certain that
  // the signals are not masked (don't ask, it appears that this
  // process, when started via vfork / fork / exec, inherits the
  // original processes mask).

  struct sigaction action;
  memset (&action, 0, sizeof (action));
  sigemptyset (&action.sa_mask);
  int signals[] = { SIGUSR1, SIGUSR2, SIGURG, SIGINT, SIGHUP, SIGPIPE, SIGALRM, SIGCHLD, SIGPWR };
  int i;
  for (i = 0; i < sizeof (signals) / sizeof(signals[0]); i++)
    sigaddset (&action.sa_mask, signals[i]);
  action.sa_handler = handler;
  sigprocmask (SIG_BLOCK, &action.sa_mask, NULL);
  for (i = 0; i < sizeof (signals) / sizeof(signals[0]); i++)
    sigaction (signals[i], &action, NULL);

  // Set up a timer so that in SEC seconds, the program is terminated.
  alarm (sec);
  server (NULL);
}
