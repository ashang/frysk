#include <stdio.h>
#include <fcntl.h>

int main (int argc, char **argv)
{
   int i;
   int loop_count = atoi (argv[1]);

   for (i = 0; i < loop_count; ++i)
       close (-1);

   return 0;
}
