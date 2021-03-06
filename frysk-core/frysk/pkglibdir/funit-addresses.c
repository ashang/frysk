// This file is part of the program FRYSK.
//
// Copyright 2007 Oracle Corporation.
// Copyright 2007, Red Hat Inc.
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
#include <stdlib.h>
#include <string.h>

char global_char = 'a';
float float_ = 4.0;
double double_ = 9.0;
long double long_double_neg_9 = -9.0;
long double long_double_7point5 = 7.5;

int *static_int_address;
volatile int *volatile_int_address;
char *global_char_address;

struct cars {
  int skoda[2];
  long* audi;
  int lexus;		
} my_cars;

struct cars* my_cars_ptr = &my_cars;

int twoD[2][3] = { {99, 88, 77},
 	           {11, 12, 13},
                 };  
int oneD[] = { 4, 3, 2, 1};                
char* string = "hello world";
char* char_ptr = NULL;
int* int_ptr = NULL;
char* ptrStrings[] = {"zero", "one", "two", "three"};
int** dynamicTwoD = NULL;
int*  dynamicOneD = NULL;

int main(int argc, char* argv[])
{
  static int static_int = 22;
  volatile int volatile_int = 33;
  register int reg = 5;	  				 
  int i, k;				  
  
  // Dynamically allocated two d array [5][3]
  dynamicTwoD =  malloc (sizeof (int*) * 5);
  for (i=0; i<5; i++)
     dynamicTwoD[i] = malloc (sizeof(int) * 3);
  // Fill with value 9
  for (i=0; i<5; i++)
     for (k=0; k<3; k++)
        dynamicTwoD[i][k] = 9;
        
  dynamicOneD = malloc (sizeof(int) * 3);
  for (i =0 ; i<3; i++)
     dynamicOneD[i] = 5;      
  
  static_int_address = &static_int;
  volatile_int_address = &volatile_int;
  global_char_address = &global_char;
  
  my_cars.skoda[0] = 2;
  my_cars.skoda[1] = 3;
  my_cars.audi = malloc (sizeof(int));
  *(my_cars.audi) = 3;
  my_cars.lexus = reg;

   // Free Dynamic memory
   /*for (i = 0; i < 5; i ++) 
      free(twoDArray[i]);
   free(twoDArray);*/

  if (strcmp(argv[1], "loop") == 0) 
    while (1) {}
 
  char* c = 0;
  c[0] = 'a';
  
  return 0;
}
