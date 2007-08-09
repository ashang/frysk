// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

#define _GNU_SOURCE
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/select.h>
#include <limits.h>
#include <pthread.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/prctl.h>
#include <stdarg.h>

#include "funit-util.h"



static void help ()
{
  printf("\
On receipt of the specified signal, perform the operation, and then\n\
send the MASTER (if specified) a SIGUSR2 acknowledge signal:\n\
    SIGUSR1:   Add a clone (clone acks with SIGUSR1)\n\
    SIGUSR2:   Delete a clone\n\
    SIGHUP:    Add a fork (clone acks with SIGUSR1)\n\
    SIGPROF:   Delete a fork\n\
    SIGURG:    Terminate a fork; results in a child zombie\n\
    SIGALRM:   Exit.\n\
    SIGFPE:    Exec program in a clone\n\
    SIGPWR:    Re-exec this program\n\
    SIGPIPE:   (internal) Parent exited event (child notifies with SIGUSR1).\n\
    SIGCHLD:   (internal) Child exited event.\n\
    SIGBUS:    Exit thread.\n\
");
}

static void usage ()
{
  printf ("Usage: funit-slave [OPTIONS] [EXEC-ARGV ...]\n");
  help ();
  printf ("\
Where valid options are:\n\
    -e EXE           Instead of PROGRAM, exec EXE\n\
    -w WAIT-TYPE     While waiting for signals, block using WAIT-TYPE.\n\
                     WAIT-TYPE is either \"suspend\" or \"busy-loop\".\n\
    -t TIMEOUT       Set an ALARM to TIMEOUT seconds; after which exit\n\
                     Default is to not exit\n\
    -m MASTER        Once running, send SIGUSR1 to process MASTER\n\
                     Default is to send signal to process 0\n\
    <exec-argv>      Optional program and arguments to exec, by default\n\
                     this program, with identical aguments, will be execed.\n\
");
}



const int CHILD_SIG = SIGUSR1;
const int PARENT_SIG = SIGUSR2;
pid_t master_tid;
int master_sig;

static void notify_master (int sig, const char *fmt, ...)  __attribute__ ((format (printf, 2, 3)));
static void
notify_master (int sig, const char *fmt, ...)
{
  // Use tkill, instead of kill so that an exact task is signalled.
  // Normal kill sends the signal to <<any available task>> which may
  // not be the intended recepient.
  char *reason;
  va_list ap;
  va_start (ap, fmt);
  if (vasprintf (&reason, fmt, ap) < 0)
    pfatal ("vasprintf");
  va_end (ap);
  trace ("%s", reason);
  if (master_tid > 0) {
    trace ("notify %d with %d (%s) -- %s", master_tid,
	   sig, strsignal (sig), reason);
    free (reason);
    if (tkill (master_tid, sig) < 0) {
      pfatal ("tkill");
    }
  }
}



// The per-TID tiddle, in addition to space to store the TID's last
// signal, contains space for the TID's created threads and forks.

#define MAX_PIDS 100
struct tiddle {
  pid_t pid;
  pid_t tid;
  volatile int sig;
  int nr_threads;
  pthread_t threads[MAX_PIDS];
  int nr_forks;
  pid_t forks[MAX_PIDS];
};
struct tiddle tids[MAX_PIDS];
pthread_mutex_t tids_mutex;

struct tiddle *find_tiddle ()
{
  OK (pthread_mutex_lock, (&tids_mutex));
  int i;
  pid_t tid = gettid ();
  for (i = 0; i < MAX_PIDS; i++) {
    if (tids[i].tid == tid) {
      OK (pthread_mutex_unlock, (&tids_mutex));
      return &tids[i];
    }
  }
  fatal ("tid %d not found in tiddle table", (int) tid);
}

struct tiddle *new_tiddle ()
{
  OK (pthread_mutex_lock, (&tids_mutex));
  int i;
  pid_t pid = getpid ();
  pid_t tid = gettid ();
  for (i = 0; i < MAX_PIDS; i++) {
    if (tids[i].tid == 0) {
      tids[i].tid = tid;
      tids[i].pid = pid;
      OK (pthread_mutex_unlock, (&tids_mutex));
      return &tids[i];
    }
  }
  fatal ("no space for %d in tiddle table", (int) tid);
}



// Interrupt handler, attached to all relevant signals on all threads.
// Save the interrupting signal in the TID's tiddle block.

static void
handler (int sig)
{
  trace ("received signal %d (%s)", sig, strsignal (sig));
  struct tiddle *tiddle = find_tiddle ();
  tiddle->sig = sig;
}



// Per-thread server, wait for a signal and then performs that
// signal's corresponding action.

int use_busy_wait = 0;
int main_pid;
int timeout;
sigset_t sigmask;
char *exec_program;
char **exec_argv;
char **exec_envp;

