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
 * Header for an {@see ElfSection}
 * @author ajocksch
 *
 */
public abstract class ElfSectionHeader {

	long pointer;
	
	protected ElfSectionHeader(long ptr){
		this.pointer = ptr;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	/**
	 * 
	 * @return The index of the section's name in the string table
	 */
	public long getNameIndex(){
		return get_sh_name();
	}
	
	/**
	 * 
	 * @return The type of the header
	 */
	public long getType(){
		return get_sh_type();
	}
	
	/**
	 * 
	 * @return The section flags
	 */
	public long getFlags(){
		return get_sh_flags();
	}
	
	/**
	 * 
	 * @return The section's virtual address at execution
	 */
	public long getAddress(){
		return get_sh_addr();
	}
	
	/**
	 * 
	 * @return The section file offset
	 */
	public long getOffset(){
		return get_sh_offset();
	}
	
	/**
	 * 
	 * @return Size of the section in bytes
	 */
	public long getSize(){
		return get_sh_size();
	}
	
	// TODO: does this point to another Section Header?
	/**
	 * @return Link to another section
	 */
	public long getLink(){
		return get_sh_link();
	}
	
	/**
	 * 
	 * @return Any additional section information
	 */
	public long getAdditionalInfo(){
		return get_sh_info();
	}
	
	/**
	 * 
	 * @return The section alignment
	 */
	public long getAlignment(){
		return get_sh_addralign();
	}
	
	/**
	 * 
	 * @return The entry size if this section holds a table
	 */
	public long getEntrySize(){
		return get_sh_entsize();
	}
	
	protected abstract long get_sh_name();
	protected abstract long get_sh_type();
	protected abstract long get_sh_flags();
	protected abstract long get_sh_addr();
	protected abstract long get_sh_offset();
	protected abstract long get_sh_size();
	protected abstract long get_sh_link();
	protected abstract long get_sh_info();
	protected abstract long get_sh_addralign();
	protected abstract long get_sh_entsize();
	
}
