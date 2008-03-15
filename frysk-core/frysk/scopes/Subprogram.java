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

/**
 * A concrete instance of a subprogram.
 */

package frysk.scopes;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

import lib.dwfl.DwAt;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.TypeFactory;
import frysk.value.ObjectDeclaration;
import frysk.value.FunctionType;
import frysk.value.Variable;

/**
 * A Subprogram refers to a concrete (not inlined) instance of a function.
 */
public class Subprogram extends Subroutine
{
  // Language language;
    FunctionType functionType;
    LinkedList parameters;
    
    private String name;
    
    public Subprogram(DwarfDie die, TypeFactory typeFactory) {
	super(die, typeFactory);
	this.name = die.getName();

	parameters = new LinkedList();
	die = die.getChild();
	while (die != null) {
	    
	    boolean artificial = die.hasAttribute(DwAt.ARTIFICIAL)
		    && die.getAttrConstant(DwAt.ARTIFICIAL) != 1;

	    if (die.getTag().equals(DwTag.FORMAL_PARAMETER) && !artificial) {
		Variable variable = new Variable(die);
		parameters.add(variable);
	    }
	    
	    die = die.getSibling();
	}
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
	super.toPrint(frame, writer, " ");
    }
    
    public ObjectDeclaration getDeclaredObjectByName(String name) {
	ObjectDeclaration objectDeclaration = null;

	Iterator iterator = this.parameters.iterator();
	while (iterator.hasNext()) {
	    ObjectDeclaration tempObjectDeclaration = (Variable) iterator.next();
	    if (tempObjectDeclaration.getName().equals(name)) {
		objectDeclaration = tempObjectDeclaration;
		continue;
	    }
	}
	
	Composite composite = this.getComposite();
	if(composite != null){
	    objectDeclaration = composite.getDeclaredObjectByName(name);
	}
	
	if(objectDeclaration == null){
	    objectDeclaration =  super.getDeclaredObjectByName(name);
	}
	
	return objectDeclaration;
    }

}
