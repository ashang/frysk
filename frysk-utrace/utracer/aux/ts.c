#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <malloc.h>
#include <alloca.h>

char * arg1 = "one\n";

main()
{
  char * arg2 = "two\n";
  char * arg3 = malloc(8);
  char * arg4 = alloca(8);

  bzero (arg3, 8);
  bzero (arg4, 8);
  strcpy (arg3, "three\n");
  strcpy (arg4, "four\n");

  write (fileno(stdout), arg1, strlen(arg1));
  write (fileno(stdout), arg2, strlen(arg2));
  write (fileno(stdout), arg3, strlen(arg3));
  write (fileno(stdout), arg4, strlen(arg4));
  _exit (0);
}
