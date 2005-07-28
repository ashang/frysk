#include <stdio.h>

struct auxv {
  long type;
  long value;
};

int
main (int argc, char **argv, char **envp)
{
  struct auxv *auxp;
  /* Auxv array starts after the terminal entry in envp */
  while (*envp != NULL)
    ++envp;
  ++envp; /* point pass terminating null entry */
  for (auxp = (struct auxv *)(envp); auxp->type != 0; auxp++) {
    printf ("%lu %lu\n", auxp->type, auxp->value);
  }
  return 0;
}
