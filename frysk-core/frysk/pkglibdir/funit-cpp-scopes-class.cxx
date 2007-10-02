#include <stdlib.h>

int first;

class A{
  int first; //*this* one should be found
public:
  void crash(int i);
};

void A::crash(int i){
  int* a = 0;
  a[0] = 0;  
}

int main(){
  A a = A();
  a.crash(0);
  return 0;
}

