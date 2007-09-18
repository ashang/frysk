#include <stdlib.h>

void foo(){
  
  enum numbers {zero, one,two,three} mynumber __attribute__ ((unused));

  int* a = 0;
  a[0] = 0;  
}

int main(){
  foo();
  return 0;
}

