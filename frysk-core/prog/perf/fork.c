/* Create a rotating barrel of processes.  */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/wait.h>

static int created_processes;
static int joined_processes;

static pid_t
process_create (void)
{
  pid_t pid;
  pid = fork ();
  switch (pid) {
  case 0: /* Child.  */
    exit (0);
  case -1:
    perror ("fork");
    exit (1);
  default:
    created_processes++;
    return pid;
  }
}

static int
process_join (pid_t pid)
{
  int status;
  int retpid = waitpid (pid, &status, 0);
  if (status < 0) {
    perror ("waitpid");
    exit (1);
  }
  if (retpid != pid) {
    printf ("waitpid (%d) got back %d\n", (int) pid,
	    (int) retpid);
    exit (1);
  }
  if (!WIFEXITED (status)) {
    printf ("waitpid (%d) got non exit staus 0x%x\n", (int) pid,
	    status);
    exit (1);
  }
  joined_processes++;
  return WEXITSTATUS (status);
}

int
main (int argc, char *argv[], char *envp[])
{
  int c;
  int i;

  if (argc < 2 || argc > 3)
    {
      printf ("Usage: %s <number-processes> [ <join-delay> ]\n", argv[0]);
      printf ("\
This test program saturates the process create and join code.\n\
It creates and then joins <number-processes> (each does nothing but exit).\n\
A processes join being delayed until a further <join-delay> processes have\n\
been created (default 0).\n\
");
      exit (1);
    }

  int number_processes = atol (argv[1]);
  int length = 1;
  if (argc == 3)
    length = atol (argv[2]) + 1;

  if (number_processes < length) {
    printf ("Need at least <join-delay> processes.\n");
    exit (1);
  }

  pid_t *process = calloc (length, sizeof (pid_t));

  /* Prime the pump.  */
  for (i = 0; i < length; i++) {
    process[i] = process_create ();
  }
  number_processes -= length;

  /* Run the pipeline.  */
  for (i = 0; i < number_processes; i++) {
    process_join (process[i % length]);
    process[i % length] = process_create ();
  }

  /* Drain the pump.  */
  for (i = 0; i < length; i++) {
    process_join (process[(number_processes + i) % length]);
  }

  printf ("Created %d, Joined %d.\n", created_processes, joined_processes);
  return 0;
}
