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

import frysk.dwfl.DwflCache;
import java.util.Iterator;
import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import lib.dwfl.DwarfDie;
import java.util.LinkedList;
import java.util.List;
import frysk.expr.ExprSymTab;
import frysk.isa.ISA;
import frysk.isa.Register;
import frysk.isa.Registers;
import frysk.isa.RegistersFactory;
import frysk.proc.Task;
import frysk.scopes.Scope;
import frysk.value.ObjectDeclaration;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.Variable;

/**
 * This engine implements the c++ scoping rules and uses when searching for
 * a variable by name:
 * 1. Search the inner-most scope containing the current pc indeicated by the
 * given frame, and return the first encounter. 
 *
 */
public class ObjectDeclarationSearchEngine implements ExprSymTab{

    private final DebugInfoFrame frame;
    private final ISA isa;
    private final Task task;

    public ObjectDeclarationSearchEngine(DebugInfoFrame frame) {
	this.frame = frame;
	this.isa = frame.getTask().getISA();
	this.task = frame.getTask();
    }
    
    public ObjectDeclaration getVariable(String name){
	ObjectDeclaration declaredObject = null;
	
	Scope scope = frame.getScopes();
	
	while(scope != null){
	    declaredObject = scope.getDeclaredObjectByName(name);
	    if(declaredObject != null){
		return declaredObject;
	    }
	    scope = scope.getOuter();
	}
	
	return null;
    }

    public Type getType(Variable variable) {
	return variable.getType(isa);
    }

    public Value getValue(String s) {
	if (s.charAt(0) == '$') {
	    Registers regs = RegistersFactory.getRegisters(frame.getTask()
							   .getISA());
	    String regName = s.substring(1).trim();
	    Register reg = regs.getRegister(regName);
	    if (reg == null) {
		throw new RuntimeException("unknown register: " + regName);
	    }
	    List pieces = new LinkedList();
	    pieces.add(new RegisterPiece(reg, reg.getType().getSize(), frame));
	    return new Value(reg.getType(), new PieceLocation(pieces));
	}
	
	ObjectDeclaration objectDeclaration = this.getVariable(s);
	return objectDeclaration.getValue(frame);
    }

    public Value getValue(Variable v) {
	return v.getValue(frame);
    }

    public ByteOrder order()
    {
	return task.getISA().order();
    }
    
    public ByteBuffer taskMemory()
    {
	return task.getMemory();
    }
    
    public int getWordSize()
    {
	return task.getISA().wordSize();
    }

    /**
     * XXX: Who knows if this works; it is certainly not implemented
     * correctly as it should use the ObjectDeclaration.
     */
    public void complete(String incomplete, List candidates) {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDieBias bias = dwfl.getCompilationUnit(pc);
	DwarfDie die = bias.die;
	DwarfDie[] allDies = die.getScopes(pc - bias.bias);
	List candidates_p = die.getScopeVarNames(allDies, incomplete);
	for (Iterator i = candidates_p.iterator(); i.hasNext();) {
            String sNext = (String) i.next();
            candidates.add(sNext);
        }
    }
}
