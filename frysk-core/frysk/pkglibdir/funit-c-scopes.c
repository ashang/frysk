#include <stdlib.h>

int var2 = 0;

void first(int arg1){
  
  int var1 = 0;

  int* a = 0;
  a[0] = var1 + var2;  
}

int main(){
  first(0);
  return 0;
}

