// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <sys/types.h>
#include <linux/unistd.h>
#include <errno.h>

#include "linux.syscall.h"

_syscall0(pid_t,gettid)

static int func_1 (int x, int y)  __attribute__ ((noinline));
static int func_2 (int x, int y)  __attribute__ ((noinline));

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
extern void assign_char_arr (char*, int);
extern void assign_long_arr (long*, int);
extern void assign_int_arr (int*, int);
extern void assign_float_arr (float*, int);
extern char assign_char (char);
extern short assign_short (short);
extern int assign_int (int);
extern long assign_long (long);
extern float assign_float (float);
extern double assign_double (double);
extern int loop_(double, int, int, short,
		  int, float, double, int, int);
static int func_2 (int x, int y) __attribute__ ((noinline));
static int func_1 (int x, int y) __attribute__ ((noinline));


char static_char = 5;
int static_int = 4;
int* static_int_ptr = &static_int;
typedef struct {double double_1; int int_1;} static_class_t;
static_class_t  static_class = {12.34, 51};
enum cars {bmw, porsche} ssportscar;

static int
func_2 (int x, int y)
{
  typedef int simode;
  static_class_t class_1;
  enum cars {bmw, porsche} sportscar;
  static_class_t classes[] __attribute__ ((unused)) = {{1.0,1},{2.0,2},{3.0,3},{4.0,4}};
  struct {static_class_t c1; static_class_t c2;} class_2  __attribute__ ((unused)) = {{1.0,1},{2.0,2}};
  asm volatile ("" : "+m" (class_2));
  struct {int arr [2][2];} class_3 __attribute__ ((unused)) = {{{1,2},{3,4}}};
  asm volatile ("" : "+m" (class_3));
  struct astruct {int x; float y;} class_4 __attribute__ ((unused));
  struct {simode x; float y;} class_5 __attribute__ ((unused));
  typedef union {double double_1; long long ll_1;} union_t __attribute__ ((unused));
  volatile int *int_p;
  long arr_1 [32];
  int arr_2 [5][6];
  float arr_3 [4][5];
  char arr_4 [4];
  static_class_t *class_p;
  volatile simode int_22 = 22;
  sportscar = porsche;
  char char_21 __attribute__((unused)) = assign_char('a');
  short short_21 = assign_short (12);
  int int_21 = assign_int (11);
  long long_21 __attribute__((unused)) = assign_long(10);
  float float_21 = assign_float (1.1);
  double double_21 = assign_double (1.2l);
  assign_long_arr (arr_1, sizeof (arr_1) / sizeof (long));
  assign_int_arr ((int*)arr_2, sizeof (arr_2) / sizeof (int));
  assign_float_arr ((float*)arr_3, sizeof (arr_3) / sizeof (float));
  assign_char_arr (arr_4, sizeof (arr_4));
  sportscar = porsche;
  
  int_p = &int_22;
  class_p = &class_1;
  class_p->double_1 = assign_double (43.21);
  class_p->int_1 = assign_int (123456789);
  class_1.double_1 = assign_double (12.34);
  
  int_21 = loop_(class_p->double_1,*int_p, int_22, short_21,
		 int_21, float_21,double_21,x,y);
  while (int_21)
    {
      int_21 = x / int_21;
    }
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
  if (argc > 1 && strcmp (argv[1],"-v") == 0)
    fprintf (stderr,"attach %s %d -task %d\n", argv[0], getpid(), gettid());

  int int_21 = 31;
  int int_1 = 1;
  return func_1 (int_21, int_1);
}
