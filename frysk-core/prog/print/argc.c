#include <stdio.h>

int
main (int argc, char **argv, char **envp, void *auxv)
{
  printf ("%d\n", argc);
  return 0;
}
