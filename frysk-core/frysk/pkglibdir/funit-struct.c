volatile int vx = 1;
static int loop_ (int, int, int, int, int, double, double) __attribute__ ((noinline));
int
loop_ (int a, int b, int c, int d, int e, double f, double g)
{
  return 2;
}

typedef struct {double double_1; int int_1;} static_class_t;
static_class_t  static_class = {12.34, 51};
typedef int simode;
static static_class_t class_1;
static_class_t classes[] = {{1.0,1},{2.0,2},{3.0,3},{4.0,4}};
static struct {static_class_t c1; static_class_t c2;} class_2 = {{1.0,1},{2.0,2}};
static struct {int arr [2][2];} class_3 = {{{1,2},{3,4}}};
static struct astruct {int x; float y;} class_4;
static struct {simode x; float y;} class_5;
typedef union {double double_1; long long ll_1;} union_t __attribute__ ((unused));
static union_t union_1 = {3.1415};
static static_class_t *class_p;
  

static int
assign_int (int arg)
{
  return arg;
}

static double
assign_double (double arg)
{
  return arg;
}

static int func_2 (int, int) __attribute__ ((noinline));
static int
func_2 (int x, int y)
{
  class_p = &class_1;
  class_p->double_1 = assign_double (43.21);
  class_p->int_1 = assign_int (123456789);
  class_1.double_1 = assign_double (12.34);
  
  x = loop_(class_1.int_1, classes[0].int_1, class_3.arr[0][0],
	    class_4.x, class_5.x, union_1.double_1, class_2.c1.double_1);
  while (vx)
    ;
  return x;
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
