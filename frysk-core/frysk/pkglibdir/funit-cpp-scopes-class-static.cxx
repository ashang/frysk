#include <stdlib.h>
#include <stdio.h>

int first = 11;

class A{
  static const int first = 22; //*this* one should be found
public:
  static void crash();
};

void A::crash(){
  printf("first: %d\n", first );
  int* a = 0;
  a[0] = 0;  
}

int main(){
  A a = A();
  a.crash();
  return 0;
}

