#include <stdlib.h>

void fourth(int param1, int param2, int param3){
  int var1 = 1;
  exit(var1);
}

void third(int param1, int param2, int param3){
  int var1 = 4;

  if(param1+var1){
    while(1){
      {
	fourth(param1,param2,param3);
      }
    }
  }
}

void second(int wassap){
  {
    int one = 1;
    {
      int two = 2;
      {
	third(one,two,wassap);
      }
    }
  }
}

void first(){

  second(3);

}


int main(){

  first ();

  return 0;
}

