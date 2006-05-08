#include <libelf.h>
#include <gcj/cni.h>
#include <stdlib.h>

#include "lib/elf/ElfArchiveHeader.h"

#ifdef __cplusplus
extern "C"
{
#endif

void
lib::elf::ElfArchiveHeader::elf_ar_new (){
	this->pointer = (jlong) malloc(sizeof(Elf_Arhdr));
}

void
lib::elf::ElfArchiveHeader::elf_ar_finalize (){
	free((Elf_Arhdr*) this->pointer);
}

jstring
lib::elf::ElfArchiveHeader::elf_ar_get_name (){
	char* name = ((Elf_Arhdr*) this->pointer)->ar_name;
	return JvNewString((const jchar*) name, strlen(name));
}

jlong
lib::elf::ElfArchiveHeader::elf_ar_get_date (){
	return ((Elf_Arhdr*) this->pointer)->ar_date;
}

jint
lib::elf::ElfArchiveHeader::elf_ar_get_uid (){
	return ((Elf_Arhdr*) this->pointer)->ar_uid;
}

jint
lib::elf::ElfArchiveHeader::elf_ar_get_gid (){
	return ((Elf_Arhdr*) this->pointer)->ar_gid;
}

jint
lib::elf::ElfArchiveHeader::elf_ar_get_mode (){
	return ((Elf_Arhdr*) this->pointer)->ar_mode;
}

jint
lib::elf::ElfArchiveHeader::elf_ar_get_size (){
	return ((Elf_Arhdr*) this->pointer)->ar_size;
}

jstring
lib::elf::ElfArchiveHeader::elf_ar_get_raw_name (){
	char* rawname = ((Elf_Arhdr*) pointer)->ar_rawname;
	return JvNewString((const jchar*) rawname, strlen(rawname));
}

#ifdef __cplusplus
}
#endif
