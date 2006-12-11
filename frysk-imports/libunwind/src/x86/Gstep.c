/* libunwind - a platform-independent unwind library
   Copyright (C) 2002-2004 Hewlett-Packard Co
	Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

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
#include "offsets.h"

/* Try to skip single-word (4 bytes) return address on stack left there in some
   cases of the signal frame.
   We cannot rely on the `UC_MCONTEXT_GREGS_ERR_ID_BIT' bit as in the x86_64 case
   as this bit is not present if no execute-disable feature is present/used.
   	http://www.intel.com/design/processor/manuals/253668.pdf
   	page 5-55 (235/642)
   If we detect `ip' points to `cr2' (we should execute at the address causing
   the fault) we can pop the real `ip' from the stack instead.
   If it is not detectable we would need to keep the return
   address on stack and improve the code path
   	dwarf_step() failed (ret=%d), trying frame-chain
   below to disassemble the return address and not to pop two words
   automatically as no stack frame pointer is present in these cases there:
   	./test-ptrace-signull code_entry_point: err=0x4 || 0x14
   	./test-ptrace-signull code_descriptor:  err=0x5 || 0x15  */

static int
code_descriptor_trap (struct cursor *c, struct dwarf_loc *eip_loc_pointer)
{
  unw_word_t trapno, cr2;
  int i;

  if (c->sigcontext_format == X86_SCF_NONE)
    return -UNW_EBADFRAME;

  i = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + LINUX_SC_TRAPNO_OFF, 0), &trapno);
  if (i < 0)
    {
      Debug (2, "failed to query [sigframe:trapno]; %d\n", i);
      return i;
    }
  /* page fault */
  if (trapno != LINUX_SC_TRAPNO_PF)
    {
      Debug (2, "[sigframe:trapno] %d not Page-Fault Exception\n", (int) trapno);
      return -UNW_EBADFRAME;
    }

  i = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + LINUX_SC_CR2_OFF, 0), &cr2);
  if (i < 0)
    {
      Debug (2, "failed to query [sigframe:cr2]; %d\n", i);
      return i;
    }
  if (cr2 != c->dwarf.ip)
    {
      Debug (2, "[sigframe:cr2] 0x%x not equal to ip 0x%x\n",
	     (int) cr2, (int) c->dwarf.ip);
      return -UNW_EBADFRAME;
    }

  Debug (1, "[sigframe] Page-Fault Exception, instruction fetch (cr2 = 0x%x)\n", (int) cr2);

  *eip_loc_pointer = DWARF_LOC (c->dwarf.cfa, 0);
  c->dwarf.cfa += 4;

  return 1;
}

