/* libunwind - a platform-independent unwind library
   Copyright (c) 2003, 2005 Hewlett-Packard Development Company, L.P.
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

#include <stddef.h>
#include "dwarf_i.h"
#include "libunwind_i.h"

#define alloc_reg_state()	(mempool_alloc (&dwarf_reg_state_pool))
#define free_reg_state(rs)	(mempool_free (&dwarf_reg_state_pool, rs))

static inline int
read_regnum (unw_addr_space_t as, unw_accessors_t *a, unw_word_t *addr,
	     unw_word_t *valp, void *arg)
{
  int ret;

  if ((ret = dwarf_read_uleb128 (as, a, addr, valp, arg)) < 0)
    return ret;

  if (*valp >= DWARF_NUM_PRESERVED_REGS)
    {
      Debug (1, "Invalid register number %u\n", (unsigned int) *valp);
      return -UNW_EBADREG;
    }
  return 0;
}

static inline void
set_reg (dwarf_state_record_t *sr, unw_word_t regnum, dwarf_where_t where,
	 unw_word_t val)
{
  sr->rs_current.reg[regnum].where = where;
  sr->rs_current.reg[regnum].val = val;
}

/* Run a CFI program to update the register state.  */
static int
run_cfi_program (struct dwarf_cursor *c, dwarf_state_record_t *sr,
		 unw_word_t ip, unw_word_t *addr, unw_word_t end_addr,
		 struct dwarf_cie_info *dci)
{
  unw_word_t curr_ip, operand = 0, regnum, val, len, fde_encoding;
  dwarf_reg_state_t *rs_stack = NULL, *new_rs, *old_rs;
  unw_addr_space_t as;
  unw_accessors_t *a;
  uint8_t u8, op;
  uint16_t u16;
  uint32_t u32;
  void *arg;
  int ret;

#ifndef UNW_LOCAL_ONLY
  as = dci->as;
  arg = dci->as_arg;
#else
  as = c->as;
  arg = c->as_arg;
#endif
  a = unw_get_accessors (as);
  curr_ip = c->pi.start_ip;

  /* Process all the instructions including the ones after `DW_CFA_advance_loc'
     up to the next advance higher than the current `ip' (therefore `<=').
     See also the `c->decrease_ip' handling in `fetch_proc_info'.  */
  while (curr_ip <= ip && *addr < end_addr)
    {
      if ((ret = dwarf_readu8 (as, a, addr, &op, arg)) < 0)
	return ret;

      if (op & DWARF_CFA_OPCODE_MASK)
	{
	  operand = op & DWARF_CFA_OPERAND_MASK;
	  op &= ~DWARF_CFA_OPERAND_MASK;
	}
      switch ((dwarf_cfa_t) op)
	{
	case DW_CFA_advance_loc:
	  curr_ip += operand * dci->code_align;
	  Debug (15, "CFA_advance_loc to 0x%lx\n", (long) curr_ip);
	  break;

	case DW_CFA_advance_loc1:
	  if ((ret = dwarf_readu8 (as, a, addr, &u8, arg)) < 0)
	    goto fail;
	  curr_ip += u8 * dci->code_align;
	  Debug (15, "CFA_advance_loc1 to 0x%lx\n", (long) curr_ip);
	  break;

	case DW_CFA_advance_loc2:
	  if ((ret = dwarf_readu16 (as, a, addr, &u16, arg)) < 0)
	    goto fail;
	  curr_ip += u16 * dci->code_align;
	  Debug (15, "CFA_advance_loc2 to 0x%lx\n", (long) curr_ip);
	  break;

	case DW_CFA_advance_loc4:
	  if ((ret = dwarf_readu32 (as, a, addr, &u32, arg)) < 0)
	    goto fail;
	  curr_ip += u32 * dci->code_align;
	  Debug (15, "CFA_advance_loc4 to 0x%lx\n", (long) curr_ip);
	  break;

	case DW_CFA_MIPS_advance_loc8:
#ifdef UNW_TARGET_MIPS
	  {
	    uint64_t u64;

	    if ((ret = dwarf_readu64 (as, a, addr, &u64, arg)) < 0)
	      goto fail;
	    curr_ip += u64 * dci->code_align;
	    Debug (15, "CFA_MIPS_advance_loc8\n");
	    break;
	  }
#else
	  Debug (1, "DW_CFA_MIPS_advance_loc8 on non-MIPS target\n");
	  ret = -UNW_EINVAL;
	  goto fail;
#endif

	case DW_CFA_offset:
	  regnum = operand;
	  if (regnum >= DWARF_NUM_PRESERVED_REGS)
	    {
	      Debug (1, "Invalid register number %u in DW_cfa_OFFSET\n",
		     (unsigned int) regnum);
	      ret = -UNW_EBADREG;
	      goto fail;
	    }
	  if ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0)
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_CFAREL, val * dci->data_align);
	  Debug (15, "CFA_offset r%lu at cfa+0x%lx\n",
		 (long) regnum, (long) (val * dci->data_align));
	  break;

