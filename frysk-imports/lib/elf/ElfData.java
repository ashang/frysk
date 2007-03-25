// This file is part of the program FRYSK.
//
// Copyright 2005,2007 Red Hat Inc.
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
 * An ElfData is a descriptor of data that will be converted to or
 * from memory format
 */
public class ElfData {

	private long pointer;
	private Elf parent;
	public byte[] internal_buffer;
	
  
        /**
	 *
	 * Package buffer in an  ElfData object.
	 *
	 */
        public ElfData(byte[] buffer, Elf parent)
        {
           this.parent = parent;
           elf_data_create_native();
	   setBuffer(buffer);
        }

	protected ElfData(long ptr, Elf parent){
		this.pointer = ptr;
		this.parent = parent;
	}
	
	/**
	* Sets the buffer to point to the byte[] array
	*
	* This will then be written to disk on elf_update
	**/
	public void setBuffer(byte[] e_buffer) {
		internal_buffer = e_buffer;
		if (internal_buffer != null)
			elf_data_set_buff(e_buffer.length);
	}
	/**
	 * Returns the byte at the provided offset into the data
	 * @param offset The offset from which to get the byte
	 * @return The data at offset
	 */
	public byte getByte(long offset){
		return elf_data_get_byte(offset);
	}

	/**
	 * Sets the Elf Data Type 
	 *
	 * @param The type of the data
	 */
	public void setType(int type) {
		elf_data_set_type (type);
	}

	/**
	 * 
	 * Returns the Elf Data type
	 *
	 * @return The type of the data
	 */
	public ElfType getType(){
		return ElfType.intern(elf_data_get_type());
	}
	
	/**
	 * 
	 * @return The size of the data in bytes
	 */
	public long getSize(){
		return elf_data_get_size();
	}

	/**
	 * 
	 * @param The size of the data in bytes
	 */
	public void setSize(long size){
		elf_data_set_size(size);
	}
	
	/**
	 * 
	 * @return The offset into the section of the data
	 */
	public int getOffset(){
		return elf_data_get_off();
	}

	/**
	 * 
	 * @param The offset into the section of the data
	 */
	public void setOffset(int offset){
		elf_data_set_off(offset);
	}
	
	/**
	 * 
	 * @return The alignment of the data in the section
	 */
	public long getAlignment(){
		return elf_data_get_align();
	}

	/**
	 * 
	 * @param The alignment of the data in the section
	 */
	public void setAlignment(long align){
		elf_data_set_align(align);
	}
	
	/**
	 * Translates the information into memory format using the
	 * provided encoding 
	 * @param encoding The encoding to use
	 * @return The data in memory format
	 */
	public ElfData translateToMemoryRepresentation(int encoding){
		long retval = elf_xlatetom(encoding);
		if(retval == 0)
			return null;
		return new ElfData(retval, this.parent);
	}
	
	/**
	 * Translates the information into Elf format using the
	 * provided encoding
	 * @param encoding The encoding to use
	 * @return The data in Elf format
	 */
	public ElfData translateToELFRepresentation(int encoding){
		long retval = elf_xlatetof(encoding);
		if(retval == 0)
			return null;
		return new ElfData(retval, this.parent);
	}
	
	/**
	 * Flags the data with the provided flag
	 * @param command An {@see ElfCommand}
	 * @param flags The flags to apply
	 * @return The new flag value
	 */
	public ElfFlags flag(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagdata(command.getValue(), flags.getValue()));
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected void finalize() throws Throwable {
		elf_data_finalize();
	}

	public Elf getParent(){
		return this.parent;
	}
	
        native private void elf_data_create_native();
	native private void elf_data_finalize();
	native protected void elf_data_set_buff(long length);
	native protected byte elf_data_get_byte(long offset);
	native protected int elf_data_get_type();
	native protected void elf_data_set_type(int type);
	native protected int elf_data_get_version();
	native protected void elf_data_set_version(int version);
	native protected long elf_data_get_size();
	native protected void elf_data_set_size(long size);
	native protected int elf_data_get_off();
	native protected void elf_data_set_off(int offset);
	native protected long elf_data_get_align();
	native protected void elf_data_set_align(long align);
	native protected int elf_flagdata(int __cmd, int __flags);
	native protected long elf_xlatetom(int __encode);
	native protected long elf_xlatetof(int __encode);
}
