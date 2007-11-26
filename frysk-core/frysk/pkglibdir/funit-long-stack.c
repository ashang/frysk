#include <stdlib.h>

void crash(){
  char* c = 0;
  c[0] = 'a';
}

void first(int stack){

  if(stack < 50){
    first(stack+1);
  }else{
    crash();
  }
}


int main(){

  first (0);

  return 0;
}

