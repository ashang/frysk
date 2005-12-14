/* fork then exec the parent from the child */ 

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <linux/unistd.h>
_syscall0(pid_t,gettid)

int
main (int argc, char **argv)
{
  char *buf ;
  int rc;
  pid_t child_pid;
  char * args [] = {"./single_exec", NULL, NULL};

  if (argc == 2)		/* did we fork/exec from below? */
    {
      int i = atoi (argv[1]);
      int j = gettid ();
      if (i != j)
	{
	  fprintf (stderr, "FAIL: Fork PID %d != Exec PID %d\n", i, j);
	  exit (EXIT_FAILURE);
	}
      else fprintf (stderr, "OK: Fork PID %d == Exec PID %d\n", i, j);
      return 0;
    }

  switch (child_pid = fork ())
    {
    case -1:
      perror ("fork");
    case 0:
      asprintf (&buf, "%d", gettid ());
      if (rc == -1)
	{
	  perror ("asprintf\n");
	  exit (EXIT_FAILURE);
	}
      args[0] = argv[0];
      args[1] = buf;
      rc = execv (argv[0], args); /* exec ourself with pid option */
      if (rc)
	{
	  perror ("execv\n");
	  exit (EXIT_FAILURE);
	}
      break;
    default:
      waitpid (child_pid, NULL, 0);
    }
  return 0;
}
