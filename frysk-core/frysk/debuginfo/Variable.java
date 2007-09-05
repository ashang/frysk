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
import javax.naming.NameNotFoundException;
import lib.dwfl.DwException;
import lib.dwfl.DwarfDie;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.Format;

/**
 * This class contains the static information corresponding to a
 * language variable.  Given a Frame it is possible to get a Value
 * corresponding to this Variable
 */

public class Variable {
    private Type type;
    private final DwarfDie variableDie;
    private DebugInfo debugInfo;
    private final String name;
  
    public Variable(DebugInfo debugInfo, DwarfDie variableDie) {
	this.debugInfo = debugInfo;
	this.type = null;
	this.variableDie = variableDie;
	this.name = variableDie.getName();
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
    public Type getType() {
	if(this.type == null){
	    this.type = debugInfo.getType(variableDie);
	}
	return type;
    }
    
    public long getLineNumber() {
	return this.variableDie.getDeclLine();
    }
  
    public int getColumnNumber() {
	return this.variableDie.getDeclColumn();
    }
  
    public void toPrint(PrintWriter printWriter, DebugInfoFrame frame) {
	if (this.getType() == null) {
	    // FIXME: This should just send the request to the Value's
	    // toPrint method and not try to figure out of the Type
	    // information was delt with.
	    printWriter.print("<<unhandled type>>");
	    return;
	}
	type.toPrint(printWriter);
	printWriter.print(" ");
	printWriter.print(this.getName());
	printWriter.print(" = ");
	try {
	    Value value = getValue(frame);
	    value.toPrint(printWriter, frame.getTask().getMemory(),
			  Format.NATURAL);
	} catch (ValueUavailableException e) {
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
	DebugInfo debugInfo = new DebugInfo(frame);
	try {
	    return debugInfo.get(frame, getVariableDie());
	} catch (NameNotFoundException e) {
	    throw new RuntimeException(e);
	}
    }
}
