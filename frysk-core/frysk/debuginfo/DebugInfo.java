// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

import frysk.expr.ExprSearchEngine;
import frysk.expr.ExpressionFactory;
import frysk.dwfl.DwflCache;
import frysk.proc.Proc;
import frysk.value.Type;
import frysk.value.Value;
import lib.dwfl.Dwarf;
import lib.dwfl.DwarfCommand;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwarfException;
import lib.dwfl.Dwfl;
import lib.dwfl.DwTag;
import lib.dwfl.DwAt;
import lib.dwfl.DwflDie;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import java.io.File;

public class DebugInfo {
    private Elf elf;
    private Dwarf dwarf;

    /**
     * Create a symbol table object.  There should be one SymTab per process.
     * @param frame
     */
    public DebugInfo (DebugInfoFrame frame) {
	Proc proc = frame.getTask().getProc();
	try {
	    elf = new Elf(new File(proc.getExeFile().getSysRootedPath()), ElfCommand.ELF_C_READ);
	    dwarf = new Dwarf(elf, DwarfCommand.READ, null);
	}
	catch (lib.dwfl.ElfException ignore) {
	    // FIXME: Why is this ignored?
	}
    }

    /**
     * Implement the cli what request
     * 
     * @param sInput
     * @return String
     */
    public String what(DebugInfoFrame frame, String sInput) {
	long pc = frame.getAdjustedAddress();
	Dwfl dwfl = DwflCache.getDwfl(frame.getTask());
	DwflDie bias = dwfl.getCompilationUnit(pc);
	TypeFactory typeFactory = new TypeFactory(frame.getTask().getISA());
	if (bias == null)
	    throw new RuntimeException("No symbol table is available.");
	DwarfDie die = bias;
	StringBuffer result = new StringBuffer();

	DwarfDie[] allDies = die.getScopes(pc - bias.getBias());
	DwarfDie varDie = die.getScopeVar(allDies, sInput);
	if (varDie == null) {
	    varDie = DwarfDie.getDecl(dwarf, sInput);
	    if (varDie == null)
		throw new RuntimeException(sInput + " not found in scope.");
	    if (varDie.getAttrBoolean(DwAt.EXTERNAL))
		result.append("extern ");
	    switch (varDie.getTag().hashCode()) {
            case DwTag.SUBPROGRAM_: {
		Value value = typeFactory.getSubprogramValue(varDie);
		result.append(value.getType().toPrint());
		break;
            }
            case DwTag.TYPEDEF_:
            case DwTag.STRUCTURE_TYPE_: {
		Type type = typeFactory.getType(varDie.getType());
		if (type != null)
		    result.append(type.toPrint());
		break;
            }
            default:
		result.append(varDie + " " + varDie.getName());
	    }
        } else {
	    Type type = typeFactory.getType(varDie.getType());
	    if (varDie.getAttrBoolean(DwAt.EXTERNAL))
		result.append("extern ");

	    if (type != null)
		result.append(type.toPrint());
        }
	if (varDie != null) {
	    try {
		result.append(" at " + varDie.getDeclFile()
			      + "#" + varDie.getDeclLine());
	    } catch (DwarfException de) {
		result.append(" at <unknown>");
	    }
        }
	return result.toString();
    }
    
    /**
     * Implement the cli print request.
     */
    public Value print(String expression, DebugInfoFrame frame) {
	ExprSearchEngine symTab
	    = new ExprSearchEngine(frame);
	return ExpressionFactory.parse(symTab, expression).getValue();
    }
   
}
