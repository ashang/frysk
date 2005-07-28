#include <stdio.h>
#include <fcntl.h>

int main ()
{
   int fd = open ("a.file", O_RDONLY);

   close (fd);

   return 0;
}
