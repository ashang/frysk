#include <stdlib.h>

void third(int param3){
  int var4 = 4;
  char* c = 0;
  c[0] = var4;
}

void second(int param2){
  int var3 = 3;
  third(var3);
}

void first(int param1){
  int var2 = 2;
  second(var2);
}

int main(){
  int var1 = 1;
  first (var1);
  return 0;
}

