#include <stdlib.h>

inline void second(){
  int* a = 0;
  a[0] = 0;
}

void first(){
  second();
}

int main(){
  first();
  return 0;
}

