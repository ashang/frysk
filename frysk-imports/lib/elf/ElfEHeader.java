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

/**
 * An ElfHeader is a header for and Elf file. This appears at the start of
 * every Elf file.
 * @author ajocksch
 *
 */
public abstract class ElfEHeader {
	
	private long pointer;
	
	protected ElfEHeader(long ptr){
		this.pointer = ptr;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	public int getFileClass(){
		return (int) get_e_fileclass();
	}
	
	public int getDataEncoding(){
		return (int) get_e_dataencoding();
	}
	
	public int getFileVersion(){
		return (int) get_e_fileversion();
	}
	
	/**
	 * 
	 * @return The object file type
	 */
	public int getType(){
		return get_e_type();
	}
	
	/**
	 * 
	 * @return The architechture
	 */
	public int getArchitecture(){
		return get_e_machine();
	}
	
	/**
	 * 
	 * @return The object file version
	 */
	public long getVersion(){
		return get_e_version();
	}
	
	/**
	 * 
	 * @return The virtual address of the entry point
	 */
	public long getEntryPoint(){
		return get_e_entry();
	}
	
	/**
	 * 
	 * @return The program header table file offset
	 */
	public long getProgramHeaderOffset(){
		return get_e_phoff();
	}
	
	/**
	 *  
	 * @return The section header table file offset
	 */
	public long getSectionHeaderOffset(){
		return get_e_shoff();
	}
	
	/**
	 * 
	 * @return The processor specific flags
	 */
	public long getFlags(){
		return get_e_flags();
	}
	
	/**
	 * The size of the Elf header in bytes
	 * @return
	 */
	public int getELFHeaderSize(){
		return get_e_ehsize();
	}
	
	/**
	 * 
	 * @return The program header table entry size
	 */
	public int getProgramHeaderEntrySize(){
		return get_e_phentsize();
	}
	
	/**
	 * 
	 * @return The number of program header entries
	 */
	public int getProgramHeaderEntryCount(){
		return get_e_phnum();
	}
	
	/**
	 * 
	 * @return The section header table entry size
	 */
	public int getSectionHeaderEntrySize(){
		return get_e_shentsize();
	}
	
	/**
	 * 
	 * @return The number of section header table entries
	 */
	public int getSectionHeaderEntryCount(){
		return get_e_shnum();
	}
	/**
	 * 
	 * @return The section header string table index
	 */
	public int getSectionHeaderStringTableIndex(){
		return get_e_shstrndx();
	}

	protected abstract byte get_e_fileclass();
	protected abstract byte get_e_dataencoding();
	protected abstract byte get_e_fileversion();
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