void *
server (void *np)
{
  // This task's tiddle.
  struct tiddle *tiddle = NULL;
  // Action for a SIGCHLD:: >0: waitpid and ack; <0: ack; =0: discard.
  int sigchld_pid = 0;

  // Come here whenever a new process is created.
 new_process:

  // Find THIS tasks tiddle entry.
  tiddle = new_tiddle ();

  // If the main thread, set up a timer so that in TIMEOUT seconds,
  // the program receives a SIGALARM causing the program to exit.
  if (tiddle->pid == tiddle->tid)
    alarm (timeout);

  // If a child processes' main thread finds that it's parent exits,
  // get a SIGPIPE.
  if (main_pid != tiddle->pid) {
    if (tiddle->pid == tiddle->tid) {
      prctl (PR_SET_PDEATHSIG, SIGPIPE);
    }
  }

  // Signal that this new "child" task is ready.
  notify_master (CHILD_SIG, "new thread %d.%d",
		  (int) tiddle->pid, (int) tiddle->tid);

  // handle any signals.
  while (1) {
    // Block waiting on a signal.
    if (use_busy_wait) {
      sigprocmask (SIG_UNBLOCK, &sigmask, NULL);
      while (tiddle->sig == 0)
	;
      sigprocmask (SIG_BLOCK, &sigmask, NULL);
    }
    else {
      sigset_t empty_mask;
      sigemptyset (&empty_mask);
      sigsuspend (&empty_mask);
    }
    // Now check the signal.
    int sig = tiddle->sig;
    tiddle->sig = 0;
    switch (sig) {
    case SIGUSR1:
      {
	trace ("add clone");
	// Create the new task.
	pthread_attr_t pthread_attr;
	OK (pthread_attr_init, (&pthread_attr));
	// pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);
	pthread_t thread;
	OK (pthread_create, (&thread, &pthread_attr, server, (void *) NULL));
	notify_master (PARENT_SIG, "clone 0x%lx @ %d created (added)",
			thread, tiddle->nr_threads);
	tiddle->threads[tiddle->nr_threads] = thread;
	tiddle->nr_threads = (tiddle->nr_threads + 1) % MAX_PIDS;
      }
      break;
    case SIGUSR2:
      {
	trace ("delete clone");
	int i;
	for (i = 0; i < MAX_PIDS; i++) {
	  trace ("delete %d 0x%lx?", i, (long) tiddle->threads[i]);
	  if (tiddle->threads[i] != 0) {
	    pthread_t thread = tiddle->threads[i];
	    tiddle->threads[i] = 0;
	    trace ("pthread_cancel");
	    OK (pthread_cancel, (thread));
	    trace ("pthread_join");
	    OK (pthread_join, (thread, NULL));
	    notify_master (PARENT_SIG, "clone 0x%lx @ %d canceled (deleted)",
			    (long) thread, i);
	    break;
	  }
	}
	if (i >= MAX_PIDS)
	  notify_master (PARENT_SIG, "clone delete failed");
      }
      break;
    case SIGHUP:
      {
	trace ("add fork");
	pid_t pid = fork ();
	switch (pid) {
	case -1:
	  pfatal ("fork");
	case 0: // child
	  goto new_process;
	  break;
	default: // parent
	  tiddle->forks[tiddle->nr_forks] = pid;
	  tiddle->nr_forks = (tiddle->nr_forks + 1) % MAX_PIDS;
	  notify_master (PARENT_SIG, "forked %d", (int) pid);
	  break;
	}
      }
      break;
    case SIGPROF:
      {
	trace ("delete fork"); // see also SIGCHLD
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
    case SIGURG:
      {
	trace ("zombie fork"); // see also SIGCHLD
	int i;
	for (i = 0; i < MAX_PIDS; i++) {
	  if (tiddle->forks[i] != 0) {
	    int pid = tiddle->forks[i];
	    tiddle->forks[i] = 0;
	    sigchld_pid = -1;
	    trace ("send signal %d to task %d -- create zombie",
		   SIGKILL, pid);
	    tkill (pid, SIGKILL);
	  }
	}
      }
      break;
    case SIGCHLD:
      {
	trace ("sub-process exited");
	if (sigchld_pid > 0) {
	  int status;
	  trace ("delete fork -- waitpid %d", sigchld_pid);
	  int pid = waitpid (sigchld_pid, &status, 0);
	  sigchld_pid = 0;
	  notify_master (PARENT_SIG,
			  "delete fork -- %d exited with status 0x%x",
			  pid, status);
	}
	else if (sigchld_pid < 0)
	  // don't bother waiting.
	  notify_master (PARENT_SIG, "zombie fork - discarding SIGCHLD");
	else
	  notify_master (PARENT_SIG, "ignoring SIGCHLD");
      }
      break;
    case SIGPIPE:
      {
	trace ("parent exit");
	// Child notifies the manager that the parent [correctly]
	// switched to process one.
	if (getppid () == 1)
	  notify_master (CHILD_SIG, "process-parent switched to 1");
      }
      break;
    case SIGINT:
    case SIGALRM: // exit all
      {
	trace ("exit");
	exit (0);
      }
    case SIGFPE:
      {
	trace ("clone exec"); // request that a peer do an exec
	OK (pthread_mutex_lock, (&tids_mutex));
	{
	  // Find a clone.
	  int i;
	  for (i = 0; i < MAX_PIDS; i++) 
	    {
	      if (tids[i].pid == getpid () && tids[i].tid != gettid ())
		{
		  tkill (tids[i].tid, SIGPWR);
 		  notify_master (PARENT_SIG, "send signal %s to %d:%d -- request exec",
		  		  strsignal (SIGPWR), tids[i].pid, tids[i].tid);
		  break;
		}
	    }
	  if (i == MAX_PIDS)
	    notify_master (PARENT_SIG, "clone exec (SIGFPE) failed");
	}
	OK (pthread_mutex_unlock, (&tids_mutex));
      }
      break;
    case SIGPWR:
      {
	trace ("exec");
	// This is mutual to SIGFPE above - don't start the exec until
	// after the SIGFPE code has notified the manager that it's
	// part is finished.
	OK (pthread_mutex_lock, (&tids_mutex));
	char **argv;
	if (exec_argv != NULL) {
	  argv = exec_argv;
	  trace ("execing %s argv[0]=%s ...", exec_program, argv[0]);
	}
	else {
	  const int ARGN = 9;
	  argv = calloc (ARGN, sizeof (const char *));
	  asprintf (&argv[0], "%d:%d", tiddle->pid, tiddle->tid);
	  asprintf (&argv[1], "-w");
	  asprintf (&argv[2], "%s", use_busy_wait ? "busy-loop" : "suspend");
	  asprintf (&argv[3], "-e");
	  asprintf (&argv[4], "%s", exec_program);
	  asprintf (&argv[5], "-t");
	  asprintf (&argv[6], "%d", timeout);
	  asprintf (&argv[7], "%d", master_tid);
	  argv[8] = NULL;
	  trace ("execing %s 0=%s 1=%s 2=%s 3=%s 4=%s 5=%s 6=%s 7=%s",
		 exec_program,
		 argv[0], argv[1], argv[2], argv[3], argv[4], argv[5],
		 argv[6], argv[7]);
	}
	execve (exec_program, argv, exec_envp);
	// Any execve return is an error.
	pfatal ("execve");
      }
      break;
    case SIGBUS:
      {
	trace ("exit this thread");
        return NULL;
      }
    default:
      fatal ("unknown signal %d", sig);
    }
  }
}

int
main (int argc, char *argv[], char *envp[])
{
  // Always print the usage
  help();

  int argi;
  for (argi = 0; argi < argc; argi++)
    trace ("argv[%d]=%s", argi, argv[argi]);

  main_pid = getpid ();
  exec_program = NULL;
  exec_argv = NULL;
  exec_envp = envp;
  timeout = 0; // infinite
  master_tid = 0;
  master_sig = 0;
  use_busy_wait = 0;
  int opt;

  while ((opt = getopt(argc, argv, "+w:e:t:m:s:h")) != -1)
    {
      switch (opt)
	{
	case 'w': // wait type
	  if (strcmp(optarg, "busy-loop") == 0)
	    use_busy_wait = 1;
	  else if (strcmp(optarg, "suspend") == 0)
	    use_busy_wait = 0;
	  else {
	    fprintf (stderr, "Unknown wait option: %s\n", optarg);
	    exit(1);
	  }
	  break;
	case 'e': // PROGRAM to exec
	  exec_program = optarg;
	  break;
	case 't': // Timeout
	  timeout = atoi(optarg);
	  break;
	case 'm': // Master pid
	  master_tid = atoi(optarg);
	  break;
	case 's': // Master sig
	  master_sig = atoi(optarg);
	  break;
	case 'h':
	  usage();
	  exit(0);
	case '?':
	  usage();
	  exit(1);
	}
    }

  // Grab the exec-argv.
  if (optind < argc)
    {
      exec_argv = argv + optind;
      if (exec_program == NULL)
	exec_program = argv[optind];
    }
  else
    {
      exec_argv = argv;
      if (exec_program == NULL)
	exec_program = argv[0];
    }
  exec_envp = envp;

  // Disable buffering; tell the world the pid.
  setbuf (stdout, NULL);
  trace ("starting %d", getpid ());

  // Create a signal-mask containing all the signals that this program
  // is interested in; mask all those signals.

  sigemptyset (&sigmask);
  int signals[] = {
    SIGUSR1, SIGUSR2, SIGURG, SIGINT, SIGHUP, SIGPIPE, SIGALRM, SIGCHLD, SIGPWR, SIGFPE, SIGBUS, SIGPROF
  };
  int i;
  for (i = 0; i < sizeof (signals) / sizeof(signals[0]); i++)
    sigaddset (&sigmask, signals[i]);
  sigprocmask (SIG_BLOCK, &sigmask, NULL);

  // Set up a signal handlers that will either add or release one
  // thread.  For each mask out the others signals (found in SIGMASK).

  struct sigaction action;
  memset (&action, 0, sizeof (action));
  action.sa_handler = handler;
  action.sa_mask = sigmask;
  for (i = 0; i < sizeof (signals) / sizeof(signals[0]); i++)
    sigaction (signals[i], &action, NULL);

  server (NULL);
  return 0;
}
