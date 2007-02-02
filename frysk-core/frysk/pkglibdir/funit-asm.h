// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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
// License in all respects for all o;f the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.


// This file provides a collection macros that map a very simple
// load-store virtual machine onto a concrete ISA.  See the end of
// this file for more details.

#ifdef __i386__

	.text
	.global main

//XXX: Replace with macros at end of file.
#define NO_OP nop
#define MOVE mov
#define INTERRUPT int
#define COMPARE cmp
#define RETURN ret
#define PUSH push
#define POP pop
#define JUMP jmp
#define JUMP_IF_NOT_EQUAL jne
#define CALL(FUNC) call FUNC
#define STORE_WORD(WORD, REG) movl $WORD, (REG)
#define STACK_BASE_POINTER %ebp // stack base pointer
#define GEN_REG_1 %eax // accumulator register
#define GEN_REG_2 %ebx // base register
#define GEN_REG_3 %ecx // counter register
#define GEN_REG_4 %edx // data register
#define PROLOGUE \
		pushl %ebp ;\
		movl %esp, %ebp 
		
#define EPILOGUE \
		popl	%ebp ;\
		ret		

#elif defined __x86_64__

	.text
	.global main

//XXX: Replace with macros at end of file.
#define NO_OP nop
#define MOVE mov
#define INTERRUPT int
#define COMPARE cmp
#define RETURN ret
#define PUSH push
#define POP pop
#define JUMP jmp
#define JUMP_IF_NOT_EQUAL jne
#define CALL(FUNC) call FUNC
#define STORE_WORD(WORD, REG) movl $WORD, (REG)
#define GEN_REG_1 %rax
#define GEN_REG_2 %rdi
#define GEN_REG_3 %rsi
#define GEN_REG_4 %rdx

#elif defined __powerpc__

.text                       # section declaration - begin code
	.global  main
main:

#elif defined __powerpc64__

	.section        ".opd","aw"
	.global main
	.align 3
main:
	.quad   ._main,.TOC.@tocbase,0
	.text                       # section declaration - begin code
	.global  ._main
._main:

//XXX: Replace with macros at end of file.
#define NO_OP nop
#define MOVE mov
#define INTERRUPT int
#define COMPARE cmp
#define RETURN ret
#define PUSH push
#define POP pop
#define JUMP jmp
#define JUMP_IF_NOT_EQUAL jne
#define CALL(FUNC) call FUNC
#define STORE_WORD(WORD, REG) movl $WORD, (REG)
#define GEN_REG_1 %gpr0
#define GEN_REG_2 %gpr3
#define GEN_REG_3 %gpr4
#define GEN_REG_4 %gpr5

#endif


// This virtual machine is based on a simple load-store architecture.
// A load-store architecture must first load each value into a
// register before performing any operation.  For instance, to
// increment a memory location, this sequence is required:

// SET(REG1, foo)       // Set REG1 to address of FOO
// LOAD(REG2, REG1)     // Load REG2 with contents of memory at REG1
// ADDC(REG3, 1)        // Increment
// STORE(REG2, REG1)    // Store again

// stack, memory and four general purpose registers.  One, the
// ACC_REG, is used to return function results.  In addition, the
// virtual machine has a stack-base and stack-top pointer demarking
// the boundaries of the current stack frame, and a compare flag.

#ifndef SP_REG
#define SP_REG Register containing pointer to of top-of-stack
#endif
#ifndef BP_REG
#define BP_REG Register containing pointer to base of current stack frame
#endif
#ifndef REG1
#define REG1 General purpose register, also used for function return
#endif
#ifndef REG2
#define REG2 General purpose register
#endif
#ifndef REG3
#define REG3 General purpose register
#endif
#ifndef REG4
#define REG4 General purpose register
#endif

// The virtual machine implements the following instructions.  All
// instructions are limited to two operands.

#ifndef NO_OP
#define NO_OP do nothing for exactly one instruction
#endif

#ifndef ENTER
#define ENTER creates the stack frame on function entry, allocates no space on the stack
#endif
#ifndef EXIT
#define EXIT destroys the stack frame and exits the function
#endif

#ifndef SET
#define SET(REGISTER, CONSTANT) sets REGISTER to CONSTANT
#endif
#ifndef LOAD
#define LOAD(DEST_REG,BASE_REG) Load register DEST_REG with value at addres BASE_REG+0
#endif
#ifndef STORE
#define STORE(SOURCE_REG,BASE_REG) Store register SOURCE_REG into address at BASE_REG+0
#endif
#ifndef MOVE
#define MOVE(DEST_REG, SOURCE_REG) move SOURCE_REG to DEST_REG
#endif

#ifndef ADD
#define ADD(DEST_REG, SOURCE_REG) adds SOURCE_REG to DEST_REG
#endif
#ifndef ADDC
#define ADDC(DEST_REG, CONSTANT) add CONSTANT to DEST_REG
#endif
#ifndef SUB
#define SUB(DEST_REG, SOURCE_REG) subtracts SOURCE_REG from DEST_REG
#endif

#ifndef COMPARE
#define COMPARE(LHS_REG,RHS_REG) Set compare flags according to LHS_REG-RHS_REG
#endif
#ifndef JUMP
#define JUMP(LABEL) Jump to label using relative addressing
#endif
#ifndef JUMP_EQ
#define JUMP_EQ(LABEL) Jump if equals
#endif
#ifndef JUMP_NE
#define JUMP_NE(LABEL) Jump if not equals
#endif
#ifndef CALL
#define CALL(LABEL) Call the function LABEL
#endif

	   // Need something better?
#ifndef TRAP
#define TRAP(NUMBER) Perform system trap NUMBER
#endif
