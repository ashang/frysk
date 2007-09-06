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

package frysk.debuginfo;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import frysk.value.FunctionType;

/**
 * A Subprogram refers to a concrete (not inlined) instance of a function.
 */
public class Subprogram extends Subroutine
{
  // Language language;
    FunctionType functionType;
    LinkedList parameters;
    
    private String name;
    
    public Subprogram (DwarfDie die)
    {
      super(die);
      this.name = die.getName();
      
//      System.out.println("\nSubprogram.Subprogram() name: " + name + " " + DwTag.toName(die.getTag()));
      
      parameters = new LinkedList();
      die = die.getChild();
      while(die != null){
//	System.out.print(" -> " + die.getName() + ": "+ DwTag.toName(die.getTag()));
	if(die.getTag() == DwTag.FORMAL_PARAMETER_){
          Variable variable = new Variable(die);
	  parameters.add(variable);
	}
	die = die.getSibling();
      }
    }

    public Subprogram ()
    {
      super();
      parameters = new LinkedList();
    }

    public String getName(){
      return this.name;
    }
    
    public LinkedList getParameters ()
    {
      return parameters;
    }

    public String toString()
    {
      return super.toString() + " " + this.getName();
    }

    public FunctionType getFunctionType ()
    {
      return functionType;
    }

    public void setFunctionType (FunctionType functionType)
    {
      this.functionType = functionType;
    }
    
    public void printParameters (PrintWriter writer, DebugInfoFrame frame)
    {
      
      Iterator iterator = this.parameters.iterator();
      while(iterator.hasNext()) {
        Variable parameter = (Variable) iterator.next();
	parameter.toPrint(writer, frame);
	writer.flush();
        if(parameters.indexOf(parameter) < (this.parameters.size()-1)){
            writer.print(",");
        }
      }
      
    }
    public void printScopes(PrintWriter writer, DebugInfoFrame frame){
	super.toPrint(frame, writer, 1);
    }
}