	case DW_CFA_offset_extended:
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_CFAREL, val * dci->data_align);
	  Debug (15, "CFA_offset_extended r%lu at cf+0x%lx\n",
		 (long) regnum, (long) (val * dci->data_align));
	  break;

	case DW_CFA_offset_extended_sf:
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_sleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_CFAREL, val * dci->data_align);
	  Debug (15, "CFA_offset_extended_sf r%lu at cf+0x%lx\n",
		 (long) regnum, (long) (val * dci->data_align));
	  break;

	case DW_CFA_restore:
	  regnum = operand;
	  if (regnum >= DWARF_NUM_PRESERVED_REGS)
	    {
	      Debug (1, "Invalid register number %u in DW_CFA_restore\n",
		     (unsigned int) regnum);
	      ret = -UNW_EINVAL;
	      goto fail;
	    }
	  sr->rs_current.reg[regnum] = sr->rs_initial.reg[regnum];
	  Debug (15, "CFA_restore r%lu\n", (long) regnum);
	  break;

	case DW_CFA_restore_extended:
	  if ((ret = dwarf_read_uleb128 (as, a, addr, &regnum, arg)) < 0)
	    goto fail;
	  if (regnum >= DWARF_NUM_PRESERVED_REGS)
	    {
	      Debug (1, "Invalid register number %u in "
		     "DW_CFA_restore_extended\n", (unsigned int) regnum);
	      ret = -UNW_EINVAL;
	      goto fail;
	    }
	  sr->rs_current.reg[regnum] = sr->rs_initial.reg[regnum];
	  Debug (15, "CFA_restore_extended r%lu\n", (long) regnum);
	  break;

	case DW_CFA_nop:
	  break;

	case DW_CFA_set_loc:
	  fde_encoding = dci->fde_encoding;
	  if ((ret = dwarf_read_encoded_pointer (as, a, addr, fde_encoding,
						 &c->pi, &curr_ip,
						 arg)) < 0)
	    goto fail;
	  Debug (15, "CFA_set_loc to 0x%lx\n", (long) curr_ip);
	  break;

	case DW_CFA_undefined:
	  if ((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_UNDEF, 0);
	  Debug (15, "CFA_undefined r%lu\n", (long) regnum);
	  break;

	case DW_CFA_same_value:
	  if ((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_SAME, 0);
	  Debug (15, "CFA_same_value r%lu\n", (long) regnum);
	  break;

	case DW_CFA_register:
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_REG, val);
	  Debug (15, "CFA_register r%lu to r%lu\n", (long) regnum, (long) val);
	  break;

	case DW_CFA_remember_state:
	  new_rs = alloc_reg_state ();
	  if (!new_rs)
	    {
	      Debug (1, "Out of memory in DW_CFA_remember_state\n");
	      ret = -UNW_ENOMEM;
	      goto fail;
	    }

	  memcpy (new_rs->reg, sr->rs_current.reg, sizeof (new_rs->reg));
	  new_rs->next = rs_stack;
	  rs_stack = new_rs;
	  Debug (15, "CFA_remember_state\n");
	  break;

	case DW_CFA_restore_state:
	  if (!rs_stack)
	    {
	      Debug (1, "register-state stack underflow\n");
	      ret = -UNW_EINVAL;
	      goto fail;
	    }
	  memcpy (&sr->rs_current.reg, &rs_stack->reg, sizeof (rs_stack->reg));
	  old_rs = rs_stack;
	  rs_stack = rs_stack->next;
	  free_reg_state (old_rs);
	  Debug (15, "CFA_restore_state\n");
	  break;

	case DW_CFA_def_cfa:
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, DWARF_CFA_REG_COLUMN, DWARF_WHERE_REG, regnum);
	  set_reg (sr, DWARF_CFA_OFF_COLUMN, 0, val);	/* NOT factored! */
	  Debug (15, "CFA_def_cfa r%lu+0x%lx\n", (long) regnum, (long) val);
	  break;

	case DW_CFA_def_cfa_sf:
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_sleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, DWARF_CFA_REG_COLUMN, DWARF_WHERE_REG, regnum);
	  set_reg (sr, DWARF_CFA_OFF_COLUMN, 0,
		   val * dci->data_align);		/* factored! */
	  Debug (15, "CFA_def_cfa_sf r%lu+0x%lx\n",
		 (long) regnum, (long) (val * dci->data_align));
	  break;

	case DW_CFA_def_cfa_register:
	  if ((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	    goto fail;
	  set_reg (sr, DWARF_CFA_REG_COLUMN, DWARF_WHERE_REG, regnum);
	  Debug (15, "CFA_def_cfa_register r%lu\n", (long) regnum);
	  break;

	case DW_CFA_def_cfa_offset:
	  if ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0)
	    goto fail;
	  set_reg (sr, DWARF_CFA_OFF_COLUMN, 0, val);	/* NOT factored! */
	  Debug (15, "CFA_def_cfa_offset 0x%lx\n", (long) val);
	  break;

	case DW_CFA_def_cfa_offset_sf:
	  if ((ret = dwarf_read_sleb128 (as, a, addr, &val, arg)) < 0)
	    goto fail;
	  set_reg (sr, DWARF_CFA_OFF_COLUMN, 0,
		   val * dci->data_align);	/* factored! */
	  Debug (15, "CFA_def_cfa_offset_sf 0x%lx\n",
		 (long) (val * dci->data_align));
	  break;

	case DW_CFA_def_cfa_expression:
	  /* Save the address of the DW_FORM_block for later evaluation. */
	  set_reg (sr, DWARF_CFA_REG_COLUMN, DWARF_WHERE_EXPR, *addr);

	  if ((ret = dwarf_read_uleb128 (as, a, addr, &len, arg)) < 0)
	    goto fail;

	  Debug (15, "CFA_def_cfa_expr @ 0x%lx [%lu bytes]\n",
		 (long) *addr, (long) len);
	  *addr += len;
	  break;

	case DW_CFA_expression:
	  if ((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	    goto fail;

	  /* Save the address of the DW_FORM_block for later evaluation. */
	  set_reg (sr, regnum, DWARF_WHERE_EXPR, *addr);

	  if ((ret = dwarf_read_uleb128 (as, a, addr, &len, arg)) < 0)
	    goto fail;

	  Debug (15, "CFA_expression r%lu @ 0x%lx [%lu bytes]\n",
		 (long) regnum, (long) addr, (long) len);
	  *addr += len;
	  break;

	case DW_CFA_GNU_args_size:
	  if ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0)
	    goto fail;
	  sr->args_size = val;
	  Debug (15, "CFA_GNU_args_size %lu\n", (long) val);
	  break;

	case DW_CFA_GNU_negative_offset_extended:
	  /* A comment in GCC says that this is obsoleted by
	     DW_CFA_offset_extended_sf, but that it's used by older
	     PowerPC code.  */
	  if (((ret = read_regnum (as, a, addr, &regnum, arg)) < 0)
	      || ((ret = dwarf_read_uleb128 (as, a, addr, &val, arg)) < 0))
	    goto fail;
	  set_reg (sr, regnum, DWARF_WHERE_CFAREL, -(val * dci->data_align));
	  Debug (15, "CFA_GNU_negative_offset_extended cfa+0x%lx\n",
		 (long) -(val * dci->data_align));
	  break;

	case DW_CFA_GNU_window_save:
#ifdef UNW_TARGET_SPARC
	  /* This is a special CFA to handle all 16 windowed registers
	     on SPARC.  */
	  for (regnum = 16; regnum < 32; ++regnum)
	    set_reg (sr, regnum, DWARF_WHERE_CFAREL,
		     (regnum - 16) * sizeof (unw_word_t));
	  Debug (15, "CFA_GNU_window_save\n");
	  break;
#else
	  /* FALL THROUGH */
#endif
	case DW_CFA_lo_user:
	case DW_CFA_hi_user:
	  Debug (1, "Unexpected CFA opcode 0x%x\n", op);
	  ret = -UNW_EINVAL;
	  goto fail;
	}
    }
  ret = 0;

 fail:
  /* Free the register-state stack, if not empty already.  */
  while (rs_stack)
    {
      old_rs = rs_stack;
      rs_stack = rs_stack->next;
      free_reg_state (old_rs);
    }
  return ret;
}

