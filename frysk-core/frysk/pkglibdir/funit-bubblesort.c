/* 1 */ // This file is part of the program FRYSK.
/* 2 */ //
/* 3 */ // Copyright 2008 Red Hat Inc.
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
/* 44 */ /* bubblesort an array A.  */
/* 45 */ 
/* 46 */ void
/* 47 */ bubblesort (int a[], int l, int r)
/* 48 */ {
/* 49 */   while (r > 1) {
/* 50 */ 
/* 51 */     int i = 1;
/* 52 */     int j;
/* 53 */     while (i < r) {
/* 54 */ 
/* 55 */       if (a[i] > a[i + 1]) {
/* 56 */         j = a[i];
/* 57 */         a[i] = a[i + 1];
/* 58 */         a[i + 1] = j;
/* 59 */       }
/* 60 */       i = i + 1;
/* 61 */     }
/* 62 */ 
/* 63 */     r = r - 1;
/* 64 */   }
/* 65 */ }
/* 66 */ 
# /* 67 */ ifndef NO_MAIN
/* 68 */ static void
/* 69 */ init_array (int *sortlist, int *littlest, int *biggest)
/* 70 */ {
/* 71 */   int i, temp;
/* 72 */   unsigned int seed;
/* 73 */   for (i = 1; i <= element_count; i++)
/* 74 */     {
/* 75 */       temp = rand_r (&seed);
/* 76 */       sortlist[i] = temp - (temp/100000) * 100000 - 50000;
/* 77 */       if (sortlist[i] > *biggest) 
/* 78 */ 	*biggest = sortlist[i];
/* 79 */       else if (sortlist[i] < *littlest) 
/* 80 */ 	*littlest = sortlist[i];
/* 81 */     }
/* 82 */ }
/* 83 */ 
/* 84 */ int
/* 85 */ main()
/* 86 */ {
/* 87 */   int sortlist[element_count + 1];
/* 88 */   int biggest = 0, littlest = 0;
/* 89 */ 
/* 90 */   init_array (sortlist, &littlest, &biggest);
/* 91 */   bubblesort(sortlist, 1, element_count);
/* 92 */   if ((sortlist[1] != littlest) || (sortlist[element_count] != biggest))
/* 93 */     {
/* 94 */       return 1;
/* 95 */     }
/* 96 */   return 0;
/* 97 */ }
# /* 98 */ endif
