#include <gcj/cni.h>
#include <libelf.h>

#include "lib/elf/ElfSection.h"

#ifdef __cplusplus
extern "C"
{
#endif

jlong
lib::elf::ElfSection::elf_ndxscn (){
	return ::elf_ndxscn((Elf_Scn*) this->pointer);
}

jlong
lib::elf::ElfSection::elf_getshdr (){
	if(this->is32bit)
		return (jlong) elf32_getshdr((Elf_Scn*) this->pointer);
	else
		return (jlong) elf64_getshdr((Elf_Scn*) this->pointer);
}

jint
lib::elf::ElfSection::elf_flagscn (jint command, jint flags){
	return ::elf_flagscn((Elf_Scn*) this->pointer, (Elf_Cmd) command, flags);
}

jint
lib::elf::ElfSection::elf_flagshdr (jint command, jint flags){
	return ::elf_flagshdr((Elf_Scn*) this->pointer, (Elf_Cmd) command, flags);
}

jlong
lib::elf::ElfSection::elf_getdata (){
	return (jlong) ::elf_getdata((Elf_Scn*) this->pointer, (Elf_Data*) NULL);
}

jlong
lib::elf::ElfSection::elf_rawdata (){
	return (jlong) ::elf_rawdata((Elf_Scn*) this->pointer, (Elf_Data*) NULL);
}

jlong
lib::elf::ElfSection::elf_newdata (){
	return (jlong) ::elf_newdata((Elf_Scn*) this->pointer);
}

#ifdef __cplusplus
}
#endif
