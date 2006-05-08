#include <gcj/cni.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <string.h>
#include <libelf.h>

#include "lib/elf/ElfEHeader.h"
#include "lib/elf/ElfEHeader32.h"

#ifdef __cplusplus
extern "C"
{
#endif

jstring
lib::elf::ElfEHeader32::get_e_ident (){
	char* ident = (char*) ((Elf32_Ehdr*) this->getPointer())->e_ident;
	return JvNewString((const jchar*) ident, strlen(ident));
}

jint
lib::elf::ElfEHeader32::get_e_type (){
	return ((Elf32_Ehdr*) this->getPointer())->e_type;
}

jint
lib::elf::ElfEHeader32::get_e_machine (){
	return ((Elf32_Ehdr*) this->getPointer())->e_machine;
}

jlong
lib::elf::ElfEHeader32::get_e_version (){
	return ((Elf32_Ehdr*) this->getPointer())->e_version;
}

jlong
lib::elf::ElfEHeader32::get_e_entry (){
	return ((Elf32_Ehdr*) this->getPointer())->e_entry;
}

jlong
lib::elf::ElfEHeader32::get_e_phoff (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phoff;
}

jlong
lib::elf::ElfEHeader32::get_e_shoff (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shoff;
}

jlong
lib::elf::ElfEHeader32::get_e_flags (){
	return ((Elf32_Ehdr*) this->getPointer())->e_flags;
}

jint
lib::elf::ElfEHeader32::get_e_ehsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_ehsize;
}

jint
lib::elf::ElfEHeader32::get_e_phentsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phentsize;
}

jint
lib::elf::ElfEHeader32::get_e_phnum (){
	return ((Elf32_Ehdr*) this->getPointer())->e_phnum;
}

jint
lib::elf::ElfEHeader32::get_e_shentsize (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shentsize;
}

jint
lib::elf::ElfEHeader32::get_e_shnum (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shnum;
}

jint
lib::elf::ElfEHeader32::get_e_shstrndx (){
	return ((Elf32_Ehdr*) this->getPointer())->e_shstrndx;
}

#ifdef __cplusplus
}
#endif
