/* Create pthreads and exec the parent from the parent */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <pthread.h>
#include <sys/time.h>
#include <linux.syscall.h>

_syscall0(pid_t,gettid)

#define NUM_THREADS 2

  char * args [] = {"./multi_parent_exec", NULL, NULL};

void *
BusyWork (void *arg)
{
  struct timeval tv;

  tv.tv_sec = 5;
  tv.tv_usec = 0;
  select (0, NULL, NULL, NULL, &tv);
  pthread_exit ((void *) 0);
} 

int
main (int argc, char **argv)
{
  char *buf ;
  pthread_t thread[NUM_THREADS];
  pthread_attr_t attr;
  int rc, t;

  args[0] = argv[0];
  if (argc != 1)		/* did we exec from below? */
    {
      int i = atoi (argv[1]);
      int j = gettid ();

#ifdef DEBUG
      asprintf (&buf, "echo 'in child ppid %d pid %d';"
               "ps  -o 'pid,ppid,nlwp,lwp,user,stat,bsdstart,bsdtime,pid,cmd'",
               getppid (), getpid ());
      system (buf);
#endif
      if (i != j)
	{
	  fprintf (stderr, "FAIL: Parent PID %d != Parent PID %d\n", i, j);
	  exit (EXIT_FAILURE);
	}
      else fprintf (stderr, "OK: Parent PID %d == Parent PID %d\n", i, j);

      pthread_exit (NULL);
      exit (EXIT_SUCCESS);
    }

  rc = pthread_attr_init (&attr);
  if (rc == -1)
    {
      perror ("pthread_attr_init\n");
      exit (EXIT_FAILURE);
    }
  rc = pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_JOINABLE);
  if (rc == -1)
    {
      perror ("pthread_attr_setdetachstate\n");
      exit (EXIT_FAILURE);
    }
  for (t = 0; t < NUM_THREADS; t++)
    {
      printf ("Creating thread %d\n", t);
      rc = pthread_create (&thread[t], &attr, BusyWork, NULL);
      if (rc)
	{
	  perror ("pthread_create\n");
	  exit (EXIT_FAILURE);
	}
    }

  asprintf (&buf, "%d", gettid ());
  if (rc == -1)
    {
      perror ("asprintf\n");
      exit (EXIT_FAILURE);
    }
  args[1] = buf;
  execv (argv[0], args); /* exec ourself with pid option */

  exit (EXIT_SUCCESS);
}
