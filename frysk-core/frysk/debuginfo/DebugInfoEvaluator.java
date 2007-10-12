// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import frysk.dwfl.DwflCache;
import frysk.expr.ExprSymTab;
import frysk.isa.Register;
import frysk.isa.Registers;
import frysk.isa.RegistersFactory;
import frysk.proc.Task;
import frysk.scopes.Subprogram;
import frysk.value.ArrayType;
import frysk.value.GccStructOrClassType;
import frysk.value.Type;
import frysk.value.UnknownType;
import frysk.value.Value;
import frysk.value.Variable;

class DebugInfoEvaluator
    implements ExprSymTab
{
    private Task task;
    private final DebugInfoFrame frame;
  
    /**
     * Create an DebugInfoEvaluator object which is the interface between
     * DebugInfo and CppTreeParser, the expression parser.
     * 
     * @param frame StackFrame
     * @param task_p Task
     * @param pid_p Pid
     */
    DebugInfoEvaluator (DebugInfoFrame frame) {
	this.frame = frame;
	this.task = frame.getTask();
    }

    /**
     * @param s Symbol s
     * @return The die for symbol s
     */
    public Variable getVariable (String s) throws NameNotFoundException {
	Dwfl dwfl;
	DwarfDie[] allDies;
	Variable variable;
	long pc = this.frame.getAdjustedAddress();

	dwfl = DwflCache.getDwfl(task);
	DwflDieBias bias = dwfl.getDie(pc);
	if (bias == null)
	    return null;
	DwarfDie die = bias.die;

 	allDies = die.getScopes(pc - bias.bias);
	Subprogram b = frame.getSubprogram();
	LinkedList vars = b.getVariables();

 	Iterator iterator = vars.iterator();
 	while (iterator.hasNext()) {
 	    variable = (Variable) iterator.next();
 	    if (variable.getName() != null && variable.getName().compareTo(s) == 0)
 		{
 		    variable.getVariableDie().setScopes(allDies);
 		    return variable;
 		}
 	}

	// Do we have something above didn't find, e.g. a static symbol?
        DwarfDie varDie = die.getScopeVar(allDies, s);
	// Do we have something above didn't find, e.g. DW_TAG_enumerator?
	if (varDie == null)
            // e.g. DW_TAG_enumerator 
	    varDie = DwarfDie.getDeclCU(allDies, s);
	if (varDie == null)
            throw new NameNotFoundException();
        variable = new Variable(varDie);
        return variable;
    }

    /**
     * @return Value for symbol s in frame f
     */
    public Value getValue (String s) throws NameNotFoundException {
	if (s.charAt(0) == '$') {
	    Registers regs = RegistersFactory.getRegisters(frame.getTask()
							   .getISA());
	    String regName = s.substring(1).trim();
	    Register reg = regs.getRegister(regName);
	    if (reg == null) {
		throw new RuntimeException("unknown register: " + regName);
	    }
	    List pieces = new LinkedList();
	    pieces.add(new RegisterPiece(reg, reg.getType().getSize()));
	    return new Value(reg.getType(), new PieceLocation(pieces));
	}

	Variable var = getVariable(s);
	return var.getValue(frame);
    }
  
    /**
     * @return Value associated with the given Variable.
     */
    public Value getValue(Variable var) {
	if (var == null)
	    return (null);
	return var.getValue(frame);
    }

    /**
     * @return Type associated with the given Variable.
     */
    public Type getType(Variable var) {
	if (var == null)
	    return (null);
	return var.getType(frame.getTask().getISA());
    }

    /**
     * Returns the value of symbol which is defined by components.
     * @param f The frame containing the symbol
     * @param components Token list of members and indices, e.g. given a.b.c[1][2]
     * {a,b,c,1,1,2,2}
     * @return Value of the symbol
     */
    public Value getValueFIXME (ArrayList components) throws NameNotFoundException {
	String s = (String)components.get(0);
	Variable variable = getVariable(s);
	if (variable == null)
	    return (null);

	Value v = getValue(s);
	if (v.getType() instanceof ArrayType)
	    return ((ArrayType)v.getType()).get(v, 1, components);
	else if (v.getType() instanceof GccStructOrClassType)
	    return ((GccStructOrClassType)v.getType()).get(v, 0, components);
	else
	    return new Value(new UnknownType(variable.getName()));
    }
    
    public ByteOrder order()
    {
	return task.getISA().order();
    }
    
    public ByteBuffer taskMemory()
    {
	return task.getMemory();
    }
}
