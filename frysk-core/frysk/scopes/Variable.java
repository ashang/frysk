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

import lib.dwfl.DwException;
import lib.dwfl.DwarfDie;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.LocationExpression;
import frysk.debuginfo.PieceLocation;
import frysk.debuginfo.TypeFactory;
import frysk.debuginfo.ValueUnavailableException;
import frysk.debuginfo.VariableOptimizedOutException;
import frysk.isa.ISA;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import frysk.value.Format;
import frysk.value.ObjectDeclaration;
import frysk.value.Type;
import frysk.value.Value;

/**
 * This class contains the static information corresponding to a
 * language variable.  Given a Frame it is possible to get a Value
 * corresponding to this Variable
 */

public class Variable implements ObjectDeclaration {
    
    private static Log fine = LogFactory.fine(Variable.class);
    private static Log finest = LogFactory.finest(Variable.class);
    
    private Type type;
    private final DwarfDie variableDie;
    private final String name;
    private final LocationExpression locationExpression;
    private SourceLocation sourceLocation;
    
    public Variable(DwarfDie variableDie) {
        this.type = null;
	this.variableDie = variableDie;
	this.name = variableDie.getName();
	locationExpression = new LocationExpression(variableDie);
	sourceLocation = SourceLocationFactory.getSourceLocation(variableDie);
    }
    
    public DwarfDie getVariableDie() {
	return variableDie;
    }
    /**
     * Return the variable's name.
     */
    public String getName() {
	return name;
    }

    public Type getType(ISA isa) {
	fine.log(this, "Entering getType, ISA: ", isa);
	
	if(this.type == null){
	    TypeFactory typeFactory = new TypeFactory(isa);
	    this.type = typeFactory.getType(variableDie);
	}
	
	finest.log(this, "Leaving getType, type: ", type);
	return type;
    }
     
    public long getLineNumber() {
	return this.sourceLocation.getLine();
    }
  
    public int getColumnNumber() {
	return this.variableDie.getDeclColumn();
    }
  
    public void toPrint(PrintWriter printWriter, DebugInfoFrame frame) {
	if (this.getType(frame.getTask().getISA()) == null) {
	    // FIXME: This should just send the request to the Value's
	    // toPrint method and not try to figure out of the Type
	    // information was delt with.
	    printWriter.print("<<unhandled type>>");
	    return;
	}
	StringBuilder stringBuilder = new StringBuilder();
	type.toPrint(stringBuilder, 0);
	printWriter.print(stringBuilder);
	printWriter.print(" ");
	printWriter.print(this.getName());

    }
  
    public void printValue(PrintWriter printWriter, DebugInfoFrame frame){
	try {
	    Value value = getValue(frame);
	    value.toPrint(printWriter, frame.getTask().getMemory(),
			  Format.NATURAL, 0);
	} catch (ValueUnavailableException e) {
	    printWriter.print("< value unavailable at pc=0x"+ Long.toHexString(frame.getAdjustedAddress())+">");
	} catch (VariableOptimizedOutException e) {
	    printWriter.print("< optimized out >");
	} catch (RuntimeException e) {
	    printWriter.print("< ERROR >");
	}
    }
    
    public void printLineCol(PrintWriter printWriter) {
	printWriter.print("line#");
	try {
	    printWriter.print(this.getLineNumber());
	} catch (DwException e) {
	    printWriter.print("< error >");
	}
      
	//      printWriter.print(" col#");
	//      try {
	//	  printWriter.print(this.getColumnNumber());
	//      } catch (DwException e) {
	//	  printWriter.print("< error >");
	//      }
    }

    public Value getValue(DebugInfoFrame frame) {
	ISA isa = frame.getTask().getISA();
	PieceLocation pieceLocation
	    = new PieceLocation(locationExpression.decode(frame, this.getType(isa)
							  .getSize()));
	Value value = new Value(this.getType(isa), pieceLocation);
	return value;
    }
    
    public SourceLocation getSourceLocation() {
	return this.sourceLocation;
    }
}
