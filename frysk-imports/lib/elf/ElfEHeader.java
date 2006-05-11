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
package lib.elf;

public abstract class ElfEHeader {
	
	private long pointer;
	
	protected ElfEHeader(long ptr){
		this.pointer = ptr;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	public String getItentifier(){
		return get_e_ident();
	}
	
	public int getType(){
		return get_e_type();
	}
	
	public int getArchitecture(){
		return get_e_machine();
	}
	
	public long getVersion(){
		return get_e_version();
	}
	
	public long getEntryPoint(){
		return get_e_entry();
	}
	
	public long getProgramHeaderOffset(){
		return get_e_phoff();
	}
	
	public long getSectionHeaderOffset(){
		return get_e_shoff();
	}
	
	public long getFlags(){
		return get_e_flags();
	}
	
	public int getELFHeaderSize(){
		return get_e_ehsize();
	}
	
	public int getProgramHeaderEntrySize(){
		return get_e_phentsize();
	}
	
	public int getProgramHeaderEntryCount(){
		return get_e_phnum();
	}
	
	public int getSectionHeaderEntrySize(){
		return get_e_shentsize();
	}
	
	public int getSectionHeaderEntryCount(){
		return get_e_shnum();
	}
	
	public int getSectionHeaderStringTableIndex(){
		return get_e_shstrndx();
	}

	protected abstract String get_e_ident();
	protected abstract int get_e_type();
	protected abstract int get_e_machine();
	protected abstract long get_e_version();
	protected abstract long get_e_entry();
	protected abstract long get_e_phoff();
	protected abstract long get_e_shoff();
	protected abstract long get_e_flags();
	protected abstract int get_e_ehsize();
	protected abstract int get_e_phentsize();
	protected abstract int get_e_phnum();
	protected abstract int get_e_shentsize();
	protected abstract int get_e_shnum();
	protected abstract int get_e_shstrndx();
}
