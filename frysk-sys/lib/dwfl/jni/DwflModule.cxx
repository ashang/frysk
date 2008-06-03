// This file is part of the program FRYSK.
//
// Copyright 2005, 2008, Red Hat Inc.
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

#include <cstdlib>
#include <cstring>
#include "libdwfl.h"

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

#define DWFL_MODULE_POINTER ((Dwfl_Module *) GetPointer(env))

lib::dwfl::ModuleElfBias
lib::dwfl::DwflModule::module_getelf(jnixx::env env) {
  Dwarf_Addr bias = 0;
  ::Elf *elf = dwfl_module_getelf(DWFL_MODULE_POINTER, &bias);
  if(elf == NULL)
    return lib::dwfl::ModuleElfBias(env, NULL);
		
  lib::dwfl::ModuleElfBias ret = lib::dwfl::ModuleElfBias::New(env);
  ret.SetElf(env, lib::dwfl::Elf::New(env, (jlong) elf));
  ret.SetBias(env, (jlong) bias);
  return ret;	
}

typedef jnixx::array<lib::dwfl::DwflLine> DwflLineArray;

DwflLineArray
lib::dwfl::DwflModule::getLines(jnixx::env env, String jfilename,
				jint lineno, jint column) {
  jstringUTFChars filename = jstringUTFChars(env, jfilename);
  ::Dwfl_Line **srcsp = 0;
  size_t nsrcs = 0;
  int result = ::dwfl_module_getsrc_file(DWFL_MODULE_POINTER,
					 filename.elements(), lineno,
					 column, &srcsp, &nsrcs);
  if (result < 0) {
    return DwflLineArray(env, NULL);
  }
  DwflLineArray array = DwflLineArray::NewObjectArray(env, nsrcs);
  for (size_t i = 0; i < nsrcs; i++) {
    lib::dwfl::DwflLine line = lib::dwfl::DwflLine::New(env, (jlong)srcsp[i],
							getParent(env));
    array.SetObjectArrayElement(env, i, line);
    line.DeleteLocalRef(env);
  }
  ::free(srcsp);
  return array;
}

void builder_callout(jnixx::env env, lib::dwfl::SymbolBuilder symbolBuilder,
		     String name, ::GElf_Sym const*sym) {
  using lib::dwfl::ElfSymbolType;
  using lib::dwfl::ElfSymbolBinding;
  using lib::dwfl::ElfSymbolVisibility;

  ElfSymbolType type
    = ElfSymbolType::intern(env, ELF64_ST_TYPE(sym->st_info));
  ElfSymbolBinding bind
    = ElfSymbolBinding::intern(env, ELF64_ST_BIND(sym->st_info));
  ElfSymbolVisibility visibility
    = ElfSymbolVisibility::intern(env, ELF64_ST_VISIBILITY(sym->st_other));
  symbolBuilder.symbol(env, name,
		       sym->st_value, sym->st_size,
		       type, bind, visibility,
		       sym->st_shndx != SHN_UNDEF);
}

void
lib::dwfl::DwflModule::getSymbol(jnixx::env env, jlong address,
				 lib::dwfl::SymbolBuilder symbolBuilder) {
  Dwarf_Addr addr = (Dwarf_Addr) address;
  GElf_Sym closest_sym;

  const char* methName = dwfl_module_addrsym(DWFL_MODULE_POINTER, addr,
					     &closest_sym, NULL);

  String jMethodName;
  if (methName == NULL)
    jMethodName = String(env, NULL);
  else
    jMethodName = String::NewStringUTF(env, methName);

  ::builder_callout(env, symbolBuilder, jMethodName, &closest_sym);
}

void
lib::dwfl::DwflModule::getSymtab(jnixx::env env,
				 lib::dwfl::SymbolBuilder symbolBuilder) {
  int count = ::dwfl_module_getsymtab(DWFL_MODULE_POINTER);
  if (count < 0)
    return;

  for (int i = 0; i < count; ++i) {
    ::GElf_Sym sym;
    char const* name = ::dwfl_module_getsym(DWFL_MODULE_POINTER,
					    i, &sym, NULL);
    String jname = String::NewStringUTF(env, name);
    ::builder_callout(env, symbolBuilder, jname, &sym);
    jname.DeleteLocalRef(env);
  }
}

