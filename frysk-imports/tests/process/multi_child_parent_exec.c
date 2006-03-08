/* Create pthreads and exec from a pthread */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <linux/unistd.h>
#include <pthread.h>
#include <string.h>
#include <sys/time.h>
_syscall0(pid_t,gettid);

#define NUM_THREADS 2

char * args [] = {"./multi_parent_exec", NULL, NULL};

void *
BusyWorkExec (void *arg)
{
  char * buf;
  char * pe_p;
  int rc;
  int pe_i;

#ifdef DEBUG
  asprintf (buf, "echo 'in child ppid %d pid %d';"
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

  pe_p = strstr (args[0], "_parent_exec");
  pe_i = pe_p - args[0];
  args[0][pe_i] = '\0';
  strcat (args[0], "_exec");	/* change to multi_child_exec */

  args[1] = buf;
  rc = execv (args[0], args); /* exec with pid option */
  if (rc)
    {
      perror ("execv\n");
      exit (EXIT_FAILURE);
    }
  pthread_exit ((void *) 0);
} 

void *
BusyWork (void *arg)
{
  struct timeval tv;

#ifdef DEBUG
  char *buf ;
  asprintf (buf, "echo 'in child ppid %d pid %d';"
	   "ps  -o 'pid,ppid,nlwp,lwp,user,stat,bsdstart,bsdtime,pid,cmd'",
	   getppid (), getpid ());
  system (buf);
#endif

  tv.tv_sec = 5;
  tv.tv_usec = 0;
  select (0, NULL, NULL, NULL, &tv);
  pthread_exit ((void *) 0);
} 

int
main (int argc, char **argv)
{
  pthread_t thread[NUM_THREADS];
  pthread_attr_t attr;
  int rc, t;
  void * status;

  args[0] = argv[0];
  if (argc != 1)		/* did we exec from below? */
    {
      int i = atoi (argv[1]);
      int j = gettid ();
      if (i == j)		/* should end up in the parent */
	{
	  fprintf (stderr, "FAIL: Fork PID %d != Exec PID %d\n", i, j);
	  exit (EXIT_FAILURE);
	}
      else fprintf (stderr, "OK: Fork PID %d == Exec PID %d\n", i, j);

      pthread_exit (NULL);
      exit (EXIT_SUCCESS);
    }

  pthread_attr_init (&attr);
  pthread_attr_setdetachstate (&attr, PTHREAD_CREATE_JOINABLE);
  for (t = 0; t < NUM_THREADS; t++)
    {
      printf ("Creating thread %d\n", t);
      if (!t)
	rc = pthread_create (&thread[t], &attr, BusyWorkExec, NULL);
      else
	rc = pthread_create (&thread[t], &attr, BusyWork, NULL);
      if (rc)
	{
	  perror ("pthread_create\n");
	  exit (EXIT_FAILURE);
	}
    }

  pthread_attr_destroy (&attr);
  for (t = 0; t < NUM_THREADS; t++)
    {
      rc = pthread_join (thread[t], &status);
      if (rc)
        {
          perror ("pthread_join\n");
          exit (EXIT_FAILURE);
        }
    }
  pthread_exit (NULL);

  exit (EXIT_SUCCESS);
}