static int
fetch_proc_info (struct dwarf_cursor *c, unw_word_t ip, int need_unwind_info)
{
  int ret, dynamic = 1;

  /* In the current (lowest) frame we must not touch `ip' as the current
     address is where we stand.  On the other hand any upper frames will stand
     on the next instruction behind our call which may have a different stack
     DWARF information (for `stdcall' called functions) or the next instruction
     even may belong already to a different continuing function.
     Also signal frames got invoked from the instruction we want to analyze.  */
  if (c->decrease_ip)
    --ip;

  if (c->pi_valid && !need_unwind_info)
    return 0;

  memset (&c->pi, 0, sizeof (c->pi));

  /* check dynamic info first --- it overrides everything else */
  ret = unwi_find_dynamic_proc_info (c->as, ip, &c->pi, need_unwind_info,
				     c->as_arg);
  if (ret == -UNW_ENOINFO)
    {
      dynamic = 0;
      if ((ret = tdep_find_proc_info (c, ip, need_unwind_info)) < 0)
	return ret;
    }

  c->pi_valid = 1;
  c->pi_is_dynamic = dynamic;

  if (ret >= 0)
    tdep_fetch_proc_info_post (c, ip, need_unwind_info);

  return ret;
}

static int
parse_dynamic (struct dwarf_cursor *c, unw_word_t ip, dwarf_state_record_t *sr)
{
  Debug (1, "Not yet implemented\n");
#if 0
  /* Don't forget to set the ret_addr_column!  */
  c->ret_addr_column = XXX;
#endif
  return -UNW_ENOINFO;
}

