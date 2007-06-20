#include <stdlib.h>

void fourth(){
  exit(1);
}

void third(){
  fourth();
}

void second(){
  third();
}

void first(){
  second();
}


int main(){
  first ();
  return 0;
}

