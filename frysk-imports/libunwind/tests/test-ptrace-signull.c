/* This testcase is part of libunwind.

   Copyright 1996, 1999, 2003, 2004, 2006 Free Software Foundation, Inc.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
 
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA.  */

#include <signal.h>
#include <setjmp.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <assert.h>

enum tests {
  data_read, data_write, code_entry_point, code_descriptor, invalid
};

/* Some basic types and zero buffers.  */

typedef long data_t;
typedef long code_t (void);
data_t *volatile data;
code_t *volatile code;
/* "desc" is intentionally initialized to a data object.  This is
   needed to test function descriptors on arches like ia64.  */
data_t zero[10];
code_t *volatile desc = (code_t *) (void *) zero;

sigjmp_buf env;

extern void
keeper (int sig)
{
#if 0
  /* `-s' invocation.  */
  alarm (0);
#endif

  raise(SIGUSR2);

  siglongjmp (env, 1);
}

extern long
bowler (enum tests test)
{
  signal(SIGUSR1,SIG_IGN);
  signal(SIGUSR2,SIG_IGN);
  raise(SIGUSR1);

  switch (test)
    {
    case data_read:
      /* Try to read address zero.  */
      return (*data);
    case data_write:
      /* Try to write (the assignment) to address zero.  */
      return (*data) = 1;
    case code_entry_point:
      /* For typical architectures, call a function at address
	 zero.  */
      return (*code) ();
    case code_descriptor:
      /* For atypical architectures that use function descriptors,
	 call a function descriptor, the code field of which is zero
	 (which has the effect of jumping to address zero).  */
      return (*desc) ();
    case invalid:;
      /* FALLTHRU */
    }
  assert (0);
}

int
main (int argc, char **argv)
{
  enum tests test = invalid;

#define ARG(string)					\
  do							\
    {							\
      if (argc == 2 && !strcmp (argv[1], #string ))	\
	test = string;					\
    }							\
  while (0)

  ARG (data_read);
  ARG (data_write);
  ARG (code_entry_point);
  ARG (code_descriptor);

  assert (test != invalid);

  struct sigaction act;
  memset (&act, 0, sizeof act);
  act.sa_handler = keeper;
  sigaction (SIGSEGV, &act, NULL);

  if (!sigsetjmp (env, 1))
    bowler (test);

  return 0;
}
