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
#include <libelf.h>
#include <gcj/cni.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "lib/elf/Elf.h"

#ifdef __cplusplus
extern "C"
{
#endif

void
lib::elf::Elf::elf_begin (jstring file, jint command){
	int len = JvGetStringUTFLength (file);
	char *fileName = (char *) malloc (len + 1);
	JvGetStringUTFRegion (file, 0, file->length (), fileName);

	int fd = open (fileName, O_RDONLY);
	::Elf* new_elf = ::elf_begin (fd, (Elf_Cmd) command, (::Elf*) 0);
	
	// Do a quick check for 32/64 bitness
	if(elf32_getehdr(new_elf) != 0)
		this->is32bit = true;
	else
		this->is32bit = false;
	
	this->pointer = (jlong) new_elf;
}

void
lib::elf::Elf::elf_clone (jlong pointer2, jint command){
	this->pointer = (jlong) ::elf_clone((::Elf*) pointer2, (Elf_Cmd) command);
}

void
lib::elf::Elf::elf_memory (jstring image, jlong size){
	int len = JvGetStringUTFLength (image);
	char *imageName = (char *) alloca (len + 1);
	JvGetStringUTFRegion (image, 0, image->length (), imageName);

	this->pointer = (jlong) ::elf_memory(imageName, (size_t) size);
}

jint
lib::elf::Elf::elf_next (){
	return (jint) ::elf_next((::Elf*) this->pointer);
}

jint
lib::elf::Elf::elf_end(){
	return ::elf_end((::Elf*) this->pointer);
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
lib::elf::Elf::elf_getident (jlong ptr){
	char* ident = ::elf_getident((::Elf*) pointer, (size_t*) &ptr);
	return JvNewString((const jchar*) ident, strlen(ident));
}

jlong
lib::elf::Elf::elf_getehdr(){
	if(this->is32bit)
		return (jlong) ::elf32_getehdr((::Elf*) this->pointer);
	else
		return (jlong) ::elf64_getehdr((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_newehdr (){
	if(this->is32bit)
		return (jlong) ::elf32_newehdr((::Elf*) this->pointer);
	else
		return (jlong) ::elf64_newehdr((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_getphdr (){
	if(this->is32bit)
		return (jlong) ::elf32_getphdr((::Elf*) this->pointer);
	else
		return (jlong) ::elf64_getphdr((::Elf*) this->pointer);
}

jlong
lib::elf::Elf::elf_newphdr (jlong cnt){
	if(this->is32bit)
		return (jlong) ::elf32_newphdr((::Elf*) this->pointer, (size_t) cnt);
	else
		return (jlong) ::elf64_newphdr((::Elf*) this->pointer, (size_t) cnt);
}

jlong
lib::elf::Elf::elf_offscn (jlong offset){
	if(this->is32bit)
		return (jlong) ::elf32_offscn((::Elf*) this->pointer, (Elf32_Off) offset);
	else
		return (jlong) ::elf64_offscn((::Elf*) this->pointer, (Elf64_Off) offset);
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

jint
lib::elf::Elf::elf_getshnum (jlong dst){
	return ::elf_getshnum((::Elf*) this->pointer, (size_t*) &dst);
}

jint
lib::elf::Elf::elf_getshstrndx (jlong dst){
	return ::elf_getshstrndx((::Elf*) this->pointer, (size_t*) &dst);
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

jlong
lib::elf::Elf::elf_getarhdr (){
	return (jlong) ::elf_getarhdr((::Elf*) this->pointer);
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
	return (jlong) ::elf_getarsym((::Elf*) this->pointer, (size_t*) &ptr);
}

jint
lib::elf::Elf::elf_cntl (jint command){
	return ::elf_cntl((::Elf*) this->pointer, (Elf_Cmd) command);
}

jstring
lib::elf::Elf::elf_rawfile (jlong ptr){
	char* file = ::elf_rawfile((::Elf*) pointer, (size_t*) &ptr);
	return JvNewString((const jchar*) file, strlen(file));
}

#ifdef __cplusplus
}
#endif
