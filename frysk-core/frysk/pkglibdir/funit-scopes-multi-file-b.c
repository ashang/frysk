#include <stdlib.h>

static inline void second(){
  int* a = 0;
  a[0] = 0;
}

static void first(){// *this* one should be found
  second();
  first();
}

// Avoid a warning about first not being  used.
void (*foo)() = first;

