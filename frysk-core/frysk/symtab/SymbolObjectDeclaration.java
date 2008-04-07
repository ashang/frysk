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

package frysk.symtab;

import java.util.ArrayList;

import lib.dwfl.ElfSymbolType;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.PieceLocation;
import frysk.isa.ISA;
import frysk.scopes.SourceLocation;
import frysk.value.ArrayType;
import frysk.value.FunctionType;
import frysk.value.Location;
import frysk.value.ObjectDeclaration;
import frysk.value.StandardTypes;
import frysk.value.Type;
import frysk.value.Value;
import frysk.value.VoidType;

/**
 * XXX: This will be folded into symtab.symbol
 */
public class SymbolObjectDeclaration extends ObjectDeclaration {

    private String name;
    private long address;
    private long size;
    private ElfSymbolType elfType;
    private Type type;

    public SymbolObjectDeclaration(String name, ElfSymbolType elfType, long address, long size){
	this.name = name;
	this.address = address;
	this.size = size;
	this.elfType = elfType;
    }
    
    public String getName() {
	return name;
    }

    public SourceLocation getSourceLocation() {
	return SourceLocation.UNKNOWN;
    }

    public Type getType(ISA isa) {
	
	if(this.type != null){
	    return this.type;
	}
	
	if(this.elfType == ElfSymbolType.ELF_STT_FUNC){
	    this.type = new FunctionType(this.name, new VoidType());
	}else{
	    // treat all other types as array lists
	    // if any special handling is needed add an if statement.
	    ArrayList dims = new ArrayList();
	    dims.add(new Integer((int) (size-1)));
	    this.type = new ArrayType(StandardTypes.uint8_t(isa.order()), (int) size, dims);	
	}
	
	return this.type;
    }

    public Value getValue(DebugInfoFrame frame) {
	Location location = PieceLocation.createSimpleLoc(address, size, frame.getTask().getMemory());
	return new Value(this.getType(frame.getTask().getISA()), location);
    }

}
