/* libunwind - a platform-independent unwind library
   Copyright (C) 2002-2004 Hewlett-Packard Co
	Contributed by David Mosberger-Tang <davidm@hpl.hp.com>

   Modified for x86_64 by Max Asbock <masbock@us.ibm.com>

   Copyright (C) 2008 Red Hat, Inc.
	Contributed by Mark Wielaard <mwielaard@redhat.com>

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
#include "ucontext_i.h"
#include <signal.h>

/* Try to skip single-word (8 bytes) return address on stack left there in some
   cases of the signal frame.
   Unsure if the real return address gets pushed to the stack exactly when this
   bit is set.  If it is not detectable we would need to keep the return
   address on stack and improve the code path
   	dwarf_step() failed (ret=%d), trying frame-chain
   below to disassemble the return address and not to pop two words
   automatically as no stack frame pointer is present in these cases there:
   	./test-ptrace-signull code_entry_point: err=0x14
   	./test-ptrace-signull code_descriptor:  err=0x15  */

static int
code_descriptor_trap (struct cursor *c, struct dwarf_loc *rip_loc_pointer)
{
  unw_word_t trapno, err;
  int i;

  if (c->sigcontext_format != X86_64_SCF_LINUX_RT_SIGFRAME)
    return -UNW_EBADFRAME;

  i = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + UC_MCONTEXT_GREGS_TRAPNO, 0), &trapno);
  if (i < 0)
    {
      Debug (2, "failed to query [sigframe:trapno]; %d\n", i);
      return i;
    }
  /* page fault */
  if (trapno != UC_MCONTEXT_GREGS_TRAPNO_PF)
    {
      Debug (2, "[sigframe:trapno] %d not Page-Fault Exception\n", (int) trapno);
      return -UNW_EBADFRAME;
    }

  i = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + UC_MCONTEXT_GREGS_ERR, 0), &err);
  if (i < 0)
    {
      Debug (2, "failed to query [sigframe:err]; %d\n", i);
      return i;
    }
  if (!(err & (1 << UC_MCONTEXT_GREGS_ERR_ID_BIT)))
    {
      Debug (2, "[sigframe:err] 0x%x not &0x%x (instruction fetch)\n",
	     (int) err, (1 << UC_MCONTEXT_GREGS_ERR_ID_BIT));
      return -UNW_EBADFRAME;
    }

  Debug (1, "[sigframe] Page-Fault Exception, instruction fetch (err = 0x%x)\n", (int) err);

  *rip_loc_pointer = DWARF_LOC (c->dwarf.cfa, 0);
  c->dwarf.cfa += 8;

  return 1;
}

// A CALL instruction starts with 0xFF.
static int
is_call_instr_at (struct cursor *c, unw_word_t addr)
{
  int ret;
  unw_word_t instr;
  ret = dwarf_get (&c->dwarf, DWARF_LOC (addr, 0), &instr);
  Debug (99, "ret %d, instr 0x%x\n", ret, instr);
  return ret >= 0 && ((instr & 0xff000000) == 0xff000000);
}

// Checks whether this looks like a plt entry like cursor and returns
// the stack offset where the return address can be found, or -1 if
// not detected (also tries to make sure this is the inner most frame).
// When this function returns positively (zero included) addr will
// contain the return address.
static int
init_stack_based_ret (struct cursor *c, unw_word_t *addr)
{
  // See if this looks "clean", everything in actual registers
  // which indicates this is most likely an inner most frame just
  // initted.
  int ret;
  unw_word_t ip, cfa;
  ret = dwarf_get (&c->dwarf, c->dwarf.loc[RIP], &ip);
  if (ret < 0)
    return ret;
  
  ret = dwarf_get (&c->dwarf, c->dwarf.loc[RSP], &cfa);
  if (ret < 0)
    return ret;
  
  // See if one of the top 3 elements on the stack contains a
  // return address.
  int i;
  for (i = 0; i <= 16; i += 8)
    {
      Debug (99, "trying %d\n", i);
      ret = dwarf_get (&c->dwarf, DWARF_LOC (c->dwarf.cfa + i, 0), addr);
      if (ret < 0)
	      return ret;
      
      Debug (99, "addr at %d: 0x%x\n", i, *addr);
      // Sanity check the address, not too low, and must
      // come from a call instruction.
      if (*addr > 0 && is_call_instr_at(c, (*addr) - 5))
	return i;
    }
  
  return -1;
}


