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

/* A little program that, in response to various signals, will grow
   and/or shrink a process / task tree.  */

#define _GNU_SOURCE
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
#include <stdarg.h>

_syscall0(pid_t,gettid);
_syscall2(int, tkill, pid_t, tid, int, sig);



static void usage ()
{
  printf ("\
Usage:\n\
  child [ <options> ] <sleep> [ <pid> ]\n\
Where:\n\
  <sleep>      Number of seconds that the program should sleep\n\
               before exiting.\n\
  <pid>        Manager process to send task acknowledgments to.\n\
And valid options are:\n\
  --wait=busy-loop\n\
               Use a busy-loop loop, instead of sigsuspend when\n\
               waiting for a signal.\n\
  --wait=suspend\n\
               Use the blocking sigsuspend call when waiting for\n\
               a signal. (default)\n\
  --filename=<program>\n\
               When performing an exec, execute <program> instead of\n\
               argv[0].\n\
Operation:\n\
  Each task, once started, sends <pid> a SIGUSR1 notification, and\n\
  then waits for and processes signal requests.  After <sleep> seconds\n\
  the program will exit.  Valid signal commands are:\n\
    SIGUSR1:   Add a clone\n\
    SIGUSR2:   Delete a clone\n\
    SIGHUP:    Add a fork\n\
    SIGINT:    Delete a fork\n\
    SIGURG:    Terminate a fork; results in a child zombie\n\
    SIGALRM:   Exit.\n\
    SIGFPE:    Exec program in a clone\n\
    SIGPWR:    Re-exec this program\n\
    SIGPIPE:   (internal) Parent exited event (child notifies with SIGUSR1).\n\
    SIGCHLD:   (internal) Child exited event.\n\
  For any operation, the parent also acks by sending a SIGUSR2\n\
");
}



// Like perror() but also abort the program.
static void pfatal (const char *syscall) __attribute__ ((noreturn));
static void
pfatal (const char *syscall)
{
  perror (syscall);
  abort ();
}

// Wrapper to check that a function's return status is zero; use as
// OK (func,(arg list)).
static void
ok (const char *call, int status)
{
  if (status != 0)
    pfatal (call);
}
#define OK(FUNC,ARGS) ok (#FUNC, FUNC ARGS)

// Print a fatal error (and abort).
static void fatal  (const char *fmt, ...) __attribute__ ((noreturn))  __attribute__ ((format (printf, 1, 2)));
static void
fatal (const char *fmt, ...)
{
  char *buf;
  va_list ap;
  va_start (ap, fmt);
  if (vasprintf (&buf, fmt, ap) < 0)
    pfatal ("vasprintf");
  fprintf (stderr, "%d.%d: %s\n", getpid (), gettid (), buf);
  abort ();
}

// Print a trace message.
static void trace (const char *fmt, ...) __attribute__ ((format (printf, 1, 2)));
static void
trace (const char *fmt, ...)
{
  char *buf;
  va_list ap;
  va_start (ap, fmt);
  if (vasprintf (&buf, fmt, ap) < 0)
    pfatal ("vasprintf");
  va_end (ap);
  printf ("%d.%d: %s\n", getpid (), gettid (), buf);
  free (buf);
}



const int CHILD_SIG = SIGUSR1;
const int PARENT_SIG = SIGUSR2;
pid_t manager_tid;

