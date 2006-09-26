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
  
void
timeout (int sig)
{
  kill (tid, SIGKILL);
  _exit (1);
}

static int
clone_func (void *arg)
{
  pause();
  return 56;
}

main (int ac, char * av[])
{
  void ** newstack;
  int pass_mode = 0;

  signal (SIGALRM, timeout);
  alarm (1);

#define STACKSIZE 16384  
  newstack = (void **) malloc(STACKSIZE);
  newstack = (void **) (STACKSIZE + (char *) newstack);
  

  if (-1 != (tid = clone (clone_func,
			  newstack,
			  SIGCHLD,
			  (void *)88))) {

    if (-1 == ptrace (PTRACE_ATTACH, tid, NULL, NULL)) _exit (1);
    if (-1 == waitpid (tid, NULL,  __WALL)) _exit (1);
  
    if (-1 == ptrace (PTRACE_DETACH, tid, NULL, (void *)SIGKILL)) _exit (1);
    if (-1 == waitpid (tid, NULL,  __WALL)) _exit (1);

    _exit (0);
  }
  else _exit (1);
}
