// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

#include <stdio.h>
#include <stdlib.h>

#include <libdw.h>
#include <dwarf.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;

#define DWARF_DIE_POINTER ((Dwarf_Die *) GetPointer(env))

jlong
lib::dwfl::DwarfDie::get_lowpc(jnixx::env env) {
  Dwarf_Addr lowpc;
  ::dwarf_lowpc(DWARF_DIE_POINTER, &lowpc);
  return (jlong) lowpc;
}

jlong
lib::dwfl::DwarfDie::get_highpc(jnixx::env env) {
  Dwarf_Addr highpc;
  ::dwarf_highpc(DWARF_DIE_POINTER, &highpc);
  return (jlong) highpc;
}

jlong
lib::dwfl::DwarfDie::get_entrypc(jnixx::env env) {
  Dwarf_Addr entrypc;
  ::dwarf_entrypc(DWARF_DIE_POINTER, &entrypc);
  return (jlong) entrypc;
}

String
lib::dwfl::DwarfDie::get_diename(jnixx::env env) {
  const char *name = dwarf_diename (DWARF_DIE_POINTER);
  if (name != NULL)
    return String::NewStringUTF(env, name);
  else
    return String::NewStringUTF(env, "");
}

String
lib::dwfl::DwarfDie::get_decl_file(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  const char *name = dwarf_decl_file (die);
  if (name == NULL) {
    lib::dwfl::DwAttributeNotFoundException::throwDwException(env, (jint)DW_AT_decl_file);
  }
  return String::NewStringUTF(env, name);
}

jint
lib::dwfl::DwarfDie::get_decl_line(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  int lineno;
  if (dwarf_decl_line (die, &lineno) != 0){
    Dwarf_Attribute type_attr;
    Dwarf_Word constant;
    if (dwarf_attr_integrate (die, DW_AT_decl_line, &type_attr)){
      dwarf_formudata (&type_attr, &constant);
      return constant;
    }else{
      lib::dwfl::DwAttributeNotFoundException::throwDwException(env, (jint)DW_AT_decl_line);
    }
  }

  return lineno;
}

jint
lib::dwfl::DwarfDie::get_decl_column(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  int column;
  int code = dwarf_decl_column (die, &column);
  if (code != 0)
    column = 0;
  return column;
}

jnixx::jlongArray
lib::dwfl::DwarfDie::get_scopes(jnixx::env env, jlong addr) {
  Dwarf_Die *dies;
  int count = dwarf_getscopes(DWARF_DIE_POINTER, (Dwarf_Addr) addr, &dies);
  if (count == -1)
    count = 0;
  jnixx::jlongArray scopes = jnixx::jlongArray::NewLongArray(env, (jint) count);
  jlongArrayElements longs = jlongArrayElements(env, scopes);
  for(int i = 0; i < count; i++) {
    longs.elements()[i] = (jlong) &dies[i];
  }
  return scopes;
}

jnixx::jlongArray
lib::dwfl::DwarfDie::get_scopes_die(jnixx::env env) {
  Dwarf_Die *dies;
  int count = dwarf_getscopes_die(DWARF_DIE_POINTER, &dies);
  if (count == -1)
    count = 0;
  jnixx::jlongArray longs = jnixx::jlongArray::NewLongArray(env, (jint) count);
  jlongArrayElements longsp = jlongArrayElements(env, longs);
  for(int i = 0; i < count; i++) {
    longsp.elements()[i] = (jlong) &dies[i];
  }
  return longs;
}

jlong
lib::dwfl::DwarfDie::get_scopevar(jnixx::env env, jnixx::jlongArray jdie_scope,
				  jnixx::jlongArray jscopes, String jvariable) {
  // FIXME: This appears to be leaked?
  Dwarf_Die *var_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  jlongArrayElements scopes = jlongArrayElements(env, jscopes);
  Dwarf_Die *dies[scopes.length()];
  for(int i = 0; i < scopes.length(); i++) {
    jlong dieptr = scopes.elements()[i];
    dies[i] = (Dwarf_Die*)dieptr;
  }
  jstringUTFChars variable = jstringUTFChars(env, jvariable);
  int code = dwarf_getscopevar (*dies, scopes.length(),
				variable.elements(),
				0, NULL, 0, 0, var_die);
  if (code >= 0) {
    if (dwarf_tag (var_die) != DW_TAG_variable)
      return -1;
    jlongArrayElements die_scope = jlongArrayElements(env, jdie_scope);
    die_scope.elements()[0] = (jlong)var_die;    // Die for variable
    die_scope.elements()[1] = code; // Die for scope
  } else if (dwarf_tag (var_die) != DW_TAG_variable) {
    return -1;
  }
  return code;
}

