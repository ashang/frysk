#include "common.h"
// this is a test program for the DOM
class bar
{
public:
  inline int min(int y, int z);
};

// main program
int bleh()
{
  bar a;
  int i = a.min(1,2);
  int j = a.min(3,4);
  return 0;
}