#include <stdlib.h>

void crash(int param1){
  int* a = 0;
  a[0] = param1;
}

inline void second(int w){
  crash(0);
}

void first(){
  second(0);
}

int main(){

  second(0);

  return 0;
}

