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
 * This class represents an Elf object.
 *
 */
public class Elf {

	private long pointer;
	private boolean is32bit;
	
	protected Elf(long ptr, boolean is32bit){
		this.pointer = ptr;
		this.is32bit = is32bit;
	}
	
	/**
	 * Creates a new elf object
	 * @param file The file to create the object from
	 * @param command The appropriate {@see ElfCommand}
	 */
	public Elf(String file, ElfCommand command){
		elf_begin(file, command.getValue());
	}
	
	/**
	 * Creates a new Elf object.
	 * @param image The name of the image in memory to use
	 * @param size The size of the image
	 */
	public Elf(String image, long size){
		elf_memory(image, size);
	}
	
	public Elf clone(ElfCommand command){
		return new Elf(elf_clone(command.getValue()), this.is32bit);
	}
	
	/**
	 * 
	 * @return The next elf command
	 */
	public ElfCommand next(){
		return ElfCommand.intern(elf_next());
	}
	
	/**
	 * Update the Elf descriptor and write the file to disk
	 * @param command The {@see ElfCommand}
	 * @return The amount written
	 */
	public long update(ElfCommand command){
		return elf_update(command.getValue());
	}
	
	/**
	 * 
	 * @return The type of file associated with this Elf object.
	 */
	public ElfKind getKind(){
		return ElfKind.intern(elf_kind());
	}
	
	/**
	 * 
	 * @return The base offset for the Elf object
	 */
	public long getBase(){
		return elf_getbase();
	}
	
	/**
	 * 
	 * @param ptr 
	 * @return The file identification data
	 */
	public String getIdentification(long ptr){
		return elf_getident(ptr);
	}
	
	/**
	 * 
	 * @return The object file header
	 */
	public ElfEHeader getEHeader(){
		if(is32bit)
			return new ElfEHeader32(elf_getehdr(), this);
		else
			return new ElfEHeader64(elf_getehdr(), this);
	}
	
	/**
	 * Creates a new Elf Header if none exists
	 * @return A new ElfHeader
	 */
	public ElfEHeader createNewEHeader(){
		if(is32bit)
			return new ElfEHeader32(elf_newehdr(), this);
		else
			return new ElfEHeader64(elf_newehdr(), this);
	}
	
	/**
	 * 
	 * @return The program header table
	 */
	public ElfPHeader[] getPHeaders(){
		long[] vals = elf_getphdrs();
		if(vals == null)
			return null;
		
		ElfPHeader[] headers = new ElfPHeader[vals.length];
		
		for(int i = 0; i < vals.length; i++){
			long val = vals[i];
			if(val == 0)
				headers[i] = null;
			else{
				if(this.is32bit)
					headers[i] = new ElfPHeader32(val, this);
				else
					headers[i] = new ElfPHeader64(val, this);
			}
		}
		
		return headers;
			
	}
	
	/**
	 * Creates a new program header table if none exists
	 * @param count 
	 * @return The program header table
	 */
	public ElfPHeader createNewPHeader(long count){
		if(is32bit)
			return new ElfPHeader32(elf_newphdr(count), this);
		else
			return new ElfPHeader64(elf_newphdr(count), this);
	}
	
	/**
	 * Returns the {@see ElfSection} at the given offset
	 * @param offset The offset to get the header at
	 * @return The ElfSection
	 */
	public ElfSection getSectionByOffset(int offset){
		return new ElfSection(elf_getscn(offset), is32bit, this);
	}
	
	/**
	 * Returns the {@see ElfSection} at the provided index
	 * @param index The index
	 * @return The ElfSection at that index
	 */
	public ElfSection getSection(long index){
		long val = elf_getscn(index);
		if(val == 0)
			return null;
		return new ElfSection(elf_getscn(index), is32bit, this);
	}
	
	/**
	 * 
	 * @param previous The current ElfSection
	 * @return The ElfSection that immediately follows it
	 */
	public ElfSection getNextSection(ElfSection previous){
		long val = elf_nextscn(previous.getPointer());
		if(val != 0)
			return new ElfSection(val, is32bit, this);
		else
			return null;
	}
	
	/**
	 * Creates a new ElfSection at the end of the table and returns it
	 * @return The new ElfSection
	 */
	public ElfSection createNewSection(){
		return new ElfSection(elf_newscn(), is32bit, this);
	}
	
