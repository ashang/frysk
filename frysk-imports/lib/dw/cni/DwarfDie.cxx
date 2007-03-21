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

#include "lib/dw/DwarfDie.h"
#include "lib/dw/BaseTypes.h"

#define DWARF_DIE_POINTER (Dwarf_Die *) this->pointer

jlong
lib::dw::DwarfDie::get_lowpc()
{
	Dwarf_Addr lowpc;
	::dwarf_lowpc(DWARF_DIE_POINTER, &lowpc);
	return (jlong) lowpc;
}

jlong
lib::dw::DwarfDie::get_highpc()
{
	Dwarf_Addr highpc;
	::dwarf_highpc(DWARF_DIE_POINTER, &highpc);
	return (jlong) highpc;
}

jstring
lib::dw::DwarfDie::get_diename()
{
  const char *name = dwarf_diename (DWARF_DIE_POINTER);
  if (name != NULL)
    return JvNewStringUTF (name);
  else
    return JvNewStringUTF ("");
}

jstring
lib::dw::DwarfDie::get_decl_file(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  const char *name = dwarf_decl_file (die);
  if (name != NULL)
    return JvNewStringLatin1 (name);
  else
    return JvNewStringLatin1 ("");
}

jlong
lib::dw::DwarfDie::get_decl_line(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  int lineno;
  dwarf_decl_line (die, &lineno);
  return lineno;
}

jlongArray
lib::dw::DwarfDie::get_scopes(jlong addr)
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

Dwarf_Die *var_die;

jlong
lib::dw::DwarfDie::get_scopevar (jlongArray die_scope, jlongArray scopes,
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

jlong
lib::dw::DwarfDie::get_scopevar_names (jlongArray scopes_arg,
				 jstring variable_arg)
{
  int nscopes = JvGetArrayLength (scopes_arg);
  Dwarf_Die *scopes[nscopes];
  jlong* scopesp = elements(scopes_arg);
  Dwarf_Die result;
  int variable_len = variable_arg->length ();
  char variable[variable_len + 1];
  JvGetStringUTFRegion (variable_arg, 0, variable_len, variable);
  variable[variable_len] = '\0';

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
		addScopeVarName (JvNewStringUTF (diename));
	      }
	  }
	while (dwarf_siblingof (&result, &result) == 0);
      }
  return 0;
}

void
lib::dw::DwarfDie::get_addr (jlong var_die, jlong pc)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr))
    {
      int i = 0;
      int nlocs;
      if (pc == 0)
	nlocs = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      else
	nlocs = dwarf_getlocation_addr (&loc_attr, pc, &fb_expr, &fb_len, 1);
      if (nlocs >= 0)
	do 
	  {
	    addOps (fb_expr[i].atom, fb_expr[i].number, fb_expr[i].number2,
		    fb_expr[i].offset);
	    i += 1;
	  }
	while (i < nlocs);
    }
}

jlong
lib::dw::DwarfDie::get_type (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * type_mem_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  Dwarf_Attribute type_attr;
  
  if (dwarf_attr_integrate (die, DW_AT_type, &type_attr))
    {
      if (dwarf_formref_die (&type_attr, type_mem_die))
	if (dwarf_tag (type_mem_die) == DW_TAG_typedef)
	  {
	    dwarf_attr_integrate (type_mem_die, DW_AT_type, &type_attr);
	    dwarf_formref_die (&type_attr, type_mem_die);
	  }
      return (jlong) type_mem_die;
    }
  return 0;
}

jlong
lib::dw::DwarfDie::get_child (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * child_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  
  if (dwarf_child (die, child_die) == 0)
    return (jlong)child_die;
  return 0;
}

jlong
lib::dw::DwarfDie::get_sibling (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die * sibling_die = (Dwarf_Die*)JvMalloc(sizeof(Dwarf_Die));
  
  if (dwarf_siblingof (die, sibling_die) == 0)
    return (jlong)sibling_die;
  return 0;
}

jint
lib::dw::DwarfDie::get_base_type (jlong var_die)
{
  Dwarf_Die *type_die = (Dwarf_Die*) var_die;
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
	    case DW_ATE_signed_char: return lib::dw::BaseTypes::baseTypeChar;
	    case DW_ATE_unsigned_char: return lib::dw::BaseTypes::baseTypeUnsignedChar;
	    }
	case 2:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dw::BaseTypes::baseTypeShort;
	    case DW_ATE_unsigned: return lib::dw::BaseTypes::baseTypeUnsignedShort;
	    case DW_ATE_unsigned_char: return lib::dw::BaseTypes::baseTypeUnicode;
	    }
	case 4:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dw::BaseTypes::baseTypeInteger;
	    case DW_ATE_unsigned: return lib::dw::BaseTypes::baseTypeUnsignedInteger;
	    case DW_ATE_float: return lib::dw::BaseTypes::baseTypeFloat;
	    }
	case 8:
	  switch (encoding)
	    {
	    case DW_ATE_signed: return lib::dw::BaseTypes::baseTypeLong;
	    case DW_ATE_unsigned: return lib::dw::BaseTypes::baseTypeUnsignedLong;
	    case DW_ATE_float: return lib::dw::BaseTypes::baseTypeDouble;
	    }
	}
    }
  return 0;
}

jint
lib::dw::DwarfDie::get_upper_bound (jlong var_die)
{
  Dwarf_Die *type_die = (Dwarf_Die*) var_die;
  Dwarf_Word byte_size;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (type_die, DW_AT_upper_bound, &type_attr))
    {
      dwarf_formudata (&type_attr, &byte_size);
      return byte_size;
    }
  return -1;
}
  
jint
lib::dw::DwarfDie::get_tag (jlong die_p)
{
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  return dwarf_tag (die);
}

jboolean
lib::dw::DwarfDie::get_attr (jlong die_p, jint attr)
{
  Dwarf_Die *die = (Dwarf_Die*)die_p;
  Dwarf_Attribute type_attr;
  if (dwarf_attr_integrate (die, attr, &type_attr))
    return 1;
  else
    return 0;
}

void
lib::dw::DwarfDie::get_framebase (jlong var_die, jlong scope_arg, jlong pc)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  Dwarf_Attribute *fb_attr;
  size_t fb_len;
  
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr) >= 0)
    {
      int i = 0;
      code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
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
	while (i < code);
      return;
    }
}


jlong
lib::dw::DwarfDie::get_data_member_location (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  size_t fb_len;
  jlong disp = -1;
  
  if (dwarf_attr_integrate (die, DW_AT_data_member_location, &loc_attr) >= 0)
    {
      code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      if (fb_len > 0 && fb_expr[0].atom == DW_OP_plus_uconst)
	disp = fb_expr[0].number;
    }
  return disp;
}

/*
 * Returns true if this die has the DW_TAG_inlined_subroutine tag
 */
jboolean
lib::dw::DwarfDie::is_inline_func ()
{
	return dwarf_tag(DWARF_DIE_POINTER) == DW_TAG_inlined_subroutine;
}

/*
 * Return die for static symbol SYM_P in DBG_P
 */

static Dwarf_Die* iterate_decl (Dwarf_Die*, char*, size_t);

jlong
lib::dw::DwarfDie::get_decl (jlong dbg_p, jstring sym_p)
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
      if (name == NULL)
	continue;

      Dwarf_Word fileidx;
      attr = dwarf_attr (die, DW_AT_decl_file, &attr_mem);
      if (dwarf_formudata (attr, &fileidx) != 0 || (size_t)fileidx >= nfiles)
	continue;

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
lib::dw::DwarfDie::getEntryBreakpoints()
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
