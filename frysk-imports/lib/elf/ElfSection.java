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
 * An ElfSection is a descriptor o an Elf file section
 * @author ajocksch
 *
 */
public class ElfSection {

	private long pointer;
	private Elf parent;
	
	protected ElfSection(long ptr, Elf parent){
		this.pointer = ptr;
		this.parent = parent;
	}
	
	public long getPointer(){
		return this.pointer;
	}
	
	/**
	 * @return The index of this section.
	 */
	public long getIndex(){
		return elf_ndxscn();
	}
	
	/**
	 * 
	 * @return The header for this ElfSection
	 */
	public ElfSectionHeader getSectionHeader(){
		return elf_getshdr();
	}
	
	/**
	 * Flags the section with the provided flags
	 * @param command An {@see ElfCommand}
	 * @param flags The flag to use
	 * @return The new flag value
	 */
	public ElfFlags flag(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagscn(command.getValue(), flags.getValue()));
	}
	
	/**
	 * Flags the section header with the provided flags
	 * @param command An {@see ElfCommand}
	 * @param flags The flags to use
	 * @return The new flag value
	 */
	public ElfFlags flagHeader(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagshdr(command.getValue(), flags.getValue()));
	}
	
	/**
	 * 
	 *  @return The ElfData contained in this section
	 */
	public ElfData getData(){
		long val = elf_getdata();
		if(val != 0)
			return new ElfData(elf_getdata(), parent);
		else return null;
	}
	
	/**
	 * 
	 * @return The uninterpreted ElfData in this section
	 */
	public ElfData getRawData(){
		return new ElfData(elf_rawdata(), parent);
	}
	
	/**
	 * 
	 * @return Creates a new ElfData for this section
	 */
	public ElfData createNewElfData(){
		return new ElfData(elf_newdata(), parent);
	}
	
	protected Elf getParent(){
		return this.parent;
	}
	
	protected native long elf_ndxscn();
	protected native ElfSectionHeader elf_getshdr();
	protected native int elf_flagscn(int __cmd, int __flags);
	protected native int elf_flagshdr(int __cmd, int __flags);
	protected native long elf_getdata();
	protected native long elf_rawdata();
	protected native long elf_newdata();
}
