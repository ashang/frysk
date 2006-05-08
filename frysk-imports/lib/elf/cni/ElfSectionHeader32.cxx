#include <gcj/cni.h>
#include <libelf.h>

#include "lib/elf/ElfSectionHeader.h"
#include "lib/elf/ElfSectionHeader32.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong 
lib::elf::ElfSectionHeader32::get_sh_name (){
	return ((Elf32_Shdr*) this->getPointer())->sh_name;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_type (){
	return ((Elf32_Shdr*) this->getPointer())->sh_type;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_flags (){
	return ((Elf32_Shdr*) this->getPointer())->sh_flags;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_addr (){
	return ((Elf32_Shdr*) this->getPointer())->sh_addr;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_offset (){
	return ((Elf32_Shdr*) this->getPointer())->sh_offset;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_size (){
	return ((Elf32_Shdr*) this->getPointer())->sh_size;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_link (){
	return ((Elf32_Shdr*) this->getPointer())->sh_link;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_info (){
	return ((Elf32_Shdr*) this->getPointer())->sh_info;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_addralign (){
	return ((Elf32_Shdr*) this->getPointer())->sh_addralign;
}

jlong
lib::elf::ElfSectionHeader32::get_sh_entsize (){
	return ((Elf32_Shdr*) this->getPointer())->sh_entsize;
}


#ifdef __cplusplus
}
#endif
