// This file is part of the program FRYSK.
//
// Copyright 2005, 2008, Red Hat Inc.
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

package lib.dwfl;

import java.util.LinkedList;

/**
 * A wrapper object around the libdwfl Dwfl_Module structure.
 */
public class DwflModule {

    protected LinkedList pubNames;
    protected LinkedList symbolTable;
    
    private final long pointer;
    private final Dwfl parent;
    private final String name;
    private final long low;
    private final long high;

    DwflModule(long val, Dwfl parent, String name, long low, long high) {
	this.pointer = val;
	this.parent = parent;
	this.name = name;
	this.low = low;
	this.high = high;
    }

  
    public String toString() {
	return name + " pointer: 0x" + Long.toHexString(pointer);
    }

    public long lowAddress() {
	return low;
    }

    public long highAddress() {
	return high;
    }

    protected long getPointer () {
	return pointer;
    }

    /**
     * Get the name of the module.
     *
     * @return the name
     */
    public String getName() {
	return name;
    }
  
    protected Dwfl getParent () {
	return this.parent;
    }

    public ModuleElfBias getElf () {
	return module_getelf();
    }

    public LinkedList getPubNames(){
	if(this.pubNames == null){
	    this.pubNames = new LinkedList();
	    get_pubnames();
	}
	return pubNames;
    }
    private native void get_pubnames();

    public native void getSymtab(SymbolBuilder symbolBuilder);
    public native void getPLTEntries(SymbolBuilder symbolBuilder);


    protected DwarfDie getDieByOffset(long offset){
	return offdie(this.getPointer(), offset);
    }
    private native DwarfDie offdie(long die, long offset);

    
    public native void getSymbol(long address, SymbolBuilder symbolBuilder);
  
    private native ModuleElfBias module_getelf();

    /**
     * Get all the line records for a source position in a file.
     *
     * @param filename the file
     * @param lineno line number of source
     * @param column column number, or 0
     * @return array of DwflLine objects.
     */
    public native DwflLine[] getLines(String filename, int lineo, int column);

    public native void getSymbolByName(String name,
				       SymbolBuilder symbolBuilder);
  
    public native void setUserData(Object data);
    
    /**
     * Get the debuginfo path for DwflModule
     * 
     * @return path to debuginfo package if found, NULL otherwise 
     */
    public native String getDebuginfo();
    
    public LinkedList getCuDies(){
	return get_cu_dies();
    }
    private native LinkedList get_cu_dies();

    public DwflDie getCompilationUnit(long addr) {
	// Find the die, grab the bias as it flies by.
	long diePointer = dwflModuleAddrdie(pointer, addr);
	return parent.factory.makeDwflDie(diePointer, this);
    }
    private static native long dwflModuleAddrdie(long pointer, long addr);

    public Dwarf getDwarf() {
	if (dwarf == null) {
	    long dwarfPointer = dwflModuleGetDwarf(pointer);
	    if (dwarfPointer != 0) {
		dwarf = new Dwarf(dwarfPointer);
	    }
	}
	return dwarf;
    }
    private Dwarf dwarf;
    private static native long dwflModuleGetDwarf(long pointer);

    public long getBias() {
	if (bias == -1) {
	    bias = dwflModuleGetBias(pointer);
	}
	return bias;
    }
    private long bias = -1;
    private static native long dwflModuleGetBias(long pointer);


    /**
     * Return line information for the specified address.
     */
    public DwflLine getSourceLine(long addr) {
	try {
	    long dwflLinePointer = dwfl_module_getsrc(pointer, addr);
	    if (dwflLinePointer == 0) {
		return null;
	    } else {
		return new DwflLine(dwflLinePointer, this);
	    }
	} catch (NullPointerException npe) {
	    System.out.println(npe.getMessage());
	    return null;
	}
    }
    private static native long dwfl_module_getsrc(long pointer, long addr);
}
