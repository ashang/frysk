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

#ifndef FRYSK_ASM_H
#define FRYSK_ASM_H

// This file provides a collection macros that map a very simple
// load-store instruction set archiecture onto a concrete ISA.

// The key here is generality through simplicity, and hence a
// load-store ISA was chosen.  Please read the notes at the end of
// this file before making changes and additions.

#ifdef __i386__

	.text
	.global main

#define REG1 %eax
#define REG2 %ebx
#define REG3 %ecx
#define REG4 %edx

#define NO_OP nop
#define ENTER pushl %ebp ; movl %esp, %ebp 
#define EXIT popl %ebp ; ret
#define JUMP(LABEL) jmp LABEL
#define JUMP_NE(LABEL) jne LABEL
#define CALL(FUNC) call FUNC
#define LOAD_IMMED(DEST_REG,CONST) mov $CONST, DEST_REG
#define STORE(SOURCE_REG,BASE_REG) movl SOURCE_REG, (BASE_REG)
#define COMPARE_IMMED(REG,CONST) cmp $CONST, REG
#define COMPARE(REG,CONST) cmpl $CONST, (REG)
#define ENTER_MAIN(ARGC_REG, ARGV_REG) leal 4(%esp), ARGC_REG ; andl $-16, %esp; pushl -4(ARGC_REG)	

//XXX: Replace with macros at end of file.

#elif defined __x86_64__

	.text
	.global main

#define REG1 %rax
#define REG2 %rdi
#define REG3 %rsi
#define REG4 %rdx

#define NO_OP nop
#define ENTER
#define EXIT ret
#define JUMP(LABEL) jmp LABEL
#define JUMP_NE(LABEL) jne LABEL
#define CALL(FUNC) call FUNC
#define LOAD_IMMED(DEST_REG,CONST) mov $CONST, DEST_REG
#define STORE(SOURCE_REG,BASE_REG) mov SOURCE_REG, (BASE_REG)
#define COMPARE_IMMED(REG,CONST) cmp $CONST, REG

//XXX: Need the following to be defined in order to compile. See Bug #3968
#define ENTER_MAIN(ARGC_REG, ARGV_REG)
#define COMPARE(REG,CONST)

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

#define REG1 %gpr0
#define REG2 %gpr3
#define REG3 %gpr4
#define REG4 %gpr5

#define NO_OP nop
#define CALL(FUNC) brlr FUNC

#endif

// These macros define a very simple register-sized two-operand
// load-store instruction set architecture.

// REGISTER-SIZED: Registers have an implicit size.  Unless othewize
// stated, all loads and stores are of that implicit size.

// TWO-OPERAND: All instructions involve at most two operands.  One is
// a register, the other is either a register, an immediate, or an
// implied address.

// LOAD-STORE: A load-store ISA restricts all arithmetic operations
// (add, subtract, compare, ...) to only registers and immediates.  To
// perform an arithmetic operation on memory memory it must first be
// loaded into a register.

// The ISA has four general purpose registers.  The calling convention
// assumes that they are all scratch; and that they are used to
// transfer parameters and the return value.

// The following general purpose registers are available:

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

// The following instructions are defined:

#ifndef NO_OP
#define NO_OP do nothing for exactly one instruction
#endif

#ifndef ENTER
#define ENTER creates the stack frame on function entry, allocates no scratch space on the stack, and moves no parameters into registers
#endif

#ifndef ENTER_MAIN
#define ENTER_MAIN (ARGC_REG, ARGV_REG) set ARGC_REG to hold ARGC, and ARGV_REG \
to hold ARGV.
#endif

#ifndef EXIT
#define EXIT destroys the stack frame and exits the function
#endif

#ifndef LOAD
#define LOAD(DEST_REG,BASE_REG) Perform a register sized load from BASE_REG into DEST_REG
#endif
#ifndef LOAD_IMMED
#define LOAD_IMMED(DEST_REG, IMMED) Sets REGISTER to IMMED
#endif
#ifndef LOAD_STACK
#define LOAD_STACK(DEST_REG, SLOT_NUM) Perform a register sized load from stack slot SLOT_NUM into DEST_REG
#endif

#ifndef STORE
#define STORE(SOURCE_REG,BASE_REG) Perform a register sized store of SOURCE_REG into address at BASE_REG
#endif
#ifndef STORE_STACK
#define STORE_STACK(SOURCE_REG, SLOT_NUM) Perform a register sized store of SOURCE_REG into stack slot SLOT_NUM
#endif

#ifndef MOVE
#define MOVE(DEST_REG, SOURCE_REG) move SOURCE_REG to DEST_REG
#endif

#ifndef ADD_IMMED
#define ADD_IMMED(DEST_REG, IMMED) add IMMED to DEST_REG
#endif
#ifndef ADD
#define ADD(DEST_REG, SOURCE_REG) adds SOURCE_REG to DEST_REG
#endif
#ifndef SUB
#define SUB(DEST_REG, SOURCE_REG) subtracts SOURCE_REG from DEST_REG
#endif

#ifndef COMPARE
#define COMPARE(LHS_REG,RHS_REG) Set compare flags according to LHS_REG-RHS_REG
#endif
#ifndef COMPARE_IMMED
#define COMPARE_IMMED(REG,IMMED) Set compare flags according to REG-IMMED
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
#define CALL(LABEL) Call the function LABEL passing no parameters
#endif

	   // XXX: Need something better?
#ifndef TRAP
#define TRAP(NUMBER) Perform system trap NUMBER
#endif

#endif
