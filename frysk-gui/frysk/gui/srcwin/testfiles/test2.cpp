#include "common.h"
#include <iostream>

using namespace std;

/*
 * This function just prints stuff from another function
 */
void foo(){
  /*
   * Get whatever bar() says and print it out
   */
  cout << bar() << endl;
}