static inline void
put_unwind_info (struct dwarf_cursor *c, unw_proc_info_t *pi)
{
  if (!c->pi_valid)
    return;

  if (c->pi_is_dynamic) {
    unwi_put_dynamic_unwind_info (c->as, pi, c->as_arg);
    return;
  }

  if (pi->unwind_info); {
    // Find the eh-frame's address space and arguments.
    unw_addr_space_t frame_as;
    void *frame_arg;
#ifndef UNW_LOCAL_ONLY
    struct dwarf_cie_info *dci = pi->unwind_info;
    frame_as = dci->as;
    frame_arg = dci->as_arg;
#else
    frame_as = c->as;
    frame_arg = c->as_arg;
#endif
    if (frame_as) {
      // notify the eh-frame accessors that the info is being
      // released so it can also release its local data (for instance
      // the frame_arg pointer).
      unw_accessors_t *a = unw_get_accessors (frame_as);
      if (a->put_unwind_info != NULL) {
	a->put_unwind_info(frame_as, pi, frame_arg);
      }
    }
    // Free this data.
    mempool_free (&dwarf_cie_info_pool, pi->unwind_info);
    pi->unwind_info = NULL;
  }
}

static inline int
parse_fde (struct dwarf_cursor *c, unw_word_t ip, dwarf_state_record_t *sr)
{
  struct dwarf_cie_info *dci;
  unw_word_t addr;
  int ret;

  dci = c->pi.unwind_info;
  c->ret_addr_column = dci->ret_addr_column;

  addr = dci->cie_instr_start;
  if ((ret = run_cfi_program (c, sr, ~(unw_word_t) 0, &addr,
			      dci->cie_instr_end, dci)) < 0)
    return ret;

  memcpy (&sr->rs_initial, &sr->rs_current, sizeof (sr->rs_initial));

  addr = dci->fde_instr_start;
  if ((ret = run_cfi_program (c, sr, ip, &addr, dci->fde_instr_end, dci)) < 0)
    return ret;

  return 0;
}

