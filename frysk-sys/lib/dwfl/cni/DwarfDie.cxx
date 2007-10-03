// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
#include <libdw.h>
#include <gcj/cni.h>
#include <dwarf.h>
#include <stdio.h>
#include <alloca.h>
#include <stdlib.h>

#include <java/lang/Long.h>
#include <java/util/ArrayList.h>

#include "lib/dwfl/DwarfDie.h"
#include "lib/dwfl/BaseTypes.h"
#include "lib/dwfl/DwarfDieFactory.h"
#include "lib/dwfl/DwarfException.h"
#include "lib/dwfl/DwException.h"


#define DWARF_DIE_POINTER (Dwarf_Die *) this->pointer

jlong
lib::dwfl::DwarfDie::get_lowpc()
{
	Dwarf_Addr lowpc;
	::dwarf_lowpc(DWARF_DIE_POINTER, &lowpc);
	return (jlong) lowpc;
}

jlong
lib::dwfl::DwarfDie::get_highpc()
{
	Dwarf_Addr highpc;
	::dwarf_highpc(DWARF_DIE_POINTER, &highpc);
	return (jlong) highpc;
}

jlong
lib::dwfl::DwarfDie::get_entrypc()
{
	Dwarf_Addr entrypc;
	::dwarf_entrypc(DWARF_DIE_POINTER, &entrypc);
	return (jlong) entrypc;
}



jstring
lib::dwfl::DwarfDie::get_diename()
{
  const char *name = dwarf_diename (DWARF_DIE_POINTER);
  if (name != NULL)
    return JvNewStringUTF (name);
  else
    return JvNewStringUTF ("");
}

jstring
lib::dwfl::DwarfDie::get_decl_file(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  const char *name = dwarf_decl_file (die);
  if (name == NULL)
    lib::dwfl::DwException::throwDwException();
  return JvNewStringLatin1 (name);
}

jlong
lib::dwfl::DwarfDie::get_decl_line(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  int lineno;
  if (dwarf_decl_line (die, &lineno) != 0)
    lib::dwfl::DwException::throwDwException();
  return lineno;
}

jint
lib::dwfl::DwarfDie::get_decl_column(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  int column;
  int code = dwarf_decl_column (die, &column);
  if (code != 0)
    column = 0;
  return column;
}

jlongArray
lib::dwfl::DwarfDie::get_scopes(jlong addr)
{
  Dwarf_Die *dies;
  
  int count = dwarf_getscopes(DWARF_DIE_POINTER, (Dwarf_Addr) addr, &dies);

  if (count == -1)
    count = 0;
  
  jlongArray longs = JvNewLongArray((jint) count);
  jlong* longp = elements(longs);
  
  for(int i = 0; i < count; i++)
    longp[i] = (jlong) &dies[i];
		
  return longs;
}

jlongArray
lib::dwfl::DwarfDie::get_scopes_die()
{
  Dwarf_Die *dies;
  
  int count = dwarf_getscopes_die(DWARF_DIE_POINTER, &dies);
  
  if (count == -1)
    count = 0;
  
  jlongArray longs = JvNewLongArray((jint) count);
  jlong* longp = elements(longs);
  
  for(int i = 0; i < count; i++)
    longp[i] = (jlong) &dies[i];
		
  return longs;
}

Dwarf_Die *var_die;

jlong
lib::dwfl::DwarfDie::get_scopevar (jlongArray die_scope, jlongArray scopes,
				 jstring variable)
{
  
  var_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  int nscopes = JvGetArrayLength (scopes);

  Dwarf_Die *dies[nscopes];
  jlong* scopesp = elements(scopes);

  for(int i = 0; i < nscopes; i++)
    {
      jlong dieptr = scopesp[i];
      dies[i] = (Dwarf_Die*)dieptr;
    }
  
  int utf_variable_len = variable->length ();
  char utf_variable[utf_variable_len + 1];
  JvGetStringUTFRegion (variable, 0, utf_variable_len, utf_variable);
  utf_variable[utf_variable_len] = '\0';

  int code = dwarf_getscopevar (*dies, nscopes,
				utf_variable,
				0, NULL, 0, 0, var_die);
  if (code >= 0)
    {
      if (dwarf_tag (var_die) != DW_TAG_variable)
	return -1;
      jlong* longp = elements(die_scope);
      longp[0] = (jlong)var_die;    // Die for variable
      longp[1] = code; // Die for scope
    }
  else if (dwarf_tag (var_die) != DW_TAG_variable)
    return -1;
  return code;
}

