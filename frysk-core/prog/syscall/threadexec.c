#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>

struct args {
  char **argv;
  char **envp;
};

static void *
exec_args (void *arg)
{
  struct args *args = arg;
  execve (args->argv[1], args->argv + 1, args->envp);
  /* Reaching here implies an error.  */
  perror ("execve");
  exit (1);
  
}

int
main (int argc, char **argv, char **envp)
{
  if (argc < 2) {
    printf ("Usage: %s command args ...\n", argv[0]);
    return 1;
  }

  struct args args;
  args.argv = argv;
  args.envp = envp;
  pthread_t thread;
  if (pthread_create (&thread, NULL, exec_args, &args)) {
    perror ("pthread_create");
    exit (1);
  }

  void *retval;
  if (pthread_join (thread, &retval)) {
    perror ("pthread_join");
    exit (1);
  }

  /* Should this ever be reached?  */
  exit (1);
}