PROTECTED int
unw_step (unw_cursor_t *cursor)
{
  struct cursor *c = (struct cursor *) cursor;
  int ret, i;
  struct dwarf_loc eip_loc;
  int eip_loc_set = 0;

  Debug (1, "(cursor=%p, ip=0x%08x)\n", c, (unsigned) c->dwarf.ip);

  /* Try DWARF-based unwinding... */
  ret = dwarf_step (&c->dwarf);

  /* Skip the faulty address left alone on the stack.  */
  if (ret >= 0 && code_descriptor_trap (c, &eip_loc) > 0)
    eip_loc_set = 1;

  if (ret < 0 && ret != -UNW_ENOINFO)
    {
      Debug (2, "returning %d\n", ret);
      return ret;
    }

  if (c->sigcontext_format == X86_SCF_LINUX_SIGFRAME)
    {
      unw_word_t trapno, err, cr2;
      int trapno_ret, err_ret, cr2_ret;

      trapno_ret = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + LINUX_SC_TRAPNO_OFF, 0), &trapno);
      err_ret = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + LINUX_SC_ERR_OFF, 0), &err);
      cr2_ret = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + LINUX_SC_CR2_OFF, 0), &cr2);

      Debug (2, "x86 sigcontext (post-step): CFA = 0x%lx, trapno = %d, err = 0x%x, cr2 = 0x%x\n",
	     (unsigned long) c->dwarf.cfa, (trapno_ret < 0 ? -1 : (int) trapno),
	     (err_ret < 0 ? -1 : (int) err), (cr2_ret < 0 ? -1 : (int) cr2));
    }

  if (unlikely (ret < 0))
    {
      /* DWARF failed, let's see if we can follow the frame-chain
	 or skip over the signal trampoline.  */

      Debug (13, "dwarf_step() failed (ret=%d), trying frame-chain\n", ret);

      if (c->dwarf.ip == 0)
        {
	  Debug (13, "[EIP=0]\n");

	  eip_loc = DWARF_LOC (c->dwarf.cfa, 0);
	  c->dwarf.cfa += 4;
	}
      else if (unw_is_signal_frame (cursor) > 0)
	{
	  /* XXX This code is Linux-specific! */

	  Debug (13, "Unwinding as non-CFI signal frame\n");

	  /* c->esp points at the arguments to the handler.  Without
	     SA_SIGINFO, the arguments consist of a signal number
	     followed by a struct sigcontext.  With SA_SIGINFO, the
	     arguments consist a signal number, a siginfo *, and a
	     ucontext *. */
	  unw_word_t sc_addr;
	  unw_word_t siginfo_ptr_addr = c->dwarf.cfa + 4;
	  unw_word_t sigcontext_ptr_addr = c->dwarf.cfa + 8;
	  unw_word_t siginfo_ptr, sigcontext_ptr;
	  struct dwarf_loc esp_loc, siginfo_ptr_loc, sigcontext_ptr_loc;

	  siginfo_ptr_loc = DWARF_LOC (siginfo_ptr_addr, 0);
	  sigcontext_ptr_loc = DWARF_LOC (sigcontext_ptr_addr, 0);
	  ret = (dwarf_get (&c->dwarf, siginfo_ptr_loc, &siginfo_ptr)
		 | dwarf_get (&c->dwarf, sigcontext_ptr_loc, &sigcontext_ptr));
	  if (ret < 0)
	    {
	      Debug (2, "returning 0\n");
	      return 0;
	    }
	  if (siginfo_ptr < c->dwarf.cfa
	      || siginfo_ptr > c->dwarf.cfa + 256
	      || sigcontext_ptr < c->dwarf.cfa
	      || sigcontext_ptr > c->dwarf.cfa + 256)
	    {
	      /* Not plausible for SA_SIGINFO signal */
	      c->sigcontext_format = X86_SCF_LINUX_SIGFRAME;
	      c->sigcontext_addr = sc_addr = c->dwarf.cfa + 4;
	    }
	  else
	    {
	      /* If SA_SIGINFO were not specified, we actually read
		 various segment pointers instead.  We believe that at
		 least fs and _fsh are always zero for linux, so it is
		 not just unlikely, but impossible that we would end
		 up here. */
	      c->sigcontext_format = X86_SCF_LINUX_RT_SIGFRAME;
	      c->sigcontext_addr = sigcontext_ptr;
	      sc_addr = sigcontext_ptr + LINUX_UC_MCONTEXT_OFF;
	    }
	  esp_loc = DWARF_LOC (sc_addr + LINUX_SC_ESP_OFF, 0);
	  eip_loc = DWARF_LOC (sc_addr + LINUX_SC_EIP_OFF, 0);
	  ret = dwarf_get (&c->dwarf, esp_loc, &c->dwarf.cfa);
	  if (ret < 0)
	    {
	      Debug (2, "returning 0\n");
	      return 0;
	    }

	  c->dwarf.loc[EAX] = DWARF_LOC (sc_addr + LINUX_SC_EAX_OFF, 0);
	  c->dwarf.loc[ECX] = DWARF_LOC (sc_addr + LINUX_SC_ECX_OFF, 0);
	  c->dwarf.loc[EDX] = DWARF_LOC (sc_addr + LINUX_SC_EDX_OFF, 0);
	  c->dwarf.loc[EBX] = DWARF_LOC (sc_addr + LINUX_SC_EBX_OFF, 0);
	  c->dwarf.loc[EBP] = DWARF_LOC (sc_addr + LINUX_SC_EBP_OFF, 0);
	  c->dwarf.loc[ESI] = DWARF_LOC (sc_addr + LINUX_SC_ESI_OFF, 0);
	  c->dwarf.loc[EDI] = DWARF_LOC (sc_addr + LINUX_SC_EDI_OFF, 0);
	  c->dwarf.loc[EFLAGS] = DWARF_NULL_LOC;
	  c->dwarf.loc[TRAPNO] = DWARF_NULL_LOC;
	  c->dwarf.loc[ST0] = DWARF_NULL_LOC;
	}
      else
	{
	  ret = dwarf_get (&c->dwarf, c->dwarf.loc[EBP], &c->dwarf.cfa);
	  if (ret < 0)
	    {
	      Debug (2, "returning %d\n", ret);
	      return ret;
	    }

	  Debug (13, "[EBP=0x%x] = 0x%x\n", DWARF_GET_LOC (c->dwarf.loc[EBP]),
		 c->dwarf.cfa);

	  /* Mark all registers unsaved, since we don't know where
	     they are saved (if at all), except for the EBP and
	     EIP.  */
	  for (i = 0; i < DWARF_NUM_PRESERVED_REGS; ++i)
	    c->dwarf.loc[i] = DWARF_NULL_LOC;

	  c->dwarf.loc[EBP] = DWARF_LOC (c->dwarf.cfa, 0);
	  eip_loc = DWARF_LOC (c->dwarf.cfa + 4, 0);
	  c->dwarf.cfa += 8;
	}
      eip_loc_set = 1;
    }
  if (eip_loc_set)
    {
      c->dwarf.loc[EIP] = eip_loc;
      c->dwarf.ret_addr_column = EIP;

      if (!DWARF_IS_NULL_LOC (c->dwarf.loc[EBP]))
	{
	  ret = dwarf_get (&c->dwarf, c->dwarf.loc[EIP], &c->dwarf.ip);
	  if (ret < 0)
	    {
	      Debug (2, "returning %d\n", ret);
	      return ret;
	    }
	}
      else
	c->dwarf.ip = 0;
    }

  ret = (c->dwarf.ip == 0) ? 0 : 1;
  Debug (2, "returning %d\n", ret);
  return ret;
}