static Dwarf_Die
get_first_struct_member (Dwarf_Die &result)
{
  Dwarf_Attribute attr_mem;
  do {
    dwarf_formref_die (dwarf_attr_integrate (&result, DW_AT_type, &attr_mem),
		       &result);
    if (dwarf_tag (&result) == DW_TAG_structure_type)
      {
	dwarf_child (&result, &result);
	break;
      }
  } while (1);
  return result;
}

jlong
lib::dwfl::DwarfDie::get_scopevar_names (jlongArray scopes_arg,
				 jstring variable_arg)
{
  int nscopes = JvGetArrayLength (scopes_arg);
  Dwarf_Die *scopes[nscopes];
  jlong* scopesp = elements(scopes_arg);
  Dwarf_Die result;
  int get_struct_members = 0;
  int variable_len = variable_arg->length ();
  char variable[variable_len + 1];
  JvGetStringUTFRegion (variable_arg, 0, variable_len, variable);
  variable[variable_len] = '\0';
  if (variable[variable_len - 1] == '.')
    {
      variable[variable_len - 1] = '\0';
      get_struct_members = 1;
    }
  using namespace java::lang;

  for(int i = 0; i < nscopes; i++)
    {
      jlong dieptr = scopesp[i];
      scopes[i] = (Dwarf_Die*)dieptr;
    }

  /* Start with the innermost scope and move out.  */
  for (int out = 0; out < nscopes; ++out)
    if (dwarf_haschildren (scopes[out]))
      {
        if (dwarf_child (scopes[out], &result) != 0)
          return -1;
        do
          {
            switch (dwarf_tag (&result))
              {
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
            if (diename != NULL && !strncmp (diename, variable, variable_len))
	      {
		if (get_struct_members)
		  {
		    result = get_first_struct_member (result);
		    do
		      {
			diename = dwarf_formstring
			  (dwarf_attr_integrate (&result, DW_AT_name, &attr_mem));
			lib::dwfl::DwarfDie::addScopeVarName (JvNewStringUTF (diename));
		      }
		    while (dwarf_siblingof (&result, &result) == 0);
		    return 0;
		  }
		addScopeVarName (JvNewStringUTF (diename));
	      }
	  }
	while (dwarf_siblingof (&result, &result) == 0);
      }
  return 0;
}

void
lib::dwfl::DwarfDie::get_addr (jlong var_die, jlong pc)
{

  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr))
    {
      size_t i = 0;
      int nlocs;
      bool ok = false;

      if (pc == 0){
	nlocs = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
	if (nlocs >= 0 && fb_len > 0)
	  ok = true;
      }else{
	nlocs = dwarf_getlocation_addr (&loc_attr,pc, &fb_expr, &fb_len, 5);
	if (nlocs > 0 && fb_len > 0)
	  ok = true;
      }

      if (ok)
	do 
	  {
	    addOps (fb_expr[i].atom, fb_expr[i].number, fb_expr[i].number2,
		    fb_expr[i].offset);
	    i += 1;
	  }
	while (i < fb_len);
    }
}

static Dwarf_Die* skip_storage_attr (Dwarf_Die *die)
{
  Dwarf_Attribute type_attr;
  while (die && (dwarf_tag (die) == DW_TAG_volatile_type
		 || (dwarf_tag (die) == DW_TAG_const_type)))
    {
      dwarf_attr_integrate (die, DW_AT_type, &type_attr);
      dwarf_formref_die (&type_attr, die);
    }
  return die;
}

