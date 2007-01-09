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

