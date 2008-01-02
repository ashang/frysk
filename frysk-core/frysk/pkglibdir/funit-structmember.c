#include <stdlib.h>

struct Foo
{
  int (*testfn)(void);
};

int testfn(void)
{
  return 42;
}

int main()
{
  (void)testfn();
  return 0;
}