void
lib::dwfl::DwflModule::getPLTEntries(jnixx::env env,
				     lib::dwfl::SymbolBuilder symbolBuilder) {
  GElf_Addr bias;

  ::Elf *elf = ::dwfl_module_getelf(DWFL_MODULE_POINTER, &bias);
  ::GElf_Ehdr ehdr;
  if (::gelf_getehdr (elf, &ehdr) == NULL)
    return;

  bool have_dynamic = false;
  ::GElf_Off off_dynamic;
  for (GElf_Half i = 0; i < ehdr.e_phnum; ++i) {
    ::GElf_Phdr ph;
    if (::gelf_getphdr (elf, i, &ph) == NULL)
      return;
    if (ph.p_type == PT_DYNAMIC)
      {
	have_dynamic = true;
	off_dynamic = ph.p_offset;
	break;
      }
  }
  if (!have_dynamic)
    return;

  ::Elf_Data *dynsym_data = NULL;
  ::GElf_Word dynsym_ct = 0;
  ::Elf_Data *dynsym_str = NULL;

  ::GElf_Addr relplt_addr = 0;
  ::GElf_Word relplt_size = 0;

  ::GElf_Addr plt_addr = 0;
  ::GElf_Word plt_size = 0;

  for (::GElf_Half i = 1; i < ehdr.e_shnum; ++i) {
    ::Elf_Scn *scn = ::elf_getscn(elf, i);
    if (scn == NULL)
      return;

    ::GElf_Shdr shdr;
    if (::gelf_getshdr(scn, &shdr) == NULL)
      return;

    const char *name = ::elf_strptr(elf, ehdr.e_shstrndx, shdr.sh_name);
    if (name == NULL)
      return;

    if (shdr.sh_type == SHT_DYNSYM) {
      dynsym_data = ::elf_getdata(scn, NULL);
      dynsym_ct = shdr.sh_size / shdr.sh_entsize;
      ::Elf_Scn *str_scn = ::elf_getscn(elf, shdr.sh_link);
      ::GElf_Shdr str_hdr;
      if (::gelf_getshdr(str_scn, &str_hdr) == NULL)
	return;
      dynsym_str = ::elf_getdata(str_scn, NULL);
      if (dynsym_str == NULL || ::elf_getdata(str_scn, dynsym_str) != NULL)
	return;
    } else if (shdr.sh_type == SHT_DYNAMIC) {
      ::Elf_Data *data = ::elf_getdata(scn, NULL);
      if (data == NULL || ::elf_getdata(scn, data) != NULL)
	return;

      ::GElf_Word ct = shdr.sh_size / shdr.sh_entsize;
      for (::GElf_Word j = 0; j < ct; ++j) {
	::GElf_Dyn dyn;
	if (::gelf_getdyn(data, j, &dyn) == NULL)
	  return;
	if (dyn.d_tag == DT_JMPREL)
	  relplt_addr = dyn.d_un.d_ptr;
	else if (dyn.d_tag == DT_PLTRELSZ)
	  relplt_size = dyn.d_un.d_val;
      }
    } else if (shdr.sh_type == SHT_PROGBITS
	       || shdr.sh_type == SHT_NOBITS) {
      if (strcmp(name, ".plt") == 0) {
	plt_addr = shdr.sh_addr;
	plt_size = shdr.sh_size;
      }
    }
  }

  if (dynsym_data == NULL || dynsym_str == NULL)
    return;
  if (relplt_addr == 0 || plt_addr == 0)
    return;
  if (plt_addr == 0 || plt_size == 0)
    return;

  ::Elf_Data *relplt_data = NULL;
  ::GElf_Word relplt_count = 0;
  for (::GElf_Half i = 1; i<ehdr.e_shnum; ++i) {
    ::Elf_Scn *scn = ::elf_getscn(elf, i);
    ::GElf_Shdr shdr;

    if (scn == NULL || ::gelf_getshdr(scn, &shdr) == NULL)
      return;

    if (shdr.sh_addr == relplt_addr && shdr.sh_size == relplt_size) {
      relplt_data = ::elf_getdata(scn, NULL);
      if (relplt_data == NULL || ::elf_getdata(scn, relplt_data) != NULL)
	return;
      relplt_count = shdr.sh_size / shdr.sh_entsize;
    }
  }

  ::GElf_Word plt_entry_size = plt_size / (relplt_count + 1);
  

  for (::GElf_Word i = 0; i < relplt_count; ++i) {
    GElf_Rela rela;
    GElf_Sym sym;
    void *ret;

    if (relplt_data->d_type == ELF_T_REL) {
      GElf_Rel rel;
      ret = ::gelf_getrel(relplt_data, i, &rel);
      rela.r_offset = rel.r_offset;
      rela.r_info = rel.r_info;
      rela.r_addend = 0;
    }
    else
      ret = ::gelf_getrela(relplt_data, i, &rela);

    if (ret == NULL
	|| ELF64_R_SYM(rela.r_info) >= dynsym_ct
	|| ::gelf_getsym(dynsym_data, ELF64_R_SYM(rela.r_info), &sym) == NULL)
      return;

    const char *name = ((const char*)dynsym_str->d_buf) + sym.st_name;
    ::GElf_Addr addr = plt_addr + (i + 1) * plt_entry_size + bias;

    String jname = String::NewStringUTF(env, name);
    symbolBuilder.symbol(env, jname,
			 addr, plt_entry_size,
			 lib::dwfl::ElfSymbolType(env, NULL),
			 lib::dwfl::ElfSymbolBinding(env, NULL),
			 lib::dwfl::ElfSymbolVisibility(env, NULL),
			 true);
    jname.DeleteLocalRef(env);
  }
}

