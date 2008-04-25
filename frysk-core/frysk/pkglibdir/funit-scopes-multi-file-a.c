#include <stdlib.h>

static inline void second(){
  int* a = 0;
  a[0] = 0;
}




static void first(){
  second();
}

int main(){
  first();
  return 0;
}