	/**
	 * 
	 * @param dst
	 * @return The number of sections in the Elf file.
	 */
	public int getSectionNumber(long dst){
		return elf_getshnum(dst);
	}
	
	/**
	 * 
	 * @param dst
	 * @return The section index of the section header string table in the Elf file.
	 */
	public int getSectionIndex(long dst){
		return elf_getshstrndx(dst);
	}
	
	/**
	 * Sets or clears flags in the Elf file
	 * @param command An {@see ElfCommand}
	 * @param flags The flags to set/clear
	 * @return the current flags
	 */
	public ElfFlags flag(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagelf(command.getValue(), flags.getValue()));
	}
	
	/**
	 * Sets or clears flags in the Elf header
	 * @param command An {@see ElfCommand}
	 * @param flags The flags to set/clear
	 * @return the current flags
	 */
	public ElfFlags flagEHeader(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagehdr(command.getValue(), flags.getValue()));
	}
	
	/**
	 * Sets or clears flags in the Elf program header
	 * @param command An {@see ElfCommand}
	 * @param flags The flags to set/clear
	 * @return the current flags
	 */
	public ElfFlags flagPHeader(ElfCommand command, ElfFlags flags){
		return ElfFlags.intern(elf_flagphdr(command.getValue(), flags.getValue()));
	}
	
	/**
	 * Returns the string at the provided offset from the provided index
	 * @param index The index
	 * @param offset The offset from index
	 * @return The string at index + offset
	 */
	public String getStringAtOffset(long index, long offset){
		return elf_strptr(index, offset);
	}
	
	/**
	 * 
	 * @return The Elf archive header
	 */
	public ElfArchiveHeader getArchiveHeader(){
		return new ElfArchiveHeader(elf_getarhdr(), this);
	}
	
	/**
	 * 
	 * @return The offset in the archive of the current elf file.
	 */
	public long getArchiveOffset(){
		return elf_getaroff();
	}
	
	/**
	 * 
	 * @param offset The offset to get the archive element from
	 * @return The archive element at the provided offset
	 */
	public long getArchiveElement(int offset){
		return elf_rand(offset);
	}
	
	/**
	 * 
	 * @param ptr
	 * @return The symbol table of the archive
	 */
	public ElfArchiveSymbol getArchiveSymbol(long ptr){
		return new ElfArchiveSymbol(elf_getarsym(ptr), this);
	}
	
	/**
	 * 
	 * @param command An {@see ElfCommand}
	 * @return The control Elf descriptor.
	 */
	public int getControlDescriptor(ElfCommand command){
		return elf_cntl(command.getValue());
	}
	
	/**
	 * 
	 * @param ptr
	 * @return The uninterpreted file conents
	 */
	public String getRawFileContents(long ptr){
		return elf_rawfile(ptr);
	}
	
	/**
	 * 
	 * @return true iff this is a 32 bit elf object
	 */
	public boolean is32Bits(){
		return this.is32bit;
	}
	
	public long getPointer(){
		return this.pointer;
	}
	
	protected void finalize() throws Throwable {
		elf_end();
	}
	
	protected native void elf_begin(String file, int __cmd);
	protected native long elf_clone(int __cmd);
	protected native void elf_memory(String __image, long __size);
	protected native int elf_next();
	protected native int elf_end();
	protected native long elf_update(int __cmd);
	protected native int elf_kind();
	protected native long elf_getbase();
	protected native String elf_getident(long ptr);
	protected native long elf_getehdr();
	protected native long elf_newehdr();
	protected native long[] elf_getphdrs();
	protected native long elf_newphdr(long __cnt);
	protected native long elf_offscn(long offset);
	protected native long elf_getscn(long __index);
	protected native long elf_nextscn(long __scn);
	protected native long elf_newscn();
	// TODO: Does this return an error/success code or the value?
	protected native int elf_getshnum(long __dst);
	// TODO: See above
	protected native int elf_getshstrndx(long __dst);
	protected native int elf_flagelf(int __cmd, int __flags);
	protected native int elf_flagehdr(int __cmd, int __flags);
	protected native int elf_flagphdr(int __cmd, int __flags);
	protected native String elf_strptr(long __index, long __offset);
	protected native long elf_getarhdr();
	protected native long elf_getaroff();
	protected native long elf_rand(int __offset);
	protected native long elf_getarsym(long __ptr);
	protected native int elf_cntl(int __cmd);
	protected native String elf_rawfile(long __ptr);
	
}
