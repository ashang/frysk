#include <libelf.h>
#include <stdlib.h>
#include <inttypes.h>
#include <gcj/cni.h>

#include "lib/elf/ElfData.h"

#ifdef __cplusplus
extern "C"
{
#endif

void
lib::elf::ElfData::elf_data_finalize (){
	free((Elf_Data*) this->pointer);
}

jbyte
lib::elf::ElfData::elf_data_get_byte (jlong offset){
	uint8_t* data = (uint8_t*) ((Elf_Data*) this->pointer)->d_buf;	
	size_t size = ((Elf_Data*) this->pointer)->d_size;
	if(offset > size)
		return -1;
	
	return (jbyte) data[offset];
}

jint
lib::elf::ElfData::elf_data_get_type (){
	return (int) ((Elf_Data*) this->pointer)->d_type;
}

jint
lib::elf::ElfData::elf_data_get_version (){
	return ((Elf_Data*) this->pointer)->d_version;
}

jlong
lib::elf::ElfData::elf_data_get_size (){
	return ((Elf_Data*) this->pointer)->d_size;
}

jint
lib::elf::ElfData::elf_data_get_off (){
	return ((Elf_Data*) this->pointer)->d_off;
}

jlong
lib::elf::ElfData::elf_data_get_align (){
	return ((Elf_Data*) this->pointer)->d_align;
}

jint
lib::elf::ElfData::elf_flagdata (jint command, jint flags){
	return ::elf_flagdata((Elf_Data*) this->pointer, (Elf_Cmd) command, flags);
}

jlong
lib::elf::ElfData::elf_xlatetom (jint encode){
	/* not sure how to deal with this one yet */
	return 0;
}
jlong
lib::elf::ElfData::elf_xlatetof (jint encode){
	/* Again, unsure of this method */
	return 0;
}

#ifdef __cplusplus
}
#endif
