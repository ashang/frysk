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
#include "dwarf_i.h"
#include "dwarf-eh.h"

int
unw_get_unwind_table(unw_addr_space_t as, unw_word_t ip,
		     unw_proc_info_t *pi, int need_unwind_info, void *arg,
		     unw_accessors_t *eh_frame_accessors,
		     unw_word_t eh_frame_hdr_address,
		     void *eh_frame_arg,
		     unw_word_t peh_vaddr)
{
  int ret;
  unw_word_t start = eh_frame_hdr_address;

  // Version
  unsigned char version;
  if ((ret = dwarf_readu8 (as, eh_frame_accessors, &start,
			   &version, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;

  if (version != DW_EH_VERSION)
    return -UNW_ENOINFO;

  unsigned char eh_frame_ptr_enc;
  if ((ret = dwarf_readu8 (as, eh_frame_accessors, &start,
			   &eh_frame_ptr_enc, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;

  unsigned char fde_count_enc;
  if ((ret = dwarf_readu8 (as, eh_frame_accessors, &start,
			   &fde_count_enc, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;

  unsigned char table_enc;
  if ((ret = dwarf_readu8 (as, eh_frame_accessors, &start,
			   &table_enc, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;
  	
  if (table_enc != (DW_EH_PE_datarel | DW_EH_PE_sdata4))
    return -UNW_ENOINFO;

  unw_word_t eh_frame_start;
  if ((ret = dwarf_read_encoded_pointer (as, eh_frame_accessors,
					 &start, eh_frame_ptr_enc, pi,
					 &eh_frame_start, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;
    
  unw_word_t fde_count;
  if ((ret = dwarf_read_encoded_pointer (as, eh_frame_accessors,
					 &start, fde_count_enc, pi,
					 &fde_count, eh_frame_arg)) < 0)
    return -UNW_ENOINFO;

  unw_dyn_info_t di;
  di.start_ip = pi->start_ip;
  di.end_ip = pi->end_ip;
  di.format = UNW_INFO_FORMAT_REMOTE_TABLE;
  di.gp = pi->gp;
  di.u.rti.name_ptr = 0;
  /* two 32-bit values (ip_offset/fde_offset) per table-entry:
     For the binary-search table in the eh_frame_hdr, data-relative
     means relative to the start of that section...
     So for now we pass in peh_vaddr and use the main as for access. */
  di.u.rti.table_len = (fde_count * 8) / sizeof (unw_word_t);
  di.u.rti.table_data = peh_vaddr + 12;
  di.u.rti.segbase = peh_vaddr;

  return tdep_search_unwind_table (as, ip, &di, pi, need_unwind_info, arg);
}
