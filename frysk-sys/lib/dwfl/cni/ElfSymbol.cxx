// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include <gcj/cni.h>
#include <gelf.h>

#include "lib/dwfl/Elf.h"
#include "lib/dwfl/ElfSymbol.h"
#include "lib/dwfl/ElfSymbol$Builder.h"
#include "lib/dwfl/ElfSymbolBinding.h"
#include "lib/dwfl/ElfSymbolType.h"
#include "lib/dwfl/ElfSymbolVisibility.h"
#include "lib/dwfl/ElfSymbol$PrivVerdef.h"
#include "lib/dwfl/ElfSymbol$PrivVerneed.h"
#include "lib/dwfl/ElfSymbol$PrivVerneed$Aux.h"

#ifdef __cplusplus
extern "C"
{
#endif

jboolean
lib::dwfl::ElfSymbol::elf_buildsymbol (lib::dwfl::Elf * parent,
				       jlong data_pointer,
				       jlong symbol_index,
				       jlong str_sect_index,
				       java::util::List * versions,
				       lib::dwfl::ElfSymbol$Builder * builder)
{
  ::GElf_Sym sym;
  if (::gelf_getsym ((::Elf_Data*)data_pointer, symbol_index, &sym) == NULL)
    return false;

  ::java::lang::String * name = parent->getStringAtOffset(str_sect_index, sym.st_name);
  jlong value = sym.st_value;
  jlong size = sym.st_size;
  ElfSymbolType * type = ElfSymbolType::intern(ELF64_ST_TYPE(sym.st_info));
  ElfSymbolBinding * bind = ElfSymbolBinding::intern(ELF64_ST_BIND(sym.st_info));
  ElfSymbolVisibility * visibility = ElfSymbolVisibility::intern(ELF64_ST_VISIBILITY(sym.st_other));
  jlong shndx = sym.st_shndx;

  builder->symbol(name, value, size, type, bind, visibility, shndx, versions);

  return true;
}

jint
lib::dwfl::ElfSymbol::elf_getversym (jlong data_pointer, jlong symbol_index)
{
  ::GElf_Versym ver;
  if (::gelf_getversym ((::Elf_Data*)data_pointer, symbol_index, &ver) == NULL)
    return -1;
  return (jint)ver;
}

jboolean
lib::dwfl::ElfSymbol::elf_load_verneed(lib::dwfl::Elf * parent,
				       jlong data_pointer,
				       jlong str_sect_index,
				       JArray<lib::dwfl::ElfSymbol$PrivVerneed*> * ret)
{
  ::Elf_Data * data = (::Elf_Data*)data_pointer;
  lib::dwfl::ElfSymbol$PrivVerneed ** ret_elems = elements(ret);

  typedef lib::dwfl::ElfSymbol$PrivVerneed Need;
  typedef lib::dwfl::ElfSymbol$PrivVerneed$Aux Aux;

  int count = ret->length;
  int offset = 0;
  for (int i = 0; i < count; ++i)
    {
      ::GElf_Verneed ver;
      if (::gelf_getverneed(data, offset, &ver) == NULL)
	return false;

      Need * verneed = new Need();
      ret_elems[i] = verneed;
      int auxcount = ver.vn_cnt;

      verneed->version = ver.vn_version;
      verneed->filename = parent->getStringAtOffset(str_sect_index, ver.vn_file);
      verneed->aux = (JArray<Aux*>*)JvNewObjectArray(auxcount, &Aux::class$, NULL);
      Aux ** aux_elems = elements(verneed->aux);

      int aux_offset = offset + ver.vn_aux;
      offset += ver.vn_next;
      for (int j = 0; j < auxcount; ++j)
	{
	  ::GElf_Vernaux aux;
	  if (::gelf_getvernaux(data, aux_offset, &aux) == NULL)
	    return false;

	  Aux * vernaux = new Aux();
	  vernaux->hash = (jint)aux.vna_hash;
	  vernaux->weak = (jboolean)((aux.vna_flags & VER_FLG_WEAK) == VER_FLG_WEAK);
	  vernaux->name = parent->getStringAtOffset(str_sect_index, aux.vna_name);
	  vernaux->index = (jint)aux.vna_other;
	  aux_elems[j] = vernaux;

	  aux_offset += aux.vna_next;
	}
    }
  return true;
}

jboolean
lib::dwfl::ElfSymbol::elf_load_verdef(lib::dwfl::Elf * parent,
				      jlong data_pointer,
				      jlong str_sect_index,
				      JArray<lib::dwfl::ElfSymbol$PrivVerdef*> * ret)
{
  ::Elf_Data * data = (::Elf_Data*)data_pointer;
  lib::dwfl::ElfSymbol$PrivVerdef ** ret_elems = elements(ret);

  typedef lib::dwfl::ElfSymbol$PrivVerdef Def;

  int count = ret->length;
  int offset = 0;
  for (int i = 0; i < count; ++i)
    {
      ::GElf_Verdef ver;
      if (::gelf_getverdef(data, offset, &ver) == NULL)
	return false;

      Def * verdef = new Def();
      ret_elems[i] = verdef;
      int auxcount = ver.vd_cnt;

      verdef->version = ver.vd_version;
      verdef->base = (jboolean)((ver.vd_flags & VER_FLG_BASE) == VER_FLG_BASE);
      verdef->index = ver.vd_ndx;
      verdef->hash = ver.vd_hash;
      verdef->names = (jstringArray)JvNewObjectArray(auxcount, &java::lang::String::class$, NULL);
      java::lang::String ** names_elems = elements(verdef->names);

      int aux_offset = offset + ver.vd_aux;
      offset += ver.vd_next;
      for (int j = 0; j < auxcount; ++j)
	{
	  ::GElf_Verdaux aux;
	  if (::gelf_getverdaux(data, aux_offset, &aux) == NULL)
	    return false;

	  names_elems[j] = parent->getStringAtOffset(str_sect_index, aux.vda_name);
	  aux_offset += aux.vda_next;
	}
    }
  return true;
}

#ifdef __cplusplus
}
#endif
