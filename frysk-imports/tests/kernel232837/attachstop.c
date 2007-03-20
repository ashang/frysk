#include <unistd.h>
#include <assert.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>


static void
child_f (void)
{
  raise (SIGSTOP);
  abort ();
  /* NOTREACHED */
}

static pid_t child_pid;

static void
handler (int signo)
{
  kill (child_pid, SIGKILL);
  //abort ();
  exit (1);
}

int main (void)
{
  int i;
  int status;

  child_pid = fork();
  switch (child_pid)
    {
      case -1:
        perror ("fork()");
	exit (1);
	/* NOTREACHED */
      case 0:
        child_f ();
	/* NOTREACHED */
      default:;
	/* PASSTHRU */
    }
  /* Parent.  */

  sleep (1);

  signal (SIGALRM, handler);
  alarm (1);

  i = ptrace (PTRACE_ATTACH, child_pid, NULL, NULL);
  assert (i == 0);
  i = waitpid (child_pid, &status, 0);
  assert (i == child_pid);
  assert (WIFSTOPPED (status) != 0);
  assert (WSTOPSIG (status) == SIGSTOP);

  alarm (0);
  
  //  puts ("OK");
  kill (child_pid, SIGKILL);
  return 0;
}
