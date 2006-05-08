#include <gcj/cni.h>
#include <libelf.h>

#include "lib/elf/ElfSectionHeader.h"
#include "lib/elf/ElfSectionHeader64.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong 
lib::elf::ElfSectionHeader64::get_sh_name (){
	return ((Elf64_Shdr*) this->getPointer())->sh_name;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_type (){
	return ((Elf64_Shdr*) this->getPointer())->sh_type;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_flags (){
	return ((Elf64_Shdr*) this->getPointer())->sh_flags;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_addr (){
	return ((Elf64_Shdr*) this->getPointer())->sh_addr;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_offset (){
	return ((Elf64_Shdr*) this->getPointer())->sh_offset;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_size (){
	return ((Elf64_Shdr*) this->getPointer())->sh_size;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_link (){
	return ((Elf64_Shdr*) this->getPointer())->sh_link;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_info (){
	return ((Elf64_Shdr*) this->getPointer())->sh_info;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_addralign (){
	return ((Elf64_Shdr*) this->getPointer())->sh_addralign;
}

jlong
lib::elf::ElfSectionHeader64::get_sh_entsize (){
	return ((Elf64_Shdr*) this->getPointer())->sh_entsize;
}


#ifdef __cplusplus
}
#endif
