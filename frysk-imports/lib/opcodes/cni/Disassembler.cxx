// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.
#include <stdio.h>
#include <stdlib.h>
#include <bfd.h>
#include <dis-asm.h>
#include <errno.h>
#include <string.h>
#include <stdarg.h>
#include <alloca.h>
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

// reads number of bytes from the byte buffer
int read_from_byte_buffer(bfd_vma memaddr, bfd_byte* myadd, unsigned int length,
		     struct disassemble_info *info);

// throws an OpcodesException
void error_func(int status, bfd_vma memaddr, struct disassemble_info *info);

// Prints the address when errors occur
void print_addr(bfd_vma addr, struct disassemble_info *info);

// saves the instruction to the java class
int save_instruction(void* data, const char *str, ...);

void
lib::opcodes::Disassembler::disassemble(jlong address, jlong instructions){

	disassemble_info disasm_info;
	int (*disasm_func) (bfd_vma, disassemble_info*);

	::init_disassemble_info(&disasm_info, (void*) this, save_instruction);

	disasm_info.flavour = bfd_target_unknown_flavour;
#ifdef __x86_64__
	disasm_info.arch = bfd_arch_i386;
	disasm_info.mach = bfd_mach_x86_64;
	disasm_func = &(::print_insn_i386_intel);
#elif defined(__i386__)
	disasm_info.arch = bfd_arch_i386;
	disasm_info.mach = bfd_mach_i386_i386;
	disasm_func = &(::print_insn_i386_intel);
#elif defined(__powerpc64__)
	disasm_info.arch = bfd_arch_powerpc;
	disasm_info.mach = bfd_mach_ppc64;
	disasm_func = &(::print_insn_big_powerpc);
#elif defined(__powerpc__)
	disasm_info.arch = bfd_arch_powerpc;
	disasm_info.mach = bfd_mach_ppc;
	disasm_func = &(::print_insn_big_powerpc);
#elif defined(__ia64__)
	disasm_info.arch = bfd_arch_ia64;
	disasm_info.mach = bfd_mach_ia64_elf64; // TODO: which mach? elf32 or elf64?
	disasm_func =&(::print_insn_ia64);
#elif defined(__s390__)
	disasm_info.arch = bfd_arch_s390;
	disasm_info.mach = bfd_mach_s390_31;
	disasm_func = &(::print_insn_s390);
#elif defined(__s390x__)
	disasm_info.arch = bfd_arch_s390;
	disasm_info.mach = bfd_mach_s390_64;
	disasm_func = &(::print_insn_s390);
#endif
 
	if(!disasm_func)
		throw new lib::opcodes::OpcodesException(JvNewStringUTF("Error: Unsupported architechture"));
 
	disasm_info.read_memory_func = read_from_byte_buffer;
	disasm_info.memory_error_func = error_func;
	disasm_info.print_address_func = print_addr;

	bfd_vma current_address = (bfd_vma) address;
	for(int i = 0; i < instructions; i++){
		this->setCurrentAddress(current_address);
		current_address += disasm_func(current_address, &disasm_info);
		this->moveToNext();
	}
}

/*
 * Instead of copying memory from memaddr to myadd, we get the section
 * starting at memaddr in the ByteBuffer.
 */
int read_from_byte_buffer(bfd_vma memaddr, bfd_byte* myadd, unsigned int length,
		     struct disassemble_info *info){
	lib::opcodes::Disassembler *obj = (lib::opcodes::Disassembler*) info->stream;
	inua::eio::ByteBuffer *buffer = obj->buffer;
	
	char tmp[length];
	for(unsigned int i = 0; i < length; i++){
		long offset = ((long) memaddr) + i;
		tmp[i] = (char) buffer->getByte(offset);
	}
	
	memcpy((void*) myadd, (void*) tmp, length);
	
	return 0;
}

/*
 * If something breaks, throw an exception
 */
void error_func(int status, bfd_vma memaddr, struct disassemble_info *info){
	throw new lib::opcodes::OpcodesException(
		JvNewStringUTF("Error occured while disassembling."), (jint) status, (jlong) memaddr
	);
}

void print_addr(bfd_vma addr, struct disassemble_info *info){}

/*
 * When we're asked to print a statement, store it on the java side instead
 */
int save_instruction(void* disassembler, const char *args, ...){
	lib::opcodes::Disassembler* obj = (lib::opcodes::Disassembler*) disassembler;
	
	va_list ap;
	::va_start(ap, args);
	char * mystr;
	if(::vasprintf(&mystr, args, ap) > 0){
		obj->setCurrentInstruction(JvNewStringUTF(mystr));
		::free(mystr);
	}
	else{
  		throw new lib::opcodes::OpcodesException(
  			JvNewStringUTF("Could not parse variable argument list")
		);
	}
	::va_end(ap);
	
	int len = strlen(mystr);
	
	return len;
}