static Dwarf_Die
get_first_struct_member(Dwarf_Die &result) {
  Dwarf_Attribute attr_mem;
  do {
    dwarf_formref_die (dwarf_attr_integrate (&result, DW_AT_type, &attr_mem),
		       &result);
    if (dwarf_tag (&result) == DW_TAG_structure_type) {
      dwarf_child (&result, &result);
      break;
    }
  } while (1);
  return result;
}

jlong
lib::dwfl::DwarfDie::get_scopevar_names(jnixx::env env,
					jnixx::jlongArray jscopes,
					String jvariable) {
  jlongArrayElements scopes = jlongArrayElements(env, jscopes);
  Dwarf_Die *scope_dies[scopes.length()];
  Dwarf_Die result;
  int get_struct_members = 0;

  jstringUTFChars variablep = jstringUTFChars(env, jvariable);
  int variable_len = variablep.length();
  char variable[variable_len + 1];
  strcpy(variable, variablep.elements());
  if (variable[variable_len - 1] == '.') {
    variable[variable_len - 1] = '\0';
    get_struct_members = 1;
  }

  for(int i = 0; i < scopes.length(); i++) {
    jlong dieptr = scopes.elements()[i];
    scope_dies[i] = (Dwarf_Die*)dieptr;
  }

  /* Start with the innermost scope and move out.  */
  for (int out = 0; out < scopes.length(); ++out) {
    if (dwarf_haschildren (scope_dies[out])) {
      if (dwarf_child (scope_dies[out], &result) != 0) {
	return -1;
      }
      do {
	switch (dwarf_tag (&result)) {
	case DW_TAG_variable:
	case DW_TAG_formal_parameter:
	case DW_TAG_subprogram:
	  break;

	default:
	  continue;
	}

	Dwarf_Attribute attr_mem;
	const char *diename = dwarf_formstring
	  (dwarf_attr_integrate (&result, DW_AT_name, &attr_mem));
	if (diename != NULL && !strncmp (diename, variable, variable_len)) {
	  if (get_struct_members) {
	    result = get_first_struct_member (result);
	    do {
	      diename = dwarf_formstring
		(dwarf_attr_integrate (&result, DW_AT_name, &attr_mem));
	      lib::dwfl::DwarfDie::addScopeVarName(env, String::NewStringUTF(env, diename));
	    } while (dwarf_siblingof (&result, &result) == 0);
	    return 0;
	  }
	  addScopeVarName(env, String::NewStringUTF(env, diename));
	}
      } while (dwarf_siblingof (&result, &result) == 0);
    }
  }
  return 0;
}

void
lib::dwfl::DwarfDie::get_addr(jnixx::env env, jlong var_die, jlong pc) {

  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr)) {
    size_t i = 0;
    int nlocs;
    bool ok = false;

    if (pc == 0){
      nlocs = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      if (nlocs >= 0 && fb_len > 0)
	ok = true;
    } else {
      nlocs = dwarf_getlocation_addr (&loc_attr,pc, &fb_expr, &fb_len, 5);
      if (nlocs > 0 && fb_len > 0)
	ok = true;
    }

    if (ok) {
      do {
	addOps(env, fb_expr[i].atom, fb_expr[i].number, fb_expr[i].number2,
	       fb_expr[i].offset);
	i += 1;
      } while (i < fb_len);
    }
  }
}

jlong
lib::dwfl::DwarfDie::get_type(jnixx::env env, jlong var_die,
			      bool follow_type_def) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * type_mem_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  Dwarf_Attribute type_attr;
  
  if (dwarf_attr_integrate (die, DW_AT_type, &type_attr))
    {
      if (dwarf_formref_die (&type_attr, type_mem_die))
	{
	  if (dwarf_tag (type_mem_die) == DW_TAG_typedef && follow_type_def)
	    {
	      dwarf_attr_integrate (type_mem_die, DW_AT_type, &type_attr);
	      dwarf_formref_die (&type_attr, type_mem_die);
	    }
	}
      return (jlong) type_mem_die;
    }
  return 0;
}

jlong
lib::dwfl::DwarfDie::get_child(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * child_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  
  if (dwarf_child (die, child_die) == 0)
    return (jlong)child_die;
  return 0;
}

jlong
lib::dwfl::DwarfDie::get_sibling(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * sibling_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  
  if (dwarf_siblingof (die, sibling_die) == 0)
    return (jlong)sibling_die;
  return 0;
}

