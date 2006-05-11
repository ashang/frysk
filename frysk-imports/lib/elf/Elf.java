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

public class Elf {

	private long pointer;
	private boolean is32bit;	
	
	protected Elf(long ptr){
		this.pointer = ptr;
	}
	
	public Elf(String file, ElfCommand command){
		elf_begin(file, command.getValue());
	}
	
	public Elf(String image, long size){
		elf_memory(image, size);
	}
	
	public ElfCommand next(){
		return ElfCommand.intern(elf_next());
	}
	
	public long update(ElfCommand command){
		return elf_update(command.getValue());
	}
	
	public ElfKind getKind(){
		return ElfKind.intern(elf_kind());
	}
	
	public long getBase(){
		return elf_getbase();
	}
	
	public String getIdentification(long ptr){
		return elf_getident(ptr);
	}
	
	public ElfEHeader getEHeader(){
		if(is32bit)
			return new ElfEHeader32(elf_getehdr());
		else
			return new ElfEHeader64(elf_getehdr());
	}
	
	public ElfEHeader createNewEHeader(){
		if(is32bit)
			return new ElfEHeader32(elf_newehdr());
		else
			return new ElfEHeader64(elf_newehdr());
	}
	
	public ElfPHeader getPHeader(){
		if(is32bit)
			return new ElfPHeader32(elf_getphdr());
		else
			return new ElfPHeader64(elf_getphdr());
	}
	
	public ElfPHeader createNewPHeader(long count){
		if(is32bit)
			return new ElfPHeader32(elf_newphdr(count));
		else
			return new ElfPHeader64(elf_newphdr(count));
	}
	
	public ElfSection getSectionByOffset(int offset){
		return new ElfSection(elf_getscn(offset), is32bit);
	}
	
	public ElfSection getSection(long index){
		return new ElfSection(elf_getscn(index), is32bit);
	}
	
	public ElfSection getNextSection(ElfSection previous){
		return new ElfSection(elf_nextscn(previous.getPointer()), is32bit);
	}
	
	public ElfSection createNewSection(){
		return new ElfSection(elf_newscn(), is32bit);
	}
	
	public int getSectionNumber(long dst){
		return elf_getshnum(dst);
	}
	
	public int getSectionIndex(long dst){
		return elf_getshstrndx(dst);
	}
	
	public int flag(ElfCommand command, int flags){
		return elf_flagelf(command.getValue(), flags);
	}
	
	public int flagEHeader(ElfCommand command, int flags){
		return elf_flagehdr(command.getValue(), flags);
	}
	
	public int flagPHeader(ElfCommand command, int flags){
		return elf_flagphdr(command.getValue(), flags);
	}
	
	public String getStringAtOffset(long index, long offset){
		return elf_strptr(index, offset);
	}
	
	public ElfArchiveHeader getArchiveHeader(){
		return new ElfArchiveHeader(elf_getarhdr());
	}
	
	public long getArchiveOffset(){
		return elf_getaroff();
	}
	
	public long getArchiveElement(int offset){
		return elf_rand(offset);
	}
	
	public ElfArchiveSymbol getArchiveSymbol(long ptr){
		return new ElfArchiveSymbol(elf_getarsym(ptr));
	}
	
	public int getControlDescriptor(ElfCommand command){
		return elf_cntl(command.getValue());
	}
	
	public String getRawFileContents(long ptr){
		return elf_rawfile(ptr);
	}
	
	public boolean is32Bits(){
		return this.is32bit;
	}
	
	protected long getPointer(){
		return this.pointer;
	}
	
	protected void finalize() throws Throwable {
		elf_end();
	}
	
	protected native void elf_begin(String file, int __cmd);
	protected native void elf_clone(long __elf, int __cmd);
	protected native void elf_memory(String __image, long __size);
	protected native int elf_next();
	protected native int elf_end();
	protected native long elf_update(int __cmd);
	protected native int elf_kind();
	protected native long elf_getbase();
	protected native String elf_getident(long ptr);
	protected native long elf_getehdr();
	protected native long elf_newehdr();
	protected native long elf_getphdr();
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
