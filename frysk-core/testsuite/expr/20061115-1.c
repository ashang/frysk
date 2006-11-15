

int static_int = 4;
struct static_class_t {int class_int_1; float class_float_1;} static_class = {51, 12.34};

short assign_short (short);
int assign_int (int);
float assign_float (float);
double assign_double (double);


int
func_2 (int x, int y)
{
  typedef struct {int class_double_1, class_int_2;} class_t;
  class_t class;
  int *int_p;
  long arr_1 [32];
  int arr_2 [5][6];
  float arr_3 [4][5];
  class_t *class_p;
  int int_22;
  short short_21 = assign_short (12);
  int int_21 = assign_int (11);
  float float_21 = assign_float (1.1);
  double double_21 = assign_double (1.2l);
  assign_long_arr (&arr_1, sizeof (arr_1) / sizeof (long));
  assign_int_arr (&arr_2, sizeof (arr_2) / sizeof (int));
  assign_float_arr (&arr_3, sizeof (arr_3) / sizeof (float));
  
  int_p = &int_22;
  class_p = &class;
  class_p->class_double_1 = assign_double (43.21);
  class_p->class_int_2 = assign_int (15);
  class.class_double_1 = assign_double (12.34);
  
  int_21 = loop_(class_p->class_double_1,*int_p, int_22, short_21,
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
  return func_2 (int_21, int_11);
}

main ()
{
  int int_21 = 31;
  int int_1 = 1;
  return func_1 (int_21, int_1);
}
