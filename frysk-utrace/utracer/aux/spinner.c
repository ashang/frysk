#include <stdio.h>
#include <unistd.h>

main()
{
  while(1) {
    fprintf (stderr, "tick\n");
    sleep (1);
  }
}
