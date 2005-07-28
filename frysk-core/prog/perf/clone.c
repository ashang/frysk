/* Create a rotating barrel of threads.  */

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <errno.h>

pthread_attr_t pthread_attr;

static void *
nothing (void *arg)
{
  return NULL;
}

static int created_threads;
static int joined_threads;

static pthread_t
thread_create (void)
{
  pthread_t thread;
  if (pthread_create (&thread, &pthread_attr, nothing, NULL)) {
    perror ("pthread_create");
    exit (1);
  }
  created_threads++;
  return thread;
}

static void *
thread_join (pthread_t thread)
{
  void *retval;
  if (pthread_join (thread, &retval)) {
    perror ("ptread_join");
    exit (1);
  }
  joined_threads++;
  return retval;
}

int
main (int argc, char *argv[], char *envp[])
{
  int c;
  int i;

  if (argc < 2 || argc > 3)
    {
      printf ("Usage: %s <number-threads> [ <join-delay> ]\n", argv[0]);
      printf ("\
This test program saturates the thread create and join code.\n\
It creates and then joins <number-threads> (each does nothing but exit).\n\
A thread's join being delayed until a further <join-delay> threads have\n\
been created (default 0).\n\
");
      exit (1);
    }

  int number_threads = atol (argv[1]);
  int length = 1;
  if (argc == 3)
    length = atol (argv[2]) + 1;

  if (number_threads < length) {
    printf ("Need at least <join-delay> threads.\n");
    exit (1);
  }

  pthread_attr_init (&pthread_attr);
  pthread_attr_setstacksize (&pthread_attr, PTHREAD_STACK_MIN);

  pthread_t *thread = calloc (length, sizeof (pthread_t));

  /* Prime the pump.  */
  for (i = 0; i < length; i++) {
    thread[i] = thread_create ();
  }
  number_threads -= length;

  /* Run the pipeline.  */
  for (i = 0; i < number_threads; i++) {
    thread_join (thread[i % length]);
    thread[i % length] = thread_create ();
  }

  /* Drain the pump.  */
  for (i = 0; i < length; i++) {
    thread_join (thread[(number_threads + i) % length]);
  }

  printf ("Created %d, Joined %d.\n", created_threads, joined_threads);
  return 0;
}