jlong
lib::dwfl::DwarfDie::get_original_die(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die *original_die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  
  Dwarf_Attribute attr_mem;
  if (dwarf_hasattr (die, DW_AT_abstract_origin)
      && dwarf_formref_die (dwarf_attr (die, DW_AT_abstract_origin, &attr_mem),
			    original_die) != NULL){
    return (jlong)original_die;
  }

  if (dwarf_hasattr (die, DW_AT_specification)
      && dwarf_formref_die (dwarf_attr (die, DW_AT_specification, &attr_mem),
			    original_die) != NULL){
    return (jlong)original_die;
  }

  return 0;
}

jint
lib::dwfl::DwarfDie::get_tag(jnixx::env env, jlong die_p) {
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  return dwarf_tag (die);
}

bool
lib::dwfl::DwarfDie::get_attr_boolean(jnixx::env env, jlong die_p, jint attr) {
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (die, attr, &type_attr))
    return 1;
  else
    return 0;
}

jint
lib::dwfl::DwarfDie::get_attr_constant(jnixx::env env, jlong die_p, jint attr) {
  Dwarf_Die *die = (Dwarf_Die*) die_p;
  Dwarf_Word constant;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (die, attr, &type_attr)) {
    dwarf_formudata (&type_attr, &constant);
    return constant;
  }
  return -1;
}
  

String
lib::dwfl::DwarfDie::get_attr_string(jnixx::env env, jlong die_p, jint attr) {
  Dwarf_Die *die = (Dwarf_Die*) die_p;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate(die, attr, &type_attr)) {
    const char *name = dwarf_formstring(&type_attr);
    if (name != NULL)
      return String::NewStringUTF(env, name);
    else	
      return String::NewStringUTF(env, "");
  }
  return String(env, NULL);
}

jlong
lib::dwfl::DwarfDie::get_offset(jnixx::env env,jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  return (jlong)dwarf_dieoffset(die);
}

void
lib::dwfl::DwarfDie::get_framebase(jnixx::env env, jlong var_die,
				   jlong scope_arg, jlong pc) {

  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  Dwarf_Attribute *fb_attr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr) >= 0) {
    size_t i = 0;
    code = dwarf_getlocation_addr (&loc_attr,pc, &fb_expr, &fb_len, 5);

    if (fb_expr[0].atom != DW_OP_fbreg)
      return;

    fb_attr = dwarf_attr_integrate ((Dwarf_Die*) scope_arg,
				    DW_AT_frame_base,
				    &loc_attr);

    code = (dwarf_getlocation_addr (fb_attr, pc, &fb_expr, &fb_len, 1));

    if (code > 0 && fb_len > 0)
      do {
	addOps(env, fb_expr[i].atom, fb_expr[i].number, fb_expr[i].number2,
	       fb_expr[i].offset);
	i += 1;
      } while (i < fb_len);
    return;
  }
}


jlong
lib::dwfl::DwarfDie::get_data_member_location(jnixx::env env, jlong var_die) {
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_data_member_location, &loc_attr) >= 0) {
    code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
    if (fb_len > 0 && fb_expr[0].atom == DW_OP_plus_uconst)
      return fb_expr[0].number;
  }
  lib::dwfl::DwAttributeNotFoundException::throwDwException(env, (jint)DW_AT_data_member_location);
  return 0;
}

/*
 * Returns true if this die has the DW_TAG_inlined_subroutine tag
 */
bool
lib::dwfl::DwarfDie::is_inline_func(jnixx::env env) {
  return dwarf_tag(DWARF_DIE_POINTER) == DW_TAG_inlined_subroutine;
}

/*
 * Return die for static symbol SYM_P in DBG_P
 */

static Dwarf_Die* iterate_decl (Dwarf_Die*, const char*, size_t);

jlong
lib::dwfl::DwarfDie::get_decl(jnixx::env env, jlong dbg_p, String jsym) {
  Dwarf_Off offset = 0;
  Dwarf_Off old_offset;
  Dwarf_Die cudie_mem;
  size_t hsize;
  Dwarf_Files *files;
  size_t nfiles;
  ::Dwarf *dbg = (::Dwarf*)dbg_p;
  jstringUTFChars sym = jstringUTFChars(env, jsym);

  while (dwarf_nextcu(dbg, old_offset = offset, &offset, &hsize, NULL, NULL,
		      NULL) == 0) {
    Dwarf_Die *cudie = dwarf_offdie (dbg, old_offset + hsize, &cudie_mem);
    if (dwarf_getsrcfiles(cudie, &files, &nfiles) != 0)
      continue;
    if (dwarf_haschildren (cudie)) {
      jlong result = (jlong)iterate_decl(cudie, sym.elements(), nfiles);
      if (result != 0)
	return result;
    }
  }
  return (jlong)0;
}

