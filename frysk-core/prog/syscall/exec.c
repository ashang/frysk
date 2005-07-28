#include <stdio.h>
#include <unistd.h>

int
main (int argc, char **argv, char **envp)
{
  if (argc < 2) {
    printf ("Usage: %s command args ...\n", argv[0]);
    return 1;
  }
  execve (argv[1], argv + 1, envp);
  /* Reaching here implies an error.  */
  perror ("execve");
  return 1;
}