static inline void
flush_rs_cache (struct dwarf_rs_cache *cache)
{
  int i;

  cache->lru_head = DWARF_UNW_CACHE_SIZE - 1;
  cache->lru_tail = 0;

  for (i = 0; i < DWARF_UNW_CACHE_SIZE; ++i)
    {
      if (i > 0)
	cache->buckets[i].lru_chain = (i - 1);
      cache->buckets[i].coll_chain = -1;
      cache->buckets[i].ip = 0;
    }
  for (i = 0; i<DWARF_UNW_HASH_SIZE; ++i)
    cache->hash[i] = -1;
}

static inline struct dwarf_rs_cache *
get_rs_cache (unw_addr_space_t as, intrmask_t *saved_maskp)
{
  struct dwarf_rs_cache *cache = &as->global_cache;
  unw_caching_policy_t caching = as->caching_policy;

  if (caching == UNW_CACHE_NONE)
    return NULL;

#ifdef HAVE_ATOMIC_H
  if (!spin_trylock_irqsave (&cache->busy, *saved_maskp))
    return NULL;
#else
# ifdef HAVE_ATOMIC_OPS_H
  if (AO_test_and_set (&cache->busy) == AO_TS_SET)
    return NULL;
# else
  sigprocmask (SIG_SETMASK, &unwi_full_mask, saved_maskp);
  if (likely (caching == UNW_CACHE_GLOBAL))
    {
      Debug (16, "%s: acquiring lock\n", __FUNCTION__);
      mutex_lock (&cache->lock);
    }
# endif
#endif

  if (atomic_read (&as->cache_generation) != atomic_read (&cache->generation))
    {
      flush_rs_cache (cache);
      cache->generation = as->cache_generation;
    }

  return cache;
}

static inline void
put_rs_cache (unw_addr_space_t as, struct dwarf_rs_cache *cache,
		  intrmask_t *saved_maskp)
{
  assert (as->caching_policy != UNW_CACHE_NONE);

  Debug (16, "unmasking signals/interrupts and releasing lock\n");
#ifdef HAVE_ATOMIC_H
  spin_unlock_irqrestore (&cache->busy, *saved_maskp);
#else
# ifdef HAVE_ATOMIC_OPS_H
  AO_CLEAR (&cache->busy);
# else
  if (likely (as->caching_policy == UNW_CACHE_GLOBAL))
    mutex_unlock (&cache->lock);
  sigprocmask (SIG_SETMASK, saved_maskp, NULL);
# endif
#endif
}

