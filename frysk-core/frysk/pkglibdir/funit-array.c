static int func_1 (int x, int y)  __attribute__ ((noinline));
static int func_2 (int x, int y)  __attribute__ ((noinline));
volatile int vx = 1;

static int loop_ (long, int, float, char) __attribute__ ((noinline));
int
loop_ (long a, int b, float f, char c)
{
  return 2;
}

static long arr_1 [32];
static int arr_2 [5][6];
static float arr_3 [4][5];
static char arr_4 [4];

void
assign_long_arr (long *arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = (i + 1) % 10;
  return;
}

void
assign_int_arr (int *arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = (i + 1) % 10;
  return;
}

void
assign_char_arr (char *arr, int arr_hb)
{
  int i;
  const char alphas [] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = alphas[i];
  return;
}

void
assign_float_arr (float *arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = ((i + 1) % 10) + (float)(i * 0.1);
  return;
}

static int func_2 (int, int) __attribute__ ((noinline));
static int
func_2 (int x, int y)
{
  assign_long_arr (arr_1, sizeof (arr_1) / sizeof (long));
  assign_int_arr ((int*)arr_2, sizeof (arr_2) / sizeof (int));
  assign_float_arr ((float*)arr_3, sizeof (arr_3) / sizeof (float));
  assign_char_arr (arr_4, sizeof (arr_4));

  x = loop_(arr_1[0], arr_2[0][0], arr_3[0][0], arr_4[0]);
  while (vx)
    ;
  return x;
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
