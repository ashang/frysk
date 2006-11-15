#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>

int
loop_ ()
{
  return 2;
}

short
assign_short (short arg )
{
  return arg;
}

int
assign_int (int arg)
{
  return arg;
}

float
assign_float (float arg)
{
  return arg;
}

double
assign_double (double arg)
{
  return arg;
}

void
assign_long_arr (long* arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = (i + 1) % 10;
  return;
}

void
assign_int_arr (int* arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = (i + 1) % 10;
  return;
}

void
assign_float_arr (float* arr, int arr_hb)
{
  int i;
  for (i = 0 ; i < arr_hb; i++)
    arr[i] = ((i + 1) % 10) + (float)(i * 0.1);
  return;
}
