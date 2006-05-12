#include <stdio.h>
#include <stdlib.h>
#include <bfd.h>
#include <dis-asm.h>
#include <errno.h>
#include <string.h>
#include <stdarg.h>
/*
 * This is an ugly hack. In /usr/lib/ansidecl.h (included by bfd.h) we have
 *       #define VOLATILE        volatile
 * This conflicts with /usr/include/c++/4.1.0/java/lang/reflect/Modifier.h:
 * 		 static const jint VOLATILE = 64L;
 * And makes cni cry
 */
#undef VOLATILE
#include <gcj/cni.h>

#include "lib/opcodes/Disassembler.h"
#include "lib/opcodes/Instruction.h"
#include "lib/opcodes/OpcodesException.h"
#include "inua/eio/ByteBuffer.h"

int my_read_mem_func(bfd_vma memaddr, bfd_byte* myadd, unsigned int length,
		     struct disassemble_info *info);

void error_func(int status, bfd_vma memaddr, struct disassemble_info *info);

void print_addr(bfd_vma addr, struct disassemble_info *info);

int my_print(void* data, const char *str, ...);

void
lib::opcodes::Disassembler::disassemble(jlong address, jlong instructions){

	disassemble_info disasm_info;

	::init_disassemble_info(&disasm_info, (void*) this, my_print);

	disasm_info.flavour = bfd_target_unknown_flavour;
	disasm_info.arch = bfd_arch_i386;
	disasm_info.mach = bfd_mach_i386_i386;
 
	disasm_info.read_memory_func = my_read_mem_func;
	disasm_info.memory_error_func = error_func;
	disasm_info.print_address_func = print_addr;

	int i;
	// Replace this with getting the information from the ByteBuffer
	bfd_vma current_address = (bfd_vma) address;
	for(i = 0; i < instructions; i++){
		this->setCurrentAddress(current_address);
		current_address += ::print_insn_i386_intel(current_address, &disasm_info);
		this->moveToNext();
	}
}

int my_read_mem_func(bfd_vma memaddr, bfd_byte* myadd, unsigned int length,
		     struct disassemble_info *info){
	lib::opcodes::Disassembler *obj = (lib::opcodes::Disassembler*) info->stream;
	inua::eio::ByteBuffer *buffer = obj->buffer;
	
	char* tmp = (char*) malloc(length);
	for(unsigned int i = 0; i < length; i++){
		long offset = ((long) memaddr) + i;
		tmp[i] = (char) buffer->getByte(offset);
	}
	
	memcpy((void*) memaddr, (void*) tmp, length);
	free(tmp);
	
	return 0;
}


void error_func(int status, bfd_vma memaddr, struct disassemble_info *info){
	throw new lib::opcodes::OpcodesException(
		JvNewString((const jchar*) "Error occured while disassembling.", 
					strlen("Error occured while disassembling.")), (jint) status, (jlong) memaddr
		);
}

void print_addr(bfd_vma addr, struct disassemble_info *info){
	
}

int my_print(void* disassembler, const char *args, ...){
	lib::opcodes::Disassembler* obj = (lib::opcodes::Disassembler*) disassembler;
	
	va_list ap;
	::va_start(ap, args);
	char * mystr;
	if(::vasprintf(&mystr, args, ap) > 0){
		obj->setCurrentInstruction(JvNewString((const jchar*) mystr, strlen(mystr)));
		::free(mystr);
	}
	else{
  		throw new lib::opcodes::OpcodesException(
  			JvNewString((const jchar*) "Could not parse variable argument list",
  						strlen("Could not parse variable argument list"))
  			);
	}
	::va_end(ap);
	
	int len = strlen(mystr);
	free(mystr);
	
	return len;
}
