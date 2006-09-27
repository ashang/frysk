/*
 * frysk-imports/tests/frysk3231
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

volatile pid_t pid;
  
void
timeout (int sig)
{
  fprintf (stderr, "test timeout, aborting\n");
  ptrace (PTRACE_DETACH, pid, NULL, NULL);
  kill (pid, SIGKILL);
  exit (1);
}

int
main (int ac, char * av[])
{
  fprintf (stderr, "Create a detached child\n");
  pid = fork ();
  switch (pid)
    {
    case -1: // Foobar
      {
	perror ("fork");
	exit (1);
      }
    case 0: // Child; sleeps until attach
      {
	int timeout = 5;
	do
	  timeout -= sleep (timeout);
	while (timeout > 0);
	exit (1);
      }
    default: // Parent
      {
	fprintf (stderr, "Set up abort cleanup\n");
	signal (SIGALRM, timeout);
	alarm (1);	

	fprintf (stderr, "Attach to child and wait for the stop\n");
	if (ptrace (PTRACE_ATTACH, pid, NULL, NULL) < 0)
	  {
	    perror ("ptrace -- for attach");
	    exit (1);
	  }
	if (waitpid (pid, NULL,  __WALL) < 0)
	  {
	    perror ("waitpid -- for attach");
	    exit (1);
	  }

	fprintf (stderr, "Detach from child with a SIGKILL\n");
	if (ptrace (PTRACE_DETACH, pid, NULL, (void *)SIGKILL) < 0)
	  {
	    perror ("ptrace -- for detach with SIGKILL");
	    exit (1);
	  }

	fprintf (stderr, "Wait for the child's death\n");
	if (waitpid (pid, NULL,  __WALL) < -1)
	  {
	    perror ("waitpid -- for detached SIGKILLed child");
	    exit (1);
	  }
      }
    }
  return 0;
}
