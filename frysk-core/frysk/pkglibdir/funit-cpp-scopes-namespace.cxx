#include <stdlib.h>

namespace N{
  int first;
}

using namespace N;

void foo(){

  int* a = 0;
  a[0] = 0;  
}

int main(){
  foo();
  return 0;
}

