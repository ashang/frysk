#include <stdio.h>

int
main (int argc, char **argv, char **envp, void *auxv)
{
  char **env;
  for (env = envp; *env; env++) {
    printf ("%s\n", *env);
  }
  return 0;
}
