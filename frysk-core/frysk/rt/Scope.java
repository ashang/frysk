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

package frysk.rt;

import lib.dw.DwarfDie;
import frysk.value.Type;
import frysk.value.Value;

/**
 * A class to represent a Scope.
 * Here are the scopes described by Dwarf debug info:
 * compile_unit
 * module
 * lexical_block
 * with_stmt (pascal Modula2)
 * catch_block
 * try_block
 * entry_point
 * inlined_subroutine
 * subprogram
 * namespace
 * imported_unit
 * 
 * The most important property of a scope is that it can own variables
 * and this object will give you acess to them.
 */
public class Scope
{
  LexicalBlock outer;
  Value[] variables;
  DwarfDie[] variableDies;
  Type[] types;
  DwarfDie[] typeDies;

  public Value[] getVariables ()
  {
    return variables;
  }

  public void setVariables (int n)
  {
    this.variables = new Value[n];
  }

  public DwarfDie[] getVariableDies ()
  {
    return variableDies;
  }

  public void setVariableDies (int n)
  {
    this.variableDies = new DwarfDie[n];
  }

  public DwarfDie[] getTypeDies ()
  {
    return typeDies;
  }

  public void setTypeDies (int n)
  {
    this.typeDies = new DwarfDie[n];
  }

}
