#include <stdlib.h>

inline void second(){
  int* a = 0;
  a[0] = 0;
}

void first(){// *this* one should be found
  second();
}

int main(){
  first();
  return 0;
}