jlong
lib::dwfl::DwarfDie::get_type (jlong var_die, jboolean follow_type_def)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * type_mem_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  Dwarf_Attribute type_attr;
  
  die = skip_storage_attr (die);
  if (dwarf_attr_integrate (die, DW_AT_type, &type_attr))
    {
      if (dwarf_formref_die (&type_attr, type_mem_die))
	{
	  type_mem_die = skip_storage_attr (type_mem_die);
	  while (dwarf_tag (type_mem_die) == DW_TAG_typedef && follow_type_def)
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
lib::dwfl::DwarfDie::get_child (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * child_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  
  if (dwarf_child (die, child_die) == 0)
    return (jlong)child_die;
  return 0;
}

jlong
lib::dwfl::DwarfDie::get_sibling (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * sibling_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  
  if (dwarf_siblingof (die, sibling_die) == 0)
    return (jlong)sibling_die;
  return 0;
}

jint
lib::dwfl::DwarfDie::get_base_type (jlong var_die)
{
  Dwarf_Die *type_die = (Dwarf_Die*) var_die;
  Dwarf_Attribute type_attr;
  while (type_die && (dwarf_tag (type_die) == DW_TAG_volatile_type
		      || (dwarf_tag (type_die) == DW_TAG_const_type)))
    {
      dwarf_attr_integrate (type_die, DW_AT_type, &type_attr);
      dwarf_formref_die (&type_attr, type_die);
    }
  
  if (dwarf_tag (type_die) == DW_TAG_base_type)
    {
      Dwarf_Word byte_size;
      Dwarf_Word encoding;
      Dwarf_Attribute type_attr;
      if (dwarf_attr_integrate (type_die, DW_AT_byte_size, &type_attr))
	dwarf_formudata (&type_attr, &byte_size);
      else return 0;
      if (dwarf_attr_integrate (type_die, DW_AT_encoding, &type_attr))
	dwarf_formudata (&type_attr, &encoding);
      switch (byte_size)
	{
	case 1:
	  switch (encoding)
	    {
	    case DW_ATE_signed_char: return lib::dwfl::BaseTypes::baseTypeByte;
	    case DW_ATE_unsigned_char: return lib::dwfl::BaseTypes::baseTypeUnsignedByte;
	    }
	case 2:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dwfl::BaseTypes::baseTypeShort;
	    case DW_ATE_unsigned: return lib::dwfl::BaseTypes::baseTypeUnsignedShort;
	    case DW_ATE_unsigned_char: return lib::dwfl::BaseTypes::baseTypeUnicode;
	    }
	case 4:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dwfl::BaseTypes::baseTypeInteger;
	    case DW_ATE_unsigned: return lib::dwfl::BaseTypes::baseTypeUnsignedInteger;
	    case DW_ATE_float: return lib::dwfl::BaseTypes::baseTypeFloat;
	    }
	case 8:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dwfl::BaseTypes::baseTypeLong;
	    case DW_ATE_unsigned: return lib::dwfl::BaseTypes::baseTypeUnsignedLong;
	    case DW_ATE_float: return lib::dwfl::BaseTypes::baseTypeDouble;
	    }
	}
    }
  return 0;
}

jint
lib::dwfl::DwarfDie::get_tag (jlong die_p)
{
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  return dwarf_tag (die);
}

jboolean
lib::dwfl::DwarfDie::get_attr_boolean (jlong die_p, jint attr)
{
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (die, attr, &type_attr))
    return 1;
  else
    return 0;
}

jint
lib::dwfl::DwarfDie::get_attr_constant (jlong die_p, jint attr)
{
  Dwarf_Die *die = (Dwarf_Die*) die_p;
  Dwarf_Word constant;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (die, attr, &type_attr))
    {
      dwarf_formudata (&type_attr, &constant);
      return constant;
    }
  return -1;
}
  
jint
lib::dwfl::DwarfDie::get_offset (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  return (jint)dwarf_dieoffset(die);
}

void
lib::dwfl::DwarfDie::get_framebase (jlong var_die, jlong scope_arg, jlong pc)
{

  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  Dwarf_Attribute *fb_attr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr) >= 0)
    {
      size_t i = 0;
      code = dwarf_getlocation_addr (&loc_attr,pc, &fb_expr, &fb_len, 5);

      if (fb_expr[0].atom != DW_OP_fbreg)
      	return;

      fb_attr = dwarf_attr_integrate ((Dwarf_Die*) scope_arg,
				      DW_AT_frame_base,
				      &loc_attr);

      code = (dwarf_getlocation_addr (fb_attr, pc, &fb_expr, &fb_len, 1));

      if (code > 0 && fb_len > 0)
	do 
	  {
	    addOps (fb_expr[i].atom, fb_expr[i].number, fb_expr[i].number2,
		    fb_expr[i].offset);
	    i += 1;
	  }
	while (i < fb_len);
      return;
    }
}


jlong
lib::dwfl::DwarfDie::get_data_member_location (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_data_member_location, &loc_attr) >= 0)
    {
      code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      if (fb_len > 0 && fb_expr[0].atom == DW_OP_plus_uconst)
	return fb_expr[0].number;
    }
  lib::dwfl::DwException::throwDwException();
  return 0;
}

/*
 * Returns true if this die has the DW_TAG_inlined_subroutine tag
 */
jboolean
lib::dwfl::DwarfDie::is_inline_func ()
{
	return dwarf_tag(DWARF_DIE_POINTER) == DW_TAG_inlined_subroutine;
}

/*
 * Return die for static symbol SYM_P in DBG_P
 */

static Dwarf_Die* iterate_decl (Dwarf_Die*, char*, size_t);

