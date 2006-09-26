/* libunwind - a platform-independent unwind library
   Copyright (c) 2003, 2005 Hewlett-Packard Development Company, L.P.
	Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

   Copied from src/x86_64/, modified slightly (or made empty stubs) for
   building frysk successfully on ppc64, by Wu Zhou <woodzltc@cn.ibm.com>
   Will be replaced when libunwind is ready on ppc64 platform.

This file is part of libunwind.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.  */

#include "unwind_i.h"
#include "dwarf_i.h"

HIDDEN pthread_mutex_t ppc64_lock = PTHREAD_MUTEX_INITIALIZER;
HIDDEN int tdep_needs_initialization = 1;

/* See comments for svr4_dbx_register_map[] in gcc/config/i386/i386.c.  */

uint8_t dwarf_to_unw_regnum_map[DWARF_REGNUM_MAP_LENGTH] =
  {
    UNW_PPC64_R0,
    UNW_PPC64_R1,
    UNW_PPC64_R2,
    UNW_PPC64_R3,
    UNW_PPC64_R4,
    UNW_PPC64_R5,
    UNW_PPC64_R6,
    UNW_PPC64_R7,
    UNW_PPC64_R8,
    UNW_PPC64_R9,
    UNW_PPC64_R10,
    UNW_PPC64_R11,
    UNW_PPC64_R12,
    UNW_PPC64_R13,
    UNW_PPC64_R14,
    UNW_PPC64_R15,
    UNW_PPC64_R16,
    UNW_PPC64_R17,
    UNW_PPC64_R18,
    UNW_PPC64_R19,
    UNW_PPC64_R20,
    UNW_PPC64_R21,
    UNW_PPC64_R22,
    UNW_PPC64_R23,
    UNW_PPC64_R24,
    UNW_PPC64_R25,
    UNW_PPC64_R26,
    UNW_PPC64_R27,
    UNW_PPC64_R28,
    UNW_PPC64_R29,
    UNW_PPC64_R30,
    UNW_PPC64_R31
  };

HIDDEN void
tdep_init (void)
{
  intrmask_t saved_mask;

  sigfillset (&unwi_full_mask);

  sigprocmask (SIG_SETMASK, &unwi_full_mask, &saved_mask);
  mutex_lock (&ppc64_lock);
  {
    if (!tdep_needs_initialization)
      /* another thread else beat us to it... */
      goto out;

    mi_init ();

    dwarf_init ();

#ifndef UNW_REMOTE_ONLY
    ppc64_local_addr_space_init ();
#endif
    tdep_needs_initialization = 0;	/* signal that we're initialized... */
  }
 out:
  mutex_unlock (&ppc64_lock);
  sigprocmask (SIG_SETMASK, &saved_mask, NULL);
}
