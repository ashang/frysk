/* Alpha specific symbolic name handling.
   Copyright (C) 2002, 2005, 2007 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2002.

   Red Hat elfutils is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat elfutils is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat elfutils; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.

   Red Hat elfutils is an included package of the Open Invention Network.
   An included package of the Open Invention Network is a package for which
   Open Invention Network licensees cross-license their patents.  No patent
   license is granted, either expressly or impliedly, by designation as an
   included package.  Should you wish to participate in the Open Invention
   Network licensing program, please visit www.openinventionnetwork.com
   <http://www.openinventionnetwork.com>.  */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <elf.h>
#include <stddef.h>

#define BACKEND		alpha_
#include "libebl_CPU.h"


const char *
alpha_dynamic_tag_name (int64_t tag, char *buf __attribute__ ((unused)),
			size_t len __attribute__ ((unused)))
{
  switch (tag)
    {
    case DT_ALPHA_PLTRO:
      return "ALPHA_PLTRO";
    default:
      break;
    }
  return NULL;
}

bool
alpha_dynamic_tag_check (int64_t tag)
{
  return tag == DT_ALPHA_PLTRO;
}

/* Check for the simple reloc types.  */
Elf_Type
alpha_reloc_simple_type (Ebl *ebl __attribute__ ((unused)), int type)
{
  switch (type)
    {
    case R_ALPHA_REFLONG:
      return ELF_T_WORD;
    case R_ALPHA_REFQUAD:
      return ELF_T_XWORD;
    default:
      return ELF_T_NUM;
    }
}


/* Check whether SHF_MASKPROC flags are valid.  */
bool
alpha_machine_section_flag_check (GElf_Xword sh_flags)
{
  return (sh_flags &~ (SHF_ALPHA_GPREL)) == 0;
}
