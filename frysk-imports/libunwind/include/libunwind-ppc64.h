/* libunwind - a platform-independent unwind library
   Copyright (C) 2002-2004 Hewlett-Packard Co
	Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

   Copied from libunwind-x86_64.h, modified slightly for building
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

#ifndef LIBUNWIND_H
#define LIBUNWIND_H

#if defined(__cplusplus) || defined(c_plusplus)
extern "C" {
#endif

#include <inttypes.h>
#include <ucontext.h>

#define UNW_TARGET		ppc64
#define UNW_TARGET_PPC64	1

#define _U_TDEP_QP_TRUE	0	/* see libunwind-dynamic.h  */

/* This needs to be big enough to accommodate "struct cursor", while
   leaving some slack for future expansion.  Changing this value will
   require recompiling all users of this library.  Stack allocation is
   relatively cheap and unwind-state copying is relatively rare, so we
   want to err on making it rather too big than too small.  

   XXX: how big should this be for ppc64.  */

#define UNW_TDEP_CURSOR_LEN	127

typedef uint64_t unw_word_t;
typedef int64_t unw_sword_t;

typedef long double unw_tdep_fpreg_t;

typedef enum
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
    UNW_PPC64_R31,

    UNW_PPC64_NIP,
    UNW_PPC64_MSR,
    UNW_PPC64_ORIG_GPR3,
    UNW_PPC64_CTR,
    UNW_PPC64_LR,
    UNW_PPC64_XER,
    UNW_PPC64_CCR,
    UNW_PPC64_MQ,
    UNW_PPC64_TRAP,
    UNW_PPC64_DAR,
    UNW_PPC64_DSISR,
    UNW_PPC64_RESULT,

    UNW_PPC64_F0 = 48,
    UNW_PPC64_F1,
    UNW_PPC64_F2,
    UNW_PPC64_F3,
    UNW_PPC64_F4,
    UNW_PPC64_F5,
    UNW_PPC64_F6,
    UNW_PPC64_F7,
    UNW_PPC64_F8,
    UNW_PPC64_F9,
    UNW_PPC64_F10,
    UNW_PPC64_F11,
    UNW_PPC64_F12,
    UNW_PPC64_F13,
    UNW_PPC64_F14,
    UNW_PPC64_F15,
    UNW_PPC64_F16,
    UNW_PPC64_F17,
    UNW_PPC64_F18,
    UNW_PPC64_F19,
    UNW_PPC64_F20,
    UNW_PPC64_F21,
    UNW_PPC64_F22,
    UNW_PPC64_F23,
    UNW_PPC64_F24,
    UNW_PPC64_F25,
    UNW_PPC64_F26,
    UNW_PPC64_F27,
    UNW_PPC64_F28,
    UNW_PPC64_F29,
    UNW_PPC64_F30,
    UNW_PPC64_F31,
    UNW_PPC64_FPSCR,

    /* XXX Add other regs here */

    /* frame info (read-only) */
    UNW_PPC64_CFA,

    UNW_TDEP_LAST_REG = UNW_PPC64_F31,

    UNW_TDEP_IP = UNW_PPC64_NIP,
    UNW_TDEP_SP = UNW_PPC64_R1,
    UNW_TDEP_EH = UNW_PPC64_R12
  }
ppc64_regnum_t;

/* XXX: the number of exception arguments.  Not sure what is the value
   for PPC64 at this time.  */
#define UNW_TDEP_NUM_EH_REGS	2

typedef struct unw_tdep_save_loc
  {
    /* Additional target-dependent info on a save location.  */
  }
unw_tdep_save_loc_t;

/* On ppc64, we can directly use ucontext_t as the unwind context.  */
typedef ucontext_t unw_tdep_context_t;

/* XXX this is not ideal: an application should not be prevented from
   using the "getcontext" name just because it's using libunwind.  We
   can't just use __getcontext() either, because that isn't exported
   by glibc...  */
#define unw_tdep_getcontext(uc)		(getcontext (uc), 0)

#include "libunwind-dynamic.h"

typedef struct
  {
    /* no ppc64-specific auxiliary proc-info */
  }
unw_tdep_proc_info_t;

#include "libunwind-common.h"

#define unw_tdep_is_fpreg		UNW_ARCH_OBJ(is_fpreg)
extern int unw_tdep_is_fpreg (int);

#if defined(__cplusplus) || defined(c_plusplus)
}
#endif

#endif /* LIBUNWIND_H */
