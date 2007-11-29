// Copyright 2007, Red Hat Inc.

/* This file is part of libunwind.

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

#include "libunwind_i.h"
//#include "remote.h"
#include "dwarf-eh.h"

int
unw_get_unwind_table(unw_addr_space_t as, unw_word_t ip, unw_proc_info_t *pi,
			int need_unwind_info, void *image, size_t size, 
			unsigned long segbase, unsigned long mapoff, void *arg)
{
	Debug(99, "Entering get_unwind_table\n");
	Elf_W(Phdr) *phdr, *ptxt = NULL, *peh_hdr = NULL, *pdyn = NULL;
	unw_word_t eh_frame_start, fde_count, load_base;
	char *addr;
	struct dwarf_eh_frame_hdr *hdr;	
	Elf_W(Ehdr) *ehdr;
	int i, ret;
	unw_dyn_info_t di_cache;
	
	 if (size <= EI_CLASS)
    		return -1;

	Debug(99, "Checking elf size\n");
 	if (!(memcmp (image, ELFMAG, SELFMAG) == 0
	  && ((uint8_t *) image)[EI_CLASS] == ELF_CLASS))
	  	return -1;
    		
    	Debug(99, "Checked elf class\n");
    	ehdr = image;
	phdr = (Elf_W(Phdr) *) ((char *) image + ehdr->e_phoff);
	for (i = 0; i < ehdr->e_phnum; ++i)
    {
      switch (phdr[i].p_type)
	{
	case PT_LOAD:
	  if (phdr[i].p_offset == mapoff)
	    ptxt = phdr + i;
	  break;

	case PT_GNU_EH_FRAME:
	  peh_hdr = phdr + i;
	  break;

	case PT_DYNAMIC:
	  pdyn = phdr + i;
	  break;

	default:
	  break;
	}
    }
    
    	Debug(99, "Traversed headers\n");
	if (!ptxt || !peh_hdr)
    		return -UNW_ENOINFO;
    		    
    	if (pdyn)
    {
    	Debug(99, "Got dynamic header\n");
      /* For dynamicly linked executables and shared libraries,
	 DT_PLTGOT is the value that data-relative addresses are
	 relative to for that object.  We call this the "gp".  */
	    Elf_W(Dyn) *dyn = (Elf_W(Dyn) *)(pdyn->p_offset
					     + (char *) image);
      for (; dyn->d_tag != DT_NULL; ++dyn)
	if (dyn->d_tag == DT_PLTGOT)
	  {
	    /* Assume that _DYNAMIC is writable and GLIBC has
	       relocated it (true for x86 at least).  */
	    di_cache.gp = dyn->d_un.d_ptr;
	    break;
	  }
    }
     else
    /* Otherwise this is a static executable with no _DYNAMIC.  Assume
       that data-relative addresses are relative to 0, i.e.,
       absolute.  */
    	di_cache.gp = 0;
    	
    	Debug(99, "Got eh_frame_hdr\n");
    	 hdr = (struct dwarf_eh_frame_hdr *) (peh_hdr->p_offset
				       + (char *) image);
  if (hdr->version != DW_EH_VERSION)
    {
      Debug (1, "table has unexpected version %d\n",
	    hdr->version);
      return 0;
    }

	Debug(99, "EH_VERSION is correct\n");

  addr = hdr + 1;
  Debug (99, "Got addr\n");
  /* Fill in a dummy proc_info structure.  We just need to fill in
     enough to ensure that dwarf_read_encoded_pointer() can do it's
     job.  Since we don't have a procedure-context at this point, all
     we have to do is fill in the global-pointer.  */
  memset (pi, 0, sizeof (*pi));  
  Debug(99, "cleared pi\n");
  pi->gp = di_cache.gp;

  Debug(99, "set pi gp\n");

// The following is a local address space memory accessor used by
// dwarf_read_encoded_pointer.  The arg pointer is the base address,
// addr is the offset from the base address.
  int 
  local_access_mem (unw_addr_space_t as, unw_word_t addr,
		    unw_word_t *val, int write, void *arg) 
  {
    Debug(99, "entering local_access_mem, reading addr: 0x%lx into: %p\n", 
	  (long) addr, val);
    if (write)
      {
	// Writing is not supported
	return -UNW_EINVAL;
      }
    else
      {
	*val = *(unw_word_t *) (addr + (char *) arg);
	Debug (16, "mem[%x] -> %x\n", (addr + (char *) arg), *val);
      }
    Debug(99, "leaving local_access_mem\n");
    return 0;
  }
  
  unw_accessors_t local_accessors = {NULL, NULL, NULL, local_access_mem, 
				     NULL, NULL, NULL, NULL, NULL};
  unw_addr_space_t local_addr_space
    = unw_create_addr_space(&local_accessors, 0);

  unw_word_t start = 0;
  	
   /* (Optionally) read eh_frame_ptr: */
  if ((ret = dwarf_read_encoded_pointer (local_addr_space, &local_accessors,
					 &start, hdr->eh_frame_ptr_enc, pi,
					 &eh_frame_start, addr)) < 0)
    return -1;
    
  Debug(99, "read eh_frame_start: 0x%lx\n", (long) eh_frame_start);

  /* (Optionally) read fde_count: */
  if ((ret = dwarf_read_encoded_pointer (local_addr_space, &local_accessors,
					 &start, hdr->fde_count_enc, pi,
					 &fde_count, addr)) < 0)
    return -1;

  Debug(99, "read fde_count: 0x%lx\n", (long) fde_count);
  if (hdr->table_enc != (DW_EH_PE_datarel | DW_EH_PE_sdata4))
    {
      return -1;
    }
    
  addr += start;

  load_base = segbase - ptxt->p_vaddr;

  di_cache.start_ip = segbase;
  di_cache.end_ip = di_cache.start_ip + ptxt->p_memsz;
  di_cache.format = UNW_INFO_FORMAT_REMOTE_TABLE;
  di_cache.u.rti.name_ptr = 0;
  /* two 32-bit values (ip_offset/fde_offset) per table-entry: */
  di_cache.u.rti.table_len = (fde_count * 8) / sizeof (unw_word_t);
  di_cache.u.rti.table_data = ((load_base + peh_hdr->p_vaddr)
				   + (addr - (unw_word_t) image
				      - peh_hdr->p_offset));

  /* For the binary-search table in the eh_frame_hdr, data-relative
     means relative to the start of that section... */
  di_cache.u.rti.segbase = ((load_base + peh_hdr->p_vaddr)
				+ ((unw_word_t) hdr - (unw_word_t) image
				   - peh_hdr->p_offset));

	Debug(99, "Leaving get_unwind_table\n");
  return tdep_search_unwind_table (as, ip, &di_cache, pi, need_unwind_info, arg);
}
