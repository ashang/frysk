#include <gcj/cni.h>
#include <libelf.h>

#include "lib/elf/ElfPHeader64.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong
lib::elf::ElfPHeader64::get_p_type (){
	return ((Elf64_Phdr*) this->getPointer())->p_type;
}

jlong
lib::elf::ElfPHeader64::get_p_offset (){
	return ((Elf64_Phdr*) this->getPointer())->p_offset;
}

jlong
lib::elf::ElfPHeader64::get_p_vaddr (){
	return ((Elf64_Phdr*) this->getPointer())->p_vaddr;
}

jlong
lib::elf::ElfPHeader64::get_p_paddr (){
	return ((Elf64_Phdr*) this->getPointer())->p_paddr;
}

jlong
lib::elf::ElfPHeader64::get_p_filesz (){
	return ((Elf64_Phdr*) this->getPointer())->p_filesz;
}

jlong
lib::elf::ElfPHeader64::get_p_memsz (){
	return ((Elf64_Phdr*) this->getPointer())->p_memsz;
}

jlong
lib::elf::ElfPHeader64::get_p_align (){
		return ((Elf64_Phdr*) this->getPointer())->p_align;
}

jlong
lib::elf::ElfPHeader64::get_p_flags (){
		return ((Elf64_Phdr*) this->getPointer())->p_flags;
}

#ifdef __cplusplus
}
#endif
