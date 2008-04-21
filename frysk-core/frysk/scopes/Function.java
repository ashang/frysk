// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.scopes;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.LocationExpression;
import frysk.debuginfo.PieceLocation;
import frysk.debuginfo.TypeFactory;
import frysk.isa.ISA;
import frysk.value.FunctionType;
import frysk.value.ObjectDeclaration;
import frysk.value.Type;
import frysk.value.Value;
import lib.dwfl.DwAt;
import lib.dwfl.DwInl;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;

/**
 * In DWARF a subroutine is used to refer to an entity that can either be a
 * concrete function (Subprogram) or an inlined function (InlinedSubprogram).
 */
public class Function extends NamedScope {

    Composite struct;
    Type type;
    private LocationExpression locationExpression;

    FunctionType functionType;
    LinkedList parameters;
    
    public Function(DwarfDie die, TypeFactory typeFactory) {
	super(die, typeFactory);
	this.type = typeFactory.getType(die);
	locationExpression = new LocationExpression(die);
	
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

    public LinkedList getParameters ()
    {
      return parameters;
    }

    public void setFunctionType (FunctionType functionType)
    {
      this.functionType = functionType;
    }
    
    public void printScopes(PrintWriter writer, DebugInfoFrame frame){
	super.toPrint(frame, writer, " ");
    }

    public FunctionType getFunctionType ()
    {
      return functionType;
    }

    public String toString()
    {
      return super.toString() + " " + this.getName();
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

    /**
     * Returns the structure that this subroutine belongs to. If this
     * subroutine does not belong to any structs/classes it returns null.
     * 
     * @return Struct containing this Subroutine or null
     */
    public Composite getComposite() {
	if (struct == null) {
	    DwarfDie die = this.getDie().getOriginalDie();
	    if (die == null) {
		die = this.getDie();
	    }

	    DwarfDie[] scopes = die.getScopesDie();
	    for (int i = 0; i < scopes.length; i++) {
		if (scopes[i].getTag().equals(DwTag.STRUCTURE_TYPE)) {
		    this.struct = new Composite(scopes[i], typeFactory);
		}
	    }
	}
	return struct;
    }

    /**
     * returns true if:
     * - this is a concrete instance of an inlined function
     * - this is an abstrace instance of an inlinable function
     * - a regular funciton which has been inlined by the compiler
     */
    public boolean isInlined(){
	
	DwTag dwTag = getDie().getTag();
	
	// Concrete inlined instance
	if(dwTag.equals(DwTag.INLINED_SUBROUTINE)){
	    return true;
	}
	
	long inlineAttribute = getDie().getAttrConstant(DwAt.INLINE);
	
	// Declared inlined and inlined by compiler
	// but has DW_TAG_SUBPROGRAM
	// this is an abstract instance of an inlineable function  
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == DwInl.DECLARED_INLINED_){
	    return true;
	}
	
	// Declared regular and inlined by compiler
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == DwInl.INLINED_){
	    return true;
	}
	
	// Declared regular and not inlined by compiler
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == DwInl.NOT_INLINED_){
	    return false;
	}

	// Declared regular and does not have an inline attribute
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == -1){
	    return false;
	}

	throw new RuntimeException("Unhandled case DwTag: " + dwTag + " inline attribute " + inlineAttribute);
    }

    public Type getType(ISA isa) {
	return this.type;
    }

    public Value getValue(DebugInfoFrame frame) {
	ISA isa = frame.getTask().getISA();
	PieceLocation pieceLocation
	    = new PieceLocation(locationExpression.decode(frame, this.getType(isa)
							  .getSize()));
	Value value = new Value(this.getType(isa), pieceLocation);
	return value;

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
    
}