PROTECTED int
unw_step (unw_cursor_t *cursor)
{
  struct cursor *c = (struct cursor *) cursor;
  int ret, i;
  struct dwarf_loc rip_loc;
  int rip_loc_set = 0;
  unw_word_t prev_ip = c->dwarf.ip, prev_cfa = c->dwarf.cfa;
  unw_word_t addr;

  Debug (1, "(cursor=%p, ip=0x%016llx)\n",
	 c, (unsigned long long) c->dwarf.ip);

  /* Try DWARF-based unwinding... */
  ret = dwarf_step (&c->dwarf);

  /* Skip the faulty address left alone on the stack.  */
  if (ret >= 0 && code_descriptor_trap (c, &rip_loc) > 0)
    rip_loc_set = 1;

  if (ret < 0 && ret != -UNW_ENOINFO)
    {
      Debug (2, "returning %d\n", ret);
      return ret;
    }

  if (c->sigcontext_format == X86_64_SCF_LINUX_RT_SIGFRAME)
    {
      unw_word_t trapno, err;
      int trapno_ret, err_ret;

      trapno_ret = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + UC_MCONTEXT_GREGS_TRAPNO, 0), &trapno);
      err_ret = dwarf_get (&c->dwarf, DWARF_LOC (c->sigcontext_addr + UC_MCONTEXT_GREGS_ERR, 0), &err);

      Debug (2, "x86_64 sigcontext (post-step): CFA = 0x%lx, trapno = %d, err = 0x%x\n",
	     (unsigned long) c->dwarf.cfa, (trapno_ret < 0 ? -1 : (int) trapno),
	     (err_ret < 0 ? -1 : (int) err));
    }

  if (unlikely (ret < 0))
    {
      /* DWARF failed.  There isn't much of a usable frame-chain on x86-64,
	 but we do need to handle two special-cases:

	  (i) signal trampoline: Old kernels and older libcs don't
	      export the vDSO needed to get proper unwind info for the
	      trampoline.  Recognize that case by looking at the code
	      and filling in things by hand.

	  (ii) PLT (shared-library) call-stubs: PLT stubs are invoked
	      via CALLQ.  Try this for all non-signal trampoline
	      code.  */

      struct dwarf_loc rbp_loc, rsp_loc;

      Debug (13, "dwarf_step() failed (ret=%d), trying frame-chain\n", ret);

      if (c->dwarf.ip == 0)
        {
	  Debug (1, "[RIP=0]\n");

	  rip_loc = DWARF_LOC (c->dwarf.cfa, 0);
	  c->dwarf.cfa += 8;
	}
      else if (unw_is_signal_frame (cursor))
	{
	  unw_word_t ucontext = c->dwarf.cfa;

	  Debug(1, "signal frame, skip over trampoline\n");

	  c->sigcontext_format = X86_64_SCF_LINUX_RT_SIGFRAME;
	  c->sigcontext_addr = c->dwarf.cfa;

	  rsp_loc = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RSP, 0);
	  rbp_loc = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RBP, 0);
	  rip_loc = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RIP, 0);

	  ret = dwarf_get (&c->dwarf, rsp_loc, &c->dwarf.cfa);
	  if (ret < 0)
	    {
	      Debug (2, "returning %d\n", ret);
	      return ret;
	    }

	  c->dwarf.loc[RAX] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RAX, 0);
	  c->dwarf.loc[RDX] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RDX, 0);
	  c->dwarf.loc[RCX] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RCX, 0);
	  c->dwarf.loc[RBX] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RBX, 0);
	  c->dwarf.loc[RSI] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RSI, 0);
	  c->dwarf.loc[RDI] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RDI, 0);
	  c->dwarf.loc[RBP] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RBP, 0);
	  c->dwarf.loc[ R8] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R8, 0);
	  c->dwarf.loc[ R9] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R9, 0);
	  c->dwarf.loc[R10] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R10, 0);
	  c->dwarf.loc[R11] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R11, 0);
	  c->dwarf.loc[R12] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R12, 0);
	  c->dwarf.loc[R13] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R13, 0);
	  c->dwarf.loc[R14] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R14, 0);
	  c->dwarf.loc[R15] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_R15, 0);
	  c->dwarf.loc[RIP] = DWARF_LOC (ucontext + UC_MCONTEXT_GREGS_RIP, 0);

	  c->dwarf.loc[RBP] = rbp_loc;
	  c->dwarf.loc[RSP] = rsp_loc;
	}
      else if((ret = init_stack_based_ret(c, &addr)) >= 0)
	{
	  Debug (99, "init_stack_based_ret() %d (0x%x)\n", ret, addr);
	  c->dwarf.cfa += ret + 8;
	  c->dwarf.loc[RSP] = DWARF_LOC (c->dwarf.cfa, 0);
	  c->dwarf.loc[RIP] = DWARF_LOC (addr, 0);
	  c->dwarf.ret_addr_column = RIP;
	  c->dwarf.ip = addr;
	  return 1;
	}
      else
	{
	  unw_word_t rbp;

	  ret = dwarf_get (&c->dwarf, c->dwarf.loc[RBP], &rbp);
	  if (ret < 0)
	    {
	      Debug (2, "returning %d\n", ret);
	      return ret;
	    }

	  if (!rbp)
	    {
	      /* Looks like we may have reached the end of the call-chain.  */
	      rbp_loc = DWARF_NULL_LOC;
	      rsp_loc = DWARF_NULL_LOC;
	      rip_loc = DWARF_NULL_LOC;
	    }
	  else
	    {
	      unw_word_t rbp1;
	      Debug (1, "[RBP=0x%Lx] = 0x%Lx (cfa = 0x%Lx)\n",
		     (unsigned long long) DWARF_GET_LOC (c->dwarf.loc[RBP]),
		     (unsigned long long) rbp,
		     (unsigned long long) c->dwarf.cfa);

	      rbp_loc = DWARF_LOC(rbp, 0);
	      rsp_loc = DWARF_NULL_LOC;
	      rip_loc = DWARF_LOC (rbp + 8, 0);
              /* Heuristic to recognize a bogus frame pointer */
	      ret = dwarf_get (&c->dwarf, rbp_loc, &rbp1);
              if (ret || ((rbp1 - rbp) > 0x4000))
                rbp_loc = DWARF_NULL_LOC;
	      c->dwarf.cfa += 16;
	    }

	  /* Mark all registers unsaved */
	  for (i = 0; i < DWARF_NUM_PRESERVED_REGS; ++i)
	    c->dwarf.loc[i] = DWARF_NULL_LOC;

	  c->dwarf.loc[RBP] = rbp_loc;
	  c->dwarf.loc[RSP] = rsp_loc;
	}
      rip_loc_set = 1;
    }
  if (rip_loc_set)
    {
      c->dwarf.loc[RIP] = rip_loc;
      c->dwarf.ret_addr_column = RIP;

      /* x86_64 ABI specifies that end of call-chain is marked with a
	 NULL RBP.  */
      if (!DWARF_IS_NULL_LOC (c->dwarf.loc[RBP]))
	{
	  ret = dwarf_get (&c->dwarf, c->dwarf.loc[RIP], &c->dwarf.ip);
	  Debug (1, "Frame Chain [RIP=0x%Lx] = 0x%Lx\n",
		     (unsigned long long) DWARF_GET_LOC (c->dwarf.loc[RIP]),
		     (unsigned long long) c->dwarf.ip);
	  if (ret < 0)
	    {
	      Debug (2, "returning %d\n", ret);
	      return ret;
	    }
	}
      else
	c->dwarf.ip = 0;
    }

  if (c->dwarf.ip == prev_ip && c->dwarf.cfa == prev_cfa)
    return -UNW_EBADFRAME;

  ret = (c->dwarf.ip == 0) ? 0 : 1;
  Debug (2, "dwarf.ip = 0x%x, returning %d\n", c->dwarf.ip, ret);
  return ret;
}
