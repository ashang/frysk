#include <unistd.h>
#include <assert.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <error.h>
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
  //  abort ();
  fprintf (stderr, "timeout\n");
  exit (1);
}

static unsigned char regs[0x10000];

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

  /* Buggy:
  kernel-xen-2.6.19-1.2898.2.3.fc7.i686
  kernel-2.6.20-1.2925.fc6.i586
  kernel-2.6.20-1.2928.rm1.fc6.x86_64
     Fixed:
  kernel-2.6.20-1.2935.rm1.fc6.x86_64
  */
  i = ptrace (PTRACE_ATTACH, child_pid, NULL, NULL);
  //assert (i == 0);
  if (i != 0) error (1, errno, "PTRACE_ATTACH");
  i = waitpid (child_pid, &status, 0);
  //  assert (i == child_pid);
  if (i != child_pid) {
    if (-1 == i)  error (1, errno, "waitpid");
    else {
      fprintf (stderr, "waitpid pid mismatch\n");
      exit (1);
    }
  }
  //  assert (WIFSTOPPED (status) != 0);
  if (WIFSTOPPED (status) == 0) {
    fprintf (stderr, "WIFSTOPPED false\n");
    exit (1);
  }
  //  assert (WSTOPSIG (status) == SIGSTOP);
  if (WSTOPSIG (status) != SIGSTOP) {
    fprintf (stderr, "WSTOPSIG !- SIGSTOP\n");
    exit (1);
  }

  alarm (0);

  /* Buggy:
  kernel-2.6.20-1.2935.rm1.fc6.x86_64
  */
  errno = 0;
  ptrace (PTRACE_PEEKUSER, child_pid, NULL, NULL);
  //assert (errno == 0);
  if (errno != 0) error (1, errno, "PTRACE_PEEKUSER");
  ptrace (PTRACE_GETREGS, child_pid, NULL, regs);
  //assert (errno == 0);
  if (errno != 0) error (1, errno, "PTRACE_GETREGS");
  
  //  puts ("OK");
  kill (child_pid, SIGKILL);
  //return 0;
  exit (0);
}
