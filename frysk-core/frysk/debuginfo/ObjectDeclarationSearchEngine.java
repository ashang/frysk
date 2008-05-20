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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lib.dwfl.Dwarf;
import lib.dwfl.DwarfCommand;
import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflDieBias;
import lib.dwfl.DwflModule;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.SymbolBuilder;
import frysk.dwfl.DwflCache;
import frysk.expr.ExprSymTab;
import frysk.isa.registers.Register;
import frysk.isa.registers.Registers;
import frysk.isa.registers.RegistersFactory;
import frysk.proc.Task;
import frysk.scopes.Scope;
import frysk.scopes.ScopeFactory;
import frysk.scopes.Variable;
import frysk.symtab.SymbolObjectDeclaration;
import frysk.value.ObjectDeclaration;
import frysk.value.Value;

/**
 * This engine implements the c++ scoping rules and uses when searching for
 * a variable by name:
 * 1. Search the inner-most scope containing the current pc indicated by the
 * given frame, and return the first encounter. 
 *
 */
public class ObjectDeclarationSearchEngine implements ExprSymTab{

    private final DebugInfoFrame frame;
    private final Task task;

    public ObjectDeclarationSearchEngine(DebugInfoFrame frame) {
	this.frame = frame;
	this.task = frame.getTask();
    }

    /**
     * Get the DwarfDie for a function symbol
     * XXX: this code has been moved here from DebugInfo
     * should be modified to
     * - use frysk search ({@link ObjectDeclarationSearchEngine})
     * - handle # syntax
     * ...
     * handles:
     * [file#]name
     *    
     */
    public LinkedList getObject(String name) {
	
	LinkedList results = new LinkedList();
	
	DwarfDie cu;
	String symbol;
	
	Elf elf = new Elf(new File(task.getProc().getExeFile().getSysRootedPath()), ElfCommand.ELF_C_READ);
	Dwarf dwarf = new Dwarf(elf, DwarfCommand.READ, null);
	TypeFactory typeFactory = new TypeFactory(task.getISA());
	
	String[] names = name.split("#");
	
	if(names.length == 2){
	    LinkedList cuDies = dwarf.getCUByName(names[0]);

	    Iterator iterator = cuDies.iterator();
	    while (iterator.hasNext()) {
		
		cu = (DwarfDie) iterator.next();
		    
		symbol = names[1];
		
		Scope scope = ScopeFactory.theFactory.getScope(cu, typeFactory);
		results.add(scope.getDeclaredObjectByNameRecursive(symbol));
	    }
	    
	}
	
	if (names.length == 1) {
	    DwarfDie resultDie = DwarfDie.getDecl(dwarf, name);

	    if (resultDie != null){
		
		try {
		    results.add((ObjectDeclaration) ScopeFactory.theFactory.getScope(
			    resultDie, typeFactory));
		} catch (IllegalArgumentException e) {
		    try {
			results.add(new Variable(resultDie));
		    } catch (Exception e2) {
			
		    }
		}
	    }
	}
	
	return results;
    }
 
    /**
     * Returns the object with the given name that is currently
     * in scope. The scope is decided by the current frame
     * object.
     */
    public ObjectDeclaration getObjectInScope(DebugInfoFrame frame, String name){
	ObjectDeclaration declaredObject = null;
	
	Scope scope = frame.getScopes();
	
	while(scope != null){
	    declaredObject = scope.getDeclaredObjectByName(name);
	    if(declaredObject != null){
		return declaredObject;
	    }
	    scope = scope.getOuter();
	}

	return getObjectUsingBinaryInfo(frame, name);
    }

    public ObjectDeclaration getObjectUsingBinaryInfo(DebugInfoFrame frame, String name){
	Dwfl dwfl = DwflCache.getDwfl(task);

	DwflModule module = dwfl.getModule(this.frame.getAdjustedAddress());
	
	if(module == null){
	    throw new RuntimeException("Module could not be found for this process");
	}
	final String objectName = name;
	
	class Builder implements SymbolBuilder {
	    ObjectDeclaration objectDeclaration = null;
	    
	    public void symbol(String name, long value, long size,
			       lib.dwfl.ElfSymbolType type,
			       lib.dwfl.ElfSymbolBinding bind,
			       lib.dwfl.ElfSymbolVisibility visibility,
			       boolean defined)
	    {
		if(name.equals(objectName)){
		    objectDeclaration =  new SymbolObjectDeclaration(name, type, value, size);
		}
	    }
	}
	Builder builder = new Builder();

	//Search module containing current pc first
	module.getSymbolByName(name, builder);
	if(builder.objectDeclaration != null ) return builder.objectDeclaration;
	
	//Still now found... now search all modules
	//XXX: should this restrect to objects with global visibility ??
	DwflModule[] modules = dwfl.getModules();
	for (int i = 0; i < modules.length; i++){
	    modules[i].getSymbolByName(name, builder);
	    if(builder.objectDeclaration != null ) return builder.objectDeclaration;
	}
	
	throw new ObjectDeclarationNotFoundException(name);
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
	DwflDieBias bias = dwfl.getCompilationUnit(pc);
	DwarfDie die = bias.die;
	DwarfDie[] allDies = die.getScopes(pc - bias.bias);
	List candidates_p = die.getScopeVarNames(allDies, incomplete);
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
