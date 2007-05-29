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

/**
 * A concrete instance of a subprogram.
 */

package frysk.rt;

import java.util.Iterator;
import java.util.LinkedList;

import lib.dw.DwTagEncodings;
import lib.dw.DwarfDie;
import frysk.debuginfo.DebugInfo;
import frysk.value.Value;

public class Subprogram
{
  // Language language;
    Subprogram outer;
    LexicalBlock block;
    Value[] parameters;
    
    private String name;

    public Subprogram (DwarfDie die, DebugInfo debugInfo)
    {
      this.name = die.getName();
//      System.out.println("\nSubprogram.Subprogram() name: " + name + " " + DwTagEncodings.toName(die.getTag()));
      
      LinkedList parameters = new LinkedList();
      die = die.getChild();
      while(die != null){
//	System.out.print(" -> " + die.getName() + ": "+ DwTagEncodings.toName(die.getTag()));
	if(die.getTag() == DwTagEncodings.DW_TAG_formal_parameter_){
	  Value value = debugInfo.getVariable(die);
	  parameters.add(value);
	}
	die = die.getSibling();
      }
//      System.out.println("\nSubprogram.Subprogram()");
      //XXX: this is temporary, this.parameters should
      //     be converted to a LinkedList
      this.parameters = new Value[parameters.size()];
      Iterator iterator = parameters.iterator();
      int i = 0;
      while(iterator.hasNext()){
	Value value = (Value) iterator.next();
	this.parameters[i++] = value;
      }
    }

    public Subprogram ()
    {
      
    }

    public String getName(){
      return this.name;
    }
    
    public LexicalBlock getBlock ()
    {
      return block;
    }

    public void setBlock (LexicalBlock block)
    {
      this.block = block;
    }

    public Value[] getParameters ()
    {
      return parameters;
    }

    public void setParameters (int n)
    {
      this.parameters = new Value[n];
    }
    
    public String toString ()
    {
//      String string;
//      string = this.getName() + "(";
//      for (int i = 0; i < this.parameters.length; i++) {
//	string += parameters[i].getType() + " " + parameters[i].getText();
//	if(i < (this.parameters.length-1)){
//	  string += ",";
//	}
//      }
//      string += ")";
//      return string;
      
      return this.getName();
    }
}
