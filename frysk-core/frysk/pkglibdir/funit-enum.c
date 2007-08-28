static enum cars {bmw, porsche} ssportscar;
volatile int vx = 1;

static int func_2 (int, int) __attribute__ ((noinline));
static int
func_2 (int x, int y)
{
  int int_21 = 1;
  enum cars {bmw, porsche} sportscar  __attribute__ ((unused));

  ssportscar = porsche;
  while (vx)
    ;
  return int_21;
}

static int
func_1 (int x, int y)
{
  int int_21 = 21;
  int int_11 = 12;
  int_21 = func_2 (int_21, int_11);
  return int_21;
}

int
main (int argc, char **argv)
{
  int int_21 = 31;
  int int_1 = 1;
  return func_1 (int_21, int_1);
}
