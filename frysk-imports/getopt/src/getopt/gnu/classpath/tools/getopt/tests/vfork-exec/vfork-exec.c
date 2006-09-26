#define _GNU_SOURCE
#include <stdio.h>
#include <alloca.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <string.h>

void
exit_0_handler (int sig)
{
  printf ("exit 0: %d\n", sig);
  printf ("exit 0: %s\n", strsignal (sig));
  _exit (0);
}

void
exit_1_handler (int sig)
{
  printf ("exit 1: %d\n", sig);
  printf ("exit 1: %s\n", strsignal (sig));
  _exit (1);
}

int
main (int argc, char **argv)
{
  register int v;
  errno = 0;

  // Mask out all the signals, so that they can only be delivered when
  // ready for them.
  sigset_t mask;
  sigset_t umask;
  sigemptyset (&mask);
  sigaddset (&mask, SIGUSR1);
  sigaddset (&mask, SIGALRM);
  sigaddset (&mask, SIGCHLD); // totally ignore
  if (sigprocmask (SIG_BLOCK, &mask, &umask) < 0) {
    perror ("sigprocmask");
    _exit (1);
  }

  // Set up a SIGUSR1 handler, that, when the signal is received, just
  // exits.
  signal (SIGUSR1, exit_0_handler);

  // Set up an alarm that, after a second will fire causing the
  // program to exit with status 1.
  {
    signal (SIGALRM, exit_1_handler);
    struct itimerval timerval = { { 0, 0 }, { 1, 0 } };
    if (setitimer (ITIMER_REAL, &timerval, NULL) < 0) {
      perror ("setitimer");
      _exit (1);
    }
  }
  
  // Construct the argv of the program to be run -- here kill which
  // will turn around and send this process a SIGUSR1.
  char *vec[4];
  vec[0] = "/bin/kill";
  vec[1] = "-USR1";
  asprintf (&vec[2], "%d", getpid ());
  vec[3] = NULL;
  printf ("%s %s %s\n", vec[0], vec[1], vec[2]);

  v = vfork ();
  printf ("vfork %d\n", v);
  switch (v) {
  case -1:
    perror ("vfork");
    _exit (1);
  case 0:
    {
      // This is executed by the child with the parent blocked.
      errno = 0;
      pid_t pid = fork ();
      printf ("fork %d\n", pid);
      switch (pid) {
      case -1:
	// Fork failed.
	perror ("fork");
	_exit (1);
      case 0:
	// Child
	printf ("exec\n");
	execvp (vec[0], vec);
	// This should not happen.
	perror ("execvp");
	_exit (errno);
      default:
	// Parent, exit turning the child into a zombie.
	_exit (0);
      }
    }
  default:
    {
      // This is executed after the child, created using vfork, exits
      // (which helps guarentee that the waitpid, below, doesn't
      // block).  Both consume the child's wait event and verify that
      // the child exited with a valid status.
      int status;
      errno = 0;
      printf ("waiting\n");
      if (waitpid (v, &status, 0) < 0) {
	perror ("waitpid");
	_exit (1);
      }
      if (!WIFEXITED (status) || WEXITSTATUS (status) != 0) {
	printf ("bad vfork child exit status 0x%x\n", status);
	_exit (1);
      }
      printf ("suspending\n");
      errno = 0;
      sigsuspend (&umask);
      if (errno != EINTR) {
	perror ("sigsuspend");
	_exit (1);
      }
      printf ("exiting???\n");
      return 1;
    }
  }
}
