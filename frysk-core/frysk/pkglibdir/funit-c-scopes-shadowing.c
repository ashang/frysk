#include <stdlib.h>

int i = 0;//first i

void foo(){
  
  int i = 0;//second i

  {
    int i =0; //third i
    i++;
  }
  int* a = 0;
  a[0] = i;  
}

int main(){
  foo();
  return 0;
}

