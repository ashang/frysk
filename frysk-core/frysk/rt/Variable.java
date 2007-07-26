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

import java.io.PrintWriter;

import javax.naming.NameNotFoundException;

import lib.dwfl.DwarfDie;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.ValueUavailableException;
import frysk.debuginfo.VariableOptimizedOutException;
import frysk.stack.Frame;
import frysk.value.Type;
import frysk.value.Value;

/**
 * This class contains the static information corresponding to a language variable.
 * Given a Frame it is possible to get a Value corresponding to this Variable
 */
public class Variable
{

  private Value variable;
  private DwarfDie variableDie;
  private Type type;
  private DwarfDie typeDie;
  
  public Variable(Value variable, DwarfDie variableDie){
   this.variable = variable;
   this.variableDie = variableDie;
   if(variable != null){
       this.type = variable.getType();
   }
   if(variableDie == null){
       throw new IllegalArgumentException();
   }
//   this.typeDie =  typeDie;
  }
  public void setVariable (Value variable)
  {
    this.variable = variable;
  }
  public Value getVariable ()
  {
    return variable;
  }
  public void setVariableDie (DwarfDie variableDie)
  {
      if(variableDie == null){
	       throw new IllegalArgumentException();
	   }
    this.variableDie = variableDie;
  }
  public DwarfDie getVariableDie ()
  {
    return variableDie;
  }
  public void setType (Type type)
  {
    this.type = type;
  }
  public Type getType ()
  {
    return type;
  }
  public void setTypeDie (DwarfDie typeDie)
  {
    this.typeDie = typeDie;
  }
  public DwarfDie getTypeDie ()
  {
    return typeDie;
  }

  public void toPrint(PrintWriter printWriter, Frame frame){
      printWriter.print(this.getType() + " " + this.getVariable().getText() + " = ");
      try{
	  Value value = getValue(frame);
	  printWriter.print(value.toString());
      }
      catch (ValueUavailableException e) {
	  printWriter.print("< value unavailable at pc=0x"+ Long.toHexString(frame.getAdjustedAddress())+">");
      }
      catch (VariableOptimizedOutException e) {
	  printWriter.print("< optimized out >");
      }
      catch (RuntimeException e) {
	  printWriter.print("< ERROR >");
      }
  }
  
  public Value getValue(Frame frame)
  {
      DebugInfo debugInfo = new DebugInfo(frame);
      try {
	return debugInfo.get(frame, getVariableDie());
    } catch (NameNotFoundException e) {
	throw new RuntimeException(e);
    }
  }
}
