#include <gcj/cni.h>
#include <stdlib.h>
#include <libelf.h>

#include "lib/elf/ElfArchiveSymbol.h"

#ifdef __cplusplus
extern "C"
{
#endif

void
lib::elf::ElfArchiveSymbol::elf_as_new (){
	this->pointer = (jlong) malloc(sizeof(Elf_Arsym));
}

void
lib::elf::ElfArchiveSymbol::elf_as_finalize (){
	free((Elf_Arsym*) this->pointer);
}

jstring
lib::elf::ElfArchiveSymbol::elf_as_get_name (){
	char* name = ((Elf_Arsym*) this->pointer)->as_name;
	return JvNewString((const jchar*) name, strlen(name));
}

jint
lib::elf::ElfArchiveSymbol::elf_as_get_offset (){
	return ((Elf_Arsym*) this->pointer)->as_off;
}

jlong
lib::elf::ElfArchiveSymbol::elf_as_get_hash (){
	return (jlong) ((Elf_Arsym*) this->pointer)->as_hash;
}

#ifdef __cplusplus
}
#endif
