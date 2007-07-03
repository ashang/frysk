#include <stdlib.h>

void crash(){
  int* a = 0;
  a[0] = 0;
}

// Was 'extern' also but GCC doesn't like that when compiling/linking with -O0.
inline void second(){
  crash();
}

void first(){
  second();
}

int main(){

  first();

  return 0;
}

