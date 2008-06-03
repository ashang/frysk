// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
public class DwflModule
{

    protected LinkedList pubNames;
    protected LinkedList symbolTable;
    
    private long pointer;

    protected final Dwfl parent;

    protected String name;
  
    public String toString() {
	return name + " pointer: 0x" + Long.toHexString(pointer);
    }
    public DwflModule(long val, Dwfl parent) {
	this(val, parent, null);
    }
  
    DwflModule (long val, Dwfl parent, String name) {
	this.pointer = val;
	this.parent = parent;
	this.name = name;
    }


    protected long getPointer () {
	return pointer;
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

    
    /**
     * Get the name of the module.
     *
     * @return the name
     */
    public native String getName();
  
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
}
