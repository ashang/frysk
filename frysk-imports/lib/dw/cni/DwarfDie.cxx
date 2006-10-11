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

#include "lib/dw/DwarfDie.h"

#define DW_OP_fbreg 0x91
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
	return JvNewStringUTF(dwarf_diename(DWARF_DIE_POINTER));
}

jstring
lib::dw::DwarfDie::get_decl_file(jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  return JvNewStringLatin1 (dwarf_decl_file (die));
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
      longp[1] = (jlong)dies[code]; // Die for scope
    }
  else if (dwarf_tag (var_die) != DW_TAG_variable)
    return -1;
  return code;
}

void
lib::dw::DwarfDie::get_addr (jlongArray fbreg_and_disp, jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  size_t fb_len;
  
  jlong* longp = elements(fbreg_and_disp);
  longp[0] = -1;
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr))
    {
      dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      longp[0] = fb_expr[0].atom;
      longp[1] = fb_expr[0].number;
    }
}

jstring
lib::dw::DwarfDie::get_type (jlong var_die)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Die *type_die, type_mem_die;
  Dwarf_Attribute type_attr;
  
  if (dwarf_attr_integrate (die, DW_AT_type, &type_attr))
    {
      type_die = dwarf_formref_die (&type_attr, &type_mem_die);
      if (dwarf_tag (type_die) == DW_TAG_base_type)
	{
	  return JvNewStringLatin1
	    (dwarf_formstring (dwarf_attr_integrate
			       (type_die, DW_AT_name, &type_attr)));
	}
    }
  return 0;
}

#define GETREGNO(r) (r == DW_OP_breg0) ? 0 : (r == DW_OP_breg1) ? 1     \
  : (r == DW_OP_breg2) ? 2 : (r == DW_OP_breg3) ? 3 : (r == DW_OP_breg4) ? 4 \
  : (r == DW_OP_breg5) ? 5 : (r == DW_OP_breg6) ? 6 : (r == DW_OP_breg7) ? 7 \
  : (r == DW_OP_reg0) ? 0 : (r == DW_OP_reg1) ? 1			\
  : (r == DW_OP_reg2) ? 2 : (r == DW_OP_reg3) ? 3 : (r == DW_OP_reg4) ? 4 \
  : (r == DW_OP_reg5) ? 5 : (r == DW_OP_reg6) ? 6 : (r == DW_OP_reg7) ? 7 : 7


void
lib::dw::DwarfDie::get_framebase (jlongArray fbreg_and_disp, jlong var_die,
				  jlong scope_arg, jlong pc)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  Dwarf_Attribute *fb_attr;
  size_t fb_len;
  
  jlong* longp = elements(fbreg_and_disp);
  longp[0] = -1;
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr))
    {
      code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      if (fb_expr[0].atom != DW_OP_fbreg)
	return;

      fb_attr = dwarf_attr_integrate ((Dwarf_Die*) scope_arg,
				      DW_AT_frame_base,
				      &loc_attr);

      code = (dwarf_getlocation_addr (fb_attr, pc, &fb_expr, &fb_len, 1));
      if (code != 1 || fb_len <= 0)
	return;
      longp[0] = GETREGNO (fb_expr[0].atom);
      longp[1] = fb_expr[0].number;
      return;
    }
}

void
lib::dw::DwarfDie::get_formdata (jlongArray fbreg_and_disp, jlong var_die,
				  jlong scope_arg, jlong pc)
{
  Dwarf_Die *die = (Dwarf_Die*) var_die;
  Dwarf_Attribute loc_attr;
  Dwarf_Op *fb_expr;
  int code;
  Dwarf_Attribute *fb_attr;
  size_t fb_len;
  
  jlong* longp = elements(fbreg_and_disp);
  longp[0] = -1;
  if (dwarf_attr_integrate (die, DW_AT_location, &loc_attr))
    {
      code = dwarf_getlocation (&loc_attr, &fb_expr, &fb_len);
      if (code == -1)		// block form
	{
	  fb_attr = dwarf_attr_integrate (die, DW_AT_location, &loc_attr);
	  code = (dwarf_getlocation_addr (fb_attr, pc, &fb_expr, &fb_len, 1));
	  if (code > 0)
	    {
	      longp[0] = GETREGNO (fb_expr[0].atom);
	      longp[1] = fb_expr[0].number;
	      return;
	    }
	  else
	    return;
	}
      return;
    }
}

/*
 * Returns true if this die has the DW_TAG_inlined_subroutine tag
 */
jboolean
lib::dw::DwarfDie::is_inline_func ()
{
	return dwarf_tag(DWARF_DIE_POINTER) == DW_TAG_inlined_subroutine;
}
