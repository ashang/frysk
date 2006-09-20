/*
  frysk-imports/tests/frysk3231
 */

#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <alloca.h>
#include <signal.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/ptrace.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <linux/ptrace.h>

int tid;

char *
decode_status (int status)
{
  char * rc;
  
  if (0)
    ;
  else if (WIFEXITED (status))		// (s & 0x7f) == 0
    rc = "WIFEXITED";
  else if (WIFSIGNALED (status))	// (((s & 0x7f) + 1) >> 1) > 0
    rc = "WIFSIGNALED";
  else if (WIFSTOPPED (status)) {	// (s & 0xff) == 0x7f
    asprintf (&rc, "WIFSTOPPED with signal %d", WSTOPSIG(status));
#if 0 // fixme -- will need work
    switch (WSTOPEVENT (status)) {
    case PTRACE_EVENT_CLONE:
      rc = "PTRACE_EVENT_CLONE";
      break;
    case PTRACE_EVENT_FORK:
      rc = "PTRACE_EVENT_FORK";
      break;
    case PTRACE_EVENT_EXIT:
      rc = "PTRACE_EVENT_EXIT";
      break;
    case PTRACE_EVENT_EXEC:
      rc = "PTRACE_EVENT_EXEC";
      break;
    case 0:
      rc = "0";
      break;
    default:
      rc = "Unknown";
    }
#endif
  }
  else if (WIFCONTINUED (status))		// (s == 0xffff) == 0
    rc = "WIFEXITED";
  else
    rc = "Unknown";

  return rc;
}
  
void
handler (int sig)
{
  fprintf (stderr, "handler\n");
}
  
void
timeout (int sig)
{
  fprintf (stderr, "waitpid timed out... exiting.\n");
  kill (tid, SIGKILL);
  _exit (1);
}

static int
clone_func (void *arg)
{
  fprintf (stderr, "clone %#08x\n", arg);
  while(1) {
    signal (SIGUSR1, handler);
    fprintf (stderr, "clone waiting\n");
    pause();
    fprintf (stderr, "clone caught\n");
  }
  return 56;
}

static void
do_attach(int tid)
{
  long prc;
  
  prc = ptrace (PTRACE_ATTACH, tid, NULL, NULL);
  if (-1 == prc) {
    fprintf (stderr, "\t\t\terrno = %d: ", errno);
    perror ("");
  }
  else fprintf (stderr, "\t\t\tOK\n");
  
  {
    pid_t spid;
    int sstatus;
    
    fprintf (stderr, "wait for stop\n");
    spid = waitpid (tid, &sstatus, __WALL);
    fprintf (stderr, "\treturn pid = %d status %#08x (%s)\n",
	     spid, sstatus, decode_status (sstatus));
  }
}

static void
do_detach(int tid, void * data)
{
  long prc;
  
  prc = ptrace (PTRACE_DETACH, tid, NULL, data);
  if (-1 == prc) {
    fprintf (stderr, "\terrno = %d: ", errno);
    perror ("");
  }
  else fprintf (stderr, "\tOK\n");
}

static void
do_cont (int tid, void * data)
{
  long prc;

  prc = ptrace (PTRACE_CONT, tid, NULL, data);
  if (-1 == prc) {
    fprintf (stderr, "\terrno = %d: ", errno);
    perror ("");
  }
  else fprintf (stderr, "\tOK\n");
}

main (int ac, char * av[])
{
  void ** newstack;
  int pass_mode = 0;

  {
    int c;
    while (-1 != ( c = getopt (ac, av, "pf"))) {
      switch (c) {
      case 'p': pass_mode = 1; break;
      case 'f': pass_mode = 0; break;
      default:
	fprintf (stderr, "Exiting\n");
	_exit (1);
      }
    }
  }

#define STACKSIZE 16384  
  newstack = (void **) malloc(STACKSIZE);
  newstack = (void **) (STACKSIZE + (char *) newstack);
  

  if (-1 != (tid = clone (clone_func,
			  newstack,
			  SIGCHLD,
			  (void *)88))) {

    fprintf (stderr, "started tid %d\n", tid);
    usleep (100000);		// give clone() a while to get going

    fprintf (stderr, "PTRACE_ATTACH:");
    do_attach(tid);

#if 0
    fprintf (stderr, "PTRACE_CONT with zero arg: ");
    do_cont (tid, 0);
#else
    fprintf (stderr, "PTRACE_CONT with SIGUSR1: ");
    do_cont (tid, (void *)SIGUSR1);
#endif

    usleep (100000);

    fprintf (stderr, "emitting SIGUSR1\n");
    kill (tid, SIGUSR1);

    usleep (100000);

    fprintf (stderr, "PTRACE_DETACH with SIGUSR1: ");
    do_detach(tid, (void *)SIGUSR1);

    usleep (100000);

    fprintf (stderr, "PTRACE_DETACH with zero arg -- should fail: ");
    do_detach(tid, 0);

    usleep (100000);

    fprintf (stderr, "emitting SIGUSR1\n");
    kill (tid, SIGUSR1);

    usleep (100000);

    fprintf (stderr, "PTRACE_ATTACH again:");
    do_attach(tid);
    
    usleep (100000);

    if (pass_mode) {
      fprintf (stderr, "PTRACE_DETACH with zero arg -- should fail: ");
      do_detach(tid, 0);
      usleep (100000);
      fprintf (stderr, "emitting SIGKILL\n");
      kill (tid, SIGKILL);
    }
    else {
      fprintf (stderr, "PTRACE_DETACH with SIGKILL: ");
      do_detach(tid, (void *)SIGKILL);
    }

    usleep (100000);

    signal (SIGALRM, timeout);
    alarm (5);
    
    while (1) {
      int status;
      fprintf (stderr, "waiting...\n");
      pid_t pid = waitpid (-1, &status,  __WALL);
      switch((int)pid) {
      case -1:
	perror ("\twaitpid");
	sleep (1);
	break;
      case 0:
	fprintf (stderr, "\twaitpid returned 0\n");
	break;
      default:
	fprintf (stderr, "\treturn pid = %d status %#08x (%s)\n",
		 pid, status, decode_status (status));
	_exit (0);
	break;
      }
    }
  }
  else {
    perror ("clone");
    _exit (1);
  }
}