static inline unw_hash_index_t
hash (unw_word_t ip)
{
  /* based on (sqrt(5)/2-1)*2^64 */
# define magic	((unw_word_t) 0x9e3779b97f4a7c16ULL)

  return (ip) * magic >> ((sizeof(unw_word_t) * 8) - DWARF_LOG_UNW_HASH_SIZE);
}

static inline long
cache_match (dwarf_reg_state_t *rs, unw_word_t ip)
{
  if (ip == rs->ip)
    return 1;
  return 0;
}

static dwarf_reg_state_t *
rs_lookup (struct dwarf_rs_cache *cache, struct dwarf_cursor *c)
{
  dwarf_reg_state_t *rs = cache->buckets + c->hint;
  unsigned short index;
  unw_word_t ip;

  ip = c->ip;

  if (cache_match (rs, ip))
    return rs;

  index = cache->hash[hash (ip)];
  if (index >= DWARF_UNW_CACHE_SIZE)
    return 0;

  rs = cache->buckets + index;
  while (1)
    {
      if (cache_match (rs, ip))
        {
          /* update hint; no locking needed: single-word writes are atomic */
          c->hint = cache->buckets[c->prev_rs].hint =
            (rs - cache->buckets);
          return rs;
        }
      if (rs->coll_chain >= DWARF_UNW_HASH_SIZE)
        return 0;
      rs = cache->buckets + rs->coll_chain;
    }
}

static inline dwarf_reg_state_t *
rs_new (struct dwarf_rs_cache *cache, struct dwarf_cursor * c)
{
  dwarf_reg_state_t *rs, *prev, *tmp;
  unw_hash_index_t index;
  unsigned short head;

  head = cache->lru_head;
  rs = cache->buckets + head;
  cache->lru_head = rs->lru_chain;

  /* re-insert rs at the tail of the LRU chain: */
  cache->buckets[cache->lru_tail].lru_chain = head;
  cache->lru_tail = head;

  /* remove the old rs from the hash table (if it's there): */
  if (rs->ip)
    {
      index = hash (rs->ip);
      tmp = cache->buckets + cache->hash[index];
      prev = 0;
      while (1)
	{
	  if (tmp == rs)
	    {
	      if (prev)
		prev->coll_chain = tmp->coll_chain;
	      else
		cache->hash[index] = tmp->coll_chain;
	      break;
	    }
	  else
	    prev = tmp;
	  if (tmp->coll_chain >= DWARF_UNW_CACHE_SIZE)
	    /* old rs wasn't in the hash-table */
	    break;
	  tmp = cache->buckets + tmp->coll_chain;
	}
    }

  /* enter new rs in the hash table */
  index = hash (c->ip);
  rs->coll_chain = cache->hash[index];
  cache->hash[index] = rs - cache->buckets;

  rs->hint = 0;
  rs->ip = c->ip;
  rs->ret_addr_column = c->ret_addr_column;

  return rs;
}

static int
create_state_record_for (struct dwarf_cursor *c, dwarf_state_record_t *sr,
			 unw_word_t ip)
{
  int i, ret;

  assert (c->pi_valid);

  memset (sr, 0, sizeof (*sr));
  for (i = 0; i < DWARF_NUM_PRESERVED_REGS + 2; ++i)
    set_reg (sr, i, DWARF_WHERE_SAME, 0);

  switch (c->pi.format)
    {
    case UNW_INFO_FORMAT_TABLE:
    case UNW_INFO_FORMAT_REMOTE_TABLE:
      ret = parse_fde (c, ip, sr);
      break;

    case UNW_INFO_FORMAT_DYNAMIC:
      ret = parse_dynamic (c, ip, sr);
      break;

    default:
      Debug (1, "Unexpected unwind-info format %d\n", c->pi.format);
      ret = -UNW_EINVAL;
    }
  return ret;
}

