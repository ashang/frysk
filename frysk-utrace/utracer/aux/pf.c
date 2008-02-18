#include <stdio.h>

main (int ac, char * av[])
{
  fprintf (stderr, "\"%.*s\"\n", atoi (av[1]), av[2]);
}
