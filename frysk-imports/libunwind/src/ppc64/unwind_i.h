/* libunwind - a platform-independent unwind library
   Copyright (C) 2002, 2005 Hewlett-Packard Co
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

#ifndef unwind_i_h
#define unwind_i_h

#include <memory.h>
#include <stdint.h>

#include <libunwind-ppc64.h>

#include "libunwind_i.h"
#include <sys/ucontext.h>

/* DWARF column numbers for ppc64: */
#define R0	0
#define R1	1
#define R2	2
#define R3	3
#define R4	4
#define R5	5
#define R6	6
#define R7	7
#define R8	8
#define R9	9
#define R10	10
#define R11	11
#define R12	12
#define R13	13
#define R14	14
#define R15	15
#define R16	16
#define R17	17
#define R18	18
#define R19	19
#define R20	20
#define R21	21
#define R22	22
#define R23	23
#define R24	24
#define R25	25
#define R26	26
#define R27	27
#define R28	28
#define R29	29
#define R30	30
#define R31	31

#define F0	32
#define F1	33
#define F2	34
#define F3	35
#define F4	36
#define F5	37
#define F6	38
#define F7	39
#define F8	40
#define F9	41
#define F10	42
#define F11	43
#define F12	44
#define F13	45
#define F14	46
#define F15	47
#define F16	48
#define F17	49
#define F18	50
#define F19	51
#define F20	52
#define F21	53
#define F22	54
#define F23	55
#define F24	56
#define F25	57
#define F26	58
#define F27	59
#define F28	60
#define F29	61
#define F30	62
#define F31	63

#define CR	64
#define FPSCR	65
#define MSR	66
#define MQ	100
#define XER	101
#define LR	108
#define CTR	109

#define ppc64_lock			UNW_OBJ(lock)
#define ppc64_local_resume		UNW_OBJ(local_resume)
#define ppc64_local_addr_space_init	UNW_OBJ(local_addr_space_init)
#if 0
#define ppc64_scratch_loc		UNW_OBJ(scratch_loc)
#endif

extern void ppc64_local_addr_space_init (void);
extern int ppc64_local_resume (unw_addr_space_t as, unw_cursor_t *cursor,
			     void *arg);
#if 0
extern dwarf_loc_t ppc64_scratch_loc (struct cursor *c, unw_regnum_t reg);
#endif

#endif /* unwind_i_h */
