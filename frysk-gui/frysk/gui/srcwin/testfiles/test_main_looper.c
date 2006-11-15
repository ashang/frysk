#include <stdio.h>
#include <stdarg.h>

#define ABS(a) (a) < 0 ? -(a) : (a)

int
   loop_ ()
{
  return 1;
}

int static_int = 4;
struct static_class_t {int class_int_1, class_int_2;} static_class = {51, 52};

int
     func_2 (int x, int y)
{
  typedef struct {int class_int_1, class_int_2;} class_t;
  class_t class;
  int *int_p;
  class_t *class_p;
  int int_22 = 22;
  short short_21 = 12;
  int int_21 = 11;
  float float_21 = 1.1;
  double double_21 = 1.22658;
  int_p = &int_22;
  class_p = &class;
  class_p->class_int_1 = 14;
  class_p->class_int_2 = 15;
  class.class_int_1 = 13;
  
  int_21 = loop_(class_p->class_int_1,*int_p, int_22, short_21,
		 int_21, float_21,double_21,x,y);
  while (int_21)
    {
      int_21 = x / int_21;
    }
  return int_21;
}

int
func_1 (int x, int y)
{
  int int_21 = 21;
  int int_11 = 12;
  printf("abs of -1 and 1: %d %d",ABS(-1), ABS(1));
  return func_2 (int_21, int_11);
}
int main ()
{
  int int_21 = 31;
  int int_1 = 1;
  return func_1 (int_21, int_1);
}

