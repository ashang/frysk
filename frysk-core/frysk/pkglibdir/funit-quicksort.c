/* 1 */ // This file is part of the program FRYSK.
/* 2 */ //
/* 3 */ // Copyright 2007, 2008 Red Hat Inc.
/* 4 */ //
/* 5 */ // FRYSK is free software; you can redistribute it and/or modify it
/* 6 */ // under the terms of the GNU General Public License as published by
/* 7 */ // the Free Software Foundation; version 2 of the License.
/* 8 */ //
/* 9 */ // FRYSK is distributed in the hope that it will be useful, but
/* 10 */ // WITHOUT ANY WARRANTY; without even the implied warranty of
/* 11 */ // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
/* 12 */ // General Public License for more details.
/* 13 */ // 
/* 14 */ // You should have received a copy of the GNU General Public License
/* 15 */ // along with FRYSK; if not, write to the Free Software Foundation,
/* 16 */ // Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
/* 17 */ // 
/* 18 */ // In addition, as a special exception, Red Hat, Inc. gives You the
/* 19 */ // additional right to link the code of FRYSK with code not covered
/* 20 */ // under the GNU General Public License ("Non-GPL Code") and to
/* 21 */ // distribute linked combinations including the two, subject to the
/* 22 */ // limitations in this paragraph. Non-GPL Code permitted under this
/* 23 */ // exception must only link to the code of FRYSK through those well
/* 24 */ // defined interfaces identified in the file named EXCEPTION found in
/* 25 */ // the source code files (the "Approved Interfaces"). The files of
/* 26 */ // Non-GPL Code may instantiate templates or use macros or inline
/* 27 */ // functions from the Approved Interfaces without causing the
/* 28 */ // resulting work to be covered by the GNU General Public
/* 29 */ // License. Only Red Hat, Inc. may make changes or additions to the
/* 30 */ // list of Approved Interfaces. You must obey the GNU General Public
/* 31 */ // License in all respects for all of the FRYSK code and other code
/* 32 */ // used in conjunction with FRYSK except the Non-GPL Code covered by
/* 33 */ // this exception. If you modify this file, you may extend this
/* 34 */ // exception to your version of the file, but you are not obligated to
/* 35 */ // do so. If you do not wish to provide this exception without
/* 36 */ // modification, you must delete this exception statement from your
/* 37 */ // version and license this file solely under the GPL without
/* 38 */ // exception.
/* 39 */ 
# /* 40 */ include <sys/types.h>
# /* 41 */ include <stdlib.h>
# /* 42 */ define element_count 256
/* 43 */ 
/* 44 */ void quicksort (int [], int, int);
/* 45 */ void bubblesort (int [], int, int);
/* 46 */ 
/* 47 */ /* quicksort the array A from start to finish */
/* 48 */ 
/* 49 */ void
/* 50 */ quicksort (int a[], int l, int r)
/* 51 */ {
/* 52 */   int i, j, x, w;
/* 53 */ 
/* 54 */   i = l;
/* 55 */   j = r;
/* 56 */   x = a[(l + r) / 2];
/* 57 */   do
/* 58 */     {
/* 59 */       while (a[i] < x) i = i + 1;
/* 60 */       while (x < a[j]) j = j - 1;
/* 61 */       if (i <= j)
/* 62 */        {
/* 63 */          w = a[i];
/* 64 */          a[i] = a[j];
/* 65 */          a[j] = w;
/* 66 */          i = i + 1;
/* 67 */          j = j - 1;
/* 68 */        }
/* 69 */     } while (i <= j);
/* 70 */   if (l < j)
/* 71 */     quicksort (a, l, j);
/* 72 */   if (i < r)
/* 73 */     quicksort (a, i, r);
/* 74 */ }
/* 75 */ 
/* 76 */ void
/* 77 */ init_array (int *sortlist, int *littlest, int *biggest)
/* 78 */ {
/* 79 */   int i, temp;
/* 80 */   unsigned int seed;
/* 81 */   for (i = 1; i <= element_count; i++)
/* 82 */     {
/* 83 */       temp = rand_r(&seed);
/* 84 */       sortlist[i] = temp - (temp/100000) * 100000 - 50000;
/* 85 */       if (sortlist[i] > *biggest) 
/* 86 */ 	*biggest = sortlist[i];
/* 87 */       else if (sortlist[i] < *littlest) 
/* 88 */ 	*littlest = sortlist[i];
/* 89 */     }
/* 90 */ }
/* 91 */ int sortlist[element_count + 1];
/* 92 */ int
/* 93 */ main()
/* 94 */ {
/* 95 */
/* 96 */   int biggest, littlest;
/* 97 */ 
/* 98 */   init_array (sortlist, &littlest, &biggest);
/* 99 */   quicksort (sortlist, 1, element_count);
/* 100 */   if ((sortlist[1] != littlest) || (sortlist[element_count] != biggest))
/* 101 */     {
/* 102 */       return 1;
/* 103 */     }
/* 104 */ 
/* 105 */   init_array (sortlist, &littlest, &biggest);
/* 106 */   bubblesort(sortlist, 1, element_count);
/* 107 */   if ((sortlist[1] != littlest) || (sortlist[element_count] != biggest))
/* 108 */     {
/* 109 */       return 1;
/* 110 */     }
/* 111 */   return 0;
/* 112 */ }
