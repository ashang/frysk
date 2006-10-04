#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <linux/unistd.h>
#include <linux/ptrace.h>

pid_t pid;

void
timeout (int sig)
{
  _exit (1);
}
  
int
main (int ac, char * av[])
{
  int rc = 1;		/* assume we're going to fail	*/
  
  /* dummy out SIGUSR1 so it won't default to TERM */
  signal (SIGUSR1, SIG_IGN);
  
  signal (SIGALRM, timeout);
  alarm (2);
  
  pid = fork();

  switch ((int)pid) {
  case -1:
    perror ("fork():");
    exit (1);
  case 0:			/* child */
    pause();
    break;
  default:			/* parent */
    if (0 == ptrace (PTRACE_ATTACH, pid, NULL, NULL)) {

      /* wait for the attach */
      if (-1 != waitpid (pid, NULL,  0)) {

	/* this PTRACE_DETACH should fail because of the invalid signr;	*/
	/* if it passes, then the behaviour is wrong so the test fails.	*/
	if ( -1 == ptrace (PTRACE_DETACH, pid, NULL, (void *)999)) {
    
	  /* but if the previous DETACH failed, as it should, there's	*/
	  /* no way to tell whether te failure is due to the invalid	*/
	  /* signal or to an actual failed detach except by trying to 	*/
	  /* detach again, this time with a good signal.  if this	*/
	  /* DETACH passes, that means the previous DETACH didn't	*/
	  /* detach, which it shouldn't have because of the bad signr,	*/
	  /* so the behaviour matches historical ptrace and therefore 	*/
	  /* the test passes.  if this DETACH fails, it can only, in 	*/
	  /* theory, be beacuse the previous DETACH succeeded, which it	*/
	  /*  shouldn't have, so the test fails.			*/
	  if (0 == ptrace (PTRACE_DETACH, pid, NULL, (void *)SIGUSR1)) rc = 0;
	}
      }
    }
    
    kill (pid, SIGKILL);
    if (-1 == waitpid (pid, NULL,  0)) _exit (1);

    exit (rc);
    break;
  }
  exit (1);  /* shouldn't be reached */
}
