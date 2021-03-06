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

package frysk.expr;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDie;

import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.debuginfo.PieceLocation;
import frysk.debuginfo.RegisterPiece;
import frysk.dwfl.DwflCache;
import frysk.isa.registers.Register;
import frysk.isa.registers.Registers;
import frysk.isa.registers.RegistersFactory;
import frysk.proc.Task;
import frysk.value.ObjectDeclaration;
import frysk.value.Value;

/**
 * This class adapts {@link ObjectDeclarationSearchEngine} so that it is
 * usable by ExprSymTab.
 * 
 * This class should be eliminated by modifying ExprSymTab to use 
 * {@link ObjectDeclarationSearchEngine}
 */
public class ExprSearchEngine extends ObjectDeclarationSearchEngine implements ExprSymTab{

    private final DebugInfoFrame frame;
    private Task task;
    
    public ExprSearchEngine(DebugInfoFrame frame) {
	super(frame.getTask());
	this.frame = frame;
	this.task = frame.getTask();
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
	
	ObjectDeclaration objectDeclaration = this.getObjectInScope(frame, s);
	return objectDeclaration.getValue(frame);
    }


    /**
     * XXX: Who knows if this works; it is certainly not implemented
     * correctly as it should use the ObjectDeclaration.
     */
    public void complete(String incomplete, List candidates) {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDie bias = dwfl.getCompilationUnit(pc);
	DwarfDie[] allDies = bias.getScopes(pc);
	List candidates_p = bias.getScopeVarNames(allDies, incomplete);
	for (Iterator i = candidates_p.iterator(); i.hasNext();) {
            String sNext = (String) i.next();
            candidates.add(sNext);
        }
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
    
}
