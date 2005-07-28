/* Use processes to compute a Fibonacci number.  */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/wait.h>

int
main (int argc, char *argv[], char *envp[])
{
  int result = 0;
  int want;
  int n;
  if (argc != 2)
    {
      printf ("Usage: fib <number>\n");
      exit (1);
    }
  want = atol (argv[1]);
  if (want > 13)
    {
      printf ("<number> > 13 not allowed, limit due to size of wait.status.");
      exit (1);
    }

  n = want;
 compute_fib_n:
  /* Children jump back here with an updated N.  */
  switch (n)
    {
    case 0:
      result = 0;
      break;
    case 1:
      result = 1;
      break;
    default:
      {
	pid_t child[2];
	int i;
	for (i = 0; i < 2; i++)
	  {
	    child[i] = fork ();
	    switch (child[i])
	      {
	      case 0:
		/* Child; loop back round performing the same
		   computation but with ...  */
		n = n - 1 - i;
		goto compute_fib_n;
	      case -1:
		/* Ulgh.  */
		perror ("fork");
		exit (1);
	      }
	  }
	for (i = 0; i < 2; i++)
	  {
	    int status;
	    pid_t pid = waitpid (child[i], &status, 0);
	    if (pid < 0)
	      {
		perror ("waitpid");
		exit (1);
	      }
	    if (pid != child[i])
	      {
		printf ("waitpid (%ld) got back %ld\n", (long) child[i],
			(long) pid);
		exit (1);
	      }
	    if (!WIFEXITED (status))
	      {
		printf ("waitpid (%ld) got non exit staus 0x%x\n",
			(long) child[i], status);
		exit (1);
	      }
	    result += WEXITSTATUS (status);
	  }
      }
    }
  if (n == want)
    {
      printf ("fib (%d) = %d\n", n, result);
      exit (0);
    }
  else
    exit (result);
}