static void notify_manager (int sig, const char *fmt, ...)  __attribute__ ((format (printf, 2, 3)));
static void
notify_manager (int sig, const char *fmt, ...)
{
  if (manager_tid > 0) {
    // Use tkill, instead of kill so that an exact task is signalled.
    // Normal kill can send the signal to "any task" and "any [other]
    // task" may not be ready.
    char *reason;
    va_list ap;
    va_start (ap, fmt);
    if (vasprintf (&reason, fmt, ap) < 0)
      pfatal ("vasprintf");
    va_end (ap);
    trace ("notify %d with %d (%s) -- %s", manager_tid,
	   sig, strsignal (sig), reason);
    free (reason);
    if (tkill (manager_tid, sig) < 0) {
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
char *main_filename;
char **main_argv;
char **main_envp;
sigset_t sigmask;

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

  // If a child processes' main thread finds that it's parent exits,
  // get a SIGPIPE.
  if (main_pid != tiddle->pid) {
    if (tiddle->pid == tiddle->tid) {
      prctl (PR_SET_PDEATHSIG, SIGPIPE);
    }
  }

  // Signal that this new "child" task is ready.
  notify_manager (CHILD_SIG, "new thread %d.%d",
		  (int) tiddle->pid, (int) tiddle->tid);

  // handle any signals.
  while (1) {
    // Block waiting on a signal.
    if (use_busy_wait) {
      sigprocmask (SIG_UNBLOCK, &sigmask, NULL);
      while (tiddle->sig == 0);
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
	notify_manager (PARENT_SIG, "clone 0x%lx @ %d created (added)",
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
	    notify_manager (PARENT_SIG, "clone 0x%lx @ %d canceled (deleted)",
			    (long) thread, i);
	    break;
	  }
	}
	if (i >= MAX_PIDS)
	  notify_manager (PARENT_SIG, "clone delete failed");
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
	  notify_manager (PARENT_SIG, "forked %d", (int) pid);
	  break;
	}
      }
      break;
    case SIGINT:
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
	  int pid = waitpid (sigchld_pid, &status, 0);
	  sigchld_pid = 0;
	  notify_manager (PARENT_SIG, "%d exited with status 0x%x",
			  pid, status);
	}
	else if (sigchld_pid < 0)
	  // don't bother waiting.
	  notify_manager (PARENT_SIG, "discarded SIGCHLD");
      }
      break;
    case SIGPIPE:
      {
	trace ("parent exit");
	// Child notifies the manager that the parent [correctly]
	// switched to process one.
	if (getppid () == 1)
	  notify_manager (CHILD_SIG, "process-parent switched to 1");
      }
      break;
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
 		  notify_manager (PARENT_SIG, "send signal %s to %d:%d -- request exec",
		  		  strsignal (SIGPWR), tids[i].pid, tids[i].tid);
		  break;
		}
	    }
	  if (i == MAX_PIDS)
	    notify_manager (PARENT_SIG, "clone exec (SIGFPE) failed");
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
	const int ARGN = 7;
	char **argv = calloc (ARGN, sizeof (const char *));
	asprintf (&argv[0], "%d:%d", tiddle->pid, tiddle->tid);
	asprintf (&argv[1], "--wait=%s", use_busy_wait ? "busy-loop" : "suspend");
	asprintf (&argv[2], "--filename=%s", main_filename);
	int argi;
	for (argi = 0; main_argv[argi] != NULL; argi++)
	  argv[3 + argi] = main_argv[argi];
	argv[3 + argi] = NULL;
	if (ARGN <= 3 + argi)
	  fatal ("argument overflow");
	trace ("exec from %d:%d, argv[0]=%s argv[1]=%s argv[2]=%s",
		getpid (), gettid (), argv[0], argv[1], argv[2]); 
	execve (main_filename, argv, main_envp);
	// Any execve return is an error.
	pfatal ("execve");
      }
      break;
    default:
      fatal ("unknown signal %d", sig);
    }
  }
}

int
main (int argc, char *argv[], char *envp[])
{
  int argi;
  const char FILENAME_OPT[] = "--filename=";

  main_filename = argv[0];
  main_pid = getpid ();
  main_argv = argv;
  main_envp = envp;

  for (argi = 0; argi < argc; argi++)
    trace ("argv[%d]=%s", argi, argv[argi]);

  // Parse any arguments; do it on the cheap.
  for (main_argv++; *main_argv != NULL; main_argv++) {
    if (strcmp (*main_argv, "--wait=busy-loop") == 0)
      use_busy_wait = 1;
    else if (strcmp (*main_argv, "--wait=suspend") == 0)
      use_busy_wait = 0;
    else if (strncmp (*main_argv, FILENAME_OPT, strlen (FILENAME_OPT)) == 0)
      main_filename = *main_argv + strlen (FILENAME_OPT);
    else
      break;
  }

  // Too few, too many, parameters?
  if (argv + argc <= main_argv
      || argv + argc > main_argv + 2) {
    usage ();
    exit (1);
  }
  int sec = atol (main_argv[0]);

  if (main_argv[1] != NULL)
    manager_tid = atol (main_argv[1]);

  // Disable buffering; tell the world the pid.
  setbuf (stdout, NULL);
  trace ("starting %d", getpid ());

  // Create a signal-mask containing all the signals that this program
  // is interested in; mask all those signals.

  sigemptyset (&sigmask);
  int signals[] = { SIGUSR1, SIGUSR2, SIGURG, SIGINT, SIGHUP, SIGPIPE, SIGALRM, SIGCHLD, SIGPWR, SIGFPE };
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

  // Set up a timer so that in SEC seconds, the program is terminated.
  alarm (sec);
  server (NULL);
  return 0;
}
