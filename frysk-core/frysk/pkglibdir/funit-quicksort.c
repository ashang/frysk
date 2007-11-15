/* 1 */ // This file is part of the program FRYSK.
/* 2 */ //
/* 3 */ // Copyright 2007 Red Hat Inc.
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
# /* 41 */ include <stdio.h>
# /* 42 */ define element_count 256
/* 43 */ 
/* 44 */ int sortlist[element_count+1];
/* 45 */ int seed;
/* 46 */ int biggest, littlest;
/* 47 */ int top;
/* 48 */ 
/* 49 */ void
/* 50 */ init_rand ()
/* 51 */ {
/* 52 */   seed = 74755;
/* 53 */ }
/* 54 */ 
/* 55 */ int
/* 56 */ rand ()
/* 57 */ {
/* 58 */   seed = (seed * 1309 + 13849) & 65535;
/* 59 */   return (seed);
/* 60 */ }
/* 61 */ 
/* 62 */ void
/* 63 */ init_array ()
/* 64 */ {
/* 65 */   int i, temp;
/* 66 */   init_rand ();
/* 67 */   biggest = 0; littlest = 0;
/* 68 */   for (i = 1; i <= element_count; i++)
/* 69 */     {
/* 70 */       temp = rand ();
/* 71 */       sortlist[i] = temp - (temp/100000)*100000 - 50000;
/* 72 */       if (sortlist[i] > biggest) biggest = sortlist[i];
/* 73 */       else if (sortlist[i] < littlest) littlest = sortlist[i];
/* 74 */     }
/* 75 */ }
/* 76 */ 
/* 77 */ /* quicksort the array A from start to finish */
/* 78 */ 
/* 79 */ void
/* 80 */ quicksort (a,l,r) int a[], l, r;
/* 81 */ {
/* 82 */   int i,j,x,w;
/* 83 */ 
/* 84 */   i = l;
/* 85 */   j = r;
/* 86 */   x = a[(l+r) / 2];
/* 87 */   do
/* 88 */     {
/* 89 */       while (a[i] < x) i = i+1;
/* 90 */       while (x < a[j]) j = j-1;
/* 91 */       if (i <= j)
/* 92 */        {
/* 93 */          w = a[i];
/* 94 */          a[i] = a[j];
/* 95 */          a[j] = w;
/* 96 */          i = i+1;
/* 97 */          j = j-1;
/* 98 */        }
/* 99 */     } while (i <= j);
/* 100 */   if (l < j)
/* 101 */     quicksort (a,l,j);
/* 102 */   if (i < r)
/* 103 */     quicksort (a,i,r);
/* 104 */ }
/* 105 */ 
/* 106 */ /* setup an array, quicksort the array, validate the quicksort */
/* 107 */ 
/* 108 */ int
/* 109 */ quick ()
/* 110 */ {
/* 111 */   init_array ();
/* 112 */   quicksort (sortlist, 1, element_count);
/* 113 */   if ((sortlist[1] != littlest) || (sortlist[element_count] != biggest))
/* 114 */     {
/* 115 */       printf (" Error in quicksort.\n");        
/* 116 */       return 1;
/* 117 */     }
/* 118 */   return 0;
/* 119 */ }
/* 120 */ 
/* 121 */ int
/* 122 */ main()
/* 123 */ {
/* 124 */   return quick();
/* 125 */ }
