#include <stdlib.h>

void crash(){
  int* a = 0;
  a[0] = 0;
}

extern inline void second(){
  crash();
}

void first(){
  second();
}

int main(){

  first();

  return 0;
}

