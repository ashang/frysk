// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

public class Dwarf {
	
    private final boolean owner;
    private long pointer;
	
    public Dwarf(Elf elf, DwarfCommand cmd, ElfSection section){
	dwarf_begin_elf(elf.getPointer(), cmd.getValue(), (section == null) ? 0 : section.getPointer());
	owner = true;
    }
	
    public Dwarf(String file, DwarfCommand cmd){
	dwarf_begin(file, cmd.getValue());
	owner = true;
    }
	
    Dwarf(long pointer){
	this.pointer = pointer;
	owner = false;
    }
	
    protected long getPointer(){
	return pointer;
    }
	
    protected void finalize(){
	if (owner && pointer != 0) {
	    dwarfEnd(pointer);
	    pointer = 0;
	}
    }
    private static native int dwarfEnd(long pointer);
	
    public String[] getSourceFiles(){
	return get_source_files();
    }
    
    /**
     * Returns a list of compilation units matching
     * the given name.
     * If a full path is give only one cu is likely
     * to be returned.
     * 
     * @param name
     * @return
     */
    public LinkedList getCUByName(String name){
	return this.get_cu_by_name(name);
    }
    
    protected native void dwarf_begin_elf(long elf, int command, long section);
    protected native void dwarf_begin(String file, int command);
    protected native String[] get_source_files();
    protected native LinkedList get_cu_by_name(String name);
}
