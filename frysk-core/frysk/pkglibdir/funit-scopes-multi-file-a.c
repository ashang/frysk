#include <stdlib.h>

void second();

void first(){ //*other*
  second();
}

void second(){
  int* a = 0;
  a[0] = 0;
}

int main(){
  first();
  return 0;
}

