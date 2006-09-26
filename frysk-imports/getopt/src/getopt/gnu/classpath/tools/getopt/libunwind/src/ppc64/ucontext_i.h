/* Copyright (C) 2004 Hewlett-Packard Co.
     Contributed by David Mosberger-Tang <davidm@hpl.hp.com>.

   Copied from src/x86_64/, modified slightly for building
   frysk successfully on ppc64, by Wu Zhou <woodzltc@cn.ibm.com>
   Will be replaced when libunwind is ready on ppc64 platform.

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

/* XXX: what are these for, to find the value of dwarf register from
   the ucontext structure?  Need to do more investigation for this.  */
#define UC_MCONTEXT_GREGS_R0
#define UC_MCONTEXT_GREGS_R1
#define UC_MCONTEXT_GREGS_R2
#define UC_MCONTEXT_GREGS_R3
#define UC_MCONTEXT_GREGS_R4
#define UC_MCONTEXT_GREGS_R5
#define UC_MCONTEXT_GREGS_R6
#define UC_MCONTEXT_GREGS_R7
#define UC_MCONTEXT_GREGS_R8
#define UC_MCONTEXT_GREGS_R9
#define UC_MCONTEXT_GREGS_R10
#define UC_MCONTEXT_GREGS_R11
#define UC_MCONTEXT_GREGS_R12
#define UC_MCONTEXT_GREGS_R13
#define UC_MCONTEXT_GREGS_R14
#define UC_MCONTEXT_GREGS_R15
#define UC_MCONTEXT_GREGS_R16
#define UC_MCONTEXT_GREGS_R17
#define UC_MCONTEXT_GREGS_R18
#define UC_MCONTEXT_GREGS_R19
#define UC_MCONTEXT_GREGS_R20
#define UC_MCONTEXT_GREGS_R21
#define UC_MCONTEXT_GREGS_R22
#define UC_MCONTEXT_GREGS_R23
#define UC_MCONTEXT_GREGS_R24
#define UC_MCONTEXT_GREGS_R25
#define UC_MCONTEXT_GREGS_R26
#define UC_MCONTEXT_GREGS_R27
#define UC_MCONTEXT_GREGS_R28
#define UC_MCONTEXT_GREGS_R29
#define UC_MCONTEXT_GREGS_R30
#define UC_MCONTEXT_GREGS_R31