void
lib::dwfl::DwflModule::getSymbolByName(jnixx::env env, String jname,
				       lib::dwfl::SymbolBuilder symbolBuilder) {
  jstringUTFChars name = jstringUTFChars(env, jname);
  int numSymbols = dwfl_module_getsymtab(DWFL_MODULE_POINTER);
  for (int i = 0; i < numSymbols; i++) {
    GElf_Sym sym;
    const char *symName = dwfl_module_getsym(DWFL_MODULE_POINTER, i, &sym, 0);
    if (!::strcmp(name.elements(), symName)) {
      String jsym = String::NewStringUTF(env, symName);
      ::builder_callout(env, symbolBuilder, jsym, &sym);
      jsym.DeleteLocalRef(env);
    }
  }
}

void
lib::dwfl::DwflModule::setUserData(jnixx::env env, Object data) {
  void **userdata = NULL;
  fprintf(stderr, "user data is %p\n", userdata);
  dwfl_module_info(DWFL_MODULE_POINTER, &userdata, NULL, NULL, NULL, NULL, NULL,
                   NULL);
  *userdata = data._object;
}

/* 
 * Get the DebugInfo paths if present
 */
String
lib::dwfl::DwflModule::getDebuginfo(jnixx::env env) {
  // Filter out non-binary modules
  if (module_getelf(env) == NULL) {    
    return String(env, NULL);
  }	
  Dwarf_Addr bias;

  if (dwfl_module_getdwarf (DWFL_MODULE_POINTER, &bias) == NULL) {
    // Case where debuginfo not installed or available
    return String(env, NULL);
  }
  
  // Get the path to debuginfo file
  const char* debuginfo_fname = NULL;  
  dwfl_module_info (DWFL_MODULE_POINTER, 
                    NULL, NULL, NULL, NULL, NULL, NULL,
                    &debuginfo_fname);   

  if (debuginfo_fname) {                      
    return String::NewStringUTF(env, debuginfo_fname); 
  }

  return getName(env);                 	               		      
}    

struct each_pubname_context {
  jnixx::env env;
  lib::dwfl::DwflModule dwflModule;
  Dwarf_Addr bias;
  each_pubname_context(jnixx::env env, lib::dwfl::DwflModule m, Dwarf_Addr b) {
    this->env = env;
    dwflModule = m;
    bias = b;
  }
};

int
each_pubname(Dwarf *dwarf, Dwarf_Global *gl, void* data) {
  each_pubname_context* context = static_cast<each_pubname_context*>(data);
  lib::dwfl::Dwfl dwfl = context->dwflModule.GetParent(context->env);

  Dwarf_Die* die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));

  if (dwarf_offdie (dwarf, gl->die_offset, die) == NULL) {
    lib::dwfl::DwarfException::ThrowNew(context->env,
					"failed to get object die");
  } else {
    lib::dwfl::DwarfDie dwdie = dwfl.GetFactory(context->env)
      .makeDie(context->env, (jlong)die, context->dwflModule);
    lib::dwfl::DwflDieBias bias = lib::dwfl::DwflDieBias::New(context->env,
							      dwdie,
							      context->bias);
    context->dwflModule.GetPubNames(context->env) .add(context->env, bias);
    bias.DeleteLocalRef(context->env);
  }

  return DWARF_CB_OK;
}

void
lib::dwfl::DwflModule::get_pubnames(jnixx::env env) {
  Dwarf_Addr bias;
  ::Dwarf* dwarf = ::dwfl_module_getdwarf(DWFL_MODULE_POINTER, &bias);

  if (dwarf != NULL) {
    ::each_pubname_context ctx(env, *this, bias);
    ::dwarf_getpubnames(dwarf, each_pubname, &ctx, 0);
  }
}

lib::dwfl::DwarfDie
lib::dwfl::DwflModule::offdie(jnixx::env env, jlong die, jlong offset) {
  Dwarf_Addr bias;
  Dwarf_Off dwarf_offset = (Dwarf_Off)offset;
  Dwarf_Die* dwarf_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  
  ::Dwarf* dbg = dwfl_module_getdwarf ((Dwfl_Module*)getPointer(env), &bias);
  
  dwarf_offdie (dbg, dwarf_offset, dwarf_die);
  
  lib::dwfl::DwarfDie dwarfDie = GetParent(env).GetFactory(env).makeDie(env, (jlong)dwarf_die, *this);
   
  return dwarfDie;
}
