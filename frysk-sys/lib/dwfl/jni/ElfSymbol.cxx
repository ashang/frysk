// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

#include <gelf.h>

#include "jni.hxx"

using namespace java::lang;

bool
lib::dwfl::ElfSymbol::elf_buildsymbol(jnixx::env env,
				      lib::dwfl::Elf parent,
				      jlong data_pointer,
				      jlong symbol_index,
				      jlong str_sect_index,
				      java::util::List versions,
				      lib::dwfl::ElfSymbol$Builder builder) {
  ::GElf_Sym sym;
  if (::gelf_getsym ((::Elf_Data*)data_pointer, symbol_index, &sym) == NULL)
    return false;

  String name = parent.getStringAtOffset(env, str_sect_index, sym.st_name);
  jlong value = sym.st_value;
  jlong size = sym.st_size;
  ElfSymbolType type
    = ElfSymbolType::intern(env, ELF64_ST_TYPE(sym.st_info));
  ElfSymbolBinding bind
    = ElfSymbolBinding::intern(env, ELF64_ST_BIND(sym.st_info));
  ElfSymbolVisibility visibility
    = ElfSymbolVisibility::intern(env, ELF64_ST_VISIBILITY(sym.st_other));
  jlong shndx = sym.st_shndx;

  builder.symbol(env, symbol_index, name, value, size, type, bind,
		 visibility, shndx, versions);
  return true;
}

jint
lib::dwfl::ElfSymbol::elf_getversym(jnixx::env env, jlong data_pointer,
				    jlong symbol_index) {
  ::GElf_Versym ver;
  if (::gelf_getversym ((::Elf_Data*)data_pointer, symbol_index, &ver) == NULL)
    return -1;
  return (jint)ver;
}

bool
lib::dwfl::ElfSymbol::elf_load_verneed(jnixx::env env,
				       lib::dwfl::Elf parent,
				       jlong data_pointer,
				       jlong str_sect_index,
				       jnixx::array<lib::dwfl::ElfSymbol$PrivVerneed> ret) {
  ::Elf_Data * data = (::Elf_Data*)data_pointer;

  typedef lib::dwfl::ElfSymbol$PrivVerneed Need;
  typedef lib::dwfl::ElfSymbol$PrivVerneed$Aux Aux;

  int count = ret.GetArrayLength(env);
  int offset = 0;
  for (int i = 0; i < count; ++i) {
    Need verneed;
    jnixx::array<Aux> aux_elems;
    ::GElf_Verneed ver;
    if (::gelf_getverneed(data, offset, &ver) == NULL)
      return false;

    verneed = Need::New(env);
    ret.SetObjectArrayElement(env, i, verneed);
    int auxcount = ver.vn_cnt;

    verneed.SetVersion(env, ver.vn_version);
    verneed.SetFilename(env, parent.getStringAtOffset(env, str_sect_index,
						      ver.vn_file));
    aux_elems = jnixx::array<Aux>::NewObjectArray(env, auxcount);
    verneed.SetAux(env, aux_elems);

    int aux_offset = offset + ver.vn_aux;
    offset += ver.vn_next;
    for (int j = 0; j < auxcount; ++j) {
      String jname;
      Aux vernaux;
      ::GElf_Vernaux aux;
      if (::gelf_getvernaux(data, aux_offset, &aux) == NULL)
	return false;

      vernaux = Aux::New(env);
      vernaux.SetHash(env, (jint)aux.vna_hash);
      vernaux.SetWeak(env, (bool)((aux.vna_flags & VER_FLG_WEAK) == VER_FLG_WEAK));
      jname = parent.getStringAtOffset(env, str_sect_index, aux.vna_name);
      vernaux.SetName(env, jname);
      vernaux.SetIndex(env, (jint)aux.vna_other);
      aux_elems.SetObjectArrayElement(env, j, vernaux);
      aux_offset += aux.vna_next;
      vernaux.DeleteLocalRef(env);
      jname.DeleteLocalRef(env);
    }
    aux_elems.DeleteLocalRef(env);
    verneed.DeleteLocalRef(env);
  }
  return true;
}

bool
lib::dwfl::ElfSymbol::elf_load_verdef(jnixx::env env,
				      lib::dwfl::Elf parent,
				      jlong data_pointer,
				      jlong str_sect_index,
				      jnixx::array<lib::dwfl::ElfSymbol$PrivVerdef> ret) {
  ::Elf_Data * data = (::Elf_Data*)data_pointer;

  typedef lib::dwfl::ElfSymbol$PrivVerdef Def;

  int count = ret.GetArrayLength(env);
  int offset = 0;
  for (int i = 0; i < count; ++i) {
    jnixx::array<String> names_elems;
    Def verdef;
    ::GElf_Verdef ver;
    if (::gelf_getverdef(data, offset, &ver) == NULL)
      return false;

    verdef = Def::New(env);
    ret.SetObjectArrayElement(env, i, verdef);
    int auxcount = ver.vd_cnt;

    verdef.SetVersion(env, ver.vd_version);
    verdef.SetBase(env, (bool)((ver.vd_flags & VER_FLG_BASE) == VER_FLG_BASE));
    verdef.SetIndex(env, ver.vd_ndx);
    verdef.SetHash(env, ver.vd_hash);
    names_elems = jnixx::array<String>::NewObjectArray(env, auxcount);
    verdef.SetNames(env, names_elems);
    verdef.DeleteLocalRef(env);

    int aux_offset = offset + ver.vd_aux;
    offset += ver.vd_next;
    for (int j = 0; j < auxcount; ++j) {
      String jname;
      ::GElf_Verdaux aux;
      if (::gelf_getverdaux(data, aux_offset, &aux) == NULL)
	return false;
      jname = parent.getStringAtOffset(env, str_sect_index, aux.vda_name);
      names_elems.SetObjectArrayElement(env, j, jname);
      aux_offset += aux.vda_next;
      jname.DeleteLocalRef(env);
    }
    verdef.DeleteLocalRef(env);
    names_elems.DeleteLocalRef(env);
  }
  return true;
}
