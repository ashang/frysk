/* Create pthreads and exec the parent from a pthread */

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

  char * args [] = {"./multi_child_exec", NULL, NULL};

void *
BusyWorkExec (void *arg)
{
  char *buf ;
  int rc;

#ifdef DEBUG
  asprintf (&buf, "echo 'in child ppid %d pid %d';"
	   "ps  -o 'pid,ppid,nlwp,lwp,user,stat,bsdstart,bsdtime,pid,cmd'",
	   getppid (), getpid ());
  system (buf);
#endif
  rc = asprintf (&buf, "%d", getpid ());
  if (rc == -1)
    {
      perror ("asprintf\n");
      exit (EXIT_FAILURE);
    }
  args[1] = buf;
  rc = execv (args[0], args); /* exec ourself with pid option */
  if (rc == -1)
    {
      perror ("execv\n");
      exit (EXIT_FAILURE);
    }
  pthread_exit (NULL);
} 

void *
BusyWork (void *arg)
{
  struct timeval tv;

#ifdef DEBUG
  char *buf ;
  asprintf (&buf, "echo 'in child ppid %d pid %d';"
	   "ps  -o 'pid,ppid,nlwp,lwp,user,stat,bsdstart,bsdtime,pid,cmd'",
	   getppid (), getpid ());
  system (buf);
#endif  

  tv.tv_sec = 5;
  tv.tv_usec = 0;
  select (0, NULL, NULL, NULL, &tv);
  pthread_exit (NULL);
} 

int
main (int argc, char **argv)
{
  pthread_t thread[NUM_THREADS];
  pthread_attr_t attr;
  int rc, t;

  args[0] = argv[0];
  if (argc != 1)		/* did we exec from below? */
    {
      int i = atoi (argv[1]);
      int j = gettid ();
      if (i != j)		/* should end up in the parent */
	{
	  fprintf (stderr, "FAIL: Child Parent PID %d == Exec PID %d\n", i, j);
	  exit (EXIT_FAILURE);
	}
      else fprintf (stderr, "OK: Child Parent PID %d == Exec PID %d\n", i, j);

      pthread_exit (NULL);
      exit (EXIT_SUCCESS);
    }

  rc = pthread_attr_init (&attr);
  if (rc != 0)
    {
      perror ("pthread_attr_init\n");
      exit (EXIT_FAILURE);
    }
  rc = pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_JOINABLE);
  if (rc != 0)
    {
      perror ("pthread_attr_setdetachstate\n");
      exit (EXIT_FAILURE);
    }

  for (t = 0; t < NUM_THREADS; t++)
    {
      printf ("Creating thread %d\n", t);
      if (!t)
	rc = pthread_create (&thread[t], &attr, BusyWorkExec, NULL);
      else
	rc = pthread_create (&thread[t], &attr, BusyWork, NULL);
      if (rc)
	{
	  printf ("pthread_create\n");
	  exit (EXIT_FAILURE);
	}
    }

  rc = pthread_attr_destroy (&attr);
  if (rc)
    {
      perror ("pthread_attr_destroy\n");
      exit (EXIT_FAILURE);
    }
  for (t = 0; t < NUM_THREADS; t++)
    {
      void *status;
      rc = pthread_join (thread[t], &status);
      if (rc)
        {
          perror ("pthread_join\n");
          exit (EXIT_FAILURE);
        }
      printf ("Completed join with thread %d\n", t);
    }
  pthread_exit (NULL);

  exit (EXIT_SUCCESS);
}
