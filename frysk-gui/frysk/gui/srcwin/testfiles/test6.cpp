#include "common.h"
// this is a test program for the DOM

class myClass
{
public:
  inline int min(int y, int z);
};

int myClass::min(int y, int z){
	if(y < z)
		return y;
		
	return z;
}

// main program
int bleh()
{
  myClass a;
  int i = a.min(1,2);
  int j = a.min(3,4);
  return 0;
}
