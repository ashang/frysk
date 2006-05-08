#include <gcj/cni.h>
#include <libelf.h>

#include "lib/elf/ElfPHeader.h"
#include "lib/elf/ElfPHeader32.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong
lib::elf::ElfPHeader32::get_p_type (){
	return ((Elf32_Phdr*) this->getPointer())->p_type;
}

jlong
lib::elf::ElfPHeader32::get_p_offset (){
	return ((Elf32_Phdr*) this->getPointer())->p_offset;
}

jlong
lib::elf::ElfPHeader32::get_p_vaddr (){
	return ((Elf32_Phdr*) this->getPointer())->p_vaddr;
}

jlong
lib::elf::ElfPHeader32::get_p_paddr (){
	return ((Elf32_Phdr*) this->getPointer())->p_paddr;
}

jlong
lib::elf::ElfPHeader32::get_p_filesz (){
	return ((Elf32_Phdr*) this->getPointer())->p_filesz;
}

jlong
lib::elf::ElfPHeader32::get_p_memsz (){
	return ((Elf32_Phdr*) this->getPointer())->p_memsz;
}

jlong
lib::elf::ElfPHeader32::get_p_align (){
		return ((Elf32_Phdr*) this->getPointer())->p_align;
}

jlong
lib::elf::ElfPHeader32::get_p_flags (){
		return ((Elf32_Phdr*) this->getPointer())->p_flags;
}

#ifdef __cplusplus
}
#endif
