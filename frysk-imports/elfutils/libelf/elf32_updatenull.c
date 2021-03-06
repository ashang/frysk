/* Update data structures for changes.
   Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005, 2006 Red Hat, Inc.
   This file is part of Red Hat elfutils.
   Written by Ulrich Drepper <drepper@redhat.com>, 2000.

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

   In addition, as a special exception, Red Hat, Inc. gives You the
   additional right to link the code of Red Hat elfutils with code licensed
   under any Open Source Initiative certified open source license
   (http://www.opensource.org/licenses/index.php) which requires the
   distribution of source code with any binary distribution and to
   distribute linked combinations of the two.  Non-GPL Code permitted under
   this exception must only link to the code of Red Hat elfutils through
   those well defined interfaces identified in the file named EXCEPTION
   found in the source code files (the "Approved Interfaces").  The files
   of Non-GPL Code may instantiate templates or use macros or inline
   functions from the Approved Interfaces without causing the resulting
   work to be covered by the GNU General Public License.  Only Red Hat,
   Inc. may make changes or additions to the list of Approved Interfaces.
   Red Hat's grant of this exception is conditioned upon your not adding
   any new exceptions.  If you wish to add a new Approved Interface or
   exception, please contact Red Hat.  You must obey the GNU General Public
   License in all respects for all of the Red Hat elfutils code and other
   code used in conjunction with Red Hat elfutils except the Non-GPL Code
   covered by this exception.  If you modify this file, you may extend this
   exception to your version of the file, but you are not obligated to do
   so.  If you do not wish to provide this exception without modification,
   you must delete this exception statement from your version and license
   this file solely under the GPL without exception.

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

#include <assert.h>
#include <endian.h>
#include <libelf.h>
#include <stdbool.h>
#include <string.h>
#include <sys/param.h>

#include "libelfP.h"
#include "elf-knowledge.h"

#ifndef LIBELFBITS
# define LIBELFBITS 32
#endif



static int
ELFW(default_ehdr,LIBELFBITS) (Elf *elf, ElfW2(LIBELFBITS,Ehdr) *ehdr,
			       size_t shnum, int *change_bop)
{
  /* Always write the magic bytes.  */
  if (memcmp (&ehdr->e_ident[EI_MAG0], ELFMAG, SELFMAG) != 0)
    {
      memcpy (&ehdr->e_ident[EI_MAG0], ELFMAG, SELFMAG);
      elf->state.ELFW(elf,LIBELFBITS).ehdr_flags |= ELF_F_DIRTY;
    }

  /* Always set the file class.  */
  update_if_changed (ehdr->e_ident[EI_CLASS], ELFW(ELFCLASS,LIBELFBITS),
		     elf->state.ELFW(elf,LIBELFBITS).ehdr_flags);

  /* Set the data encoding if necessary.  */
  if (unlikely (ehdr->e_ident[EI_DATA] == ELFDATANONE))
    {
      ehdr->e_ident[EI_DATA] =
	BYTE_ORDER == BIG_ENDIAN ? ELFDATA2MSB : ELFDATA2LSB;
      elf->state.ELFW(elf,LIBELFBITS).ehdr_flags |= ELF_F_DIRTY;
    }
  else if (unlikely (ehdr->e_ident[EI_DATA] >= ELFDATANUM))
    {
      __libelf_seterrno (ELF_E_DATA_ENCODING);
      return 1;
    }
  else
    *change_bop = ((BYTE_ORDER == LITTLE_ENDIAN
		    && ehdr->e_ident[EI_DATA] != ELFDATA2LSB)
		   || (BYTE_ORDER == BIG_ENDIAN
		       && ehdr->e_ident[EI_DATA] != ELFDATA2MSB));

  /* Unconditionally overwrite the ELF version.  */
  update_if_changed (ehdr->e_ident[EI_VERSION], EV_CURRENT,
		     elf->state.ELFW(elf,LIBELFBITS).ehdr_flags);

  if (unlikely (ehdr->e_version == EV_NONE)
      || unlikely (ehdr->e_version >= EV_NUM))
    {
      __libelf_seterrno (ELF_E_UNKNOWN_VERSION);
      return 1;
    }

  if (unlikely (shnum >= SHN_LORESERVE))
    {
      update_if_changed (ehdr->e_shnum, 0,
			 elf->state.ELFW(elf,LIBELFBITS).ehdr_flags);
    }
  else
    update_if_changed (ehdr->e_shnum, shnum,
		       elf->state.ELFW(elf,LIBELFBITS).ehdr_flags);

  if (unlikely (ehdr->e_ehsize != elf_typesize (LIBELFBITS, ELF_T_EHDR, 1)))
    {
      ehdr->e_ehsize = elf_typesize (LIBELFBITS, ELF_T_EHDR, 1);
      elf->state.ELFW(elf,LIBELFBITS).ehdr_flags |= ELF_F_DIRTY;
    }

  return 0;
}


