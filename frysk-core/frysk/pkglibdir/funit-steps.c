#include <stdlib.h>

int first(int a){
  return ++a;
}

int second(int b){
  return ++b;
}

int third(int a, int b){
  return a+b;
}

int main(){
  
  third(second(0),first(1));
  return 0;
}

