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
package lib.dw;

import lib.elf.Elf;
import lib.elf.ElfSection;

public class Dwarf {
	
	private long pointer;
	
	public Dwarf(Elf elf, DwarfCommand cmd, ElfSection section){
		dwarf_begin_elf(elf.getPointer(), cmd.getValue(), (section == null) ? 0 : section.getPointer());
	}
	
	public Dwarf(String file, DwarfCommand cmd){
		dwarf_begin(file, cmd.getValue());
	}
	
//	public DwarfDie[] getFunctions(){
//		long[] vals = dwarf_get_functions();
//		if(vals == null || vals.length == 0)
//			return new DwarfDie[0];
//		
//		DwarfDie[] dwarfs = new DwarfDie[vals.length];
//		for(int i = 0; i < vals.length; i++){
//			if(vals[i] == 0)
//				dwarfs[i] = null;
//			else
//				dwarfs[i] = new DwarfDie(vals[i]);
//		}
//		
//		return dwarfs;
//	}
//	
//	public DwarfDie getDIEByAddr(long address){
//		long val = dwarf_addrdie(address);
//		if(val == 0)
//			return null;
//		
//		return new DwarfDie(val);
//	}
	
	protected Dwarf(long pointer){
		this.pointer = pointer;
	}
	
	protected long getPointer(){
		return pointer;
	}
	
	protected void finalize(){
		dwarf_end();
	}
	
    public String[] getSourceFiles(){
      return get_source_files();
    }
	protected native void dwarf_begin_elf(long elf, int command, long section);
	protected native void dwarf_begin(String file, int command);
    protected native String[] get_source_files();
	protected native int dwarf_end();
//	protected native long[] dwarf_get_functions();
//	protected native long dwarf_addrdie(long addr);
}
