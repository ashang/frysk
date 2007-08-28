volatile int vx = 1;
static int loop_ (double,int,int,short,int,float,double,int,int,int,char)
 __attribute__ ((noinline));
static int
loop_ (double d1, int i1, int i2, short s, int i3, float f, double d2, int i4,
       int i5, int i6, char ch)
{
  return 2;
}

static char
assign_char (char arg)
{
  return arg;
}

static short
assign_short (short arg)
{
  return arg;
}

static int
assign_int (int arg)
{
  return arg;
}

static long
assign_long (long arg)
{
  return arg;
}

static float
assign_float (float arg)
{
  return arg;
}

static double
assign_double (double arg)
{
  return arg;
}

int static_int = 4;
typedef int simode;
static volatile int *int_p;
static volatile simode int_22 = 22;
static unsigned int_23 = 23;
static char char_21;
static short short_21;
static int int_21;
static long long_21;
static float float_21;
static double double_21;

static int func_2 (int, int) __attribute__ ((noinline));
static int
func_2 (int x, int y) 
{
  char_21  = assign_char('a');
  short_21 = assign_short (12);
  int_21 = assign_int (11);
  long_21 = assign_long(10);
  float_21 = assign_float (1.1);
  double_21 = assign_double (1.2l);
  int_p = &int_22;

  int_21 = loop_(double_21,*int_p, int_22, short_21,
		 int_21, float_21,double_21,x,y, int_23, char_21);
  while (vx)
    ;
  return int_21;
}

static int
func_1 (int x, int y)
{
  int int_21 = 21;
  int int_11 = 12;
  int_21 = assign_int (int_21);
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
