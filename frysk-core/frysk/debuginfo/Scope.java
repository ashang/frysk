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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import lib.dwfl.DwTagEncodings;
import lib.dwfl.DwarfDie;
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
 * and this object will give you access to them.
 */
public class Scope
{
  
    Scope outer;
    
    LinkedList scopes;
  
    LinkedList variables;
    
  public Scope(DwarfDie die, DebugInfo debugInfo){
    this.variables = new LinkedList();
    this.scopes = new LinkedList();
    
//    System.out.println("\nScope.Scope() name: " + die.getName() + " " + DwTagEncodings.toName(die.getTag()));
    
    die = die.getChild();
    
    while(die != null){
//      System.out.println(" -> " + die.getName() + ": "+ DwTagEncodings.toName(die.getTag()));
      
      if(die.getTag() == DwTagEncodings.DW_TAG_variable_){
        Value value = debugInfo.getValue(die);
        Variable variable = new Variable(value, die);
        variables.add(variable);
      }
      
      if(die.getTag() == DwTagEncodings.DW_TAG_lexical_block_){
        this.scopes.add(new LexicalBlock(die, debugInfo));
      }else{
        if(isScopeDie(die)){
          this.scopes.add(new Scope(die,debugInfo));
        }
      }
      die = die.getSibling();
    }
    
  }
  
  public LinkedList getScopes(){
    return this.scopes;
  }
  
  public Scope(){
    
  }
  
  public LinkedList getVariables ()
  {
    return variables;
  }

  public static boolean isScopeDie(DwarfDie die){
    switch (die.getTag())
      {
      case DwTagEncodings.DW_TAG_compile_unit_:
      case DwTagEncodings.DW_TAG_module_:
      case DwTagEncodings.DW_TAG_lexical_block_:
      case DwTagEncodings.DW_TAG_with_stmt_:
      case DwTagEncodings.DW_TAG_catch_block_:
      case DwTagEncodings.DW_TAG_try_block_:
      case DwTagEncodings.DW_TAG_entry_point_:
      case DwTagEncodings.DW_TAG_inlined_subroutine_:
      case DwTagEncodings.DW_TAG_subprogram_:
      case DwTagEncodings.DW_TAG_namespace_:
      case DwTagEncodings.DW_TAG_imported_unit_:
        return true;
      default:
        return false;
      }
  }
  

  public void toPrint(DebugInfoFrame frame, PrintWriter writer, int indent){
  
    char[] indentArray = new char[indent];
    Arrays.fill(indentArray, ' ');
    String indentString = new String(indentArray);
    
    Iterator iterator = this.variables.iterator();
    while(iterator.hasNext()){
	Variable variable = (Variable) iterator.next();
	writer.println();
	writer.print(indentString);
	variable.toPrint(writer, frame);
	writer.print(" ");
	variable.printLineCol(writer);
	writer.flush();
    }
    
    iterator = this.getScopes().iterator();
    while(iterator.hasNext()){
      Scope scope = (Scope) iterator.next();
      scope.toPrint(frame, writer, indent+1);
    }
    
  }
}