jlong
lib::dwfl::DwarfDie::get_decl_cu(jnixx::env env, jlong die_p, String jsym) {
  Dwarf_Die *die = (Dwarf_Die*) die_p;
  jstringUTFChars sym = jstringUTFChars(env, jsym);
  return (jlong)iterate_decl(die, sym.elements(), 99);
}

static Dwarf_Die*
iterate_decl(Dwarf_Die *die_p, const char *sym, size_t nfiles) {
  Dwarf_Die *die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  memcpy(die, die_p, sizeof (Dwarf_Die));
  
  /* Iterate over all immediate children of the CU DIE.  */
  dwarf_child (die, die);
  do {
    Dwarf_Attribute attr_mem;
    Dwarf_Attribute *attr = dwarf_attr (die, DW_AT_name, &attr_mem);
    const char *name = dwarf_formstring (attr);
    // Want either a die with a name OR look at its DW_TAG_enumerators
    if (name == NULL && dwarf_tag (die) != DW_TAG_enumeration_type)
      continue;

    Dwarf_Word fileidx;
    attr = dwarf_attr (die, DW_AT_decl_file, &attr_mem);
    int tag = dwarf_tag(die);
    // DW_TAG_enumerator doesn't have a DW_AT_decl_file
    if ((dwarf_formudata (attr, &fileidx) != 0 || (size_t)fileidx >= nfiles)
	&& tag != DW_TAG_enumerator)
      continue;
    if (name)
      if (strcmp(name, sym) == 0)
	return die;
    if (dwarf_haschildren (die) && tag != DW_TAG_structure_type
	&& tag != DW_TAG_union_type) {
      Dwarf_Die *result = iterate_decl(die, sym, nfiles);
      if (result != NULL)
	return result;
    }
  }
  while (dwarf_siblingof (die, die) == 0);
  ::free(die);
  return NULL;
}

java::util::ArrayList
lib::dwfl::DwarfDie::getEntryBreakpoints(jnixx::env env) {
  Dwarf_Addr *bkpts = 0;
  int count = ::dwarf_entry_breakpoints(DWARF_DIE_POINTER, &bkpts);
  if (count > 0) {
    java::util::ArrayList alist = java::util::ArrayList::New(env);
    for (int i = 0; i < count; i++) {
      Long l = Long::New(env, (jlong)bkpts[i]);
      alist.add(env, l);
      l.DeleteLocalRef(env);
    }
    ::free(bkpts);
    return alist;
  } else {
    return java::util::ArrayList(env, NULL);
  }
}

bool
lib::dwfl::DwarfDie::isInlineDeclaration(jnixx::env env) {
  return dwarf_func_inline(DWARF_DIE_POINTER) != 0;
}

struct CallbackArgs {
  jnixx::env env;
  java::util::ArrayList arrayList;
  lib::dwfl::DwarfDieFactory factory;
  CallbackArgs(jnixx::env env, java::util::ArrayList arrayList,
	       lib::dwfl::DwarfDieFactory factory) {
    this->env = env;
    this->arrayList = arrayList;
    this->factory = factory;
  }
};

static int
inlineInstanceCallback(Dwarf_Die *instance, void *arg) {
  CallbackArgs* cbArg = static_cast<CallbackArgs *>(arg);
  Dwarf_Die *die = (Dwarf_Die*)::malloc(sizeof(Dwarf_Die));
  memcpy(die, instance, sizeof(*die));
  lib::dwfl::DwarfDie dwarfDie
    = cbArg->factory.makeDie(cbArg->env, (jlong)die,
			     lib::dwfl::DwflModule(cbArg->env, NULL));
  dwarfDie.setManageDie(cbArg->env, true);
  cbArg->arrayList.add(cbArg->env, dwarfDie);
  dwarfDie.DeleteLocalRef(cbArg->env);
  return DWARF_CB_OK;
}

java::util::ArrayList
lib::dwfl::DwarfDie::getInlinedInstances(jnixx::env env) {
  CallbackArgs cbArgs
    = CallbackArgs(env, java::util::ArrayList::New(env),
		   lib::dwfl::DwarfDieFactory::getFactory(env));
  if (dwarf_func_inline_instances(DWARF_DIE_POINTER, inlineInstanceCallback,
				  &cbArgs) != 0) {
    lib::dwfl::DwarfException::ThrowNew(env, "Unknown error while searching for inline instances");
  } else {
    return cbArgs.arrayList;
  }
}

bool
lib::dwfl::DwarfDie::hasattr(jnixx::env env, jlong pointer, jint attr) {
  Dwarf_Die *die = (Dwarf_Die*) pointer;
  return dwarf_hasattr(die, attr) != 0;
}

void
lib::dwfl::DwarfDie::finalize(jnixx::env env) {
  if (GetManageDie(env))
    ::free(DWARF_DIE_POINTER);
}
