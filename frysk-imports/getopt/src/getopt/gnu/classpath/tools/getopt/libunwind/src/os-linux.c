/* libunwind - a platform-independent unwind library
   Copyright (C) 2003-2005 Hewlett-Packard Co
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

#ifndef UNW_REMOTE_ONLY

#include <limits.h>
#include <stdio.h>

#include "libunwind_i.h"
#include "os-linux.h"

#ifndef MAX_VDSO_SIZE
# define MAX_VDSO_SIZE ((size_t) sysconf (_SC_PAGESIZE))
#endif

PROTECTED int
tdep_get_elf_image (unw_addr_space_t as, struct elf_image *ei,
		    pid_t pid, unw_word_t ip,
		    unsigned long *segbase, unsigned long *mapoff,
		    void *arg)
{
  struct map_iterator mi;
  char path[PATH_MAX];
  int found = 0;
  unsigned long hi;
  unw_accessors_t *a;
  unw_word_t magic;

  maps_init (&mi, pid);
  while (maps_next (&mi, segbase, &hi, mapoff, path, sizeof (path)))
    if (ip >= *segbase && ip < hi)
      {
	found = 1;
	break;
      }
  maps_close (&mi);

  if (!found)
    return -1;

  found = elf_map_image (ei, path);
  if (found != -1)
    return found;

  /* If the above failed, try to bring in page-sized segments directly
     from process memory.  This enables us to locate VDSO unwind
     tables.  */
  ei->size = hi - *segbase;
  if (ei->size > MAX_VDSO_SIZE)
    return found;

  a = unw_get_accessors (as);
  if (! a->access_mem)
    return found;

  /* Try to decide whether it's an ELF image before bringing it all
     in.  */
  if (ei->size <= EI_CLASS || ei->size <= sizeof (magic))
    return found;

  if (sizeof (magic) >= SELFMAG)
    {
      int ret = (*a->access_mem) (as, *segbase, &magic, 0, arg);
      if (ret < 0)
	return ret;

      if (memcmp (&magic, ELFMAG, SELFMAG) != 0)
	return found;
    }

  ei->image = mmap (0, ei->size, PROT_READ | PROT_WRITE,
		    MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if (ei->image == MAP_FAILED)
    return found;

  if (sizeof (magic) >= SELFMAG)
    {
      *(unw_word_t *)ei->image = magic;
      hi = sizeof (magic);
    }
  else
    hi = 0;

  for (; hi < ei->size; hi += sizeof (unw_word_t))
    {
      found = (*a->access_mem) (as, *segbase + hi, ei->image + hi,
				0, arg);
      if (found < 0)
	{
	  munmap (ei->image, ei->size);
	  return found;
	}
    }

  if (*segbase == *mapoff
      && (*path == 0 || strcmp (path, "[vdso]") == 0))
    *mapoff = 0;

  return 0;
}

#endif /* UNW_REMOTE_ONLY */