off_t
internal_function
__elfw2(LIBELFBITS,updatenull) (Elf *elf, int *change_bop, size_t shnum)
{
  ElfW2(LIBELFBITS,Ehdr) *ehdr = INTUSE(elfw2(LIBELFBITS,getehdr)) (elf);
  int changed = 0;
  int ehdr_flags = 0;

  /* Set the default values.  */
  if (ELFW(default_ehdr,LIBELFBITS) (elf, ehdr, shnum, change_bop) != 0)
    return -1;

  /* At least the ELF header is there.  */
  off_t size = elf_typesize (LIBELFBITS, ELF_T_EHDR, 1);

  /* Set the program header position.  */
  if (elf->state.ELFW(elf,LIBELFBITS).phdr == NULL
      && (ehdr->e_type == ET_EXEC || ehdr->e_type == ET_DYN
	  || ehdr->e_type == ET_CORE))
    (void) INTUSE(elfw2(LIBELFBITS,getphdr)) (elf);
  if (elf->state.ELFW(elf,LIBELFBITS).phdr != NULL)
    {
      /* Only executables, shared objects, and core files have a program
	 header.  */
      if (ehdr->e_type != ET_EXEC && ehdr->e_type != ET_DYN
	  && unlikely (ehdr->e_type != ET_CORE))
	{
	  __libelf_seterrno (ELF_E_INVALID_PHDR);
	  return -1;
	}

      if (elf->flags & ELF_F_LAYOUT)
	{
	  /* The user is supposed to fill out e_phoff.  Use it and
	     e_phnum to determine the maximum extend.  */
	  size = MAX ((size_t) size,
		      ehdr->e_phoff
		      + elf_typesize (LIBELFBITS, ELF_T_PHDR, ehdr->e_phnum));
	}
      else
	{
	  update_if_changed (ehdr->e_phoff,
			     elf_typesize (LIBELFBITS, ELF_T_EHDR, 1),
			     ehdr_flags);

	  /* We need no alignment here.  */
	  size += elf_typesize (LIBELFBITS, ELF_T_PHDR, ehdr->e_phnum);
	}
    }

  if (shnum > 0)
    {
      Elf_ScnList *list;
      bool first = true;

      assert (elf->state.ELFW(elf,LIBELFBITS).scns.cnt > 0);

      if (shnum >= SHN_LORESERVE)
	{
	  /* We have to  fill in the number of sections in the header
	     of the zeroth section.  */
	  Elf_Scn *scn0 = &elf->state.ELFW(elf,LIBELFBITS).scns.data[0];

	  update_if_changed (scn0->shdr.ELFW(e,LIBELFBITS)->sh_size,
			     shnum, scn0->shdr_flags);
	}

      /* Go over all sections and find out how large they are.  */
      list = &elf->state.ELFW(elf,LIBELFBITS).scns;

      /* Load the section headers if necessary.  This loads the
	 headers for all sections.  */
      if (list->data[1].shdr.ELFW(e,LIBELFBITS) == NULL)
	(void) INTUSE(elfw2(LIBELFBITS,getshdr)) (&list->data[1]);

      do
	{
	  for (size_t cnt = first == true; cnt < list->cnt; ++cnt)
	    {
	      Elf_Scn *scn = &list->data[cnt];
	      ElfW2(LIBELFBITS,Shdr) *shdr = scn->shdr.ELFW(e,LIBELFBITS);
	      off_t offset = 0;

	      assert (shdr != NULL);
	      ElfW2(LIBELFBITS,Word) sh_entsize = shdr->sh_entsize;
	      ElfW2(LIBELFBITS,Word) sh_align = shdr->sh_addralign ?: 1;

	      /* Set the sh_entsize value if we can reliably detect it.  */
	      switch (shdr->sh_type)
		{
		case SHT_SYMTAB:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_SYM, 1);
		  break;
		case SHT_RELA:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_RELA, 1);
		  break;
		case SHT_GROUP:
		  /* Only relocatable files can contain section groups.  */
		  if (ehdr->e_type != ET_REL)
		    {
		      __libelf_seterrno (ELF_E_GROUP_NOT_REL);
		      return -1;
		    }
		  /* FALLTHROUGH */
		case SHT_SYMTAB_SHNDX:
		  sh_entsize = elf_typesize (32, ELF_T_WORD, 1);
		  break;
		case SHT_HASH:
		  sh_entsize = SH_ENTSIZE_HASH (ehdr);
		  break;
		case SHT_DYNAMIC:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_DYN, 1);
		  break;
		case SHT_REL:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_REL, 1);
		  break;
		case SHT_DYNSYM:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_SYM, 1);
		  break;
		case SHT_SUNW_move:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_MOVE, 1);
		  break;
		case SHT_SUNW_syminfo:
		  sh_entsize = elf_typesize (LIBELFBITS, ELF_T_SYMINFO, 1);
		  break;
		default:
		  break;
		}

	      /* If the section header contained the wrong entry size
		 correct it and mark the header as modified.  */
	      update_if_changed (shdr->sh_entsize, sh_entsize,
				 scn->shdr_flags);

	      if (scn->data_read == 0 && __libelf_set_rawdata (scn) != 0)
		/* Something went wrong.  The error value is already set.  */
		return -1;

	      /* Iterate over all data blocks.  */
	      if (list->data[cnt].data_list_rear != NULL)
		{
		  Elf_Data_List *dl = &scn->data_list;

		  while (dl != NULL)
		    {
		      Elf_Data *data = &dl->data.d;
		      if (dl == &scn->data_list && data->d_buf == NULL
			  && scn->rawdata.d.d_buf != NULL)
			data = &scn->rawdata.d;

		      if (unlikely (data->d_version == EV_NONE)
			  || unlikely (data->d_version >= EV_NUM))
			{
			  __libelf_seterrno (ELF_E_UNKNOWN_VERSION);
			  return -1;
			}

		      if (unlikely (! powerof2 (data->d_align)))
			{
			  __libelf_seterrno (ELF_E_INVALID_ALIGN);
			  return -1;
			}

		      sh_align = MAX (sh_align, data->d_align);

		      if (elf->flags & ELF_F_LAYOUT)
			{
			  /* The user specified the offset and the size.
			     All we have to do is check whether this block
			     fits in the size specified for the section.  */
			  if (unlikely ((GElf_Word) (data->d_off
						     + data->d_size)
					> shdr->sh_size))
			    {
			      __libelf_seterrno (ELF_E_SECTION_TOO_SMALL);
			      return -1;
			    }
			}
		      else
			{
			  /* Determine the padding.  */
			  offset = ((offset + data->d_align - 1)
				    & ~(data->d_align - 1));

			  update_if_changed (data->d_off, offset, changed);

			  offset += data->d_size;
			}

		      /* Next data block.  */
		      dl = dl->next;
		    }
		}
	      else
		/* Get the size of the section from the raw data.  If
		   none is available the value is zero.  */
		offset += scn->rawdata.d.d_size;

	      if (elf->flags & ELF_F_LAYOUT)
		{
		  size = MAX ((GElf_Word) size,
			      shdr->sh_offset
			      + (shdr->sh_type != SHT_NOBITS
				 ? shdr->sh_size : 0));

		  /* The alignment must be a power of two.  This is a
		     requirement from the ELF specification.  Additionally
		     we test for the alignment of the section being large
		     enough for the largest alignment required by a data
		     block.  */
		  if (unlikely (! powerof2 (shdr->sh_addralign))
		      || unlikely (shdr->sh_addralign < sh_align))
		    {
		      __libelf_seterrno (ELF_E_INVALID_ALIGN);
		      return -1;
		    }
		}
	      else
		{
		  /* How much alignment do we need for this section.  */
		  update_if_changed (shdr->sh_addralign, sh_align,
				     scn->shdr_flags);

		  size = (size + sh_align - 1) & ~(sh_align - 1);
		  int offset_changed = 0;
		  update_if_changed (shdr->sh_offset, (GElf_Word) size,
				     offset_changed);
		  changed |= offset_changed;

		  if (offset_changed && scn->data_list_rear == NULL)
		    {
		      /* The position of the section in the file
			 changed.  Create the section data list.  */
		      if (INTUSE(elf_getdata) (scn, NULL) == NULL)
			return -1;
		    }

		  /* See whether the section size is correct.  */
		  update_if_changed (shdr->sh_size, (GElf_Word) offset,
				     changed);

		  if (shdr->sh_type != SHT_NOBITS)
		    size += offset;

		  scn->flags |= changed;
		}

	      /* Check that the section size is actually a multiple of
		 the entry size.  */
	      if (shdr->sh_entsize != 0
		  && unlikely (shdr->sh_size % shdr->sh_entsize != 0)
		  && (elf->flags & ELF_F_PERMISSIVE) == 0)
		{
		  __libelf_seterrno (ELF_E_INVALID_SHENTSIZE);
		  return -1;
		}
	    }

	  assert (list->next == NULL || list->cnt == list->max);

	  first = false;
	}
      while ((list = list->next) != NULL);

      /* Store section information.  */
      if (elf->flags & ELF_F_LAYOUT)
	{
	  /* The user is supposed to fill out e_phoff.  Use it and
	     e_phnum to determine the maximum extend.  */
	  size = MAX ((GElf_Word) size,
		      (ehdr->e_shoff
		       + (elf_typesize (LIBELFBITS, ELF_T_SHDR, shnum))));
	}
      else
	{
	  /* Align for section header table.

	     Yes, we use `sizeof' and not `__alignof__' since we do not
	     want to be surprised by architectures with less strict
	     alignment rules.  */
#define SHDR_ALIGN sizeof (ElfW2(LIBELFBITS,Off))
	  size = (size + SHDR_ALIGN - 1) & ~(SHDR_ALIGN - 1);

	  update_if_changed (ehdr->e_shoff, (GElf_Word) size, elf->flags);
	  update_if_changed (ehdr->e_shentsize,
			     elf_typesize (LIBELFBITS, ELF_T_SHDR, 1),
			     ehdr_flags);

	  /* Account for the section header size.  */
	  size += elf_typesize (LIBELFBITS, ELF_T_SHDR, shnum);
	}
    }

  elf->state.ELFW(elf,LIBELFBITS).ehdr_flags |= ehdr_flags;

  return size;
}
