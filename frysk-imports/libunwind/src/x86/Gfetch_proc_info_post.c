/* libunwind - a platform-independent unwind library
   Copyright (c) 2006 Hewlett-Packard Development Company, L.P.
	Contributed by Jan Kratochvil <jan.kratochvil@redhat.com>

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

#include "libunwind_i.h"

HIDDEN int
tdep_fetch_proc_info_post (struct dwarf_cursor *c, unw_word_t ip, int need_unwind_info)
{
  struct cursor *cursor = (struct cursor *) c;

  /* Should happen only if `!need_unwind_info'.  */
  if (!c->pi_valid)
    return 0;
  /* Should happen only if `!need_unwind_info'.  */
  if (!c->pi.unwind_info)
    return 0;

  /* Reset the value for this frame.  */
  cursor->sigcontext_format = X86_SCF_NONE;

  /* Normal non-signal frames case.  */
  if (!((struct dwarf_cie_info *) c->pi.unwind_info)->signal_frame)
    return 0;

  cursor->sigcontext_format = X86_SCF_LINUX_SIGFRAME;
  cursor->sigcontext_addr = c->cfa + 4;

  return 0;
}
