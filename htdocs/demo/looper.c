#include <stdio.h>
#include <stdlib.h>

int main(){
  printf("Main: %p\n", &main);
  printf("Pid: %d\n", getpid());

  while(1);

  return 0;
}
