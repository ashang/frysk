#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>

int
loop_ (double d1, int i1, int i2, short s, int i3, float f, double d2, int i4, int i5)
{
  return 2;
}

char
assign_char (char arg)
{
  return arg;
}

short
assign_short (short arg)
{
  return arg;
}

int
assign_int (int arg)
{
  return arg;
}

long
assign_long (long arg)
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
