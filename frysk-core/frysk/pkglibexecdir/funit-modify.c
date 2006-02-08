// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
#include <signal.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>

int j;
volatile int *k;

void handler (int sig)
{
  signal (SIGSEGV, SIG_IGN);
}

typedef union {
  int16_t val;
  char ch[2];
} union16_t;

typedef union {
  int32_t val;
  char ch[4];
} union32_t;

typedef union {
  int64_t val;
  char ch[8];
} union64_t;

int
main (int argc, char **argv)
{
  int i;
  FILE *fp;
  char arr[17016];
  char *arrPtr = arr;
  char ch_array[] = "abcdefghijklmnopqrstuvwxyz";
  union16_t x;
  union32_t y;
  union64_t z;

  memset (arr, 0xff, sizeof (arr));

  /* Write out the memory area address (and its size) so frysk
     can modify the memory without having to know the architecture.  */
  fp = fopen ("memAddr.file", "wb");
  if (fwrite (&arrPtr, sizeof (arrPtr), 1, fp) != 1) {
    perror ("fwrite");
    abort ();
  }
  fclose (fp);
 
  /* Signal to frysk that memory address is accessible via special file.  */ 
  signal (SIGSEGV, &handler);
  kill (getpid (), SIGSEGV);

  /* Verify modifications.  */

  /* Start with individual bytes across a typical page boundary.  */
  for (i = 0; i < 4097; ++i) {
     if (arr[i] != ch_array[i % 26]) {
     	printf ("char %d was %x instead of %x\n", i, arr[i], 
		 ch_array[i%26]);
	abort ();
     }
  }

  /* Next verify 16-bit int values (some negative) across the next page.  */
  for (i = 0; i < 100; ++i) {
     int16_t val = *((int16_t *) (arr + 8000 + i * 2));
     if (val != 50 - i) {
     	printf ("short %d was %hx instead of %hx\n", i, val, 50 - i);
	abort ();
     }
  }

  /* Verify a 16-bit value that is unaligned.  */
  x.ch[0] = arr[9999];
  x.ch[1] = arr[10000];

  if (x.val != (int16_t) 0xdeaf) {
     printf ("unaligned int16_t value was %hx instead of %hx\n", 
	     x.val, (int16_t) 0xdeaf);
     abort ();
  }

  /* Next verify 32-bit int values (some negative) across the next page.  */
  for (i = 0; i < 100; ++i) {
     int32_t val = *((int32_t *) (arr + 12096 + i * 4));
     if (val != 50 - i) {
     	printf ("int value %d is <%x> instead of <%x>\n", i, val, 50 - i);
	abort ();
     }
  }

  /* Verify a 32-bit value that is unaligned.  */
  memcpy (y.ch, arr + 14001, 4);
  if (y.val != (int32_t) 0xabcdef01) {
     printf ("unaligned int32_t value is incorrect\n");
     abort ();
  }

  /* Next verify 64-bit int values (some negative) across the next page.  */
  for (i = 0; i < 100; ++i) {
     int64_t val = *((int64_t *) (arr + 16192 + i * 8));
     if (val != (int64_t) (50 - i)) {
     	printf ("int64_t value %d is incorrect\n", i);
	abort ();
     }
  }

  /* Verify a 64-bit value that is unaligned.  */
  memcpy (z.ch, arr + 17003, 8);
  if (z.val != (int64_t) 0xabcdef0123456789LL) {
     printf ("unaligned int64_t value is incorrect");
     abort ();
  }

  exit (0);
}


