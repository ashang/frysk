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

package frysk.debuginfo;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;

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
 * and this object will give you access to them.
 */
public class Scope
{
  
    private final DwarfDie die;
    private Scope outer;
    private Scope inner;
    
    private LinkedList scopes;
  
    LinkedList variables;

    LinkedList collections;
    
  public Scope(DwarfDie die){
      this.die = die;
      this.scopes = new LinkedList();
  }
  
  public Scope getOuter(){
      return this.outer;
  }
  
  public Scope getInner(){
      return this.inner;
  }
  
  public void setOuter(Scope outer){
      this.outer = outer;
      outer.inner = this;
  }
  
  public LinkedList getScopes(){
    return this.scopes;
  }
  
  protected DwarfDie getDie(){
      return this.die;
  }
  
  public LinkedList getVariables() {
      if (this.variables == null) {
	  this.variables = new LinkedList();
	  DwarfDie die = this.die.getChild();
	  
	  while (die != null) {
	      
	      if (die.getTag().equals(DwTag.VARIABLE)) {
		  Variable variable = new Variable(die);
		  variables.add(variable);
	      }
	      die = die.getSibling();
	  }
      }
      return variables;
  }

  public LinkedList getEnums(){
      if(this.collections == null){
	  this.collections = new LinkedList();
	  DwarfDie die = this.die.getChild();
	    
	  while(die != null){
		      
	      if(die.getTag().equals(DwTag.ENUMERATION_TYPE)){
		  this.collections.add(new Enumiration(die));
	      }

	      die = die.getSibling();
	  }
      }
      return collections;
  }
  
  public static boolean isScopeDie(DwarfDie die){
    switch (die.getTag().hashCode())
      {
      case DwTag.COMPILE_UNIT_:
      case DwTag.MODULE_:
      case DwTag.LEXICAL_BLOCK_:
      case DwTag.WITH_STMT_:
      case DwTag.CATCH_BLOCK_:
      case DwTag.TRY_BLOCK_:
      case DwTag.ENTRY_POINT_:
      case DwTag.INLINED_SUBROUTINE_:
      case DwTag.SUBPROGRAM_:
      case DwTag.NAMESPACE_:
      case DwTag.IMPORTED_UNIT_:
        return true;
      default:
        return false;
      }
  }
  
  public Variable getVariableByName(String name){
      Variable variable = null;
      
      Iterator iterator = this.getVariables().iterator();
      while (iterator.hasNext()) {
	variable = (Variable) iterator.next();
	if(variable.getName().equals(name)){
	    return variable;
	}
      }
      
      iterator = this.getEnums().iterator();
      while (iterator.hasNext()) {
	Enumiration enumiration = (Enumiration) iterator.next();
	variable = enumiration.getVariableByName(name);
	
	if(variable != null){
	    return variable;
	}
      }
      
      
      return null;
  }
  
  public void toPrint(DebugInfoFrame frame, PrintWriter writer, String indentString){
  
    Iterator iterator = this.getVariables().iterator();
    while(iterator.hasNext()){
	Variable variable = (Variable) iterator.next();
	writer.println();
	writer.print(indentString + " ");
	variable.toPrint(writer, frame);
	writer.print(" ");
	variable.printLineCol(writer);
	writer.flush();
    }
    writer.println();

  }
  
}