jlong
lib::dwfl::DwarfDie::get_decl (jlong dbg_p, jstring sym_p)
{
  Dwarf_Off offset = 0;
  Dwarf_Off old_offset;
  Dwarf_Die cudie_mem;
  size_t hsize;
  Dwarf_Files *files;
  size_t nfiles;
  ::Dwarf *dbg = (::Dwarf*)dbg_p;
  int sym_len = sym_p->length ();
  char sym[sym_len + 1];
  JvGetStringUTFRegion (sym_p, 0, sym_len, sym);
  sym[sym_len] = '\0';

  while (dwarf_nextcu (dbg, old_offset = offset, &offset, &hsize, NULL, NULL,
                       NULL) == 0)
    {
      Dwarf_Die *cudie = dwarf_offdie (dbg, old_offset + hsize, &cudie_mem);

      if (dwarf_getsrcfiles (cudie, &files, &nfiles) != 0)
	continue;

      if (dwarf_haschildren (cudie))
	{
	  jlong result = (jlong)iterate_decl(cudie, sym, nfiles);
	  if (result != 0)
	    return result;
	}
    }
  return (jlong)0;
}

jlong
lib::dwfl::DwarfDie::get_decl_cu (jlong die_p, jstring sym_p)
{
  Dwarf_Die *die = (Dwarf_Die*) die_p;
  int sym_len = sym_p->length ();
  char sym[sym_len + 1];
  JvGetStringUTFRegion (sym_p, 0, sym_len, sym);
  sym[sym_len] = '\0';
  return (jlong)iterate_decl(die, sym, 99);
}

static Dwarf_Die*
iterate_decl (Dwarf_Die *die_p, char *sym, size_t nfiles)
{
  Dwarf_Die *die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  memcpy (die, die_p, sizeof (Dwarf_Die));
  
  /* Iterate over all immediate children of the CU DIE.  */
  dwarf_child (die, die);
  do
    {
      Dwarf_Attribute attr_mem;
      Dwarf_Attribute *attr = dwarf_attr (die, DW_AT_name, &attr_mem);
      const char *name = dwarf_formstring (attr);
      // Want either a die with a name OR look at its DW_TAG_enumerators
      if (name == NULL && dwarf_tag (die) != DW_TAG_enumeration_type)
	continue;

      Dwarf_Word fileidx;
      attr = dwarf_attr (die, DW_AT_decl_file, &attr_mem);
      // DW_TAG_enumerator doesn't have a DW_AT_decl_file
      if ((dwarf_formudata (attr, &fileidx) != 0 || (size_t)fileidx >= nfiles)
	  && dwarf_tag (die) != DW_TAG_enumerator)
	continue;

      if (name)
	if (strcmp(name, sym) == 0)
	  return die;

      if (dwarf_haschildren (die))
	{
	  Dwarf_Die *result = iterate_decl (die, sym, nfiles);
	  if (result != NULL)
	    return result;
	}
    }
  while (dwarf_siblingof (die, die) == 0);
  JvFree (die);
  return NULL;
}

java::util::ArrayList *
lib::dwfl::DwarfDie::getEntryBreakpoints()
{
  Dwarf_Addr *bkpts = 0;
  int count = ::dwarf_entry_breakpoints(DWARF_DIE_POINTER, &bkpts);
  if (count > 0)
    {
      java::util::ArrayList *alist = new java::util::ArrayList();
      for (int i = 0; i < count; i++)
	{
	  alist->add(new java::lang::Long((jlong)bkpts[i]));
	}
      ::free(bkpts);
      return alist;
    }
  else
    return 0;
}

jboolean
lib::dwfl::DwarfDie::isInlineDeclaration()
{
  return dwarf_func_inline(DWARF_DIE_POINTER) != 0;
}

struct CallbackArgs
{
  java::util::ArrayList *arrayList;
  lib::dwfl::DwarfDieFactory *factory;
};

extern "C" int inlineInstanceCallback(Dwarf_Die *instance, void *arg)
{
  CallbackArgs *cbArg = static_cast<CallbackArgs *>(arg);
  if (!cbArg->arrayList)
    cbArg->arrayList = new java::util::ArrayList();
  Dwarf_Die *die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  memcpy(die, instance, sizeof(*die));
  lib::dwfl::DwarfDie *dwarfDie = cbArg->factory->makeDie((jlong)die, 0);
  dwarfDie->setManageDie(true);
  cbArg->arrayList->add(dwarfDie);
  return DWARF_CB_OK;
}

java::util::ArrayList*
lib::dwfl::DwarfDie::getInlinedInstances()
{
  CallbackArgs cbArgs = { 0, lib::dwfl::DwarfDieFactory::getFactory() };

  if (dwarf_func_inline_instances(DWARF_DIE_POINTER, inlineInstanceCallback,
				  &cbArgs) != 0)
    {
      throw new lib::dwfl::DwarfException(JvNewStringUTF("Unknown error while searching for inline instances"));
    }
  else
    return cbArgs.arrayList;
}

void
lib::dwfl::DwarfDie::finalize()
{
  if (manageDie)
    JvFree(DWARF_DIE_POINTER);
}
