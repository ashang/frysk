#include <stdlib.h>
#include <stdio.h>

class A{
  static int static_i;
  int i;
public:
  static void static_crash();
  void crash();
};

void A::crash(){
  int* a = 0;
  a[0] = 0;  
}

int main(){
  A a = A();
  a.crash();
  return 0;
}

