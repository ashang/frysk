#include <stdio.h>

int
main (int argc, char **argv, char **envp, void *auxv)
{
  char **argp;
  for (argp = argv + 1; *argp; argp++) {
    printf ("%s\n", *argp);
  }
  return 0;
}
