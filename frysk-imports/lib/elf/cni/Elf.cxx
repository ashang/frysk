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
#include <stdlib.h>
#include <unistd.h>
#include <gelf.h>
#include <gcj/cni.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>

#include "lib/elf/ElfException.h"
#include "lib/elf/ElfFileException.h"
#include "lib/elf/Elf.h"
#include "lib/elf/ElfEHeader.h"
#include "lib/elf/ElfPHeader.h"
#include "lib/elf/ElfArchiveHeader.h"

#ifdef __cplusplus
extern "C"
{
#endif


void
lib::elf::Elf::elf_begin (jstring file, jint command, jboolean write){
	int fileNameLen = JvGetStringUTFLength(file);
	char fileName[fileNameLen + 1];
	
	JvGetStringUTFRegion (file, 0, file->length (), fileName);
	fileName[fileNameLen]='\0';

	errno = 0;
    if (write == false)
  	    fd = open (fileName, O_RDONLY);
    else
	    fd = open (fileName, O_RDWR | O_CREAT,00644);
	if(errno != 0){
		char* message = "Could not open %s";
		char error[strlen(fileName) + strlen(message) - 2];
		sprintf(error, message, fileName);
		throw new lib::elf::ElfFileException(JvNewStringUTF(error),
		    file);
	}
	
	if(::elf_version(EV_CURRENT) == EV_NONE) {
		::close(fd);
		throw new lib::elf::ElfException(JvNewStringUTF("Elf library version out of date"));
	}
	errno = 0;	
	::Elf* new_elf = ::elf_begin (fd, (Elf_Cmd) command, (::Elf*) 0);
	
	if(errno != 0 || !new_elf) {
		::close(fd);
		throw new lib::elf::ElfException(JvNewStringUTF("Could not open Elf file"));
	}
	
	this->pointer = (jlong) new_elf;
}

jlong
lib::elf::Elf::elf_clone (jint command){
	return (jlong) ::elf_clone((::Elf*) this->pointer, (Elf_Cmd) command);
}

#if 0
void
lib::elf::Elf::elf_memory (jstring image, jlong size){
	int len = JvGetStringUTFLength (image);
	char imageName[len + 1];
	JvGetStringUTFRegion (image, 0, image->length (), imageName);

	this->pointer = (jlong) ::elf_memory(imageName, (size_t) size);
}
#endif
    
jint
lib::elf::Elf::elf_next (){
	return (jint) ::elf_next((::Elf*) this->pointer);
}

jint
lib::elf::Elf::elf_end()
{
  if (this->pointer)
    {
      jint val = ::elf_end((::Elf*) this->pointer);
      if (fd >= 0)
	::close(fd);
      this->pointer = 0;
      fd = -1;
      return val;
    }
  else
    {
      return 0;
    }
}



jlong
lib::elf::Elf::elf_update (jint command){
	return (jlong) ::elf_update((::Elf*) this->pointer, (Elf_Cmd) command);
}

jint
lib::elf::Elf::elf_kind (){
	return ::elf_kind((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_getbase (){
	return ::elf_getbase((::Elf*) this->pointer);
}

jstring
lib::elf::Elf::elf_getident (){
	size_t length;
	char* ident = ::elf_getident((::Elf*) pointer, &length);
	return JvNewString((const jchar*) ident, length);
}

jstring
lib::elf::Elf::elf_get_last_error_msg (){
	const char *error = ::elf_errmsg(elf_errno());
	return JvNewStringLatin1(error, strlen(error));
}

jint 
lib::elf::Elf::elf_get_last_error_no (){
	return elf_errno();
}

void fillEHeader(lib::elf::ElfEHeader *header, GElf_Ehdr *ehdr){
	header->ident = JvNewByteArray(EI_NIDENT);
	jbyte *bytes = elements(header->ident);
	for(int i = 0; i < EI_NIDENT; i++)
		bytes[i] = (jbyte) ehdr->e_ident[i];
	
	header->type = (jint) ehdr->e_type;
	header->machine = (jint) ehdr->e_machine;
	header->version = (jint) ehdr->e_version;
	header->entry = (jlong) ehdr->e_entry;
	header->phoff = (jlong) ehdr->e_phoff;
	header->shoff = (jlong) ehdr->e_shoff;
	header->flags = (jint) ehdr->e_flags;
	header->ehsize = (jint) ehdr->e_ehsize;
	header->phentsize = (jint) ehdr->e_phentsize;
	header->phnum = (jint) ehdr->e_phnum;
	header->shentsize = (jint) ehdr->e_shentsize;
	header->shnum = (jint) ehdr->e_shnum;
	header->shstrndx = (jint) ehdr->e_shstrndx;
}

lib::elf::ElfEHeader*
lib::elf::Elf::elf_getehdr(){
	GElf_Ehdr hdr;
	if(::gelf_getehdr((::Elf*) this->pointer, &hdr) == NULL)
		return NULL;
	
	lib::elf::ElfEHeader *header = new lib::elf::ElfEHeader(this);
	fillEHeader(header, &hdr);
	
	return header;
}

jint
lib::elf::Elf::elf_newehdr (){
	::Elf* elf = (::Elf*) this->pointer;
	return (jint) ::gelf_newehdr(elf, gelf_getclass(elf));
}

jint
lib::elf::Elf::elf_updatehdr(ElfEHeader *phdr) {
	::Elf* elf = (::Elf*) this->pointer;
	GElf_Ehdr hdr;

	if(::gelf_getehdr((::Elf*) this->pointer, &hdr) == NULL)
		return elf_get_last_error_no();

	jbyte *bytes = elements(phdr->ident);	
	for(int i = 0; i < EI_NIDENT; i++)
		hdr.e_ident[i] = (jbyte) bytes[i];
	
	hdr.e_type = (int) phdr->type;
	hdr.e_machine = (int) phdr->machine;
	hdr.e_version = (int) phdr->version;
	hdr.e_entry = (long) phdr->entry;
	hdr.e_phoff = (long) phdr->phoff;
	hdr.e_shoff = (long) phdr->shoff;
	hdr.e_flags = (int) phdr->flags;
	hdr.e_ehsize = (int) phdr->ehsize;
	hdr.e_phentsize = (int) phdr->phentsize;
	hdr.e_phnum = (int) phdr->phnum;
	hdr.e_shentsize = (int) phdr->shentsize;
	hdr.e_shnum = (int) phdr->shnum;
	hdr.e_shstrndx = (int) phdr->shstrndx;

	return gelf_update_ehdr (elf,&hdr);
}

jint
lib::elf::Elf::elf_get_version() {
	return EV_CURRENT;
}

void fillPHeader(lib::elf::ElfPHeader *header, GElf_Phdr *phdr){
	header->type = (jint) phdr->p_type;
	header->flags = (jint) phdr->p_flags;
	header->offset = (jlong) phdr->p_offset;
	header->vaddr = (jlong) phdr->p_vaddr;
	header->paddr = (jlong) phdr->p_paddr;
	header->filesz = (jlong) phdr->p_filesz;
	header->memsz = (jlong) phdr->p_memsz;
	header->align = (jlong) phdr->p_align;
}

lib::elf::ElfPHeader*
lib::elf::Elf::elf_getphdr (jint index){
	GElf_Phdr phdr;
	if(::gelf_getphdr((::Elf*) this->pointer, index, &phdr) == NULL)
		return NULL;
		
	lib::elf::ElfPHeader *header = new lib::elf::ElfPHeader(this);
	fillPHeader(header, &phdr);
	
	return header;
}

jint
lib::elf::Elf::elf_updatephdr(jint index, lib::elf::ElfPHeader *phdr) {
	GElf_Phdr header;
	if (::gelf_getphdr((::Elf*) this->pointer, index, &header) == NULL)
		return -1;
	::Elf* elf = (::Elf*) this->pointer;

	header.p_type = (jint) phdr->type;
        header.p_flags = (jint) phdr->flags;
        header.p_offset = (jlong) phdr->offset;
        header.p_vaddr = (jlong) phdr->vaddr;
        header.p_paddr = (jlong) phdr->paddr;
        header.p_filesz = (jlong) phdr->filesz;
        header.p_memsz = (jlong) phdr->memsz;
        header.p_align = (jlong) phdr->align;
	
	return gelf_update_phdr (elf, index, &header);
} 


jint
lib::elf::Elf::elf_newphdr (jlong cnt){
	return (jint) ::gelf_newphdr((::Elf*) this->pointer, (size_t) cnt);
}

jlong
lib::elf::Elf::elf_offscn (jlong offset){
	return (jlong) gelf_offscn((::Elf*) this->pointer, (Elf32_Off) offset);
}

jlong
lib::elf::Elf::elf_getscn (jlong index){
	return (jlong) ::elf_getscn((::Elf*) this->pointer, (size_t) index);
}

jlong
lib::elf::Elf::elf_nextscn (jlong section){
	return (jlong) ::elf_nextscn((::Elf*) this->pointer, (Elf_Scn*) section);
}

jlong
lib::elf::Elf::elf_newscn (){
	return (jlong) ::elf_newscn((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_getshnum (){
	size_t count;
	/* XXX: What to do if this fails */
	::elf_getshnum((::Elf*) this->pointer, &count);
	return count;
}

jlong
lib::elf::Elf::elf_getshstrndx (){
	size_t index;
	/* XXX: What to do if this fails */
	::elf_getshstrndx((::Elf*) this->pointer, &index);
	return index;
}

jint
lib::elf::Elf::elf_flagelf (jint command, jint flags){
	return ::elf_flagelf((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::elf::Elf::elf_flagehdr (jint command, jint flags){
	return ::elf_flagehdr((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::elf::Elf::elf_flagphdr (jint command, jint flags){
	return ::elf_flagphdr((::Elf*) this->pointer, (Elf_Cmd) command, flags);
}

jstring
lib::elf::Elf::elf_strptr (jlong index, jlong offset){
	char* strptr = ::elf_strptr((::Elf*) this->pointer, (size_t) index, (size_t) offset);
	return JvNewString((const jchar*) strptr, strlen(strptr));
}

lib::elf::ElfArchiveHeader*
lib::elf::Elf::elf_getarhdr (){
	Elf_Arhdr *hdr = ::elf_getarhdr((::Elf*) this->pointer);
	
	if(hdr == NULL)
		return NULL;
		
	lib::elf::ElfArchiveHeader *header = new lib::elf::ElfArchiveHeader(this);
	
	header->name = JvNewString((const jchar*)hdr->ar_name, strlen(hdr->ar_name));
	header->date = (jlong) hdr->ar_date;
	header->uid = (jint) hdr->ar_uid;
	header->gid = (jint) hdr->ar_gid;
	header->mode = (jint) hdr->ar_mode;
	header->size = (jlong) hdr->ar_size;
	header->rawname = JvNewString((const jchar*) hdr->ar_rawname, strlen(hdr->ar_rawname));
	
	return header;
}

jlong
lib::elf::Elf::elf_getaroff (){
	return ::elf_getaroff((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_rand (jint offset){
	return ::elf_rand((::Elf*) this->pointer, (size_t) offset);
}

jlong
lib::elf::Elf::elf_getarsym (jlong ptr){
	return (jlong) ::elf_getarsym((::Elf*) this->pointer, (size_t*)(long) &ptr);
}

jint
lib::elf::Elf::elf_cntl (jint command){
	return ::elf_cntl((::Elf*) this->pointer, (Elf_Cmd) command);
}

jstring
lib::elf::Elf::elf_rawfile (jlong ptr){
	char* file = ::elf_rawfile((::Elf*) pointer, (size_t*)(long) &ptr);
	return JvNewString((const jchar*) file, strlen(file));
}

#ifdef __cplusplus
}
#endif
