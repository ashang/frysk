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
 * Elf Program segment header
 * @author ajocksch
 *
 */
public abstract class ElfPHeader {

	private long pointer;
	private Elf parent;
	
	protected ElfPHeader(long ptr, Elf parent){
		this.pointer = ptr;
		this.parent = parent;
	}
	
	/**
	 * 
	 * @return The type of the program segment 
	 */
	public long getType(){
		return get_p_type();
	}
	
	/**
	 * 
	 * @return The file offset of the segment
	 */
	public long getOffset(){
		return get_p_offset();
	}
	
	/**
	 * 
	 * @return The virtual address of the program segment
	 */
	public long getVirtualAddress(){
		return get_p_vaddr();
	}
	
	/**
	 * @return The physical address of the program segment
	 */
	public long getPhysicalAddress(){
		return get_p_paddr();
	}
	
	/**
	 * 
	 * @return The size of the program segment in the file
	 */
	public long getSegmentSizeInFile(){
		return get_p_filesz();
	}
	
	/**
	 * 
	 * @return The size of the program segment in memory
	 */
	public long getSegmentSizeInMem(){
		return get_p_memsz();
	}
	
	/**
	 * 
	 * @return The program segment flags
	 */
	public long getFlags(){
		return get_p_flags();
	}
	
	/**
	 * 
	 * @return The program segment's alignment
	 */
	public long getAlignment(){
		return get_p_align();
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected Elf getParent(){
		return this.parent;
	}
	
	protected abstract long get_p_type();
	protected abstract long get_p_offset();
	protected abstract long get_p_vaddr();
	protected abstract long get_p_paddr();
	protected abstract long get_p_filesz();
	protected abstract long get_p_memsz();
	protected abstract long get_p_align();
	protected abstract long get_p_flags();
}