static inline int
eval_location_expr (struct dwarf_cursor *c, unw_addr_space_t as,
		    unw_accessors_t *a, unw_word_t addr,
		    dwarf_loc_t *locp, void *arg)
{
  int ret, is_register;
  unw_word_t len, val;

  /* read the length of the expression: */
  if ((ret = dwarf_read_uleb128 (as, a, &addr, &len, arg)) < 0)
    return ret;

  /* evaluate the expression: */
  if ((ret = dwarf_eval_expr (c, &addr, len, &val, &is_register)) < 0)
    return ret;

  if (is_register)
    *locp = DWARF_REG_LOC (c, dwarf_to_unw_regnum (val));
  else
    *locp = DWARF_MEM_LOC (c, val);

  return 0;
}

static int
apply_reg_state (struct dwarf_cursor *c, struct dwarf_reg_state *rs)
{
  unw_word_t regnum, addr, cfa, ip;
  unw_word_t prev_ip, prev_cfa;
  unw_addr_space_t as;
  dwarf_loc_t cfa_loc;
  unw_accessors_t *a;
  int i, ret;
  void *arg;

  prev_ip = c->ip;
  prev_cfa = c->cfa;

  as = c->as;
  arg = c->as_arg;
  a = unw_get_accessors (as);

  /* Evaluate the CFA first, because it may be referred to by other
     expressions.  */

  if (rs->reg[DWARF_CFA_REG_COLUMN].where == DWARF_WHERE_REG)
    {
      /* CFA is equal to [reg] + offset: */

      /* As a special-case, if the stack-pointer is the CFA and the
	 stack-pointer wasn't saved, popping the CFA implicitly pops
	 the stack-pointer as well.  */
      if ((rs->reg[DWARF_CFA_REG_COLUMN].val == UNW_TDEP_SP)
	  && (rs->reg[UNW_TDEP_SP].where == DWARF_WHERE_SAME))
	  cfa = c->cfa;
      else
	{
	  regnum = dwarf_to_unw_regnum (rs->reg[DWARF_CFA_REG_COLUMN].val);
	  if ((ret = unw_get_reg ((unw_cursor_t *) c, regnum, &cfa)) < 0)
	    return ret;
	}
      cfa += rs->reg[DWARF_CFA_OFF_COLUMN].val;
    }
  else
    {
      /* CFA is equal to EXPR: */

      assert (rs->reg[DWARF_CFA_REG_COLUMN].where == DWARF_WHERE_EXPR);

      addr = rs->reg[DWARF_CFA_REG_COLUMN].val;
      if ((ret = eval_location_expr (c, as, a, addr, &cfa_loc, arg)) < 0)
	return ret;
      /* the returned location better be a memory location... */
      if (DWARF_IS_REG_LOC (cfa_loc))
	return -UNW_EBADFRAME;
      cfa = DWARF_GET_LOC (cfa_loc);
    }

  for (i = 0; i < DWARF_NUM_PRESERVED_REGS; ++i)
    {
      switch ((dwarf_where_t) rs->reg[i].where)
	{
	case DWARF_WHERE_UNDEF:
	  c->loc[i] = DWARF_NULL_LOC;
	  break;

	case DWARF_WHERE_SAME:
	  break;

	case DWARF_WHERE_CFAREL:
	  c->loc[i] = DWARF_MEM_LOC (c, cfa + rs->reg[i].val);
	  break;

	case DWARF_WHERE_REG:
	  c->loc[i] = DWARF_REG_LOC (c, dwarf_to_unw_regnum (rs->reg[i].val));
	  break;

	case DWARF_WHERE_EXPR:
	  addr = rs->reg[i].val;
	  if ((ret = eval_location_expr (c, as, a, addr, c->loc + i, arg)) , 0)
	    return ret;
	  break;
	}
    }
  c->cfa = cfa;
  /* After fixing glibc's `__restore_rt' unwinding by CFI in:
     	http://sources.redhat.com/cgi-bin/cvsweb.cgi/libc/sysdeps/unix/sysv/linux/x86_64/sigaction.c.diff?cvsroot=glibc&r1=text&tr1=1.10&r2=text&tr2=1.12&f=u
     we need to check for the frame stop (indicated by `ip == 0').  */
  if (DWARF_IS_NULL_LOC (c->loc[c->ret_addr_column]))
    c->ip = 0;
  else
    {
      ret = dwarf_get (c, c->loc[c->ret_addr_column], &ip);
      if (ret < 0)
	return ret;
      c->ip = ip;
    }
  /* XXX: check for ip to be code_aligned */

  if (c->ip == prev_ip && c->cfa == prev_cfa)
    {
      dprintf ("%s: ip and cfa unchanged; stopping here (ip=0x%lx)\n",
	       __FUNCTION__, (long) c->ip);
      return -UNW_EBADFRAME;
    }
  return 0;
}

