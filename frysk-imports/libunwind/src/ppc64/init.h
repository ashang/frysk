/* libunwind - a platform-independent unwind library
   Copyright (C) 2002 Hewlett-Packard Co
	Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

   Copied from src/x86_64/, modified slightly for building
   frysk successfully on ppc64, by Wu Zhou <woodzltc@cn.ibm.com>
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

static inline int
common_init (struct cursor *c)
{
  int ret;

  c->dwarf.loc[R0] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R0);
  c->dwarf.loc[R1] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R1);
  c->dwarf.loc[R2] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R2);
  c->dwarf.loc[R3] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R3);
  c->dwarf.loc[R4] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R4);
  c->dwarf.loc[R5] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R5);
  c->dwarf.loc[R6] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R6);
  c->dwarf.loc[R7] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R7);
  c->dwarf.loc[R8] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R8);
  c->dwarf.loc[R9] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R9);
  c->dwarf.loc[R10] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R10);
  c->dwarf.loc[R11] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R11);
  c->dwarf.loc[R12] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R12);
  c->dwarf.loc[R13] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R13);
  c->dwarf.loc[R14] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R14);
  c->dwarf.loc[R15] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R15);
  c->dwarf.loc[R16] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R16);
  c->dwarf.loc[R17] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R17);
  c->dwarf.loc[R18] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R18);
  c->dwarf.loc[R19] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R19);
  c->dwarf.loc[R20] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R20);
  c->dwarf.loc[R21] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R21);
  c->dwarf.loc[R22] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R22);
  c->dwarf.loc[R23] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R23);
  c->dwarf.loc[R24] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R24);
  c->dwarf.loc[R25] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R25);
  c->dwarf.loc[R26] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R26);
  c->dwarf.loc[R27] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R27);
  c->dwarf.loc[R28] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R28);
  c->dwarf.loc[R29] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R29);
  c->dwarf.loc[R30] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R30);
  c->dwarf.loc[R31] = DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R31);

// XXX: how to get nip?
//  ret = dwarf_get (&c->dwarf, c->dwarf.loc[NIP], &c->dwarf.ip);
//  if (ret < 0)
//    return ret;

  ret = dwarf_get (&c->dwarf, DWARF_REG_LOC (&c->dwarf, UNW_PPC64_R1),
		   &c->dwarf.cfa);
  if (ret < 0)
    return ret;

  c->sigcontext_format = PPC64_SCF_NONE;
  c->sigcontext_addr = 0;

  c->dwarf.decrease_ip = 0;
  c->dwarf.args_size = 0;
  c->dwarf.ret_addr_column = 0;
  c->dwarf.pi_valid = 0;
  c->dwarf.pi_is_dynamic = 0;
  c->dwarf.hint = 0;
  c->dwarf.prev_rs = 0;

  return 0;
}