static int
is_signal_frame (struct dwarf_cursor *c)
{
  struct dwarf_cie_info *dci;

  assert (c->pi_valid);

  dci = c->pi.unwind_info;
  return dci->signal_frame;
}

static int
uncached_dwarf_find_save_locs (struct dwarf_cursor *c)
{
  dwarf_state_record_t sr;
  int ret;

  if ((ret = fetch_proc_info (c, c->ip, 1)) < 0)
    return ret;

  c->decrease_ip = !is_signal_frame (c);

  if ((ret = create_state_record_for (c, &sr, c->ip)) < 0)
    return ret;

  if ((ret = apply_reg_state (c, &sr.rs_current)) < 0)
    return ret;

  put_unwind_info (c, &c->pi);
  return 0;
}

/* The function finds the saved locations and applies the register
   state as well. */
HIDDEN int
dwarf_find_save_locs (struct dwarf_cursor *c)
{
  dwarf_state_record_t sr;
  dwarf_reg_state_t *rs, *rs1;
  struct dwarf_rs_cache *cache;
  int ret = 0;
  intrmask_t saved_mask;

  if (c->as->caching_policy == UNW_CACHE_NONE)
    return uncached_dwarf_find_save_locs (c);

  cache = get_rs_cache(c->as, &saved_mask);
  if (!cache)
    return -UNW_ENOINFO;	/* cache is busy */
  rs = rs_lookup(cache, c);

  if (rs)
    {
      c->ret_addr_column = rs->ret_addr_column;
      goto apply;
    }

  if ((ret = fetch_proc_info (c, c->ip, 1)) < 0)
    goto out;

  c->decrease_ip = !is_signal_frame (c);

  if ((ret = create_state_record_for (c, &sr, c->ip)) < 0)
    goto out;

  rs1 = &sr.rs_current;
  if (rs1)
    {
      rs = rs_new (cache, c);
      memcpy(rs, rs1, offsetof(struct dwarf_reg_state, ip));
      if (!rs)
        {
          dprintf ("%s: failed to create unwind rs\n", __FUNCTION__);
          ret = -UNW_EUNSPEC;
	  goto out;
        }
    }
  cache->buckets[c->prev_rs].hint = rs - cache->buckets;

  c->hint = rs->hint;
  c->prev_rs = rs - cache->buckets;

  put_unwind_info (c, &c->pi);
  ret = apply_reg_state (c, rs);

out:
  put_rs_cache (c->as, cache, &saved_mask);
  return ret;

apply:
  put_rs_cache (c->as, cache, &saved_mask);
  if ((ret = apply_reg_state (c, rs)) < 0)
    return ret;

  return 0;
}

/* The proc-info must be valid for IP before this routine can be
   called.  */
HIDDEN int
dwarf_create_state_record (struct dwarf_cursor *c, dwarf_state_record_t *sr)
{
  return create_state_record_for (c, sr, c->ip);
}

HIDDEN int
dwarf_make_proc_info (struct dwarf_cursor *c)
{
#if 0
  if (c->as->caching_policy == UNW_CACHE_NONE
      || get_cached_proc_info (c) < 0)
#endif
    /* Lookup it up the slow way... */
    return fetch_proc_info (c, c->ip, 0);
  return 0;
}
